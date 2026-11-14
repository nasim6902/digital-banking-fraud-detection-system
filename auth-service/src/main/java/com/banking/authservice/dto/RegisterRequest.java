package com.banking.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email shouldn't be blank")
    private String email;

    @NotBlank(message = "Password shouldn't be blank")
    @Size(min = 8, max = 72,
            message = "Password must be between 8 and 72 characters")
    private String password;
}