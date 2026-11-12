package com.bank4z.backend.sessionservice.dto;

import com.bank4z.backend.sessionservice.UserSession;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String deviceName,
        String ipAddress,
        Instant createdAt,
        Instant lastUsedAt,
        boolean current
) {
    public static SessionResponse from(UserSession session, UUID currentSessionId) {
        return new SessionResponse(
                session.getId(),
                session.getDeviceName(),
                session.getIpAddress(),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                session.getId().equals(currentSessionId)
        );
    }
}