package br.com.sabios.api.notifications.controller;

import br.com.sabios.api.notifications.dto.NotificationResponse;
import br.com.sabios.api.notifications.service.NotificationService;
import br.com.sabios.api.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notificações", description = "Notificações do usuário logado (ex.: nova solicitação de mentoria)")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Lista as notificações do usuário logado, da mais recente para a mais antiga")
    public ResponseEntity<Page<NotificationResponse>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(notificationService.list(user, unreadOnly, pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Quantidade de notificações não lidas (para o contador do sino)")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(user)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marca uma notificação como lida")
    public ResponseEntity<NotificationResponse> markAsRead(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.markAsRead(user, id));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Marca todas as notificações do usuário como lidas")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("updated", notificationService.markAllAsRead(user)));
    }
}
