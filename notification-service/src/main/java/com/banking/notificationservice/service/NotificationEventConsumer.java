package com.banking.notificationservice.service;

import com.banking.notificationservice.client.AccountServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.swing.text.SimpleAttributeSet;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final EmailService emailService;
    private final AccountServiceClient accountServiceClient;

    @Value("${internal-api.secret}")
    private String internalApiSecret;

    /**
     * Transaction completed — debit sender, credit receiver.
     */
    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(
            @Payload Map<String, Object> payload) {
        try {
            String senderAccount = (String) payload
                    .get("senderAccountNumber");
            String receiverAccount = (String) payload
                    .get("receiverAccountNumber");
            String amount = payload.get("amount").toString();

            String sEmail = accountServiceClient.getEmail(
                    senderAccount, internalApiSecret);

            emailService.sendMail(sEmail, "DEBIT ALERT",
                    String.format("₹%s debited from account %s",
                            amount, senderAccount));

            String rEmail = accountServiceClient.getEmail(
                    receiverAccount, internalApiSecret);

            emailService.sendMail(rEmail, "CREDIT ALERT",
                    String.format("₹%s credited to account %s",
                            amount, receiverAccount));

        } catch (Exception e) {
            log.error("Error sending transaction notification: {}",
                    e.getMessage(), e);
        }
    }


    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(
            @Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String reason = (String) payload.get("reason");

            String email = accountServiceClient.getEmail(
                    accountNumber, internalApiSecret);
            emailService.sendMail(email,
                    "🚨 ACCOUNT BLOCKED",
                    String.format(
                            "Your account %s has been blocked. " +
                                    "Reason: %s. " +
                                    "Please contact bank immediately.",
                            accountNumber, reason));

        } catch (Exception e) {
            log.error("Error sending fraud alert: {}", e.getMessage(), e);
        }
    }


    @KafkaListener(topics = "transaction.otp.generated")
    public void consumeOtpGenerated(
            @Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String otp = (String) payload.get("otp");
            String transactionId = (String) payload.get("transactionId");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");

            String email = accountServiceClient.getEmail(
                    accountNumber, internalApiSecret);
            emailService.sendMail(email,
                    "🔐 TRANSACTION VERIFICATION REQUIRED",
                    String.format(
                            "Suspicious activity detected on your account. " +
                                    "Reason: %s. " +
                                    "A transaction of ₹%s is pending verification. " +
                                    "Your OTP is: %s. Valid for 5 minutes. " +
                                    "If this wasn't you — ignore this message. " +
                                    "Transaction: %s will be cancelled and amount refunded automatically.",
                            reason, amount, otp, transactionId));

        } catch (Exception e) {
            log.error("Error sending OTP notification: {}",
                    e.getMessage(), e);
        }
    }


    @KafkaListener(topics = "transaction.refunded")
    public void consumeTransactionRefunded(
            @Payload Map<String, Object> payload) {
        try {
            String senderAccount = (String) payload
                    .get("senderAccountNumber");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");

            String email = accountServiceClient.getEmail(
                    senderAccount, internalApiSecret);
            emailService.sendMail(email, "💰 REFUND PROCESSED",
                    String.format(
                            "Your transaction of ₹%s was cancelled. " +
                                    "Reason: %s. " +
                                    "₹%s has been refunded to account %s.",
                            amount, reason, amount, senderAccount));

        } catch (Exception e) {
            log.error("Error sending refund notification: {}",
                    e.getMessage(), e);
        }
    }

    /**
     * Payment completed via Razorpay.
     */
    @KafkaListener(topics = "payment.completed")
    public void consumePaymentCompleted(
            @Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String amount = payload.get("amount").toString();

            String email = accountServiceClient.getEmail(
                    accountNumber, internalApiSecret);
            emailService.sendMail(email, "PAYMENT SUCCESSFUL",
                    String.format(
                            "Payment of ₹%s completed. " +
                                    "Razorpay ID: %s",
                            amount, payload.get("razorpayPaymentId")));

        } catch (Exception e) {
            log.error("Error sending payment notification: {}",
                    e.getMessage(), e);
        }
    }

    /**
     * Payment failed via Razorpay.
     */
    @KafkaListener(topics = "payment.failed")
    public void consumePaymentFailed(
            @Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String amount = payload.get("amount").toString();

            String email = accountServiceClient.getEmail(
                    accountNumber, internalApiSecret);
            emailService.sendMail(email, "❌ PAYMENT FAILED",
                    String.format(
                            "Your payment of %s could not be processed. " +
                                    "Please try again or contact support.",
                            amount));

        } catch (Exception e) {
            log.error("Error sending payment failure notification: {}",
                    e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "registration.completed")
    public void consumeRegistrationCompleted(
            @Payload Map<String, Object> payload) {

        try {
            String email = (String) payload.get("email");
            String subject = "Welcome mail";
            String message = "Congratulation! Your registration has been completed. " +
                    "Please login using your email and password and create your account." +
                    """
                            Instruction for account creation:
                            1. Account Holder Name
                            2. Email Id (same as used for registration.)
                            3. Phone number (same as used for registration.)
                            4. Account Type (SAVINGS, CURRENT AND FIXED-DEPOSIT)
                            5. Initial Deposit (Minimum should be 1000)
                            """ +
                    "You can connect with support at support@dbs.com for any assistance.";

            emailService.sendMail(email, subject, message);

        } catch (Exception e) {
            log.error("Error sending welcome email to the user");
        }
    }

    @KafkaListener(topics = "account.created")
    public void consumeAccountCreated(
            @Payload Map<String, Object> payload) {

        try {
            String email = (String) payload.get("email");
            String subject = "Account Confirmation";
            String message = "Congratulation! Your account has been created successfully." +
                    "You can connect with support at support@dbs.com for any assistance.";

            emailService.sendMail(email, subject, message);

        } catch (Exception e) {
            log.error("Error sending account creation email to the user");
        }
    }
}
