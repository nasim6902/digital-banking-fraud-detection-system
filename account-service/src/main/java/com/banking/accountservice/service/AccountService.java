package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.exception.DuplicateResourceException;
import com.banking.accountservice.exception.InsufficientBalanceException;
import com.banking.accountservice.exception.InvalidAccountStateException;
import com.banking.accountservice.exception.ResourceNotFoundException;
import com.banking.accountservice.model.Account;
import com.banking.accountservice.model.AccountStatus;
import com.banking.accountservice.model.AccountType;
import com.banking.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private static final SecureRandom secureRandom = new SecureRandom();

    private static final String ACCOUNT_CREATED_TOPIC = "account.created";
    private final KafkaTemplate<String, Object> kafkaTemplate;


    @Transactional
    public AccountResponse createAccount(
            CreateAccountRequest request,
            String userId,
            String authenticatedEmail) {

        if (!request.getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new AccessDeniedException(
                    "Request email must match authenticated user email");
        }

        log.info("Creating account for user: {}", userId);

        if (accountRepository.existsByEmail(authenticatedEmail)) {
            throw new DuplicateResourceException(
                    "Account already exists for email: " + authenticatedEmail);
        }

        Account account = new Account();
        account.setOwnerUserId(userId);
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(authenticatedEmail);
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getInitialDeposit());
        account.setAccountNumber(generateAccountNumber());

        account.setDailyTransactionLimit(
                request.getAccountType() == AccountType.SAVINGS
                        ? new BigDecimal("100000")
                        : new BigDecimal("500000")
        );

        Account saved = accountRepository.saveAndFlush(account);
        log.info("Account created: {}", saved.getAccountNumber());

        Map<String, Object> acEvent = new HashMap<>();
        acEvent.put("accountId", saved.getId());
        acEvent.put("userId", saved.getOwnerUserId());
        acEvent.put("accountNumber", saved.getAccountNumber());
        acEvent.put("accountHolderName", saved.getAccountHolderName());
        acEvent.put("email", saved.getEmail());
        acEvent.put("phone", saved.getPhone());
        acEvent.put("accountType", saved.getAccountType());
        acEvent.put("status", saved.getStatus());
        acEvent.put("balance", saved.getBalance());
        acEvent.put("dailyTransactionLimit", saved.getDailyTransactionLimit());
        acEvent.put("createdAt", saved.getCreatedAt());

        kafkaTemplate.send(ACCOUNT_CREATED_TOPIC, saved.getEmail(), acEvent);

        return mapToResponse(saved);
    }


    public AccountResponse getAccount(String accountNumber, String userId) {
        Account account = accountRepository
                .findByAccountNumberAndOwnerUserId(accountNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountNumber));

        return mapToResponse(account);
    }

    public BigDecimal getBalance(String accountNumber, String userId) {
        return accountRepository
                .findByAccountNumberAndOwnerUserId(accountNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountNumber))
                .getBalance();
    }

    public BigDecimal getInternalBalance(String accountNumber) {
        return accountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountNumber))
                .getBalance();
    }


    @Transactional
    public void blockAccount(String accountNumber) {
        log.info("Blocking account: {}", accountNumber);
        Account account = findByAccountNumber(accountNumber);
        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
        log.info("Account blocked: {}", accountNumber);
    }


    @Transactional
    public void deductBalance(
            String accountNumber,
            BigDecimal amount,
            String userId) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Debit amount must be greater than zero");
        }

        log.info("Deducting {} from account: {}", amount, accountNumber);

        Account account = accountRepository
                .findByAccountNumberAndOwnerUserId(accountNumber, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountStateException(
                    "Account is not active: " + accountNumber);
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        log.info("Balance deducted successfully for account: {}", accountNumber);
    }


    @Transactional
    public void creditBalance(String accountNumber, BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Credit amount must be greater than zero");
        }

        log.info("Crediting {} to account: {}", amount, accountNumber);

        Account account = findByAccountNumber(accountNumber);

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        log.info("Balance credited successfully for account: {}", accountNumber);
    }


    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(
            @Payload Map<String, Object> payload) {
        try {
            String receiverAccount = (String) payload.get("receiverAccountNumber");
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());

            log.info("Crediting account: {} amount: {}", receiverAccount, amount);
            creditBalance(receiverAccount, amount);
        } catch (Exception e) {
            log.error("Error crediting account: {}", e.getMessage(), e);
        }
    }


    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(
            @Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            log.info("Fraud detected — blocking account: {}", accountNumber);
            blockAccount(accountNumber);
        } catch (Exception e) {
            log.error("Error blocking account: {}", e.getMessage());
        }
    }


    private String generateAccountNumber() {
        String accountNumber;

        do {
            long number = secureRandom.nextLong(1_000_000_000_000L);

            accountNumber = String.format("%012d", number);

        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    private Account findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountNumber));
    }

    private AccountResponse mapToResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setOwnerUserId(account.getOwnerUserId());
        response.setAccountNumber(account.getAccountNumber());
        response.setAccountHolderName(account.getAccountHolderName());
        response.setEmail(account.getEmail());
        response.setPhone(account.getPhone());
        response.setAccountType(account.getAccountType());
        response.setStatus(account.getStatus());
        response.setBalance(account.getBalance());
        response.setDailyTransactionLimit(account.getDailyTransactionLimit());
        response.setCreatedAt(account.getCreatedAt());
        return response;
    }

    public boolean isAccountOwner(String accountNumber, String userId) {
        return accountRepository
                .findByAccountNumberAndOwnerUserId(accountNumber, userId)
                .isPresent();
    }

    public String getEmail(String accountNumber) {

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "There is no account with this account number: " + accountNumber));
        return account.getEmail();
    }
}
