package br.com.sabios.api.auth.notification;

import br.com.sabios.api.users.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Uso exclusivo de desenvolvimento (MAIL_ENABLED=false): escreve o link no log em vez de enviar e-mail.
 * Em produção, habilite o envio SMTP, pois este log expõe o token.
 */
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetNotifier.class);

    @Override
    public void send(User user, String resetLink) {
        log.warn("[DEV] E-mail desabilitado. Link de redefinição de senha para {}: {}", user.getEmail(), resetLink);
    }
}
