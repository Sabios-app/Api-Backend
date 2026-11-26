package br.com.sabios.api;

import br.com.sabios.api.actionplan.domain.ActionPlan;
import br.com.sabios.api.actionplan.repository.ActionPlanRepository;
import br.com.sabios.api.mentoring.domain.Mentoring;
import br.com.sabios.api.mentoring.repository.MentoringRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ActionPlanAndReviewIntegrationTests {

    private static final String PASSWORD = "senhaForte123";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MentoringRepository mentoringRepository;
    @Autowired
    private ActionPlanRepository actionPlanRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    // ---------- Plano de ação ----------

    @Test
    @DisplayName("Plano de ação: conselheiro atribui tarefa, cliente conclui, histórico e permissões")
    void shouldManageActionPlan() throws Exception {
        Account counselor = counselorWithProfile("Plano");
        Account client = account("Cliente Plano", "CLIENT");
        Account stranger = account("Intruso Plano", "CLIENT");
        String mentoring = confirmedMentoring(client, counselor);

        // tarefa própria do cliente, sem mentoria (comportamento antigo continua funcionando)
        String own = idOf(postAs(client, "/api/v1/action-plans",
                "{\"title\":\"Separar contas\",\"deadline\":\"Até sexta\",\"conselheiroNome\":\"Fulano\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deadline").value("Até sexta"))
                .andExpect(jsonPath("$.conselheiroNome").value("Fulano"))
                .andExpect(jsonPath("$.assignedByCounselor").value(false))
                .andExpect(jsonPath("$.completed").value(false)));

        // cliente não pode atribuir tarefa nem vincular mentoria que não é dele
        postAs(client, "/api/v1/action-plans/mentorings/" + mentoring,
                "{\"title\":\"Tarefa\"}").andExpect(status().isForbidden());
        postAs(stranger, "/api/v1/action-plans",
                "{\"title\":\"X\",\"mentoringId\":\"" + mentoring + "\"}").andExpect(status().isForbidden());

        // prazo no passado é rejeitado
        postAs(counselor, "/api/v1/action-plans/mentorings/" + mentoring,
                "{\"title\":\"Atrasada\",\"dueDate\":\"" + LocalDate.now().minusDays(1) + "\"}")
                .andExpect(status().isBadRequest());

        // conselheiro atribui com prazo estruturado; o texto do prazo é derivado
        LocalDate due = LocalDate.now().plusDays(5);
        String assigned = idOf(postAs(counselor, "/api/v1/action-plans/mentorings/" + mentoring,
                "{\"title\":\"Montar fluxo de caixa\",\"dueDate\":\"" + due + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignedByCounselor").value(true))
                .andExpect(jsonPath("$.mentoringId").value(mentoring))
                .andExpect(jsonPath("$.dueDate").value(due.toString()))
                .andExpect(jsonPath("$.conselheiroNome").value("Conselheiro Plano"))
                .andExpect(jsonPath("$.overdue").value(false)));

        // o cliente é notificado
        JsonNode notifications = getAs(client, "/api/v1/notifications");
        assertEquals("ACTION_PLAN_ASSIGNED", notifications.get("content").get(0).get("type").asText());

        // cliente vê as duas; conselheiro vê só a que atribuiu
        assertEquals(2, getAs(client, "/api/v1/action-plans").size());
        JsonNode counselorList = getAs(counselor, "/api/v1/action-plans");
        assertEquals(1, counselorList.size());
        assertEquals(assigned, counselorList.get(0).get("id").asText());

        // por mentoria: cliente e conselheiro enxergam; estranho não
        assertEquals(1, getAs(client, "/api/v1/action-plans/mentorings/" + mentoring).size());
        assertEquals(1, getAs(counselor, "/api/v1/action-plans/mentorings/" + mentoring).size());
        mockMvc.perform(get("/api/v1/action-plans/mentorings/" + mentoring)
                        .header("Authorization", "Bearer " + stranger.token))
                .andExpect(status().isForbidden());

        // edição: só quem criou. O cliente não edita nem apaga a tarefa do conselheiro
        patchAs(client, "/api/v1/action-plans/" + assigned, "{\"title\":\"Mudei\"}").andExpect(status().isForbidden());
        deleteAs(client, "/api/v1/action-plans/" + assigned).andExpect(status().isForbidden());
        patchAs(counselor, "/api/v1/action-plans/" + own, "{\"title\":\"Mudei\"}").andExpect(status().isForbidden());

        LocalDate newDue = LocalDate.now().plusDays(10);
        patchAs(counselor, "/api/v1/action-plans/" + assigned, "{\"title\":\"Fluxo de caixa 12 meses\",\"dueDate\":\"" + newDue + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Fluxo de caixa 12 meses"))
                .andExpect(jsonPath("$.dueDate").value(newDue.toString()));
        patchAs(counselor, "/api/v1/action-plans/" + assigned, "{\"title\":\"  \"}").andExpect(status().isBadRequest());

        // conclusão: só o cliente; registra o momento e alimenta o histórico
        patchAs(counselor, "/api/v1/action-plans/" + assigned + "/toggle", null).andExpect(status().isForbidden());
        patchAs(client, "/api/v1/action-plans/" + assigned + "/toggle", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completedAt").exists());

        JsonNode history = getAs(client, "/api/v1/action-plans?completed=true");
        assertEquals(1, history.size());
        assertEquals(assigned, history.get(0).get("id").asText());
        assertEquals(1, getAs(client, "/api/v1/action-plans?completed=false").size());
        assertEquals(1, getAs(client, "/api/v1/action-plans?mentoringId=" + mentoring).size());

        // voltar para pendente limpa a data de conclusão
        patchAs(client, "/api/v1/action-plans/" + assigned + "/toggle", null)
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.completedAt").doesNotExist());

        // remoção pelo autor
        deleteAs(counselor, "/api/v1/action-plans/" + assigned).andExpect(status().isNoContent());
        assertEquals(1, getAs(client, "/api/v1/action-plans").size());
    }

    @Test
    @DisplayName("Plano de ação: mentoria só pendente não aceita tarefas; tarefa vencida aparece como overdue")
    void shouldRejectTasksOnPendingMentoringAndFlagOverdue() throws Exception {
        Account counselor = counselorWithProfile("Pendente");
        Account client = account("Cliente Pendente", "CLIENT");
        openEveryDay(counselor);
        String requested = idOf(requestMentoring(client, counselor, freeSlots(counselor).get(0)).andExpect(status().isCreated()));

        postAs(counselor, "/api/v1/action-plans/mentorings/" + requested, "{\"title\":\"Cedo demais\"}")
                .andExpect(status().isBadRequest());
        postAs(client, "/api/v1/action-plans", "{\"title\":\"Cedo demais\",\"mentoringId\":\"" + requested + "\"}")
                .andExpect(status().isBadRequest());

        // tarefa com prazo de hoje não está vencida; depois de "envelhecida" no banco fica overdue
        String task = idOf(postAs(client, "/api/v1/action-plans",
                "{\"title\":\"Vai vencer\",\"dueDate\":\"" + LocalDate.now() + "\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.overdue").value(false))
                .andExpect(jsonPath("$.deadline").value(org.hamcrest.Matchers.matchesPattern("[0-9]{2}/[0-9]{2}/[0-9]{4}"))));
        assertEquals(0, getAs(client, "/api/v1/action-plans?overdue=true").size());

        ActionPlan entity = actionPlanRepository.findById(UUID.fromString(task)).orElseThrow();
        entity.setDueDate(LocalDate.now().minusDays(2));
        actionPlanRepository.save(entity);

        JsonNode overdue = getAs(client, "/api/v1/action-plans?overdue=true");
        assertEquals(1, overdue.size());
        assertEquals(task, overdue.get(0).get("id").asText());

        // concluída deixa de ser considerada vencida
        patchAs(client, "/api/v1/action-plans/" + task + "/toggle", null).andExpect(jsonPath("$.overdue").value(false));
        assertEquals(0, getAs(client, "/api/v1/action-plans?overdue=true").size());
    }

    // ---------- Avaliações ----------

    @Test
    @DisplayName("Avaliação: só de mentoria concluída, uma por mentoria, média recalculada e sem vazar dados do cliente")
    void shouldReviewCompletedMentoring() throws Exception {
        Account counselor = counselorWithProfile("Avaliacao");
        Account client = account("Cliente Avalia", "CLIENT");
        Account stranger = account("Intruso Avalia", "CLIENT");
        openEveryDay(counselor);

        List<OffsetDateTime> slots = freeSlots(counselor);
        String first = idOf(requestMentoring(client, counselor, slots.get(0)).andExpect(status().isCreated()));

        // mentoria ainda não concluída: não pode avaliar
        postAs(client, "/api/v1/reviews", review(first, 5, "Ótimo")).andExpect(status().isConflict());

        patchAs(counselor, "/api/v1/mentorings/" + first + "/accept", null).andExpect(status().isOk());
        postAs(client, "/api/v1/reviews", review(first, 5, "Ótimo")).andExpect(status().isConflict());

        completeInThePast(first, counselor);

        // aparece como pendente de avaliação
        JsonNode pending = getAs(client, "/api/v1/reviews/pending");
        assertEquals(1, pending.size());
        assertEquals(first, pending.get(0).get("id").asText());

        // validações e permissão
        postAs(client, "/api/v1/reviews", review(first, 6, "nota alta")).andExpect(status().isBadRequest());
        postAs(client, "/api/v1/reviews", review(first, 0, "nota baixa")).andExpect(status().isBadRequest());
        postAs(client, "/api/v1/reviews", review(first, 5, "x".repeat(1001))).andExpect(status().isBadRequest());
        postAs(stranger, "/api/v1/reviews", review(first, 5, "não sou eu")).andExpect(status().isForbidden());
        postAs(counselor, "/api/v1/reviews", review(first, 5, "auto-avaliação")).andExpect(status().isForbidden());

        // avaliação válida
        String reviewId = idOf(postAs(client, "/api/v1/reviews", review(first, 4, "  Muito bom  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Muito bom"))
                .andExpect(jsonPath("$.client.name").value("Cliente Avalia")));

        // segunda avaliação da mesma mentoria é conflito, e deixa de ser pendente
        postAs(client, "/api/v1/reviews", review(first, 5, "de novo")).andExpect(status().isConflict());
        assertEquals(0, getAs(client, "/api/v1/reviews/pending").size());

        // conselheiro é notificado
        JsonNode notifications = getAs(counselor, "/api/v1/notifications?unreadOnly=true");
        boolean notified = false;
        for (JsonNode n : notifications.get("content")) {
            notified |= "REVIEW_RECEIVED".equals(n.get("type").asText());
        }
        assertTrue(notified, "conselheiro deveria receber REVIEW_RECEIVED");

        // lista pública: não vaza e-mail/telefone/role do cliente
        String body = mockMvc.perform(get("/api/v1/reviews/specialists/" + counselor.specialistId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode list = objectMapper.readTree(body);
        assertEquals(1, list.size());
        JsonNode reviewer = list.get(0).get("client");
        assertEquals("Cliente Avalia", reviewer.get("name").asText());
        assertFalse(reviewer.has("email") || reviewer.has("phone") || reviewer.has("role") || reviewer.has("id"),
                "dados privados do avaliador não podem ser expostos publicamente");

        // média do especialista = 4.00 e resumo
        assertEquals(0, new java.math.BigDecimal("4.00").compareTo(
                new java.math.BigDecimal(getPublic("/api/v1/specialists/" + counselor.specialistId).get("rating").asText())));
        JsonNode summary = getPublic("/api/v1/reviews/specialists/" + counselor.specialistId + "/summary");
        assertEquals(1, summary.get("total").asInt());
        assertEquals(1, summary.get("distribution").get("4").asInt());
        assertEquals(0, summary.get("distribution").get("5").asInt());

        // segunda mentoria, nota 2 -> média 3.00
        String second = idOf(requestMentoring(client, counselor, slots.get(2)).andExpect(status().isCreated()));
        patchAs(counselor, "/api/v1/mentorings/" + second + "/accept", null).andExpect(status().isOk());
        completeInThePast(second, counselor);
        String secondReview = idOf(postAs(client, "/api/v1/reviews", review(second, 2, null)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment").doesNotExist()));
        assertEquals(0, new java.math.BigDecimal("3.00").compareTo(
                new java.math.BigDecimal(getPublic("/api/v1/specialists/" + counselor.specialistId).get("rating").asText())));

        // edição: só o autor; recalcula a média
        patchAs(stranger, "/api/v1/reviews/" + secondReview, "{\"rating\":5}").andExpect(status().isForbidden());
        patchAs(counselor, "/api/v1/reviews/" + secondReview, "{\"rating\":5}").andExpect(status().isForbidden());
        patchAs(client, "/api/v1/reviews/" + secondReview, "{\"rating\":6}").andExpect(status().isBadRequest());
        patchAs(client, "/api/v1/reviews/" + secondReview, "{\"rating\":5,\"comment\":\"Melhorou\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Melhorou"));
        assertEquals(0, new java.math.BigDecimal("4.50").compareTo(
                new java.math.BigDecimal(getPublic("/api/v1/specialists/" + counselor.specialistId).get("rating").asText())));

        // "minhas": cliente vê as que fez; conselheiro as que recebeu
        assertEquals(2, getAs(client, "/api/v1/reviews/me").size());
        assertEquals(2, getAs(counselor, "/api/v1/reviews/me").size());

        // remoção: só o autor; média volta a considerar só a restante
        deleteAs(stranger, "/api/v1/reviews/" + secondReview).andExpect(status().isForbidden());
        deleteAs(client, "/api/v1/reviews/" + secondReview).andExpect(status().isNoContent());
        assertEquals(0, new java.math.BigDecimal("4.00").compareTo(
                new java.math.BigDecimal(getPublic("/api/v1/specialists/" + counselor.specialistId).get("rating").asText())));
        // a mentoria volta a ser avaliável
        assertEquals(1, getAs(client, "/api/v1/reviews/pending").size());

        // remover a última avaliação devolve a nota padrão
        deleteAs(client, "/api/v1/reviews/" + reviewId).andExpect(status().isNoContent());
        JsonNode empty = getPublic("/api/v1/reviews/specialists/" + counselor.specialistId + "/summary");
        assertEquals(0, empty.get("total").asInt());
        assertTrue(empty.get("average").isNull(), "sem avaliações a média deve ser nula");

        // especialista inexistente
        mockMvc.perform(get("/api/v1/reviews/specialists/" + UUID.randomUUID() + "/summary")).andExpect(status().isNotFound());
    }

    // ---------- Helpers ----------

    private record Account(String token, String email, String specialistId) {}

    private Account account(String name, String role) throws Exception {
        String email = name.toLowerCase().replace(' ', '.') + "." + UUID.randomUUID() + "@email.com";
        String body = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode().put("name", name).put("email", email)
                                .put("password", PASSWORD).put("role", role).toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Account(objectMapper.readTree(body).get("token").asText(), email, null);
    }

    private Account counselorWithProfile(String label) throws Exception {
        Account base = account("Conselheiro " + label, "COUNSELOR");
        String body = mockMvc.perform(post("/api/v1/specialists/me")
                        .header("Authorization", "Bearer " + base.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Consultor\",\"specialty\":\"Plano e Avaliacao\",\"bio\":\"Bio\",\"hourlyRate\":100.00}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Account(base.token, base.email, objectMapper.readTree(body).get("id").asText());
    }

    private void openEveryDay(Account counselor) throws Exception {
        for (String day : new String[]{"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"}) {
            postAs(counselor, "/api/v1/agenda",
                    "{\"dayOfWeek\":\"" + day + "\",\"startTime\":\"09:00\",\"endTime\":\"12:00\"}").andExpect(status().isCreated());
        }
    }

    private List<OffsetDateTime> freeSlots(Account counselor) throws Exception {
        String body = mockMvc.perform(get("/api/v1/agenda/specialists/" + counselor.specialistId + "/slots")
                        .param("from", LocalDate.now().plusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(7).toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<OffsetDateTime> slots = new ArrayList<>();
        objectMapper.readTree(body).forEach(n -> slots.add(OffsetDateTime.parse(n.asText())));
        return slots;
    }

    private ResultActions requestMentoring(Account client, Account counselor, OffsetDateTime at) throws Exception {
        return postAs(client, "/api/v1/mentorings", objectMapper.createObjectNode()
                .put("specialistId", counselor.specialistId).put("title", "Tema da mentoria")
                .put("scheduledAt", at.toString()).toString());
    }

    /** Mentoria confirmada e futura entre o cliente e o conselheiro. */
    private String confirmedMentoring(Account client, Account counselor) throws Exception {
        openEveryDay(counselor);
        String id = idOf(requestMentoring(client, counselor, freeSlots(counselor).get(0)).andExpect(status().isCreated()));
        patchAs(counselor, "/api/v1/mentorings/" + id + "/accept", null).andExpect(status().isOk());
        return id;
    }

    /** Simula a passagem do tempo e marca a mentoria como concluída pelo conselheiro. */
    private void completeInThePast(String mentoringId, Account counselor) throws Exception {
        Mentoring entity = mentoringRepository.findById(UUID.fromString(mentoringId)).orElseThrow();
        entity.setScheduledAt(OffsetDateTime.now().minusHours(3));
        mentoringRepository.save(entity);
        patchAs(counselor, "/api/v1/mentorings/" + mentoringId + "/complete", null).andExpect(status().isOk());
    }

    private String review(String mentoringId, int rating, String comment) {
        var node = objectMapper.createObjectNode().put("mentoringId", mentoringId).put("rating", rating);
        if (comment != null) {
            node.put("comment", comment);
        }
        return node.toString();
    }

    private ResultActions postAs(Account who, String url, String body) throws Exception {
        return mockMvc.perform(post(url)
                .header("Authorization", "Bearer " + who.token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions patchAs(Account who, String url, String body) throws Exception {
        var request = patch(url)
                .header("Authorization", "Bearer " + who.token);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    private ResultActions deleteAs(Account who, String url) throws Exception {
        return mockMvc.perform(delete(url)
                .header("Authorization", "Bearer " + who.token));
    }

    private JsonNode getAs(Account who, String url) throws Exception {
        String body = mockMvc.perform(get(url)
                        .header("Authorization", "Bearer " + who.token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private JsonNode getPublic(String url) throws Exception {
        String body = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String idOf(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString()).get("id").asText();
    }
}
