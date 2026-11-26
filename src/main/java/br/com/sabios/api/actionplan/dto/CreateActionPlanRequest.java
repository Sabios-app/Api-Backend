package br.com.sabios.api.actionplan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateActionPlanRequest(
        @NotBlank(message = "O título da tarefa/meta é obrigatório.")
        @Size(max = 255, message = "O título não pode exceder 255 caracteres.")
        String title,

        @Size(max = 100, message = "O prazo em texto não pode exceder 100 caracteres.")
        String deadline,

        @Size(max = 255, message = "O nome do conselheiro não pode exceder 255 caracteres.")
        String conselheiroNome,

        /** Prazo estruturado (AAAA-MM-DD). Não pode estar no passado. */
        LocalDate dueDate,

        /** Mentoria de origem (opcional): precisa ser do cliente e estar confirmada ou concluída. */
        UUID mentoringId
) {}
