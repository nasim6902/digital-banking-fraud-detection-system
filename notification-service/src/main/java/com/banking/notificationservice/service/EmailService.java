package com.banking.notificationservice.service;

import com.banking.notificationservice.client.AccountServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final AccountServiceClient accountServiceClient;
    private final JavaMailSender javaMailSender;

    @Value("${internal-api.secret}")
    private String internalApiSecret;

    public void sendEmail(
            String accountNumber, String subject, String message){

        String email = accountServiceClient.getEmail(
                accountNumber,internalApiSecret);

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(email);
        mail.setSubject(subject);
        mail.setText(message);

        javaMailSender.send(mail);
        log.info("Email sent successfully with OTP");
    }
}
