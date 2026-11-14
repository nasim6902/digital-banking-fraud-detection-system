package com.banking.notificationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "account-service")
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/internal/email/{accountNumber}")
    public String getEmail(
            @PathVariable String accountNumber,
            @RequestHeader("X-Internal-Secret") String internalSecret);
}
