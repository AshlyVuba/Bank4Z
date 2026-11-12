package com.bank4z.backend.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * deviceId and deviceName are optional so older clients and quick API tests
 * still work; without them the login is recorded as "Unknown device".
 */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password,

        @Size(max = 100, message = "Device id is too long")
        String deviceId,

        @Size(max = 100, message = "Device name is too long")
        String deviceName
) {}