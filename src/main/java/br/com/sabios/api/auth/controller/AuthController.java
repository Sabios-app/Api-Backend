package br.com.sabios.api.auth.controller;

import br.com.sabios.api.auth.dto.AuthResponse;
import br.com.sabios.api.auth.dto.ForgotPasswordRequest;
import br.com.sabios.api.auth.dto.LoginRequest;
import br.com.sabios.api.auth.dto.RegisterRequest;
import br.com.sabios.api.auth.dto.ResetPasswordRequest;
import br.com.sabios.api.auth.service.AuthRateLimiter;
import br.com.sabios.api.auth.service.AuthService;
import br.com.sabios.api.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Endpoints para cadastro e login de usuários")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final AuthRateLimiter rateLimiter;

    public AuthController(AuthService authService, PasswordResetService passwordResetService,
                          AuthRateLimiter rateLimiter) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastra um novo usuário (Cliente ou Conselheiro)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Realiza o login e retorna o Token JWT (429 após várias tentativas falhas)")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        AuthResponse response = authService.login(request, http.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicita a recuperação de senha. Sempre responde 202, exista ou não o e-mail")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        rateLimiter.checkAndRecordForgotPassword(request.email(), http.getRemoteAddr());
        passwordResetService.requestReset(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "message", "Se o e-mail estiver cadastrado, enviaremos as instruções para redefinir a senha."
        ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefine a senha a partir do token recebido por e-mail (uso único, expira em 1h)")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Senha redefinida com sucesso."));
    }
}
