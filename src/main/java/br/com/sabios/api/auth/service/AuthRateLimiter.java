package br.com.sabios.api.auth.service;

import br.com.sabios.api.shared.exception.TooManyRequestsException;
import br.com.sabios.api.shared.ratelimit.AttemptLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Limites de tentativas dos endpoints públicos de autenticação.
 *
 * <ul>
 *   <li>Login: só tentativas <b>falhas</b> contam, por e-mail e por IP. Um login correto zera o e-mail.</li>
 *   <li>Recuperação de senha: toda solicitação conta (por e-mail e por IP), existindo ou não o e-mail,
 *       para o bloqueio não revelar quais e-mails estão cadastrados.</li>
 * </ul>
 */
@Component
public class AuthRateLimiter {

    private final AttemptLimiter loginByEmail;
    private final AttemptLimiter loginByIp;
    private final AttemptLimiter forgotByEmail;
    private final AttemptLimiter forgotByIp;

    public AuthRateLimiter(
            @Value("${app.rate-limit.login-max-failures-per-email}") int loginPerEmail,
            @Value("${app.rate-limit.login-max-failures-per-ip}") int loginPerIp,
            @Value("${app.rate-limit.login-window-minutes}") long loginWindowMinutes,
            @Value("${app.rate-limit.forgot-max-per-email}") int forgotPerEmail,
            @Value("${app.rate-limit.forgot-max-per-ip}") int forgotPerIp,
            @Value("${app.rate-limit.forgot-window-minutes}") long forgotWindowMinutes
    ) {
        Duration loginWindow = Duration.ofMinutes(loginWindowMinutes);
        Duration forgotWindow = Duration.ofMinutes(forgotWindowMinutes);
        this.loginByEmail = new AttemptLimiter(loginPerEmail, loginWindow);
        this.loginByIp = new AttemptLimiter(loginPerIp, loginWindow);
        this.forgotByEmail = new AttemptLimiter(forgotPerEmail, forgotWindow);
        this.forgotByIp = new AttemptLimiter(forgotPerIp, forgotWindow);
    }

    public void checkLogin(String email, String ip) {
        failIfBlocked(loginByEmail, normalize(email), "Muitas tentativas de login. Tente novamente em instantes.");
        failIfBlocked(loginByIp, ip, "Muitas tentativas de login. Tente novamente em instantes.");
    }

    public void recordLoginFailure(String email, String ip) {
        loginByEmail.record(normalize(email));
        loginByIp.record(ip);
    }

    public void recordLoginSuccess(String email) {
        loginByEmail.reset(normalize(email));
    }

    /** Verifica e já registra a solicitação de recuperação de senha. */
    public void checkAndRecordForgotPassword(String email, String ip) {
        String message = "Muitas solicitações de recuperação de senha. Tente novamente mais tarde.";
        failIfBlocked(forgotByEmail, normalize(email), message);
        failIfBlocked(forgotByIp, ip, message);
        forgotByEmail.record(normalize(email));
        forgotByIp.record(ip);
    }

    private void failIfBlocked(AttemptLimiter limiter, String key, String message) {
        long retryAfter = limiter.retryAfterSeconds(key);
        if (retryAfter > 0) {
            throw new TooManyRequestsException(message, retryAfter);
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
