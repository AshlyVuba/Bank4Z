package com.bank4z.backend.sessionservice;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    boolean existsByUserId(UUID userId);

    boolean existsByUserIdAndDeviceId(UUID userId, String deviceId);

    List<UserSession> findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastUsedAtDesc(UUID userId, Instant now);

    // Always looked up by session AND owner, so nobody can touch a session by guessing its id.
    Optional<UserSession> findByIdAndUserId(UUID id, UUID userId);

    /**
     * SELECT ... FOR UPDATE. Used when rotating a refresh token: two requests
     * presenting the same refresh token must be handled one after the other,
     * so the second one sees the already-rotated state instead of both
     * "winning".
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserSession s where s.id = :id and s.user.id = :userId")
    Optional<UserSession> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    // Bulk update bypasses JPA auditing, but the V1 trigger still keeps updated_at correct.
    @Modifying(clearAutomatically = true)
    @Query("update UserSession s set s.revokedAt = :now "
            + "where s.user.id = :userId and s.id <> :currentId and s.revokedAt is null")
    int revokeAllExcept(@Param("userId") UUID userId,
                        @Param("currentId") UUID currentId,
                        @Param("now") Instant now);
}