package br.com.sabios.api.notifications.service;

import br.com.sabios.api.notification.dto.SendPushNotificationRequest;
import br.com.sabios.api.notification.service.PushNotificationService;
import br.com.sabios.api.notifications.domain.Notification;
import br.com.sabios.api.notifications.domain.NotificationType;
import br.com.sabios.api.notifications.dto.NotificationResponse;
import br.com.sabios.api.notifications.repository.NotificationRepository;
import br.com.sabios.api.users.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;

    public NotificationService(
            NotificationRepository notificationRepository,
            PushNotificationService pushNotificationService
    ) {
        this.notificationRepository = notificationRepository;
        this.pushNotificationService = pushNotificationService;
    }

    /** Cria uma notificação na mesma transação do fato que a originou e dispara push FCM. */
    @Transactional
    public void notify(User recipient, NotificationType type, String title, String message, UUID mentoringId) {
        notificationRepository.save(Notification.builder()
                .user(recipient)
                .type(type)
                .title(title)
                .message(message)
                .mentoringId(mentoringId)
                .build());

        try {
            pushNotificationService.sendToUser(new SendPushNotificationRequest(
                    recipient.getId(),
                    title,
                    message,
                    Map.of(
                            "type", type != null ? type.name() : "",
                            "mentoringId", mentoringId != null ? mentoringId.toString() : ""
                    )
            ));
        } catch (Exception ex) {
            log.warn("Falha ao enviar push FCM para usuário {}: {}", recipient.getId(), ex.getMessage());
        }
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