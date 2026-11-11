package com.bank4z.backend.fraudservice.dto;

import com.bank4z.backend.accountservice.Transaction;
import com.bank4z.backend.fraudservice.FraudFlag;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudFlagResponse(
        UUID id,
        UUID transactionId,
        String accountNumber,
        BigDecimal amount,
        String rule,
        String riskLevel,
        String actionTaken,
        String reason,
        boolean resolved,
        Instant createdAt
) {
    /** Must be called inside a transaction (or with the entity graph loaded): it reads lazy relations. */
    public static FraudFlagResponse from(FraudFlag flag) {
        Transaction tx = flag.getTransaction();
        return new FraudFlagResponse(
                flag.getId(),
                tx.getId(),
                tx.getAccount().getAccountNumber(),
                tx.getAmount(),
                flag.getRule().name(),
                flag.getRiskLevel().name(),
                flag.getActionTaken().name(),
                flag.getReason(),
                flag.isResolved(),
                flag.getCreatedAt()
        );
    }
}