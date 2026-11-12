package com.bank4z.backend.sessionservice;

import com.bank4z.backend.authservice.User;
import com.bank4z.backend.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One login on one device. Named UserSession (not Session) so it never gets
 * confused with Hibernate's or the servlet API's Session types.
 */
@Entity
@Table(name = "sessions")
public class UserSession extends BaseAuditableEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** The id (jti) of the one refresh token that is currently valid for this session. */
    @Column(name = "refresh_jti", nullable = false)
    private UUID refreshJti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected UserSession() {
        // required by JPA
    }

    public UserSession(User user, String deviceId, String deviceName, String ipAddress,
                       UUID refreshJti, Instant expiresAt, Instant now) {
        this.user = user;
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.ipAddress = ipAddress;
        this.refreshJti = refreshJti;
        this.expiresAt = expiresAt;
        this.lastUsedAt = now;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
    public String getIpAddress() { return ipAddress; }
    public UUID getRefreshJti() { return refreshJti; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public Instant getRevokedAt() { return revokedAt; }

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    /** Safe to call twice: the first revocation time wins. */
    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    /** Swap in a new refresh token id and push the expiry out. Old refresh tokens stop working. */
    public void rotate(UUID newRefreshJti, Instant newExpiresAt, Instant now) {
        this.refreshJti = newRefreshJti;
        this.expiresAt = newExpiresAt;
        this.lastUsedAt = now;
    }

    /** Only writes if the last update was over a minute ago, so busy clients don't cause a write per request. */
    public void touchIfStale(Instant now) {
        if (lastUsedAt.isBefore(now.minusSeconds(60))) {
            lastUsedAt = now;
        }
    }
}