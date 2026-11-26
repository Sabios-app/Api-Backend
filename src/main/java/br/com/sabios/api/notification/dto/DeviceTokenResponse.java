package br.com.sabios.api.notification.dto;

import br.com.sabios.api.notification.domain.DeviceToken;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeviceTokenResponse(
        UUID id,
        String fcmToken,
        String deviceInfo,
        Boolean active,
        OffsetDateTime createdAt
) {
    public static DeviceTokenResponse fromEntity(DeviceToken entity) {
        return new DeviceTokenResponse(
                entity.getId(),
                entity.getFcmToken(),
                entity.getDeviceInfo(),
                entity.getActive(),
                entity.getCreatedAt()
        );
    }
}
