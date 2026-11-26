package br.com.sabios.api.notification.service;

import br.com.sabios.api.notification.domain.DeviceToken;
import br.com.sabios.api.notification.dto.DeviceTokenResponse;
import br.com.sabios.api.notification.dto.RegisterTokenRequest;
import br.com.sabios.api.notification.repository.DeviceTokenRepository;
import br.com.sabios.api.users.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DeviceTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenService.class);

    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    /**
     * Registra ou atualiza um token FCM para o usuário autenticado.
     * Se o token já existir para outro usuário, ele é reatribuído ao usuário atual
     * (cenário: mesmo dispositivo, login com conta diferente).
     */
    @Transactional
    public DeviceTokenResponse registerToken(User user, RegisterTokenRequest request) {
        log.info("[FCM] Processando registro/atualização de token FCM para o usuário ID: {} | DeviceInfo: '{}' | Token: {}", 
                user.getId(), request.deviceInfo(), request.fcmToken());

        DeviceToken deviceToken = deviceTokenRepository.findByFcmToken(request.fcmToken())
                .map(existing -> {
                    // Token já existe — atualiza o owner e reativa
                    existing.setUser(user);
                    existing.setDeviceInfo(request.deviceInfo());
                    existing.setActive(true);
                    log.info("[FCM] Token FCM existente encontrado (ID: {}). Reatribuído ao usuário ID: {} e reativado.", 
                            existing.getId(), user.getId());
                    return existing;
                })
                .orElseGet(() -> {
                    // Novo token
                    DeviceToken newToken = DeviceToken.builder()
                            .user(user)
                            .fcmToken(request.fcmToken())
                            .deviceInfo(request.deviceInfo())
                            .active(true)
                            .build();
                    log.info("[FCM] Criando novo registro de DeviceToken para o usuário ID: {}", user.getId());
                    return newToken;
                });

        DeviceToken saved = deviceTokenRepository.save(deviceToken);
        log.info("[FCM] Token FCM persistido com sucesso no banco de dados. ID: {}, Ativo: {}, UserID: {}", 
                saved.getId(), saved.getActive(), saved.getUser().getId());
        return DeviceTokenResponse.fromEntity(saved);
    }

    /**
     * Remove (desativa) um token FCM — ex: quando o usuário faz logout.
     */
    @Transactional
    public void deactivateToken(String fcmToken) {
        log.info("[FCM] Tentando desativar token FCM: {}", fcmToken);
        deviceTokenRepository.findByFcmToken(fcmToken)
                .ifPresentOrElse(token -> {
                    token.setActive(false);
                    deviceTokenRepository.save(token);
                    log.info("[FCM] Token FCM ID: {} (UserID: {}) foi desativado com sucesso.", 
                            token.getId(), token.getUser() != null ? token.getUser().getId() : "null");
                }, () -> {
                    log.warn("[FCM] Token FCM não encontrado no banco para desativação: {}", fcmToken);
                });
    }

    /**
     * Retorna todos os tokens ativos registrados no sistema (para broadcast).
     */
    @Transactional(readOnly = true)
    public List<String> getAllActiveTokens() {
        List<String> tokens = deviceTokenRepository.findByActiveTrue()
                .stream()
                .map(DeviceToken::getFcmToken)
                .toList();
        log.info("[FCM] Consulta de todos os tokens ativos no sistema - Encontrados {} token(s).", tokens.size());
        return tokens;
    }

    /**
     * Retorna todos os tokens ativos de um usuário.
     */
    @Transactional(readOnly = true)
    public List<String> getActiveTokensByUserId(UUID userId) {
        List<String> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(DeviceToken::getFcmToken)
                .toList();
        log.info("[FCM] Consulta de tokens ativos para usuário ID: {} - Encontrados {} token(s).", userId, tokens.size());
        return tokens;
    }

    /**
     * Lista os dispositivos registrados do usuário autenticado.
     */
    @Transactional(readOnly = true)
    public List<DeviceTokenResponse> getMyDevices(User user) {
        return deviceTokenRepository.findByUserIdAndActiveTrue(user.getId())
                .stream()
                .map(DeviceTokenResponse::fromEntity)
                .toList();
    }
}
