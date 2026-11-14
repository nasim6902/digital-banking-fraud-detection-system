package com.banking.frauddetectionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;


@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/internal/{accountNumber}/balance")
    BigDecimal getBalance(
            @PathVariable("accountNumber") String accountNumber,
            @RequestHeader("X-Internal-Secret") String internalSecret
    );
}
