
package com.banking.authservice.service;

import com.banking.authservice.dto.AuthResponse;
import com.banking.authservice.dto.LoginRequest;
import com.banking.authservice.dto.RegisterRequest;
import com.banking.authservice.dto.RegistrationResponse;
import com.banking.authservice.entity.Role;
import com.banking.authservice.entity.User;
import com.banking.authservice.exception.EmailAlreadyRegisteredException;
import com.banking.authservice.exception.InvalidCredentialsException;
import com.banking.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    private static final String REGISTRATI0N_COMPLETED_TOPIC = "registration.completed";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RegistrationResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        log.info("User registration initiated");

        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Registration rejected: email already registered");
            throw new EmailAlreadyRegisteredException(
                    "Email is already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);

        log.info("User registration completed successfully. User ID: {}",
                savedUser.getId());

        Map<String, Object> rcEvent = new HashMap<>();
        rcEvent.put("userId", savedUser.getId());
        rcEvent.put("email", savedUser.getEmail());
        rcEvent.put("createdAt", savedUser.getCreatedAt());

        kafkaTemplate.send(REGISTRATI0N_COMPLETED_TOPIC, savedUser.getId(), rcEvent);

        return new RegistrationResponse(savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getCreatedAt(),
                "Registration successful.");

    }


    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        log.info("Login attempt received");

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.getPassword()
                    )
            );

        } catch (AuthenticationException ex) {
            log.warn("Login rejected: invalid credentials");
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password"
                        )
                );

        String token = jwtService.generateToken(user);

        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setToken(token);

        log.info("User authenticated successfully");

        return response;
    }
}
