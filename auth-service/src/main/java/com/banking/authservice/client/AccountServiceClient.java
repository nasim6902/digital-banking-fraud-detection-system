package com.banking.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/internal/status/{email}")
    String getStatus(
            @PathVariable String email,
            @RequestHeader("X-Internal-Secret") String internalSecret);
}
