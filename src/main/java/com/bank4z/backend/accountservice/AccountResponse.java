package com.bank4z.backend.accountservice.dto;

import com.bank4z.backend.accountservice.Account;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String accountNumber,
        BigDecimal balance,
        String status
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getStatus().name()
        );
    }
}