package br.com.sabios.api;

import br.com.sabios.api.auth.notification.PasswordResetNotifier;
import br.com.sabios.api.mentoring.domain.Mentoring;
import br.com.sabios.api.mentoring.repository.MentoringRepository;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(SchedulingAndRecoveryIntegrationTests.CapturingNotifierConfig.class)
class SchedulingAndRecoveryIntegrationTests {

    /** Substitui o envio de e-mail para o teste conseguir ler o link de redefinição. */
    @TestConfiguration
    static class CapturingNotifierConfig {
        static final List<String> LINKS = new CopyOnWriteArrayList<>();

        @Bean
        @Primary
        PasswordResetNotifier capturingNotifier() {
            return (User user, String link) -> LINKS.add(user.getEmail() + "|" + link);
        }
    }

    private static final String PASSWORD = "senhaForte123";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MentoringRepository mentoringRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ---------- Recuperação de senha ----------

    @Test
    @DisplayName("Recuperação de senha: token é de uso único, troca a senha e não revela e-mails inexistentes")
    void shouldRecoverPassword() throws Exception {
        String email = uniqueEmail("recupera");
        register("Cliente Recupera", email, "CLIENT");

        // e-mail inexistente responde igual (sem enumeração de usuários)
        forgot("naoexiste." + UUID.randomUUID() + "@email.com").andExpect(status().isAccepted());

        forgot(email).andExpect(status().isAccepted());
        String link = CapturingNotifierConfig.LINKS.stream()
                .filter(l -> l.startsWith(email + "|")).reduce((a, b) -> b).orElseThrow();
        String token = link.substring(link.indexOf("token=") + "token=".length());

        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json("token", token, "newPassword", "novaSenha456")))
                .andExpect(status().isOk());

        login(email, "novaSenha456").andExpect(status().isOk());
        login(email, PASSWORD).andExpect(status().isUnauthorized());

        // o mesmo token não pode ser reutilizado
        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json("token", token, "newPassword", "outraSenha789")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Redefinição com token inválido ou senha curta deve ser rejeitada")
    void shouldRejectInvalidResetRequests() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json("token", "token-que-nao-existe", "newPassword", "novaSenha456")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json("token", "qualquer", "newPassword", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Cadastro não pode criar usuário ADMIN")
    void shouldNotAllowSelfRegistrationAsAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "Quero Ser Admin", "email", uniqueEmail("admin"),
                                "password", PASSWORD, "role", "ADMIN")))
                .andExpect(status().isBadRequest());
    }

    // ---------- Agenda e agendamento ----------

    @Test
    @DisplayName("Agenda: dia inválido é rejeitado e faixas sobrepostas geram conflito")
    void shouldValidateAvailability() throws Exception {
        Account counselor = counselorWithProfile("Agenda", "Agenda Teste");

        availability(counselor, "MONDAY", "09:00", "12:00").andExpect(status().isCreated());
        availability(counselor, "MONDAY", "11:00", "13:00").andExpect(status().isConflict());
        availability(counselor, "MONDAY", "12:00", "14:00").andExpect(status().isCreated()); // encostar não é sobrepor
        availability(counselor, "SEGUNDA", "09:00", "12:00").andExpect(status().isBadRequest());
        availability(counselor, "TUESDAY", "12:00", "09:00").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Agendamento: respeita disponibilidade, impede reserva dupla e libera o horário ao cancelar")
    void shouldEnforceSchedulingRules() throws Exception {
        Account counselor = counselorWithProfile("Reserva", "Reserva Teste");
        openEveryDay(counselor);
        Account clientA = register("Cliente A", uniqueEmail("a"), "CLIENT");
        Account clientB = register("Cliente B", uniqueEmail("b"), "CLIENT");

        List<OffsetDateTime> slots = freeSlots(counselor.specialistId);
        OffsetDateTime slot = slots.get(0);

        // fora da disponibilidade (as faixas vão de 09:00 a 12:00)
        mentoring(clientA, counselor, slot.withHour(20)).andExpect(status().isBadRequest());

        // solicitação válida nasce REQUESTED e sem link inventado
        String mentoringA = idOf(mentoring(clientA, counselor, slot)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.meetingLink").doesNotExist()));

        // o mesmo cliente não pode pedir o mesmo horário de novo
        mentoring(clientA, counselor, slot).andExpect(status().isConflict());

        // outro cliente pode pedir (pedidos pendentes não bloqueiam), mas só um será confirmado
        String mentoringB = idOf(mentoring(clientB, counselor, slot).andExpect(status().isCreated()));

        // cliente não pode aceitar
        patchAs(clientA, "/api/v1/mentorings/" + mentoringA + "/accept").andExpect(status().isForbidden());

        // aceite com link real
        mockMvc.perform(patch("/api/v1/mentorings/" + mentoringA + "/accept")
                        .header("Authorization", "Bearer " + counselor.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("meetingLink", "https://meet.google.com/abc-defg-hij")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.meetingLink").value("https://meet.google.com/abc-defg-hij"));

        // segundo aceite no mesmo horário é conflito
        patchAs(counselor, "/api/v1/mentorings/" + mentoringB + "/accept").andExpect(status().isConflict());

        // o horário saiu da lista de livres
        assertFalse(freeSlots(counselor.specialistId).contains(slot));

        // solicitação nova num horário já confirmado é conflito
        Account clientC = register("Cliente C", uniqueEmail("c"), "CLIENT");
        mentoring(clientC, counselor, slot).andExpect(status().isConflict());

        // outro cliente não pode cancelar; o cliente dono pode
        patchAs(clientB, "/api/v1/mentorings/" + mentoringA + "/cancel").andExpect(status().isForbidden());
        patchAs(clientA, "/api/v1/mentorings/" + mentoringA + "/cancel")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
        patchAs(clientA, "/api/v1/mentorings/" + mentoringA + "/cancel").andExpect(status().isConflict());

        // o horário volta a ficar livre e o pedido pendente do cliente B agora pode ser aceito
        assertTrue(freeSlots(counselor.specialistId).contains(slot));
        patchAs(counselor, "/api/v1/mentorings/" + mentoringB + "/accept").andExpect(status().isOk());
    }

    @Test
    @DisplayName("Conclusão: só após o horário, só pelo conselheiro; histórico e painel separam as mentorias")
    void shouldCompleteMentoringAndSplitHistory() throws Exception {
        Account counselor = counselorWithProfile("Historico", "Historico Teste");
        openEveryDay(counselor);
        Account client = register("Cliente Hist", uniqueEmail("hist"), "CLIENT");

        List<OffsetDateTime> slots = freeSlots(counselor.specialistId);
        String confirmed = idOf(mentoring(client, counselor, slots.get(0)).andExpect(status().isCreated()));
        String rejected = idOf(mentoring(client, counselor, slots.get(1)).andExpect(status().isCreated()));
        patchAs(counselor, "/api/v1/mentorings/" + confirmed + "/accept").andExpect(status().isOk());
        patchAs(counselor, "/api/v1/mentorings/" + rejected + "/reject").andExpect(status().isOk());

        // ainda é futura: não pode concluir
        patchAs(counselor, "/api/v1/mentorings/" + confirmed + "/complete").andExpect(status().isConflict());

        // simula a passagem do tempo
        Mentoring entity = mentoringRepository.findById(UUID.fromString(confirmed)).orElseThrow();
        entity.setScheduledAt(OffsetDateTime.now().minusHours(2));
        mentoringRepository.save(entity);

        patchAs(client, "/api/v1/mentorings/" + confirmed + "/complete").andExpect(status().isForbidden());
        patchAs(counselor, "/api/v1/mentorings/" + confirmed + "/complete")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // histórico: concluída + recusada; nada pendente
        JsonNode history = getAs(client, "/api/v1/mentorings?scope=HISTORY");
        assertEquals(2, history.size());
        assertEquals(0, getAs(client, "/api/v1/mentorings?scope=UPCOMING").size());

        JsonNode completed = getAs(client, "/api/v1/mentorings?status=COMPLETED");
        assertEquals(1, completed.size());
        assertEquals(confirmed, completed.get(0).get("id").asText());

        // painel do conselheiro enxerga o mesmo histórico
        assertEquals(2, getAs(counselor, "/api/v1/mentorings?scope=HISTORY").size());
    }

    // ---------- Busca de especialistas ----------

    @Test
    @DisplayName("Busca: filtra por preço, nota e dia atendido, e rejeita faixas inválidas")
    void shouldFilterSpecialists() throws Exception {
        String specialty = "Especialidade" + UUID.randomUUID().toString().substring(0, 8);
        Account counselor = counselorWithProfile(specialty, "Filtro Teste");
        availability(counselor, "WEDNESDAY", "09:00", "12:00").andExpect(status().isCreated());

        // hourlyRate = 150.00, rating inicial = 5.00
        assertEquals(1, searchTotal(specialty, ""));
        assertEquals(1, searchTotal(specialty, "&minPrice=100&maxPrice=200"));
        assertEquals(0, searchTotal(specialty, "&maxPrice=100"));
        assertEquals(0, searchTotal(specialty, "&minPrice=151"));
        assertEquals(1, searchTotal(specialty, "&minRating=4.5"));
        assertEquals(1, searchTotal(specialty, "&availableDay=WEDNESDAY"));
        assertEquals(0, searchTotal(specialty, "&availableDay=THURSDAY"));
        assertEquals(1, searchTotal(specialty, "&availableDay=WEDNESDAY&minPrice=100&minRating=4"));

        mockMvc.perform(get("/api/v1/specialists?minRating=9")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/specialists?minPrice=200&maxPrice=100")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/specialists?availableDay=SEGUNDA")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/specialists?sort=rating,desc&size=5")).andExpect(status().isOk());
    }

    // ---------- Helpers ----------

    private record Account(String token, String email, String specialistId) {}

    private Account register(String name, String email, String role) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", name, "email", email, "password", PASSWORD, "role", role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Account(objectMapper.readTree(body).get("token").asText(), email, null);
    }

    private Account counselorWithProfile(String specialty, String name) throws Exception {
        Account base = register("Conselheiro " + name, uniqueEmail("conselheiro"), "COUNSELOR");
        String body = mockMvc.perform(post("/api/v1/specialists/me")
                        .header("Authorization", "Bearer " + base.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Consultor\",\"specialty\":\"" + specialty + "\",\"bio\":\"Bio\",\"hourlyRate\":150.00}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Account(base.token, base.email, objectMapper.readTree(body).get("id").asText());
    }

    private void openEveryDay(Account counselor) throws Exception {
        for (String day : new String[]{"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"}) {
            availability(counselor, day, "09:00", "12:00").andExpect(status().isCreated());
        }
    }

    private ResultActions availability(Account counselor, String day, String start, String end) throws Exception {
        return mockMvc.perform(post("/api/v1/agenda")
                .header("Authorization", "Bearer " + counselor.token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("dayOfWeek", day, "startTime", start, "endTime", end)));
    }

    private List<OffsetDateTime> freeSlots(String specialistId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/agenda/specialists/" + specialistId + "/slots")
                        .param("from", LocalDate.now().plusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(7).toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<OffsetDateTime> slots = new ArrayList<>();
        objectMapper.readTree(body).forEach(n -> slots.add(OffsetDateTime.parse(n.asText())));
        assertFalse(slots.isEmpty(), "esperava horários livres");
        return slots;
    }

    private ResultActions mentoring(Account client, Account counselor, OffsetDateTime at) throws Exception {
        return mockMvc.perform(post("/api/v1/mentorings")
                .header("Authorization", "Bearer " + client.token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("specialistId", counselor.specialistId, "title", "Tema da mentoria",
                        "scheduledAt", at.toString())));
    }

    private ResultActions patchAs(Account who, String url) throws Exception {
        return mockMvc.perform(patch(url)
                .header("Authorization", "Bearer " + who.token));
    }

    private JsonNode getAs(Account who, String url) throws Exception {
        String body = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + who.token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private long searchTotal(String specialty, String extraQuery) throws Exception {
        String body = mockMvc.perform(get("/api/v1/specialists?specialty=" + specialty + extraQuery))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("totalElements").asLong();
    }

    private ResultActions forgot(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content(json("email", email)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json("email", email, "password", password)));
    }

    private String idOf(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID() + "@email.com";
    }

    private String json(String... keyValues) throws Exception {
        var node = objectMapper.createObjectNode();
        for (int i = 0; i < keyValues.length; i += 2) {
            node.put(keyValues[i], keyValues[i + 1]);
        }
        return objectMapper.writeValueAsString(node);
    }
}
