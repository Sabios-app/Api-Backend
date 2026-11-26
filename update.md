# Atualizações do Backend - Sábios App

Este documento registra o progresso, configurações e funcionalidades implementadas no backend em Spring Boot do projeto Sábios App.

---

## 🚀 Versão Inicial e Infraestrutura

### 1. Compatibilização e Build
- **Java 21 LTS**: Ajustada a versão do Java no `pom.xml` para compatibilidade com o ambiente local.
- **Maven Wrapper**: Permissões de execução atribuídas ao `./mvnw`.
- **Docker Compose**: Criado `docker-compose.yml` para orquestração de container PostgreSQL 16 isolado na porta `5432`.
- **Variáveis de Ambiente**: Criado modelo `.env.example` com credenciais locais e parâmetros de JWT e CORS. Arquivo `.env` protegido no `.gitignore`.

### 2. Integração e Documentação
- **CORS Configurado**: Configurado `CorsConfig` liberando origens do React Web (`http://localhost:5173`) e aplicações locais, com suporte a headers de autorização e métodos REST (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`).
- **OpenAPI / Swagger UI**: Configurado Springdoc OpenAPI (`OpenApiConfig`) com esquema de autenticação JWT Bearer. Documentação visual interativa disponível em:
  - `/swagger-ui/index.html`
  - `/v3/api-docs`
- **Flyway Migrations**: Adicionado `V1__create_initial_schema.sql` para criação automática das tabelas essenciais (`users`, `specialists`, `mentorings`) utilizando UUIDs (`pgcrypto`), chaves estrangeiras e índices otimizados.

---

## 🔐 Autenticação e Usuários (Módulo `auth` e `users`)

### 1. Modelo de Domínio
- **Entidade `User`**: Mapeada com JPA contendo `id` (UUID), `name`, `email` único, `password_hash`, `role` (`CLIENT`, `COUNSELOR`, `ADMIN`), telefone, avatar e timestamps de auditoria (`created_at`, `updated_at`).
- **Repositório `UserRepository`**: Métodos para busca e verificação de duplicidade por e-mail.

### 2. Segurança e Criptografia
- **BCrypt**: Hashing seguro de senhas com `PasswordEncoder`. Nenhuma senha em texto puro é gravada ou trafegada.
- **Token JWT (Auth0)**: `TokenService` implementado para emissão e validação de tokens stateless assinados com HMAC-256 (duração padrão de 24 horas).
- **Filtro JWT**: `JwtAuthenticationFilter` configurado na cadeia do Spring Security para interceptar cabeçalhos `Authorization: Bearer <token>` e registrar a sessão autenticada.

### 3. Endpoints Implementados e Testados
- `GET /api/v1/status`: Health check da aplicação para monitoramento.
- `POST /api/v1/auth/register`: Cadastro de novos usuários com validação de dados e retorno imediato de token JWT.
- `POST /api/v1/auth/login`: Autenticação por e-mail/senha com retorno do token JWT.
- `GET /api/v1/users/me`: Retorno do perfil do usuário logado via token Bearer.

### 4. Tratamento Global de Erros (`GlobalExceptionHandler`)
- Padronização conforme contrato REST do projeto:
  - `400 Bad Request`: Erros de Bean Validation detalhando campos e mensagens.
  - `401 Unauthorized`: Credenciais inválidas no login ou token ausente/expirado.
  - `404 Not Found`: Recurso não localizado (ex: especialista inexistente).
  - `409 Conflict`: E-mail já cadastrado ou perfil já existente.

---

## 👨‍💼 Módulo de Especialistas (`specialists`)

### 1. Modelo de Domínio
- **Entidade `Specialist`**: Mapeada com JPA vinculada a `User` (1:1), contendo `title`, `specialty`, `bio`, `rating`, `hourly_rate`, `is_available` e timestamps de auditoria.
- **Repositório `SpecialistRepository`**: Consulta otimizada com `@EntityGraph` para evitar N+1 ao buscar o usuário associado, além de query personalizada para busca textual por cargo, nome ou especialidade.

### 2. DTOs e Regras de Negócio
- `SpecialistResponse`: Dados estruturados para alimentar os cartões e detalhes da aplicação Web e Mobile.
- `CreateSpecialistRequest` & `UpdateSpecialistRequest`: Validações de tamanho, obrigatoriedade e valores não-negativos para hora.
- `SpecialistService`: Regras para criação e edição de perfil (restrito a conselheiros/administradores) e consulta paginada.

### 3. Endpoints Disponíveis
- `GET /api/v1/specialists`: Listagem pública paginada com suporte a filtros por especialidade e termo de busca.
- `GET /api/v1/specialists/{id}`: Detalhes de um especialista específico.
- `POST /api/v1/specialists/me`: Cadastro do perfil profissional do conselheiro autenticado.
- `PATCH /api/v1/specialists/me`: Atualização de informações de perfil do conselheiro autenticado.

---

## 🤝 Módulo de Mentorias (`mentorings`)

### 1. Modelo de Domínio e Status
- **Status das Mentorias (`MentoringStatus`)**: `CONFIRMED`, `REQUESTED`, `COMPLETED`, `REJECTED`, `CANCELED`.
- **Entidade `Mentoring`**: Vinculada com `User` (cliente) e `Specialist` (conselheiro), com `title`, `description`, `doubts`, `scheduledAt`, `meetingLink` (Google Meet) e timestamps.

### 2. Endpoints Implementados
- `POST /api/v1/mentorings`: Cliente solicita mentoria (status inicial `REQUESTED`).
- `GET /api/v1/mentorings`: Lista as mentorias/solicitações do usuário logado (cliente ou conselheiro).
- `GET /api/v1/mentorings/next`: Retorna a próxima mentoria futura confirmada com o link de conferência virtual.
- `PATCH /api/v1/mentorings/{id}/accept`: Conselheiro indicado aceita a solicitação (`CONFIRMED`).
- `PATCH /api/v1/mentorings/{id}/reject`: Conselheiro indicado recusa a solicitação (`REJECTED`).

---

## 📋 Módulo de Planos de Ação (`action-plans`)

### 1. Modelo e Endpoints
- **Entidade `ActionPlan`**: Tarefas e metas vinculadas ao cliente (`client_id`, `title`, `deadline`, `conselheiroNome`, `completed`).
- `GET /api/v1/action-plans`: Lista tarefas do cliente autenticado ordenadas por data.
- `POST /api/v1/action-plans`: Cria nova meta vinculada ao cliente.
- `PATCH /api/v1/action-plans/{id}/toggle`: Alterna status entre concluído e pendente, validando posse do usuário.

---

## 🗓️ Módulo de Agenda (`agenda`)
- **Entidade `Availability`**: Faixas de horários (`startTime`, `endTime`) e dias da semana dos especialistas.
- `GET /api/v1/agenda/specialists/{specialistId}`: Consulta pública dos horários de atendimento de um conselheiro.
- `GET /api/v1/agenda/me`: Horários cadastrados pelo conselheiro logado.
- `POST /api/v1/agenda`: Conselheiro adiciona novo horário disponível na semana.
- `DELETE /api/v1/agenda/{id}`: Conselheiro remove um horário.

---

## 💬 Módulo de Mensagens & Chat (`messages`)
- **Entidades `Conversation` e `Message`**: Histórico de chat entre cliente e conselheiro.
- `GET /api/v1/messages/conversations`: Lista todas as conversas do usuário logado ordenadas pela mensagem mais recente.
- `GET /api/v1/messages/conversations/{id}`: Histórico completo de mensagens de uma conversa específica (apenas participantes autorizados).
- `POST /api/v1/messages`: Envio de mensagem (criação automática da conversa caso seja o primeiro contato).

---

## ⭐ Módulo de Avaliações (`reviews`)
- **Entidade `Review`**: Avaliação de 1 a 5 estrelas vinculada à mentoria realizada.
- `GET /api/v1/reviews/specialists/{specialistId}`: Consulta pública das avaliações e comentários recebidos pelo especialista.
- `POST /api/v1/reviews`: Cliente avalia mentoria concluída. A média geral (`rating`) do especialista é recalculada automaticamente no banco.

---

## 🗄️ Migração do Banco de Dados (`Flyway`)
- **`V1__create_initial_schema.sql`**: Tabelas fundamentais (`users`, `specialists`, `mentorings`) com UUIDs nativos e índices.
- **`V2__update_mentorings_and_create_action_plans.sql`**: Adiciona suporte a link de chamada (`meeting_link`) e dúvidas (`doubts`) em `mentorings`, além da criação da tabela `action_plans`.
- **`V3__create_agenda_messages_reviews.sql`**: Criação das tabelas `availabilities`, `conversations`, `messages` e `reviews`. Schema 100% limpo, sem dados fictícios.

---

## 📋 Próximos Passos
- [ ] Conectar os serviços de API do Frontend React (`axios` / `fetch`) apontando para `http://localhost:8080/api/v1`.
- [ ] Conectar os serviços de API do Mobile Flutter (`dio` / `http`).
