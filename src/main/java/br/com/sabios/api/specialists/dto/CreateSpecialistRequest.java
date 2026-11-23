package br.com.sabios.api.specialists.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateSpecialistRequest(
        @NotBlank(message = "O cargo/título é obrigatório.")
        @Size(max = 150, message = "O título não pode exceder 150 caracteres.")
        String title,

        @NotBlank(message = "A especialidade é obrigatória.")
        @Size(max = 150, message = "A especialidade não pode exceder 150 caracteres.")
        String specialty,

        String bio,

        @DecimalMin(value = "0.0", inclusive = true, message = "O valor/hora não pode ser negativo.")
        BigDecimal hourlyRate
) {}
