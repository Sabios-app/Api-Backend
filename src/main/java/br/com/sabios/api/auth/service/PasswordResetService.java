package br.com.sabios.api.auth.service;

import br.com.sabios.api.auth.domain.PasswordResetToken;
import br.com.sabios.api.auth.notification.PasswordResetNotifier;
import br.com.sabios.api.auth.repository.PasswordResetTokenRepository;
import br.com.sabios.api.shared.exception.BadRequestException;
import br.com.sabios.api.users.domain.User;
import br.com.sabios.api.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetNotifier notifier;
    private final long expirationMinutes;
    private final String urlTemplate;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetNotifier notifier,
            @Value("${app.password-reset.expiration-minutes}") long expirationMinutes,
            @Value("${app.password-reset.url-template}") String urlTemplate
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.notifier = notifier;
        this.expirationMinutes = expirationMinutes;
        this.urlTemplate = urlTemplate;
    }

    /**
     * Gera o token e dispara a notificação. Não revela se o e-mail existe: o resultado é o mesmo
     * para e-mails cadastrados e não cadastrados.
     */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email.trim()).ifPresent(user -> {
            tokenRepository.deleteByUserId(user.getId());

            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            tokenRepository.save(PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(hash(rawToken))
                    .expiresAt(OffsetDateTime.now().plusMinutes(expirationMinutes))
                    .build());

            deliver(user, rawToken);
        });
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken.trim()))
                .orElseThrow(() -> new BadRequestException("Token inválido ou expirado."));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Token inválido ou expirado.");
        }

        User user = token.getUser();
        OffsetDateTime now = OffsetDateTime.now();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(now); // invalida todos os JWT emitidos antes da redefinição
        token.setUsedAt(now);
    }

    private void deliver(User user, String rawToken) {
        try {
            notifier.send(user, String.format(urlTemplate, rawToken));
        } catch (RuntimeException ex) {
            // Falha de envio não pode revelar ao cliente que o e-mail existe nem derrubar a requisição.
            log.error("Falha ao enviar e-mail de redefinição de senha para o usuário {}", user.getId(), ex);
        }
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
