package br.com.sabios.api.config;

import br.com.sabios.api.users.domain.Role;
import br.com.sabios.api.users.domain.User;
import br.com.sabios.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@sabios.com.br").isEmpty()) {
            User admin = User.builder()
                    .name("Admin Sábios")
                    .email("admin@sabios.com.br")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("[AdminSeeder] Usuário administrador padrão criado com sucesso: admin@sabios.com.br");
        }
    }
}
