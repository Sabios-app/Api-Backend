package br.com.sabios.api.actionplan.controller;

import br.com.sabios.api.actionplan.dto.ActionPlanResponse;
import br.com.sabios.api.actionplan.dto.CreateActionPlanRequest;
import br.com.sabios.api.actionplan.service.ActionPlanService;
import br.com.sabios.api.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/action-plans")
@Tag(name = "Planos de Ação", description = "Endpoints para gerenciamento de metas e tarefas do cliente")
@SecurityRequirement(name = "bearerAuth")
public class ActionPlanController {

    private final ActionPlanService actionPlanService;

    public ActionPlanController(ActionPlanService actionPlanService) {
        this.actionPlanService = actionPlanService;
    }

    @GetMapping
    @Operation(summary = "Retorna a lista de tarefas/metas do cliente autenticado")
    public ResponseEntity<List<ActionPlanResponse>> listMyActionPlans(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(actionPlanService.listClientActionPlans(user));
    }

    @PostMapping
    @Operation(summary = "Cria uma nova meta/tarefa no plano de ação do cliente logado")
    public ResponseEntity<ActionPlanResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateActionPlanRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(actionPlanService.createActionPlan(user, request));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Alterna o status de conclusão da tarefa (concluída / pendente)")
    public ResponseEntity<ActionPlanResponse> toggle(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(actionPlanService.toggleStatus(user, id));
    }
}
