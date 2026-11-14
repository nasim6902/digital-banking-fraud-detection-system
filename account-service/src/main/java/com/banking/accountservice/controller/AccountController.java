
package com.banking.accountservice.controller;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    @Value("${internal-api.secret}")
    private String internalApiSecret;

    @PostMapping("/create")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(
                        request,
                        jwt.getSubject(),
                        jwt.getClaimAsString("email")
                ));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                accountService.getAccount(accountNumber, jwt.getSubject()));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                accountService.getBalance(accountNumber, jwt.getSubject()));
    }

    @GetMapping("/internal/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getInternalBalance(
            @PathVariable String accountNumber,
            @RequestHeader("X-Internal-Secret") String suppliedSecret) {

        boolean validSecret = MessageDigest.isEqual(
                internalApiSecret.getBytes(StandardCharsets.UTF_8),
                suppliedSecret.getBytes(StandardCharsets.UTF_8)
        );

        if (!validSecret) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Unauthorized internal operation"
            );
        }

        return ResponseEntity.ok(
                accountService.getInternalBalance(accountNumber)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> blockAccount(
            @PathVariable String accountNumber) {

        accountService.blockAccount(accountNumber);
        return ResponseEntity.ok("Account blocked successfully");
    }

    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<String> deductBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestHeader("X-Internal-Secret") String suppliedSecret,
            @AuthenticationPrincipal Jwt jwt) {

        validateInternalSecret(suppliedSecret);

        accountService.deductBalance(
                accountNumber,
                amount,
                jwt.getSubject()
        );

        return ResponseEntity.ok("Balance deducted successfully");
    }

    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<String> creditBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount,
            @RequestHeader("X-Internal-Secret") String suppliedSecret) {

        validateInternalSecret(suppliedSecret);

        accountService.creditBalance(accountNumber, amount);

        return ResponseEntity.ok("Balance credited successfully");
    }

    @GetMapping("/{accountNumber}/verify-owner")
    public ResponseEntity<Boolean> verifyAccountOwnership(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                accountService.isAccountOwner(
                        accountNumber, jwt.getSubject()));
    }

    private void validateInternalSecret(String suppliedSecret) {
        boolean validSecret = MessageDigest.isEqual(
                internalApiSecret.getBytes(StandardCharsets.UTF_8),
                suppliedSecret.getBytes(StandardCharsets.UTF_8)
        );

        if (!validSecret) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Unauthorized internal operation"
            );
        }
    }

    @GetMapping("/internal/email/{accountNumber}")
    public ResponseEntity<String> getEmail(
            @PathVariable String accountNumber,
            @RequestHeader("X-Internal-Secret") String suppliedSecret) {

        validateInternalSecret(suppliedSecret);

        return ResponseEntity.ok(accountService.getEmail(accountNumber));
    }
}
