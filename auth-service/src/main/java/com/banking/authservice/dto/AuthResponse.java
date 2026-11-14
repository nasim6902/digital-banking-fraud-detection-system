package com.banking.authservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

    private String userId;
    private String email;
    private String role;
    private String token;
}
