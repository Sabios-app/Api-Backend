package br.com.sabios.api.messages.dto;

import br.com.sabios.api.messages.domain.Message;
import br.com.sabios.api.users.dto.UserResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UserResponse sender,
        String content,
        Boolean isRead,
        OffsetDateTime createdAt
) {
    public static MessageResponse fromEntity(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getConversation().getId(),
                UserResponse.fromEntity(m.getSender()),
                m.getContent(),
                m.getIsRead(),
                m.getCreatedAt()
        );
    }
}
