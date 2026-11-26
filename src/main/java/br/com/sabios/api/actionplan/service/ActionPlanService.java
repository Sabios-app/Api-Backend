package br.com.sabios.api.actionplan.service;

import br.com.sabios.api.actionplan.domain.ActionPlan;
import br.com.sabios.api.actionplan.dto.ActionPlanResponse;
import br.com.sabios.api.actionplan.dto.CreateActionPlanRequest;
import br.com.sabios.api.actionplan.repository.ActionPlanRepository;
import br.com.sabios.api.users.domain.Role;
import br.com.sabios.api.users.domain.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ActionPlanService {

    private final ActionPlanRepository actionPlanRepository;

    public ActionPlanService(ActionPlanRepository actionPlanRepository) {
        this.actionPlanRepository = actionPlanRepository;
    }

    @Transactional(readOnly = true)
    public List<ActionPlanResponse> listClientActionPlans(User user) {
        return actionPlanRepository.findByClientIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(ActionPlanResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ActionPlanResponse createActionPlan(User client, CreateActionPlanRequest request) {
        if (client.getRole() != Role.CLIENT && client.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Apenas clientes podem criar metas no plano de ação.");
        }

        ActionPlan actionPlan = ActionPlan.builder()
                .client(client)
                .title(request.title())
                .deadline(request.deadline() != null && !request.deadline().isBlank() ? request.deadline() : "Sem prazo")
                .conselheiroNome(request.conselheiroNome())
                .completed(false)
                .build();

        ActionPlan saved = actionPlanRepository.save(actionPlan);
        return ActionPlanResponse.fromEntity(saved);
    }

    @Transactional
    public ActionPlanResponse toggleStatus(User user, UUID id) {
        ActionPlan actionPlan = actionPlanRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item do plano de ação não encontrado."));

        if (!actionPlan.getClient().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Você não tem permissão para alterar este plano de ação.");
        }

        actionPlan.setCompleted(!Boolean.TRUE.equals(actionPlan.getCompleted()));
        return ActionPlanResponse.fromEntity(actionPlan);
    }
}
