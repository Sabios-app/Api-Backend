package br.com.sabios.api.actionplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateActionPlanRequest(
        @NotBlank(message = "O título da tarefa/meta é obrigatório.")
        @Size(max = 255, message = "O título não pode exceder 255 caracteres.")
        String title,

        String deadline,

        String conselheiroNome
) {}
