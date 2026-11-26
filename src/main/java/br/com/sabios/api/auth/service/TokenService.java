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
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(expirationHours, ChronoUnit.HOURS))
                .sign(algorithm);
    }

    /** Dados mínimos de um token válido. */
    public record TokenData(UUID userId, Instant issuedAt) {}

    public Optional<TokenData> parse(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var decoded = JWT.require(algorithm)
                    .withIssuer("sabios-api")
                    .build()
                    .verify(token);
            Instant issuedAt = decoded.getIssuedAtAsInstant() != null ? decoded.getIssuedAtAsInstant() : Instant.EPOCH;
            return Optional.of(new TokenData(UUID.fromString(decoded.getSubject()), issuedAt));
        } catch (JWTVerificationException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpirationHours() {
        return expirationHours;
    }
}
