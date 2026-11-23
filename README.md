# Digital Banking System

A microservices-based digital banking application built with Java and Spring Boot. The project demonstrates core banking operations, JWT-based authentication, inter-service communication, transaction processing, fraud detection, payment integration, and event-driven email notifications.

## Architecture

The application is divided into independent services, with an API Gateway handling client requests and Eureka providing service discovery.

| Service | Port | Responsibility |
|---|---:|---|
| API Gateway | 8080 | Central entry point and request routing |
| Account Service | 8081 | Account creation, account details, balance and account operations |
| Transaction Service | 8082 | Money transfers and transaction management |
| Payment Service | 8083 | Payment order creation and Razorpay integration |
| Fraud Detection Service | 8084 | Transaction monitoring and fraud detection |
| Notification Service | 8085 | Event-driven email notifications |
| Auth Service | 8086 | User registration, login and JWT generation |
| Eureka Server | 8761 | Service discovery and registration |

## Technologies Used

- **Backend:** Java, Spring Boot, Spring Security
- **Security:** JWT Authentication, BCrypt password hashing
- **Database:** MySQL, Spring Data JPA, Hibernate
- **Messaging:** Apache Kafka
- **Caching:** Redis
- **Service Discovery:** Netflix Eureka
- **Inter-service Communication:** OpenFeign
- **API Gateway:** Spring Cloud Gateway (WebFlux)
- **Email:** JavaMailSender / SMTP
- **Build Tool:** Maven
- **Containerization:** Docker, Docker Compose
- **Version Control:** Git, GitHub

## Implemented Features

### Authentication
- User registration and login.
- Password hashing using BCrypt.
- JWT generation and authentication.
- Role-based authorization.

### Account Management
- Create and retrieve bank accounts.
- Generate unique account numbers.
- Retrieve account balance.
- Credit and debit account balances.
- Block accounts.
- Validate account status and available balance.

### Money Transfers
- Initiate money transfers between accounts.
- Validate transaction ownership.
- Maintain transaction records and statuses.
- Process transaction completion and compensation through event-driven communication.

### Fraud Detection
- Monitor transaction frequency.
- Detect unusually large transactions.
- Evaluate transaction amounts against available account balance.
- Flag suspicious transactions for additional OTP verification.
- Block accounts and initiate transaction compensation when required.

### Payment Integration
- Create payment orders using Razorpay.
- Store payment order details and status.
- Process payment-related events through Kafka.

### Email Notifications
- Send email notifications after successful user registration.
- Send email notifications after successful account creation.
- Send transaction verification OTP emails when suspicious activity is detected.
- Use Kafka events to communicate notification requests to the Notification Service.

## Inter-Service Communication

The application uses two communication approaches:

- **Synchronous communication:** OpenFeign is used when one service needs an immediate response from another service.
- **Asynchronous communication:** Apache Kafka is used to exchange events between services without requiring direct synchronous calls.

Redis is used for temporary data storage, including transaction OTP expiry and transaction-related checks.

## Running the Project Locally

### Prerequisites

- Java 25
- Maven 3.9+
- MySQL
- Docker and Docker Compose
- Redis
- IntelliJ IDEA or another Java IDE

### Infrastructure Setup

Start Kafka and Redis using Docker Compose:

```bash
docker compose up -d
```

MySQL is configured to run locally.

### Application Startup

1. Start the Eureka Server.
2. Start the Account Service.
3. Start the remaining microservices.
4. Start the API Gateway.
5. Send requests through the API Gateway on port `8080`.

The microservices are run locally from the IDE.

### Environment Configuration

Configure the required environment variables before starting the services:

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | JWT signing secret |
| `DBS_INTERNAL_API_SECRET` | Internal service-to-service authentication |
| `RAZORPAY_KEY_ID` | Razorpay API key ID |
| `RAZORPAY_KEY_SECRET` | Razorpay API secret |
| `RAZORPAY_WEBHOOK_SECRET` | Razorpay webhook signature verification secret |

Database credentials and other service-specific configuration must also be supplied through the corresponding application configuration.

**Do not commit actual credentials, API keys, or secrets to the repository.**

## API Access

All client-facing API requests are routed through the API Gateway:

```text
http://localhost:8080
```

Service endpoints use versioned API paths such as:

```text
/api/v1/auth
/api/v1/accounts
/api/v1/transactions
/api/v1/payments
```

## Project Structure

```text
digital-banking-system/
├── api-gateway/
├── auth-service/
├── account-service/
├── transaction-service/
├── payment-service/
├── fraud-detection-service/
├── notification-service/
├── fraud-detection-service/
├── eureka-server/
├── docker-compose.yml
└── README.md
```

## Purpose

This project was developed to gain practical experience with Spring Boot microservices, secure REST APIs, database integration, distributed service communication, Kafka-based event processing, Redis, and third-party payment integration.
