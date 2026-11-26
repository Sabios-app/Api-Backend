package br.com.sabios.api;

import br.com.sabios.api.auth.notification.PasswordResetNotifier;
import br.com.sabios.api.mentoring.domain.Mentoring;
import br.com.sabios.api.mentoring.repository.MentoringRepository;
import br.com.sabios.api.mentoring.service.MentoringService;
import br.com.sabios.api.users.domain.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(HardeningAndNotificationsIntegrationTests.CapturingNotifierConfig.class)
class HardeningAndNotificationsIntegrationTests {

    @TestConfiguration
    static class CapturingNotifierConfig {
        static final List<String> LINKS = new CopyOnWriteArrayList<>();

        @Bean
        @Primary
        PasswordResetNotifier hardeningCapturingNotifier() {
            return (User user, String link) -> LINKS.add(user.getEmail() + "|" + link);
        }
    }

    private static final String PASSWORD = "senhaForte123";
    private static final AtomicInteger NEXT_IP = new AtomicInteger(1);

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MentoringRepository mentoringRepository;
    @Autowired
    private MentoringService mentoringService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ---------- Rate limit ----------

    @Test
    @DisplayName("Login: após 5 falhas o e-mail é bloqueado (429 + Retry-After), mesmo com a senha correta")
    void shouldRateLimitLoginFailures() throws Exception {
        String email = uniqueEmail("limite");
        register("Cliente Limite", email, "CLIENT");
        RequestPostProcessor ip = newClientIp();

        for (int i = 0; i < 5; i++) {
            login(email, "senhaErrada", ip).andExpect(status().isUnauthorized());
        }
        login(email, "senhaErrada", ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
        login(email, PASSWORD, ip).andExpect(status().isTooManyRequests());

        // outro e-mail, vindo de outro IP, não é afetado
        String other = uniqueEmail("outro");
        register("Outro Cliente", other, "CLIENT");
        login(other, PASSWORD, newClientIp()).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login correto zera o contador de falhas")
    void shouldResetFailuresAfterSuccessfulLogin() throws Exception {
        String email = uniqueEmail("zera");
        register("Cliente Zera", email, "CLIENT");
        RequestPostProcessor ip = newClientIp();

        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < 4; i++) {
                login(email, "senhaErrada", ip).andExpect(status().isUnauthorized());
            }
            login(email, PASSWORD, ip).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Recuperação de senha: após 3 pedidos por e-mail o quarto recebe 429, exista ou não o e-mail")
    void shouldRateLimitForgotPassword() throws Exception {
        String unknown = uniqueEmail("inexistente");
        RequestPostProcessor ip = newClientIp();

        for (int i = 0; i < 3; i++) {
            forgot(unknown, ip).andExpect(status().isAccepted());
        }
        forgot(unknown, ip).andExpect(status().isTooManyRequests());
    }

    // ---------- JWT ----------

    @Test
    @DisplayName("Redefinir a senha invalida os tokens JWT antigos; um novo login volta a funcionar")
    void shouldInvalidateOldTokensAfterPasswordReset() throws Exception {
        String email = uniqueEmail("jwt");
        String oldToken = register("Cliente Jwt", email, "CLIENT");

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk());

        // o iat do JWT tem precisão de segundos: garante que o token antigo é de um segundo anterior
        Thread.sleep(1100);

        forgot(email, newClientIp()).andExpect(status().isAccepted());
        String link = CapturingNotifierConfig.LINKS.stream()
                .filter(l -> l.startsWith(email + "|")).reduce((a, b) -> b).orElseThrow();
        String token = link.substring(link.indexOf("token=") + "token=".length());

        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json("token", token, "newPassword", "novaSenha456")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        String newToken = objectMapper.readTree(login(email, "novaSenha456", newClientIp())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("token").asText();
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
    }

    // ---------- Notificações ----------

    @Test
    @DisplayName("Notificações: conselheiro é avisado da solicitação; cliente do aceite; cancelamento avisa a outra parte")
    void shouldNotifyOnMentoringEvents() throws Exception {
        Account counselor = counselorWithProfile();
        Account client = account("Cliente Notif", "CLIENT");
        Account stranger = account("Intruso", "CLIENT");

        for (String day : new String[]{"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"}) {
            mockMvc.perform(post("/api/v1/agenda").header("Authorization", "Bearer " + counselor.token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("dayOfWeek", day, "startTime", "09:00", "endTime", "12:00")))
                    .andExpect(status().isCreated());
        }
        List<OffsetDateTime> slots = freeSlots(counselor.specialistId);

        assertEquals(0, unreadCount(counselor));

        String first = idOf(mockMvc.perform(post("/api/v1/mentorings")
                        .header("Authorization", "Bearer " + client.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("specialistId", counselor.specialistId, "title", "Plano financeiro",
                                "scheduledAt", slots.get(0).toString())))
                .andExpect(status().isCreated()));

        // o conselheiro recebe a notificação da solicitação; o cliente não recebe nada
        assertEquals(1, unreadCount(counselor));
        assertEquals(0, unreadCount(client));
        JsonNode notifications = getJson(counselor, "/api/v1/notifications");
        assertEquals(1, notifications.get("totalElements").asInt());
        JsonNode notification = notifications.get("content").get(0);
        assertEquals("MENTORING_REQUESTED", notification.get("type").asText());
        assertEquals(first, notification.get("mentoringId").asText());
        assertEquals(false, notification.get("read").asBoolean());
        assertTrue(notification.get("message").asText().contains("Plano financeiro"));
        String notificationId = notification.get("id").asText();

        // outro usuário não consegue ler/marcar a notificação alheia
        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + stranger.token))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + counselor.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
        assertEquals(0, unreadCount(counselor));
        assertEquals(0, getJson(counselor, "/api/v1/notifications?unreadOnly=true").get("totalElements").asInt());

        // aceite notifica o cliente
        mockMvc.perform(patch("/api/v1/mentorings/" + first + "/accept")
                        .header("Authorization", "Bearer " + counselor.token))
                .andExpect(status().isOk());
        assertEquals(1, unreadCount(client));
        assertEquals("MENTORING_ACCEPTED",
                getJson(client, "/api/v1/notifications").get("content").get(0).get("type").asText());

        // cliente cancela: o conselheiro é avisado (e o cliente, que cancelou, não)
        mockMvc.perform(patch("/api/v1/mentorings/" + first + "/cancel")
                        .header("Authorization", "Bearer " + client.token))
                .andExpect(status().isOk());
        assertEquals(1, unreadCount(counselor));
        assertEquals(1, unreadCount(client));

        // marcar todas como lidas
        mockMvc.perform(patch("/api/v1/notifications/read-all").header("Authorization", "Bearer " + counselor.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(1));
        assertEquals(0, unreadCount(counselor));

        // endpoint exige autenticação
        mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
    }

    // ---------- Expiração ----------

    @Test
    @DisplayName("Solicitações pendentes cujo horário passou viram EXPIRED e vão para o histórico")
    void shouldExpireOverdueRequests() throws Exception {
        Account counselor = counselorWithProfile();
        Account client = account("Cliente Expira", "CLIENT");
        for (String day : new String[]{"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"}) {
            mockMvc.perform(post("/api/v1/agenda").header("Authorization", "Bearer " + counselor.token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("dayOfWeek", day, "startTime", "09:00", "endTime", "12:00")))
                    .andExpect(status().isCreated());
        }
        List<OffsetDateTime> slots = freeSlots(counselor.specialistId);

        String stale = idOf(mockMvc.perform(post("/api/v1/mentorings")
                        .header("Authorization", "Bearer " + client.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("specialistId", counselor.specialistId, "title", "Sem resposta",
                                "scheduledAt", slots.get(0).toString())))
                .andExpect(status().isCreated()));
        String fresh = idOf(mockMvc.perform(post("/api/v1/mentorings")
                        .header("Authorization", "Bearer " + client.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("specialistId", counselor.specialistId, "title", "Ainda vale",
                                "scheduledAt", slots.get(2).toString())))
                .andExpect(status().isCreated()));

        // simula a passagem do tempo para a primeira solicitação
        Mentoring entity = mentoringRepository.findById(UUID.fromString(stale)).orElseThrow();
        entity.setScheduledAt(OffsetDateTime.now().minusHours(1));
        mentoringRepository.save(entity);

        assertTrue(mentoringService.expireOverdueRequests() >= 1);

        JsonNode expired = getJson(client, "/api/v1/mentorings?status=EXPIRED");
        assertEquals(1, expired.size());
        assertEquals(stale, expired.get(0).get("id").asText());

        // a solicitação futura continua pendente e não é aceita se já passou
        JsonNode upcoming = getJson(client, "/api/v1/mentorings?scope=UPCOMING");
        assertEquals(1, upcoming.size());
        assertEquals(fresh, upcoming.get(0).get("id").asText());
        mockMvc.perform(patch("/api/v1/mentorings/" + stale + "/accept")
                        .header("Authorization", "Bearer " + counselor.token))
                .andExpect(status().isConflict());
    }

    // ---------- Helpers ----------

    private record Account(String token, String email, String specialistId) {}

    /** Cada teste usa um IP próprio para os limites por IP não se misturarem entre testes. */
    private RequestPostProcessor newClientIp() {
        String ip = "10.20.30." + NEXT_IP.getAndIncrement();
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private String register(String name, String email, String role) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", name, "email", email, "password", PASSWORD, "role", role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private Account account(String name, String role) throws Exception {
        String email = uniqueEmail(name.toLowerCase().replace(' ', '.'));
        return new Account(register(name, email, role), email, null);
    }

    private Account counselorWithProfile() throws Exception {
        Account base = account("Conselheiro Notif", "COUNSELOR");
        String body = mockMvc.perform(post("/api/v1/specialists/me")
                        .header("Authorization", "Bearer " + base.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Consultor\",\"specialty\":\"Notificacoes\",\"bio\":\"Bio\",\"hourlyRate\":100.00}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Account(base.token, base.email, objectMapper.readTree(body).get("id").asText());
    }

    private List<OffsetDateTime> freeSlots(String specialistId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/agenda/specialists/" + specialistId + "/slots")
                        .param("from", LocalDate.now().plusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(7).toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<OffsetDateTime> slots = new java.util.ArrayList<>();
        objectMapper.readTree(body).forEach(n -> slots.add(OffsetDateTime.parse(n.asText())));
        return slots;
    }

    private long unreadCount(Account who) throws Exception {
        return getJson(who, "/api/v1/notifications/unread-count").get("count").asLong();
    }

    private JsonNode getJson(Account who, String url) throws Exception {
        String body = mockMvc.perform(get(url).header("Authorization", "Bearer " + who.token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private ResultActions login(String email, String password, RequestPostProcessor ip) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").with(ip).contentType(MediaType.APPLICATION_JSON)
                .content(json("email", email, "password", password)));
    }

    private ResultActions forgot(String email, RequestPostProcessor ip) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/forgot-password").with(ip).contentType(MediaType.APPLICATION_JSON)
                .content(json("email", email)));
    }

    private String idOf(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID() + "@email.com";
    }

    private String json(String... keyValues) {
        var node = objectMapper.createObjectNode();
        for (int i = 0; i < keyValues.length; i += 2) {
            node.put(keyValues[i], keyValues[i + 1]);
        }
        return node.toString();
    }
}
