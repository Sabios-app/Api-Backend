package br.com.sabios.api.notifications.dto;

import br.com.sabios.api.notifications.domain.Notification;
import br.com.sabios.api.notifications.domain.NotificationType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        UUID mentoringId,
        boolean read,
        OffsetDateTime createdAt
) {
    public static NotificationResponse fromEntity(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getMentoringId(),
                notification.getReadAt() != null,
                notification.getCreatedAt()
        );
    }
}
