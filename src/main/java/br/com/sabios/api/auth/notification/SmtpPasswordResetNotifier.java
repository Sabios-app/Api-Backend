package br.com.sabios.api.auth.notification;

import br.com.sabios.api.users.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpPasswordResetNotifier implements PasswordResetNotifier {

    private final JavaMailSender mailSender;
    private final String from;
    private final long expirationMinutes;

    public SmtpPasswordResetNotifier(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.password-reset.expiration-minutes}") long expirationMinutes
    ) {
        this.mailSender = mailSender;
        this.from = from;
        this.expirationMinutes = expirationMinutes;
    }

    @Override
    public void send(User user, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Sábios App - Redefinição de senha");
        message.setText("""
                Olá, %s!

                Recebemos um pedido para redefinir a sua senha. Acesse o link abaixo para criar uma nova senha
                (válido por %d minutos):

                %s

                Se você não fez esse pedido, ignore este e-mail. Sua senha continuará a mesma.
                """.formatted(user.getName(), expirationMinutes, resetLink));
        mailSender.send(message);
    }
}
