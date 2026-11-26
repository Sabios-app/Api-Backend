package br.com.sabios.api.mentoring.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateMentoringRequest(
        @NotNull(message = "O ID do especialista é obrigatório.")
        UUID specialistId,

        @NotBlank(message = "O tema/título da mentoria é obrigatório.")
        @Size(max = 200, message = "O tema não pode exceder 200 caracteres.")
        String title,

        String description,

        String doubts,

        @NotNull(message = "A data e hora da mentoria são obrigatórias.")
        @Future(message = "A data/hora agendada deve ser futura.")
        OffsetDateTime scheduledAt
) {}
