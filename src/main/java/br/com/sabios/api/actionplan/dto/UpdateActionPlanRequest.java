package br.com.sabios.api.actionplan.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Todos os campos são opcionais: só o que for enviado é alterado. */
public record UpdateActionPlanRequest(
        @Size(max = 255, message = "O título não pode exceder 255 caracteres.")
        String title,

        @Size(max = 100, message = "O prazo em texto não pode exceder 100 caracteres.")
        String deadline,

        LocalDate dueDate
) {}
