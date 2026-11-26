package br.com.sabios.api.actionplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Tarefa que o conselheiro atribui ao cliente de uma mentoria. */
public record AssignActionPlanRequest(
        @NotBlank(message = "O título da tarefa é obrigatório.")
        @Size(max = 255, message = "O título não pode exceder 255 caracteres.")
        String title,

        @Size(max = 100, message = "O prazo em texto não pode exceder 100 caracteres.")
        String deadline,

        LocalDate dueDate
) {}
