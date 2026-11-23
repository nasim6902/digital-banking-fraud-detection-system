
package com.banking.transactionservice.client;

import com.banking.transactionservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;

@FeignClient(
        name = "account-service",
        configuration = FeignConfig.class
)
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/{accountNumber}/verify-owner")
    boolean verifyAccountOwnership(
            @PathVariable String accountNumber);

    @PutMapping("/api/v1/accounts/{accountNumber}/deduct")
    String deductBalance(
            @PathVariable("accountNumber") String accountNumber,
            @RequestParam("amount") BigDecimal amount,
            @RequestHeader("X-Internal-Secret") String internalSecret);

    @PutMapping("/api/v1/accounts/internal/{accountNumber}/refund")
    String refundBalance(
            @PathVariable("accountNumber") String accountNumber,
            @RequestParam("amount") BigDecimal amount,
            @RequestHeader("X-Internal-Secret") String internalSecret);

    @GetMapping("/api/v1/accounts/{accountNumber}/balance")
    BigDecimal getBalance(@PathVariable String accountNumber);
}
