package com.bank4z.backend.notificationservice;

import com.bank4z.backend.authservice.User;
import com.bank4z.backend.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification extends BaseAuditableEntity {

    public enum Type { TRANSFER_SENT, TRANSFER_RECEIVED }

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    @Column(nullable = false, length = 255)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    protected Notification() {
        // required by JPA
    }

    public Notification(User user, Type type, String message) {
        this.user = user;
        this.type = type;
        this.message = message;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Type getType() { return type; }
    public String getMessage() { return message; }
    public boolean isRead() { return isRead; }

    public void markRead() { this.isRead = true; }
}