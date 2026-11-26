package br.com.sabios.api.notification.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record BroadcastNotificationRequest(
        @NotBlank(message = "O título da notificação é obrigatório.")
        String title,

        @NotBlank(message = "O corpo da notificação é obrigatório.")
        String body,

        Map<String, String> data
) {}
