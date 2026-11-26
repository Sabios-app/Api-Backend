package br.com.sabios.api.notification.dto;

import java.util.List;

public record NotificationResponse(
        int successCount,
        int failureCount,
        List<String> messageIds
) {}
