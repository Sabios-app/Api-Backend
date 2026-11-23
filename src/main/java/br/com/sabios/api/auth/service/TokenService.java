package br.com.sabios.api.auth.service;

import br.com.sabios.api.users.domain.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService {

    @Value("${app.jwt.secret:chave-padrao-temporaria-desenvolvimento-local-min-32-chars}")
    private String secret;

    @Value("${app.jwt.expiration-hours:24}")
    private long expirationHours;

    public String generateToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withIssuer("sabios-api")
                .withSubject(user.getId().toString())
                .withClaim("email", user.getEmail())
                .withClaim("name", user.getName())
                .withClaim("role", user.getRole().name())
                .withExpiresAt(Instant.now().plus(expirationHours, ChronoUnit.HOURS))
                .sign(algorithm);
    }

    public Optional<UUID> validateTokenAndGetUserId(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String subject = JWT.require(algorithm)
                    .withIssuer("sabios-api")
                    .build()
                    .verify(token)
                    .getSubject();
            return Optional.of(UUID.fromString(subject));
        } catch (JWTVerificationException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpirationHours() {
        return expirationHours;
    }
}
