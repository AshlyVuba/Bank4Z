package com.bank4z.backend.sessionservice;

import com.bank4z.backend.authservice.User;
import com.bank4z.backend.notificationservice.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final long REFRESH_EXPIRY_MS = 7L * 24 * 60 * 60 * 1000;

    @Mock UserSessionRepository sessionRepository;
    @Mock NotificationService notificationService;

    private SessionService sessionService;
    private User user;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(sessionRepository, notificationService, REFRESH_EXPIRY_MS);
        user = new User("Alex", "alex@test.com", "0000000000001", "0821111111", "hash");
    }

    private UserSession sessionExpiringIn(long seconds, UUID refreshJti) {
        Instant now = Instant.now();
        return new UserSession(user, "phone-1", "Pixel", "10.0.0.1", refreshJti, now.plusSeconds(seconds), now);
    }

    // ---- new-device detection ----

    @Test
    void firstEverLoginDoesNotRaiseANewDeviceAlert() {
        when(sessionRepository.existsByUserId(any())).thenReturn(false);
        when(sessionRepository.existsByUserIdAndDeviceId(any(), eq("phone-1"))).thenReturn(false);

        UserSession session = sessionService.startSession(user, "phone-1", "Pixel", "10.0.0.1");

        assertThat(session.getDeviceName()).isEqualTo("Pixel");
        verify(sessionRepository).save(session);
        verify(notificationService, never()).notifyNewDeviceLogin(any(), any());
    }

    @Test
    void loginFromANewDeviceAfterEarlierLoginsRaisesAnAlert() {
        when(sessionRepository.existsByUserId(any())).thenReturn(true);
        when(sessionRepository.existsByUserIdAndDeviceId(any(), eq("laptop-1"))).thenReturn(false);

        sessionService.startSession(user, "laptop-1", "Alex Laptop", "10.0.0.2");

        verify(notificationService).notifyNewDeviceLogin(user, "Alex Laptop");
    }

    @Test
    void loginFromAKnownDeviceDoesNotRaiseAnAlert() {
        when(sessionRepository.existsByUserId(any())).thenReturn(true);
        when(sessionRepository.existsByUserIdAndDeviceId(any(), eq("phone-1"))).thenReturn(true);

        sessionService.startSession(user, "phone-1", "Pixel", "10.0.0.1");

        verify(notificationService, never()).notifyNewDeviceLogin(any(), any());
    }

    @Test
    void deviceInfoFromTheClientIsCleanedAndDefaulted() {
        when(sessionRepository.existsByUserId(any())).thenReturn(false);
        when(sessionRepository.existsByUserIdAndDeviceId(any(), any())).thenReturn(false);

        UserSession missing = sessionService.startSession(user, null, "   ", null);
        UserSession messy = sessionService.startSession(user, "id-1", "Pixel\n\t 7", null);

        assertThat(missing.getDeviceId()).isEqualTo("unknown");
        assertThat(missing.getDeviceName()).isEqualTo("Unknown device");
        assertThat(messy.getDeviceName()).isEqualTo("Pixel 7"); // control characters stripped
    }

    // ---- refresh rotation and reuse detection ----

    @Test
    void refreshWithTheCurrentTokenIssuesANewOneAndRetiresTheOld() {
        UUID currentJti = UUID.randomUUID();
        UserSession session = sessionExpiringIn(3600, currentJti);
        when(sessionRepository.findByIdAndUserIdForUpdate(any(), any())).thenReturn(Optional.of(session));

        Optional<UUID> newJti = sessionService.rotateRefresh(UUID.randomUUID(), UUID.randomUUID(), currentJti);

        assertThat(newJti).isPresent();
        assertThat(newJti.get()).isNotEqualTo(currentJti);
        assertThat(session.getRefreshJti()).isEqualTo(newJti.get());
        assertThat(session.isActive(Instant.now())).isTrue();
    }

    @Test
    void reusingAnOldRefreshTokenRevokesTheWholeSession() {
        UUID currentJti = UUID.randomUUID();
        UUID staleJti = UUID.randomUUID(); // a token that was already rotated away
        UserSession session = sessionExpiringIn(3600, currentJti);
        when(sessionRepository.findByIdAndUserIdForUpdate(any(), any())).thenReturn(Optional.of(session));

        Optional<UUID> result = sessionService.rotateRefresh(UUID.randomUUID(), UUID.randomUUID(), staleJti);

        assertThat(result).isEmpty();
        assertThat(session.isActive(Instant.now())).isFalse();
    }

    @Test
    void refreshOnARevokedSessionIsRefused() {
        UUID jti = UUID.randomUUID();
        UserSession session = sessionExpiringIn(3600, jti);
        session.revoke(Instant.now());
        when(sessionRepository.findByIdAndUserIdForUpdate(any(), any())).thenReturn(Optional.of(session));

        assertThat(sessionService.rotateRefresh(UUID.randomUUID(), UUID.randomUUID(), jti)).isEmpty();
    }

    @Test
    void refreshOnAnUnknownOrForeignSessionIsRefused() {
        when(sessionRepository.findByIdAndUserIdForUpdate(any(), any())).thenReturn(Optional.empty());

        assertThat(sessionService.rotateRefresh(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())).isEmpty();
    }

    // ---- is the session still allowed to make requests? ----

    @Test
    void activeSessionIsAccepted() {
        when(sessionRepository.findById(any())).thenReturn(Optional.of(sessionExpiringIn(3600, UUID.randomUUID())));

        assertThat(sessionService.isSessionActive(UUID.randomUUID())).isTrue();
    }

    @Test
    void revokedExpiredOrUnknownSessionsAreRejected() {
        UserSession revoked = sessionExpiringIn(3600, UUID.randomUUID());
        revoked.revoke(Instant.now());
        UserSession expired = sessionExpiringIn(-60, UUID.randomUUID());

        when(sessionRepository.findById(any()))
                .thenReturn(Optional.of(revoked))
                .thenReturn(Optional.of(expired))
                .thenReturn(Optional.empty());

        assertThat(sessionService.isSessionActive(UUID.randomUUID())).isFalse();
        assertThat(sessionService.isSessionActive(UUID.randomUUID())).isFalse();
        assertThat(sessionService.isSessionActive(UUID.randomUUID())).isFalse();
    }

    // ---- revoking ----

    @Test
    void revokingYourOwnSessionEndsIt() {
        UserSession session = sessionExpiringIn(3600, UUID.randomUUID());
        when(sessionRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(session));

        sessionService.revoke(UUID.randomUUID(), UUID.randomUUID());

        assertThat(session.isActive(Instant.now())).isFalse();
    }

    @Test
    void revokingSomeoneElsesSessionLooksLikeItDoesNotExist() {
        when(sessionRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.revoke(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void logOutOtherDevicesReturnsHowManyWereEnded() {
        when(sessionRepository.revokeAllExcept(any(), any(), any(Instant.class))).thenReturn(2);

        assertThat(sessionService.revokeOthers(UUID.randomUUID(), UUID.randomUUID())).isEqualTo(2);
    }
}