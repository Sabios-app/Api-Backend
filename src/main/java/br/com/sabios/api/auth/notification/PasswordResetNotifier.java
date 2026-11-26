package br.com.sabios.api.auth.notification;

import br.com.sabios.api.users.domain.User;

/** Entrega ao usuário o link de redefinição de senha. */
public interface PasswordResetNotifier {
    void send(User user, String resetLink);
}
