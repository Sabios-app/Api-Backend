package br.com.sabios.api.config;

import br.com.sabios.api.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling.authenticationEntryPoint((request, response, ex) -> {
                    // Sem token, token inválido/expirado ou emitido antes de uma troca de senha: 401 no contrato padrão de erro
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":401,\"code\":\"UNAUTHORIZED\","
                            + "\"message\":\"Autenticação necessária ou sessão expirada.\"}");
                }))
                .authorizeHttpRequests(auth -> auth
                        // Libera Swagger / OpenAPI
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()
                        // Libera status e rotas públicas de auth
                        .requestMatchers(
                                "/api/v1/status/**",
                                "/api/v1/auth/**"
                        ).permitAll()
                        // Libera consulta pública de especialistas, agenda e avaliações (GET)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/specialists/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/agenda/specialists/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/reviews/specialists/**").permitAll()
                        // Qualquer outra rota exigirá autenticação via JWT
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

