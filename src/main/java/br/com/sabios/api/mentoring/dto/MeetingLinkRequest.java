package br.com.sabios.api.mentoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Link da videochamada informado pelo conselheiro (Google Meet, Zoom, etc.). */
public record MeetingLinkRequest(
        @NotBlank(message = "O link da reunião é obrigatório.")
        @Size(max = 500, message = "O link não pode exceder 500 caracteres.")
        @Pattern(regexp = "^https://\\S+$", message = "Informe um link válido começando com https://")
        String meetingLink
) {}
