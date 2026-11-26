package br.com.sabios.api.messages.repository;

import br.com.sabios.api.messages.domain.Conversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @EntityGraph(attributePaths = {"client", "specialist", "specialist.user"})
    List<Conversation> findByClientIdOrderByLastMessageAtDesc(UUID clientId);

    @EntityGraph(attributePaths = {"client", "specialist", "specialist.user"})
    List<Conversation> findBySpecialistIdOrderByLastMessageAtDesc(UUID specialistId);

    @EntityGraph(attributePaths = {"client", "specialist", "specialist.user"})
    Optional<Conversation> findByClientIdAndSpecialistId(UUID clientId, UUID specialistId);

    @Override
    @EntityGraph(attributePaths = {"client", "specialist", "specialist.user"})
    Optional<Conversation> findById(UUID id);
}
