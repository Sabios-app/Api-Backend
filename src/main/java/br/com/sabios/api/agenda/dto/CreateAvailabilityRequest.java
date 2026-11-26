package br.com.sabios.api.agenda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record CreateAvailabilityRequest(
        @NotNull(message = "O dia da semana é obrigatório.")
        @Schema(description = "Dia da semana em inglês, em maiúsculas", example = "MONDAY")
        DayOfWeek dayOfWeek,

        @NotNull(message = "O horário de início é obrigatório.")
        @Schema(type = "string", example = "09:00")
        LocalTime startTime,

        @NotNull(message = "O horário de término é obrigatório.")
        @Schema(type = "string", example = "12:00")
        LocalTime endTime
) {}
