package br.com.sabios.api.messages.repository;

import br.com.sabios.api.messages.domain.Message;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @EntityGraph(attributePaths = {"sender", "conversation"})
    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
}
