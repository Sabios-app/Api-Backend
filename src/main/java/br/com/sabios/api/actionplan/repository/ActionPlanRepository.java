package br.com.sabios.api.actionplan.repository;

import br.com.sabios.api.actionplan.domain.ActionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActionPlanRepository extends JpaRepository<ActionPlan, UUID> {
    List<ActionPlan> findByClientIdOrderByCreatedAtDesc(UUID clientId);
}
