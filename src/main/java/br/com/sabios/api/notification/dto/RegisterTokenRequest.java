package br.com.sabios.api.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterTokenRequest(
        @NotBlank(message = "O token FCM é obrigatório.")
        String fcmToken,

        String deviceInfo
) {}
