package br.com.sabios.api;

import br.com.sabios.api.auth.dto.AuthResponse;
import br.com.sabios.api.auth.dto.LoginRequest;
import br.com.sabios.api.auth.dto.RegisterRequest;
import br.com.sabios.api.mentoring.dto.CreateMentoringRequest;
import br.com.sabios.api.specialists.dto.CreateSpecialistRequest;
import br.com.sabios.api.users.domain.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SabiosAppIntegrationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("Health check deve responder HTTP 200")
    void shouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Fluxo completo: Cadastro, Login, Perfil Especialista, Mentoria e Lista")
    void fullApplicationFlowTest() throws Exception {
        String clientEmail = "teste.cliente." + UUID.randomUUID() + "@email.com";
        String counselorEmail = "teste.conselheiro." + UUID.randomUUID() + "@email.com";

        // 1. Cadastrar Cliente
        RegisterRequest clientRegister = new RegisterRequest(
                "Cliente Teste",
                clientEmail,
                "senhaForte123",
                Role.CLIENT,
                "79999991111",
                null
        );

        MvcResult clientResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clientRegister)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        AuthResponse clientAuth = objectMapper.readValue(clientResult.getResponse().getContentAsString(), AuthResponse.class);
        String clientToken = clientAuth.token();

        // 2. Cadastrar Conselheiro
        RegisterRequest counselorRegister = new RegisterRequest(
                "Conselheiro Teste",
                counselorEmail,
                "senhaForte123",
                Role.COUNSELOR,
                "79999992222",
                null
        );

        MvcResult counselorResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(counselorRegister)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        AuthResponse counselorAuth = objectMapper.readValue(counselorResult.getResponse().getContentAsString(), AuthResponse.class);
        String counselorToken = counselorAuth.token();

        // 3. Login do Conselheiro
        LoginRequest loginRequest = new LoginRequest(counselorEmail, "senhaForte123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // 4. Conselheiro cria perfil de especialista
        CreateSpecialistRequest specRequest = new CreateSpecialistRequest(
                "Consultor Sênior",
                "Finanças & Gestão",
                "Experiência com pequenos e médios empresários.",
                new BigDecimal("150.00")
        );

        MvcResult specResult = mockMvc.perform(post("/api/v1/specialists/me")
                        .header("Authorization", "Bearer " + counselorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(specRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specialty").value("Finanças & Gestão"))
                .andReturn();

        String specResponseJson = specResult.getResponse().getContentAsString();
        String specialistId = objectMapper.readTree(specResponseJson).get("id").asText();

        // 5. Consulta pública de especialistas
        mockMvc.perform(get("/api/v1/specialists")
                        .param("specialty", "Finanças"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // 5.1 Conselheiro cadastra disponibilidade para todos os dias da semana (09:00-12:00)
        for (String day : new String[]{"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"}) {
            mockMvc.perform(post("/api/v1/agenda")
                            .header("Authorization", "Bearer " + counselorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"dayOfWeek\":\"" + day + "\",\"startTime\":\"09:00\",\"endTime\":\"12:00\"}"))
                    .andExpect(status().isCreated());
        }

        // 5.2 Cliente consulta os horários livres e escolhe o primeiro
        MvcResult slotsResult = mockMvc.perform(get("/api/v1/agenda/specialists/" + specialistId + "/slots")
                        .param("from", LocalDate.now().plusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNotEmpty())
                .andReturn();
        OffsetDateTime firstSlot = OffsetDateTime.parse(objectMapper.readTree(slotsResult.getResponse().getContentAsString()).get(0).asText());

        // 6. Cliente solicita mentoria
        CreateMentoringRequest mentRequest = new CreateMentoringRequest(
                UUID.fromString(specialistId),
                "Organização de Fluxo de Caixa",
                "Preciso de ajuda com entradas e saídas",
                "Como calcular margem de lucro?",
                firstSlot
        );

        MvcResult mentResult = mockMvc.perform(post("/api/v1/mentorings")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.meetingLink").doesNotExist())
                .andReturn();

        String mentoringId = objectMapper.readTree(mentResult.getResponse().getContentAsString()).get("id").asText();

        // 7. Conselheiro aceita mentoria
        mockMvc.perform(patch("/api/v1/mentorings/" + mentoringId + "/accept")
                        .header("Authorization", "Bearer " + counselorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 8. Cliente busca próxima mentoria
        mockMvc.perform(get("/api/v1/mentorings/next")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mentoringId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 9. Cliente cria item no plano de ação
        mockMvc.perform(post("/api/v1/action-plans")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Separar contas física e jurídica\",\"conselheiroNome\":\"Conselheiro Teste\",\"deadline\":\"Até sexta\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completed").value(false));

        // 10. Consulta lista de tarefas
        mockMvc.perform(get("/api/v1/action-plans")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
