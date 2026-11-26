package br.com.sabios.api.actionplan.repository;

import br.com.sabios.api.actionplan.domain.ActionPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionPlanRepository extends JpaRepository<ActionPlan, UUID> {

    @EntityGraph(attributePaths = {"specialist", "specialist.user"})
    List<ActionPlan> findByClientIdOrderByCreatedAtDesc(UUID clientId);

    /** Tarefas que um conselheiro atribuiu (a qualquer cliente). */
    @EntityGraph(attributePaths = {"specialist", "specialist.user"})
    List<ActionPlan> findBySpecialistIdAndCreatedByIdOrderByCreatedAtDesc(UUID specialistId, UUID createdById);

    @EntityGraph(attributePaths = {"specialist", "specialist.user"})
    List<ActionPlan> findByMentoringIdOrderByCreatedAtDesc(UUID mentoringId);

    @Override
    @EntityGraph(attributePaths = {"specialist", "specialist.user"})
    Optional<ActionPlan> findById(UUID id);
}
