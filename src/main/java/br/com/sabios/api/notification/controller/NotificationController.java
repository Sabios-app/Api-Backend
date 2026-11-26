package br.com.sabios.api.notification.controller;

import br.com.sabios.api.notification.dto.BroadcastNotificationRequest;
import br.com.sabios.api.notification.dto.NotificationResponse;
import br.com.sabios.api.notification.dto.SendNotificationRequest;
import br.com.sabios.api.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notificações", description = "Envio de push notifications via Firebase Cloud Messaging")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Envia push notification para um usuário específico (somente ADMIN)")
    public ResponseEntity<NotificationResponse> sendToUser(
            @Valid @RequestBody SendNotificationRequest request
    ) {
        NotificationResponse response = notificationService.sendToUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Envia push notification para TODOS os dispositivos cadastrados no sistema (somente ADMIN)")
    public ResponseEntity<NotificationResponse> sendBroadcast(
            @Valid @RequestBody BroadcastNotificationRequest request
    ) {
        NotificationResponse response = notificationService.sendBroadcast(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send/topic")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Envia push notification para um tópico (somente ADMIN)")
    public ResponseEntity<Map<String, String>> sendToTopic(
            @RequestParam String topic,
            @RequestParam String title,
            @RequestParam String body
    ) {
        String messageId = notificationService.sendToTopic(topic, title, body, null);
        return ResponseEntity.ok(Map.of("messageId", messageId));
    }
}
