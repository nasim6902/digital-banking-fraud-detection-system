
package com.banking.authservice.service;

import com.banking.authservice.client.AccountServiceClient;
import com.banking.authservice.dto.AuthResponse;
import com.banking.authservice.dto.LoginRequest;
import com.banking.authservice.dto.RegisterRequest;
import com.banking.authservice.dto.RegistrationResponse;
import com.banking.authservice.entity.Role;
import com.banking.authservice.entity.User;
import com.banking.authservice.exception.EmailAlreadyRegisteredException;
import com.banking.authservice.exception.InvalidCredentialsException;
import com.banking.authservice.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AccountServiceClient accountServiceClient;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${internal-api.secret}")
    private String internalApiSecret;


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

        String status = accountServiceClient.getStatus(
                email, internalApiSecret
        );

        if ("BLOCKED".equals(status) || "CLOSED".equals(status)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account is " + status);
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Invalid email or password"
                ));

        String token = jwtService.generateToken(user);

        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setToken(token);

        log.info("User authenticated successfully");

        return response;
    }

    public RegistrationResponse adminRegister(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        log.info("Admin registration initiated");

        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Admin registration rejected: email already registered");
            throw new EmailAlreadyRegisteredException(
                    "Email is already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ADMIN);

        User savedUser = userRepository.save(user);

        log.info("Admin registration completed successfully. User ID: {}",
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
                "Admin registration successful.");

    }


    public AuthResponse adminLogin(@Valid LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();
        AuthResponse response = new AuthResponse();
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                email, request.getPassword());
        try {
            authenticationManager.authenticate(token);
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new InvalidCredentialsException(
                            "Invalid email or password"
                    ));
            String adminToken = jwtService.generateToken(user);

            response.setUserId(user.getId());
            response.setEmail(user.getEmail());
            response.setRole(user.getRole().name());
            response.setToken(adminToken);

            log.info("Admin authenticated successfully");

            return response;

        } catch (AuthenticationException ex) {
            log.warn("Admin Login rejected: invalid credentials");
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }
    }
}
