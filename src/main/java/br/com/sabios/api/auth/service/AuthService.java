package br.com.sabios.api.auth.service;

import br.com.sabios.api.auth.dto.AuthResponse;
import br.com.sabios.api.auth.dto.LoginRequest;
import br.com.sabios.api.auth.dto.RegisterRequest;
import br.com.sabios.api.users.domain.User;
import br.com.sabios.api.users.dto.UserResponse;
import br.com.sabios.api.users.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail.");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .phone(request.phone())
                .avatarUrl(request.avatarUrl())
                .build();

        User savedUser = userRepository.save(user);
        String token = tokenService.generateToken(savedUser);

        return new AuthResponse(
                token,
                "Bearer",
                tokenService.getExpirationHours(),
                UserResponse.fromEntity(savedUser)
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }

        String token = tokenService.generateToken(user);

        return new AuthResponse(
                token,
                "Bearer",
                tokenService.getExpirationHours(),
                UserResponse.fromEntity(user)
        );
    }
}
