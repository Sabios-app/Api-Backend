package br.com.sabios.api.notification.controller;

import br.com.sabios.api.notification.dto.BroadcastNotificationRequest;
import br.com.sabios.api.notification.dto.PushNotificationResponse;
import br.com.sabios.api.notification.dto.SendPushNotificationRequest;
import br.com.sabios.api.notification.service.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/push-notifications")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Push Notifications", description = "Envio de push notifications via Firebase Cloud Messaging")
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;

    public PushNotificationController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Envia push notification para um usuário específico (somente ADMIN)")
    public ResponseEntity<PushNotificationResponse> sendToUser(
            @Valid @RequestBody SendPushNotificationRequest request
    ) {
        PushNotificationResponse response = pushNotificationService.sendToUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Envia push notification para TODOS os dispositivos cadastrados no sistema (somente ADMIN)")
    public ResponseEntity<PushNotificationResponse> sendBroadcast(
            @Valid @RequestBody BroadcastNotificationRequest request
    ) {
        PushNotificationResponse response = pushNotificationService.sendBroadcast(request);
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
        String messageId = pushNotificationService.sendToTopic(topic, title, body, null);
        return ResponseEntity.ok(Map.of("messageId", messageId));
    }
}
