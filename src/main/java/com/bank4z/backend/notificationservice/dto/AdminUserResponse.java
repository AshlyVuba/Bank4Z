package com.bank4z.backend.adminservice.dto;

import com.bank4z.backend.authservice.User;

import java.time.Instant;
import java.util.UUID;

/**
 * What an admin sees about a user. Note what's missing: the SA ID number and
 * the password hash. Even admins only get the data they actually need
 * (least privilege / data minimisation).
 */
public record AdminUserResponse(
        UUID id,
        String fullName,
        String email,
        String phoneNumber,
        String role,
        Instant createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}