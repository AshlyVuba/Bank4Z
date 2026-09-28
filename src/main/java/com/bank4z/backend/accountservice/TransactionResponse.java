package com.bank4z.backend.accountservice.dto;

import com.bank4z.backend.accountservice.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        BigDecimal amount,
        String type,
        String status,
        String reference,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.getReference(),
                transaction.getCreatedAt()
        );
    }
}