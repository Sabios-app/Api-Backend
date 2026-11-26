package br.com.sabios.api.messages.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendMessageRequest(
        @NotNull(message = "O ID do especialista é obrigatório para iniciar conversa.")
        UUID specialistId,

        @NotBlank(message = "O conteúdo da mensagem não pode ser vazio.")
        String content
) {}
