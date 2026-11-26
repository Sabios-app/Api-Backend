package br.com.sabios.api.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record SendNotificationRequest(
        @NotNull(message = "O ID do usuário destinatário é obrigatório.")
        UUID userId,

        @NotBlank(message = "O título da notificação é obrigatório.")
        String title,

        @NotBlank(message = "O corpo da notificação é obrigatório.")
        String body,

        Map<String, String> data
) {}
