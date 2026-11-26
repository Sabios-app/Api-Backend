package br.com.sabios.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "O token é obrigatório.")
        @Size(max = 200, message = "Token inválido.")
        String token,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 6, max = 100, message = "A senha deve ter no mínimo 6 caracteres.")
        String newPassword
) {}
