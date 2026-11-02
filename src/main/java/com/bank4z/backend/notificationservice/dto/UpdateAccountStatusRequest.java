package com.bank4z.backend.adminservice.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(

        @NotNull(message = "Status is required")
        Status status
) {
    /** Admins can freeze/unfreeze only. Closing an account is not an API action. */
    public enum Status { ACTIVE, FROZEN }
}