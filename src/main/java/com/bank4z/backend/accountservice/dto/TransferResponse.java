package com.bank4z.backend.accountservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponse(
        UUID transactionId,
        String recipientAccountNumber,
        BigDecimal amount,
        BigDecimal newBalance,
        String message
) {}