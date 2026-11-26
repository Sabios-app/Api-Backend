package br.com.sabios.api.notification.dto;

import java.util.List;

public record PushNotificationResponse(
        int successCount,
        int failureCount,
        List<String> messageIds
) {}
