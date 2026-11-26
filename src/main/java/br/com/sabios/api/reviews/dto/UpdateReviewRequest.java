package br.com.sabios.api.reviews.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Campos opcionais: só o que for enviado é alterado. Comentário vazio remove o comentário. */
public record UpdateReviewRequest(
        @Min(value = 1, message = "A nota mínima é 1.")
        @Max(value = 5, message = "A nota máxima é 5.")
        Integer rating,

        @Size(max = 1000, message = "O comentário não pode exceder 1000 caracteres.")
        String comment
) {}
