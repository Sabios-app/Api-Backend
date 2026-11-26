package br.com.sabios.api.messages.service;

import br.com.sabios.api.messages.domain.Conversation;
import br.com.sabios.api.messages.domain.Message;
import br.com.sabios.api.messages.dto.ConversationResponse;
import br.com.sabios.api.messages.dto.MessageResponse;
import br.com.sabios.api.messages.dto.SendMessageRequest;
import br.com.sabios.api.messages.repository.ConversationRepository;
import br.com.sabios.api.messages.repository.MessageRepository;
import br.com.sabios.api.specialists.domain.Specialist;
import br.com.sabios.api.specialists.repository.SpecialistRepository;
import br.com.sabios.api.users.domain.Role;
import br.com.sabios.api.users.domain.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SpecialistRepository specialistRepository;

    public MessageService(ConversationRepository conversationRepository,
                          MessageRepository messageRepository,
                          SpecialistRepository specialistRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.specialistRepository = specialistRepository;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listUserConversations(User user) {
        if (user.getRole() == Role.COUNSELOR) {
            Specialist specialist = specialistRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new NoSuchElementException("Perfil de conselheiro não encontrado."));
            return conversationRepository.findBySpecialistIdOrderByLastMessageAtDesc(specialist.getId())
                    .stream()
                    .map(ConversationResponse::fromEntity)
                    .toList();
        }

        return conversationRepository.findByClientIdOrderByLastMessageAtDesc(user.getId())
                .stream()
                .map(ConversationResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listConversationMessages(User user, UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NoSuchElementException("Conversa não encontrada."));

        validateParticipant(user, conversation);

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(MessageResponse::fromEntity)
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(User sender, SendMessageRequest request) {
        Specialist specialist = specialistRepository.findById(request.specialistId())
                .orElseThrow(() -> new NoSuchElementException("Especialista não encontrado."));

        User client = sender.getRole() == Role.CLIENT ? sender : null;
        if (client == null) {
            throw new IllegalArgumentException("Mensagens iniciais devem ser enviadas por clientes.");
        }

        Conversation conversation = conversationRepository.findByClientIdAndSpecialistId(client.getId(), specialist.getId())
                .orElseGet(() -> {
                    Conversation c = Conversation.builder()
                            .client(client)
                            .specialist(specialist)
                            .lastMessage(request.content())
                            .lastMessageAt(OffsetDateTime.now())
                            .build();
                    return conversationRepository.save(c);
                });

        conversation.setLastMessage(request.content());
        conversation.setLastMessageAt(OffsetDateTime.now());

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.content())
                .isRead(false)
                .build();

        Message saved = messageRepository.save(message);
        return MessageResponse.fromEntity(saved);
    }

    private void validateParticipant(User user, Conversation conversation) {
        boolean isClient = conversation.getClient().getId().equals(user.getId());
        boolean isCounselor = conversation.getSpecialist().getUser().getId().equals(user.getId());
        if (!isClient && !isCounselor && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Você não tem autorização para ler mensagens desta conversa.");
        }
    }
}
