package br.com.sabios.api.mentoring.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corpo opcional do aceite: o conselheiro pode já informar o link da videochamada. */
public record AcceptMentoringRequest(
        @Size(max = 500, message = "O link não pode exceder 500 caracteres.")
        @Pattern(regexp = "^https://\\S+$", message = "Informe um link válido começando com https://")
        String meetingLink
) {}
