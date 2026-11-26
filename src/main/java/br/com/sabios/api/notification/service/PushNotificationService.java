package br.com.sabios.api.notification.service;

import br.com.sabios.api.notification.dto.BroadcastNotificationRequest;
import br.com.sabios.api.notification.dto.PushNotificationResponse;
import br.com.sabios.api.notification.dto.SendPushNotificationRequest;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final DeviceTokenService deviceTokenService;

    public PushNotificationService(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    /**
     * Envia push notification para todos os dispositivos ativos de um usuário.
     */
    public PushNotificationResponse sendToUser(SendPushNotificationRequest request) {
        List<String> tokens = deviceTokenService.getActiveTokensByUserId(request.userId());

        if (tokens.isEmpty()) {
            log.warn("Nenhum token FCM ativo encontrado para o usuário {}", request.userId());
            return new PushNotificationResponse(0, 0, List.of());
        }

        return sendToTokens(tokens, request.title(), request.body(), request.data());
    }

    /**
     * Envia push notification em massa (broadcast) para todos os dispositivos ativos da base.
     * Divide automaticamente em lotes de até 500 tokens (limite da API Multicast do FCM).
     */
    public PushNotificationResponse sendBroadcast(BroadcastNotificationRequest request) {
        List<String> tokens = deviceTokenService.getAllActiveTokens();

        if (tokens.isEmpty()) {
            log.warn("[FCM] Nenhum token ativo encontrado para envio em massa.");
            return new PushNotificationResponse(0, 0, List.of());
        }

        log.info("[FCM] Iniciando envio em massa para {} dispositivo(s) ativos.", tokens.size());

        int totalSuccess = 0;
        int totalFailure = 0;
        List<String> allMessageIds = new ArrayList<>();

        // Limite da API FCM sendEachForMulticast é 500 tokens por requisição
        int batchSize = 500;
        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batch = tokens.subList(i, Math.min(i + batchSize, tokens.size()));
            PushNotificationResponse batchResponse = sendToTokens(batch, request.title(), request.body(), request.data());
            totalSuccess += batchResponse.successCount();
            totalFailure += batchResponse.failureCount();
            allMessageIds.addAll(batchResponse.messageIds());
        }

        log.info("[FCM] Broadcast finalizado. Total de sucessos: {}, falhas: {}", totalSuccess, totalFailure);
        return new PushNotificationResponse(totalSuccess, totalFailure, allMessageIds);
    }

    /**
     * Envia push notification para um único token (uso interno).
     */
    public String sendToToken(String token, String title, String body, Map<String, String> data) {
        Message.Builder builder = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("click_action", "FLUTTER_NOTIFICATION_CLICK");

        if (data != null) {
            builder.putAllData(data);
        }

        try {
            String response = FirebaseMessaging.getInstance().send(builder.build());
            log.info("Push enviado com sucesso: {}", response);
            return response;
        } catch (FirebaseMessagingException e) {
            log.error("Erro ao enviar push para token {}: {}", token, e.getMessage());

            // Se o token não é mais válido, desativa
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                deviceTokenService.deactivateToken(token);
                log.info("Token inválido desativado: {}", token);
            }
            throw new RuntimeException("Falha ao enviar notificação: " + e.getMessage(), e);
        }
    }

    /**
     * Envia push notification para múltiplos tokens.
     * Quando houver apenas 1 token, utiliza diretamente FirebaseMessaging.getInstance().send(message),
     * evitando o bug conhecido de descompressão GZIP da API em lote (sendEachForMulticast).
     */
    public PushNotificationResponse sendToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens.isEmpty()) {
            return new PushNotificationResponse(0, 0, List.of());
        }

        List<String> messageIds = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        // Se for um ou poucos tokens, usa send individual direto (evita o problema do GZIP e multicast do HTTP v1)
        for (String token : tokens) {
            try {
                Message.Builder builder = Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("click_action", "FLUTTER_NOTIFICATION_CLICK");

                if (data != null) {
                    builder.putAllData(data);
                }

                String messageId = FirebaseMessaging.getInstance().send(builder.build());
                messageIds.add(messageId);
                successCount++;
                log.info("[FCM] Push enviado com sucesso para o token [{}...]: ID {}", 
                        token.substring(0, Math.min(token.length(), 15)), messageId);
            } catch (FirebaseMessagingException ex) {
                failureCount++;
                log.error("[FCM] Falha ao enviar para o token [{}...]: {}", 
                        token.substring(0, Math.min(token.length(), 15)), ex.getMessage());

                if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    deviceTokenService.deactivateToken(token);
                    log.info("[FCM] Token desativado por estar inválido/desregistrado: {}", token);
                }
            } catch (Exception ex) {
                failureCount++;
                log.error("[FCM] Erro inesperado ao enviar push: {}", ex.getMessage());
            }
        }

        return new PushNotificationResponse(successCount, failureCount, messageIds);
    }

    /**
     * Envia notificação para um tópico (ex: "news", "promotions").
     */
    public String sendToTopic(String topic, String title, String body, Map<String, String> data) {
        Message.Builder builder = Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build());

        if (data != null) {
            builder.putAllData(data);
        }

        try {
            String response = FirebaseMessaging.getInstance().send(builder.build());
            log.info("Push enviado ao tópico '{}': {}", topic, response);
            return response;
        } catch (FirebaseMessagingException e) {
            log.error("Erro ao enviar push ao tópico '{}': {}", topic, e.getMessage());
            throw new RuntimeException("Falha ao enviar notificação ao tópico: " + e.getMessage(), e);
        }
    }
}
