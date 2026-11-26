package br.com.sabios.api.agenda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateAvailabilityRequest(
        @NotBlank(message = "O dia da semana é obrigatório.")
        String dayOfWeek,

        @NotNull(message = "O horário de início é obrigatório.")
        LocalTime startTime,

        @NotNull(message = "O horário de término é obrigatório.")
        LocalTime endTime
) {}
