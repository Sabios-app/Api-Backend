package br.com.sabios.api.auth.dto;

import br.com.sabios.api.users.dto.UserResponse;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInHours,
        UserResponse user
) {}
