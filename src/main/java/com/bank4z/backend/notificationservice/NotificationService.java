package com.bank4z.backend.notificationservice;

import com.bank4z.backend.authservice.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification create(User user, Notification.Type type, String message) {
        return notificationRepository.save(new Notification(user, type, message));
    }

    @Transactional
    public void notifyTransferSent(User sender, BigDecimal amount, String recipientAccountNumber) {
        create(sender, Notification.Type.TRANSFER_SENT,
                "You sent R" + amount.toPlainString() + " to " + recipientAccountNumber + " 💸");
    }

    @Transactional
    public void notifyTransferReceived(User recipient, BigDecimal amount, String senderAccountNumber) {
        create(recipient, Notification.Type.TRANSFER_RECEIVED,
                "Money moved! R" + amount.toPlainString() + " landed from " + senderAccountNumber + " 🤑");
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countUnread(userId);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException("We couldn't find that notification"));
        notification.markRead();
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllRead(userId);
    }
}