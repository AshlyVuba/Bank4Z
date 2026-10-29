package com.bank4z.backend.notificationservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Always looked up by id AND owner, so a user can never touch someone
    // else's notification just by guessing its id.
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    @Query("select count(n) from Notification n where n.user.id = :userId and n.isRead = false")
    long countUnread(@Param("userId") UUID userId);

    // Bulk update bypasses JPA auditing, but the database trigger from V1
    // still keeps updated_at correct.
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.user.id = :userId and n.isRead = false")
    int markAllRead(@Param("userId") UUID userId);
}