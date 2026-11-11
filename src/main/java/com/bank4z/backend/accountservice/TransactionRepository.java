package com.bank4z.backend.accountservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);

    /** Used by the fraud engine to count recent completed transfers out of an account. */
    long countByAccountIdAndTypeAndStatusAndCreatedAtAfter(
            UUID accountId, Transaction.Type type, Transaction.Status status, Instant createdAt);
}