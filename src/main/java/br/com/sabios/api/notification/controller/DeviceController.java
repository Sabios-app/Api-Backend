package br.com.sabios.api.notification.controller;

import br.com.sabios.api.notification.dto.DeviceTokenResponse;
import br.com.sabios.api.notification.dto.RegisterTokenRequest;
import br.com.sabios.api.notification.service.DeviceTokenService;
import br.com.sabios.api.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dispositivos", description = "Registro e gestão de tokens FCM para push notifications")
public class DeviceController {

    private static final Logger log = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceTokenService deviceTokenService;

    public DeviceController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @PostMapping("/token")
    @Operation(summary = "Registra ou atualiza o token FCM do dispositivo do usuário logado")
    public ResponseEntity<DeviceTokenResponse> registerToken(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RegisterTokenRequest request
    ) {
        log.info("[FCM] Requisição recebida em POST /api/v1/devices/token - Usuário: {} ({}) | Token FCM: {} | Dispositivo: {}",
                user.getId(), user.getEmail(), request.fcmToken(), request.deviceInfo());
        DeviceTokenResponse response = deviceTokenService.registerToken(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/token")
    @Operation(summary = "Desativa o token FCM (ex: logout)")
    public ResponseEntity<Map<String, String>> deactivateToken(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body
    ) {
        String fcmToken = body.get("fcmToken");
        log.info("[FCM] Requisição recebida em DELETE /api/v1/devices/token para usuário: {} - token: {}", 
                user != null ? user.getId() : "anônimo", fcmToken);
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("[FCM] Falha ao desativar token FCM: token ausente ou em branco na requisição.");
            return ResponseEntity.badRequest().body(Map.of("message", "fcmToken é obrigatório."));
        }
        deviceTokenService.deactivateToken(fcmToken, user);
        return ResponseEntity.ok(Map.of("message", "Token desativado com sucesso."));
    }

    @DeleteMapping("/tokens/all")
    @Operation(summary = "Desativa todos os tokens FCM do usuário autenticado (logout de todos os aparelhos)")
    public ResponseEntity<Map<String, String>> deactivateAllTokens(@AuthenticationPrincipal User user) {
        deviceTokenService.deactivateAllUserTokens(user);
        return ResponseEntity.ok(Map.of("message", "Todos os dispositivos foram desativados."));
    }

    @GetMapping("/me")
    @Operation(summary = "Lista os dispositivos registrados do usuário logado")
    public ResponseEntity<List<DeviceTokenResponse>> getMyDevices(
            @AuthenticationPrincipal User user
    ) {
        log.info("[FCM] Requisição recebida em GET /api/v1/devices/me - Usuário: {} ({})", user.getId(), user.getEmail());
        List<DeviceTokenResponse> devices = deviceTokenService.getMyDevices(user);
        log.info("[FCM] Retornando {} dispositivo(s) para o usuário {}", devices.size(), user.getId());
        return ResponseEntity.ok(devices);
    }
}
