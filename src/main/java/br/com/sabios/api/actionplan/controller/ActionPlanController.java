package br.com.sabios.api.actionplan.controller;

import br.com.sabios.api.actionplan.dto.ActionPlanResponse;
import br.com.sabios.api.actionplan.dto.AssignActionPlanRequest;
import br.com.sabios.api.actionplan.dto.CreateActionPlanRequest;
import br.com.sabios.api.actionplan.dto.UpdateActionPlanRequest;
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
@Tag(name = "Planos de Ação", description = "Metas e tarefas do cliente, que o conselheiro também pode atribuir a partir de uma mentoria")
@SecurityRequirement(name = "bearerAuth")
public class ActionPlanController {

    private final ActionPlanService actionPlanService;

    public ActionPlanController(ActionPlanService actionPlanService) {
        this.actionPlanService = actionPlanService;
    }

    @GetMapping
    @Operation(summary = "Cliente: lista as próprias tarefas. Conselheiro: lista as tarefas que atribuiu. "
            + "Filtros opcionais: completed (histórico = true), overdue e mentoringId")
    public ResponseEntity<List<ActionPlanResponse>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) UUID mentoringId
    ) {
        return ResponseEntity.ok(actionPlanService.list(user, completed, overdue, mentoringId));
    }

    @GetMapping("/mentorings/{mentoringId}")
    @Operation(summary = "Tarefas de uma mentoria, visíveis para o cliente e para o conselheiro participantes")
    public ResponseEntity<List<ActionPlanResponse>> listByMentoring(
            @AuthenticationPrincipal User user,
            @PathVariable UUID mentoringId
    ) {
        return ResponseEntity.ok(actionPlanService.listByMentoring(user, mentoringId));
    }

    @PostMapping
    @Operation(summary = "Cliente cria uma meta/tarefa no próprio plano de ação (mentoringId e dueDate opcionais)")
    public ResponseEntity<ActionPlanResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateActionPlanRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(actionPlanService.createActionPlan(user, request));
    }

    @PostMapping("/mentorings/{mentoringId}")
    @Operation(summary = "Conselheiro atribui uma tarefa ao cliente de uma mentoria confirmada ou concluída; o cliente é notificado")
    public ResponseEntity<ActionPlanResponse> assign(
            @AuthenticationPrincipal User user,
            @PathVariable UUID mentoringId,
            @Valid @RequestBody AssignActionPlanRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(actionPlanService.assignToClient(user, mentoringId, request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edita título e prazo. Apenas quem criou a tarefa")
    public ResponseEntity<ActionPlanResponse> update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateActionPlanRequest request
    ) {
        return ResponseEntity.ok(actionPlanService.update(user, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a tarefa. Apenas quem a criou")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        actionPlanService.delete(user, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Cliente alterna a tarefa entre concluída e pendente (registra o momento da conclusão)")
    public ResponseEntity<ActionPlanResponse> toggle(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(actionPlanService.toggleStatus(user, id));
    }
}
