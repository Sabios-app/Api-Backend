package br.com.sabios.api.actionplan.dto;

import br.com.sabios.api.actionplan.domain.ActionPlan;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ActionPlanResponse(
        UUID id,
        String title,
        /** Prazo em texto (mantido por compatibilidade). Prefira {@code dueDate}. */
        String deadline,
        Boolean completed,
        String conselheiroNome,
        OffsetDateTime createdAt,
        UUID mentoringId,
        UUID specialistId,
        LocalDate dueDate,
        OffsetDateTime completedAt,
        /** Pendente e com prazo (dueDate) já vencido. */
        boolean overdue,
        /** true quando a tarefa foi atribuída pelo conselheiro, e não criada pelo próprio cliente. */
        boolean assignedByCounselor
) {
    public static ActionPlanResponse fromEntity(ActionPlan plan, LocalDate today) {
        boolean completed = Boolean.TRUE.equals(plan.getCompleted());
        String counselorName = plan.getSpecialist() != null
                ? plan.getSpecialist().getUser().getName()
                : plan.getConselheiroNome();
        boolean assignedByCounselor = plan.getCreatedBy() != null
                && !plan.getCreatedBy().getId().equals(plan.getClient().getId());

        return new ActionPlanResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getDeadline(),
                plan.getCompleted(),
                counselorName,
                plan.getCreatedAt(),
                plan.getMentoring() != null ? plan.getMentoring().getId() : null,
                plan.getSpecialist() != null ? plan.getSpecialist().getId() : null,
                plan.getDueDate(),
                plan.getCompletedAt(),
                !completed && plan.getDueDate() != null && plan.getDueDate().isBefore(today),
                assignedByCounselor
        );
    }
}
