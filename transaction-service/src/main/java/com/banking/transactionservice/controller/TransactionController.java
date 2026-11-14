package com.banking.transactionservice.controller;

import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.dto.TransferRequest;
import com.banking.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    // Transfer money between accounts
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(transactionService.transfer(request, jwt.getSubject()));
    }

    // Get transaction by ID
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable String transactionId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                transactionService.getTransaction(transactionId, jwt.getSubject()));
    }

    // Get transaction history for account
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getHistory(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                transactionService.getTransactionHistory(accountNumber, jwt.getSubject()));
    }

    // Verify the transaction with OTP
    @PostMapping("/{transactionId}/verify")
    public ResponseEntity<TransactionResponse> verifyTransaction(
            @PathVariable String transactionId,
            @RequestParam String otp,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("OTP verification request — transaction: {}",
                transactionId);

        return ResponseEntity.ok(
                transactionService.verifyOTP(
                        transactionId,
                        otp,
                        jwt.getSubject()
                )
        );
    }
}
