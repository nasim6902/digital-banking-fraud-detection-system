package com.banking.frauddetectionservice.service;

import com.banking.frauddetectionservice.client.AccountServiceClient;
import com.banking.frauddetectionservice.model.FraudCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    @Value("${internal-api.secret}")
    private String internalApiSecret;

    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${fraud.max-transactions-per-minute}")
    private int maxTransactionsPerMinute;

    @Value("${fraud.suspicious-amount-multiplier}")
    private double suspiciousAmountMultiplier;

    @Value("${fraud.max-balance-percentage}")
    private double maxBalancePercentage;

    private static final String VERIFICATION_REQUIRED_TOPIC = "verification.required";
    private static final String FRAUD_CHECK_CLEAN_RESULT_TOPIC = "fraud.check.clean";

    public void checkTransaction(Map<String, Object> payload) {
        String transactionId = (String) payload.get("transactionId");
        String accountNumber = (String) payload.get("senderAccountNumber");
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());


        // Fetch real balance from Account Service
        BigDecimal senderBalance = accountServiceClient.getBalance(
                accountNumber,
                internalApiSecret
        );
        log.info("Checking transaction: {}", transactionId);

        FraudCheckResult result = performFraudChecks(accountNumber, amount, senderBalance);

        if (result.isFraud()) {

            log.info("Suspicious activity detected for transaction: {}. Reason: {}",
                    transactionId, result.getReason());

            Map<String, Object> verificationEvent = new HashMap<>();
            verificationEvent.put("transactionId", transactionId);
            verificationEvent.put("accountNumber", accountNumber);
            verificationEvent.put("amount", amount);
            verificationEvent.put("reason", result.getReason());

            kafkaTemplate.send(
                    VERIFICATION_REQUIRED_TOPIC,
                    transactionId,
                    verificationEvent
            );
        } else {
            // Transaction is clean
            log.info("Transaction clean");

            Map<String, Object> transactionCleanEvent = new HashMap<>();
            transactionCleanEvent.put("transactionId", transactionId);
            transactionCleanEvent.put("isFraud", false);
            transactionCleanEvent.put("reason", null);

            kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC, transactionId, transactionCleanEvent).join();
        }
    }

    private FraudCheckResult performFraudChecks(
            String accountNumber,
            BigDecimal amount,
            BigDecimal senderBalance) {

        // Pattern 1: Velocity Check
        if (isVelocityExceeded(accountNumber)) {
            return new FraudCheckResult(
                    true, "Too many transactions in 60 seconds " +
                    " - Velocity limit exceeded");
        }

        // Pattern 2: Amount check
        if (isAmountSuspicious(accountNumber, amount)) {
            return new FraudCheckResult(
                    true,
                    "Unusual transaction amount - exceeds "
                            + BigDecimal.valueOf(suspiciousAmountMultiplier)
                            .stripTrailingZeros().toPlainString()
                            + "x your average"
            );
        }

        // Pattern 3: Balance Check
        if (senderBalance.compareTo(BigDecimal.ZERO) > 0
                && isBalanceCheckFailed(senderBalance, amount)) {
            String percentage = BigDecimal.valueOf(maxBalancePercentage)
                    .multiply(BigDecimal.valueOf(100))
                    .stripTrailingZeros()
                    .toPlainString();
            return new FraudCheckResult(
                    true, "Transaction exceeds " + percentage
                    + "% of account balance");
        }

        return new FraudCheckResult(false, null);
    }


    private boolean isVelocityExceeded(String accountNumber) {
        String key = "fraud:velocity:" + accountNumber;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }

        log.info("Velocity check - count: {}/{}",
                count, maxTransactionsPerMinute);

        return count != null && count > maxTransactionsPerMinute;
    }

    // Pattern 2: Flag transactions exceeding the configured average multiplier.
    private boolean isAmountSuspicious(String accountNumber, BigDecimal amount) {
        String avgKey = "fraud:avg_amount:" + accountNumber;
        String countKey = "fraud:amount_count:" + accountNumber;

        String avgStr = redisTemplate.opsForValue().get(avgKey);
        String countStr = redisTemplate.opsForValue().get(countKey);

        if (avgStr == null || countStr == null) {
            redisTemplate.opsForValue().set(avgKey, amount.toString());
            redisTemplate.opsForValue().set(countKey, "1");
            return false;
        }

        BigDecimal avgAmount = new BigDecimal(avgStr);
        long count = Long.parseLong(countStr);

        BigDecimal threshold = avgAmount.multiply(
                BigDecimal.valueOf(suspiciousAmountMultiplier)
        );

        boolean suspicious = amount.compareTo(threshold) > 0;

        if (!suspicious) {
            long newCount = count + 1;

            BigDecimal newAvg = avgAmount
                    .multiply(BigDecimal.valueOf(count))
                    .add(amount)
                    .divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

            redisTemplate.opsForValue().set(avgKey, newAvg.toString());
            redisTemplate.opsForValue().set(countKey, String.valueOf(newCount));
        }

        log.info("Amount check - threshold: {} suspicious: {}",
                threshold, suspicious);

        return suspicious;
    }

    private boolean isBalanceCheckFailed(BigDecimal senderBalance, BigDecimal amount) {

        BigDecimal maxAllowed = senderBalance.multiply(
                BigDecimal.valueOf(maxBalancePercentage));

        log.info("Balance check completed. Suspicious: {}",
                amount.compareTo(maxAllowed) > 0);

        return amount.compareTo(maxAllowed) > 0;
    }

}
