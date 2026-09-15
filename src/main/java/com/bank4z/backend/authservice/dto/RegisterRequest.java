package com.bank4z.backend.authservice.dto;

import com.bank4z.backend.common.validation.ValidSaId;
import jakarta.validation.constraints.*;

public record RegisterRequest(

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        String email,

        @NotBlank(message = "ID number is required")
        @ValidSaId
        String idNumber,

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^(\\+27|0)[6-8][0-9]{8}$",
                message = "Enter a valid South African phone number, e.g. 0821234567 or +27821234567"
        )
        String phoneNumber,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password needs at least one uppercase letter, one lowercase letter, and one number"
        )
        String password
) {}