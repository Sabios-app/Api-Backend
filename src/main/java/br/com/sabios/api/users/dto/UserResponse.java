package br.com.sabios.api.users.dto;

import br.com.sabios.api.users.domain.Role;
import br.com.sabios.api.users.domain.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        String phone,
        String avatarUrl,
        OffsetDateTime createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getCreatedAt()
        );
    }
}
