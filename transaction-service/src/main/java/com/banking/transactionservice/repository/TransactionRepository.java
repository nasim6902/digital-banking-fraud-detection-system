package com.banking.transactionservice.repository;

import com.banking.transactionservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Optional<Transaction> findByIdAndUserId(String id, String userId);
    List<Transaction> findBySenderAccountNumberAndUserIdOrderByCreatedAtDesc(
            String accountNumber, String userId);

}
