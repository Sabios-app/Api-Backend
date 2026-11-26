package br.com.sabios.api.auth.security;

import br.com.sabios.api.auth.service.TokenService;
import br.com.sabios.api.users.domain.User;
import br.com.sabios.api.users.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Optional<TokenService.TokenData> tokenData = tokenService.parse(token);

            if (tokenData.isPresent()) {
                Optional<User> userOpt = userRepository.findById(tokenData.get().userId());
                if (userOpt.isPresent() && !isIssuedBeforePasswordChange(tokenData.get(), userOpt.get())) {
                    User user = userOpt.get();
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /** O "iat" do JWT tem precisão de segundos, então a comparação também é feita em segundos. */
    private boolean isIssuedBeforePasswordChange(TokenService.TokenData token, User user) {
        return user.getPasswordChangedAt() != null
                && token.issuedAt().isBefore(user.getPasswordChangedAt().toInstant().truncatedTo(ChronoUnit.SECONDS));
    }
}
