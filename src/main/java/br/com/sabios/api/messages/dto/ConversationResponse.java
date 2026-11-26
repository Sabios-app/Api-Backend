package br.com.sabios.api.messages.dto;

import br.com.sabios.api.messages.domain.Conversation;
import br.com.sabios.api.specialists.dto.SpecialistResponse;
import br.com.sabios.api.users.dto.UserResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UserResponse client,
        SpecialistResponse specialist,
        String lastMessage,
        OffsetDateTime lastMessageAt,
        OffsetDateTime createdAt
) {
    public static ConversationResponse fromEntity(Conversation c) {
        return new ConversationResponse(
                c.getId(),
                UserResponse.fromEntity(c.getClient()),
                SpecialistResponse.fromEntity(c.getSpecialist()),
                c.getLastMessage(),
                c.getLastMessageAt(),
                c.getCreatedAt()
        );
    }
}
