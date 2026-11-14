
package com.banking.authservice.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.banking.authservice.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private final Algorithm algorithm;
    private final long expirationTime;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationTime) {

        byte[] secretBytes = Base64.getDecoder().decode(secret);

        this.algorithm = Algorithm.HMAC256(secretBytes);
        this.expirationTime = expirationTime;
    }

    public String generateToken(User user) {
        return JWT.create()
                .withSubject(user.getId())
                .withClaim("email", user.getEmail())
                .withClaim("role", user.getRole().name())
                .withIssuedAt(new Date())
                .withExpiresAt(
                        new Date(System.currentTimeMillis() + expirationTime)
                )
                .sign(algorithm);
    }
}
