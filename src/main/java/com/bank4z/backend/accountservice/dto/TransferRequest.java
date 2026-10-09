package com.bank4z.backend.accountservice.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TransferRequest(

        @NotBlank(message = "Recipient account number is required")
        @Pattern(regexp = "^4Z\\d{8}$", message = "That doesn't look like a Bank4Z account number")
        String recipientAccountNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Minimum transfer is R0.01")
        @Digits(integer = 13, fraction = 2, message = "Amount can have at most 2 decimal places")
        BigDecimal amount,

        @Size(max = 100, message = "Reference can be at most 100 characters")
        String reference
) {}