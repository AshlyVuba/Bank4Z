package com.bank4z.backend.sessionservice;

import com.bank4z.backend.authservice.User;
import com.bank4z.backend.notificationservice.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private static final int MAX_TEXT_LENGTH = 100;
    private static final int MAX_IP_LENGTH = 45;

    private final UserSessionRepository sessionRepository;
    private final NotificationService notificationService;
    private final long refreshExpiryMs;

    public SessionService(UserSessionRepository sessionRepository,
                          NotificationService notificationService,
                          @Value("${jwt.refresh-expiration-ms}") long refreshExpiryMs) {
        this.sessionRepository = sessionRepository;
        this.notificationService = notificationService;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    /**
     * Called after a successful password check. Creates the session the new
     * tokens will belong to, and raises a "new phone who dis" alert if this
     * is a device the user has never logged in on before.
     *
     * A user's very first login never alerts (there is nothing to compare
     * against), and neither does a device we've seen before.
     */
    @Transactional
    public UserSession startSession(User user, String deviceId, String deviceName, String ipAddress) {
        String safeDeviceId = clean(deviceId, "unknown");
        String safeDeviceName = clean(deviceName, "Unknown device");
        String safeIp = ipAddress == null ? null
                : (ipAddress.length() > MAX_IP_LENGTH ? ipAddress.substring(0, MAX_IP_LENGTH) : ipAddress);

        // Checked BEFORE saving, otherwise the new session would make every device look "known".
        boolean hadSessionsBefore = sessionRepository.existsByUserId(user.getId());
        boolean deviceKnown = sessionRepository.existsByUserIdAndDeviceId(user.getId(), safeDeviceId);

        Instant now = Instant.now();
        UserSession session = new UserSession(
                user, safeDeviceId, safeDeviceName, safeIp,
                UUID.randomUUID(), now.plusMillis(refreshExpiryMs), now);
        sessionRepository.save(session);

        if (hadSessionsBefore && !deviceKnown) {
            notificationService.notifyNewDeviceLogin(user, safeDeviceName);
        }
        return session;
    }

    /**
     * Checked on every authenticated request: a token with a valid signature
     * is no longer enough, its session must still be active. This is what makes
     * "log out that device" take effect immediately instead of in up to an hour.
     */
    @Transactional
    public boolean isSessionActive(UUID sessionId) {
        Instant now = Instant.now();
        return sessionRepository.findById(sessionId)
                .filter(session -> session.isActive(now))
                .map(session -> {
                    session.touchIfStale(now);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Refresh token rotation. Every refresh hands out a NEW refresh token and
     * kills the old one. If an OLD refresh token shows up again, the only
     * explanations are a bug or a stolen token, so the whole session is
     * revoked and both the thief and the real owner have to log in again.
     *
     * Returns the new refresh token id, or empty if the refresh must be refused.
     * It returns normally in the reuse case (rather than throwing) on purpose,
     * so the revocation is committed instead of being rolled back.
     */
    @Transactional
    public Optional<UUID> rotateRefresh(UUID sessionId, UUID userId, UUID presentedRefreshJti) {
        Instant now = Instant.now();

        Optional<UserSession> found = sessionRepository.findByIdAndUserIdForUpdate(sessionId, userId);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        UserSession session = found.get();
        if (!session.isActive(now)) {
            return Optional.empty();
        }

        if (!session.getRefreshJti().equals(presentedRefreshJti)) {
            session.revoke(now);
            log.warn("Refresh token reuse detected, session {} revoked", sessionId);
            return Optional.empty();
        }

        UUID newJti = UUID.randomUUID();
        session.rotate(newJti, now.plusMillis(refreshExpiryMs), now);
        return Optional.of(newJti);
    }

    @Transactional(readOnly = true)
    public List<UserSession> activeSessions(UUID userId) {
        return sessionRepository.findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastUsedAtDesc(
                userId, Instant.now());
    }

    /** TODO(B4Z-14): record device logouts in the audit log. */
    @Transactional
    public void revoke(UUID userId, UUID sessionId) {
        UserSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException("We couldn't find that device"));
        session.revoke(Instant.now());
    }

    /** "Log out other devices": everything except the session making the request. */
    @Transactional
    public int revokeOthers(UUID userId, UUID currentSessionId) {
        return sessionRepository.revokeAllExcept(userId, currentSessionId, Instant.now());
    }

    // Device info comes from the client, so treat it as untrusted text:
    // strip control characters, trim, cap the length, fall back to a default.
    private static String clean(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String cleaned = value.replaceAll("\\p{Cntrl}", "").trim();
        if (cleaned.isEmpty()) {
            return fallback;
        }
        return cleaned.length() > MAX_TEXT_LENGTH ? cleaned.substring(0, MAX_TEXT_LENGTH) : cleaned;
    }
}