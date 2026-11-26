package br.com.sabios.api.users.dto;

import br.com.sabios.api.users.domain.User;

import java.util.UUID;

/** Dados de um usuário que podem aparecer para terceiros (sem e-mail, telefone, perfil ou datas internas). */
public record PublicUserResponse(
        UUID id,
        String name,
        String avatarUrl
) {
    public static PublicUserResponse fromEntity(User user) {
        return new PublicUserResponse(user.getId(), user.getName(), user.getAvatarUrl());
    }
}
