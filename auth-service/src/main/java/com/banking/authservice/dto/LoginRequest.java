package com.banking.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email shouldn't be blank")
    private String email;

    @NotBlank(message = "Password shouldn't be blank")
    private String password;
}
