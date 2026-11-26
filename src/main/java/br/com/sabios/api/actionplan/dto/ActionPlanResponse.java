package br.com.sabios.api.actionplan.dto;

import br.com.sabios.api.actionplan.domain.ActionPlan;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ActionPlanResponse(
        UUID id,
        String title,
        String deadline,
        Boolean completed,
        String conselheiroNome,
        OffsetDateTime createdAt
) {
    public static ActionPlanResponse fromEntity(ActionPlan actionPlan) {
        return new ActionPlanResponse(
                actionPlan.getId(),
                actionPlan.getTitle(),
                actionPlan.getDeadline(),
                actionPlan.getCompleted(),
                actionPlan.getConselheiroNome(),
                actionPlan.getCreatedAt()
        );
    }
}
