package br.com.sabios.api.notifications.service;

import br.com.sabios.api.notifications.domain.Notification;
import br.com.sabios.api.notifications.domain.NotificationType;
import br.com.sabios.api.notifications.dto.NotificationResponse;
import br.com.sabios.api.notifications.repository.NotificationRepository;
import br.com.sabios.api.users.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /** Cria uma notificação na mesma transação do fato que a originou. */
    @Transactional
    public void notify(User recipient, NotificationType type, String title, String message, UUID mentoringId) {
        notificationRepository.save(Notification.builder()
                .user(recipient)
                .type(type)
                .title(title)
                .message(message)
                .mentoringId(mentoringId)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(User user, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByUserIdAndReadAtIsNull(user.getId(), pageable)
                : notificationRepository.findByUserId(user.getId(), pageable);
        return page.map(NotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public long countUnread(User user) {
        return notificationRepository.countByUserIdAndReadAtIsNull(user.getId());
    }

    @Transactional
    public NotificationResponse markAsRead(User user, UUID id) {
        // Notificação de outro usuário é tratada como inexistente, sem revelar que ela existe.
        Notification notification = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NoSuchElementException("Notificação não encontrada."));
        if (notification.getReadAt() == null) {
            notification.setReadAt(OffsetDateTime.now());
        }
        return NotificationResponse.fromEntity(notification);
    }

    @Transactional
    public int markAllAsRead(User user) {
        return notificationRepository.markAllAsRead(user.getId(), OffsetDateTime.now());
    }
}
