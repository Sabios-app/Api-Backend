# Atualizações do Backend - Sábios App

Este documento registra o progresso, configurações e funcionalidades implementadas no backend em Spring Boot do projeto Sábios App.

---

# Estado atual por módulo

## 🚀 Versão Inicial e Infraestrutura

### 1. Compatibilização e Build
- **Java 21 LTS**: Ajustada a versão do Java no `pom.xml` para compatibilidade com o ambiente local.
- **Maven Wrapper**: Permissões de execução atribuídas ao `./mvnw`.
- **Docker Compose**: Criado `docker-compose.yml` para orquestração de container PostgreSQL 16 isolado na porta `5432`.
- **Variáveis de Ambiente**: Criado modelo `.env.example` com credenciais locais e parâmetros de JWT e CORS. Arquivo `.env` protegido no `.gitignore`.

### 2. Integração e Documentação
- **CORS Configurado**: Configurado `CorsConfig` liberando origens do React Web (`http://localhost:5173`) e aplicações locais, com suporte a headers de autorização e métodos REST (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`).
- **OpenAPI / Swagger UI**: Configurado Springdoc OpenAPI (`OpenApiConfig`) com esquema de autenticação JWT Bearer. Documentação visual interativa disponível em `/swagger-ui.html`.
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
- **Status das Mentorias (`MentoringStatus`)**: `CONFIRMED`, `REQUESTED`, `COMPLETED`, `REJECTED`, `CANCELED`, `EXPIRED`.
- **Entidade `Mentoring`**: Vinculada com `User` (cliente) e `Specialist` (conselheiro), com `title`, `description`, `doubts`, `scheduledAt`, `meetingLink` (Google Meet) e timestamps.

### 2. Endpoints Implementados
- `POST /api/v1/mentorings`: Cliente solicita mentoria (status inicial `REQUESTED`).
- `GET /api/v1/mentorings`: Lista as mentorias/solicitações do usuário logado (cliente ou conselheiro).
- `GET /api/v1/mentorings/next`: Retorna a próxima mentoria futura confirmada com o link de conferência virtual.
- `PATCH /api/v1/mentorings/{id}/accept`: Conselheiro indicado aceita a solicitação (`CONFIRMED`).
- `PATCH /api/v1/mentorings/{id}/reject`: Conselheiro indicado recusa a solicitação (`REJECTED`).
- `PATCH /api/v1/mentorings/{id}/cancel`: Cancelamento de mentoria futura pendente ou confirmada.
- `PATCH /api/v1/mentorings/{id}/complete`: Conclusão da mentoria após seu início.
- `PATCH /api/v1/mentorings/{id}/meeting-link`: Atualização do link de reunião pelo conselheiro.

---

## 📋 Módulo de Planos de Ação (`action-plans`)
- **Entidade `ActionPlan`**: tarefa do cliente (`client`), com `title`, `deadline` (texto, compatível com o front), `dueDate` (data), `completed`/`completedAt`, mentoria e conselheiro de origem (opcionais) e quem a criou (cliente ou conselheiro).
- `GET /api/v1/action-plans?completed=&overdue=&mentoringId=`: cliente vê as próprias tarefas; conselheiro vê as que atribuiu.
- `GET /api/v1/action-plans/mentorings/{mentoringId}`: tarefas de uma mentoria (cliente e conselheiro participantes).
- `POST /api/v1/action-plans`: cliente cria a própria tarefa (`mentoringId` e `dueDate` opcionais).
- `POST /api/v1/action-plans/mentorings/{mentoringId}`: conselheiro atribui uma tarefa ao cliente (mentoria confirmada ou concluída) e o cliente é notificado.
- `PATCH /api/v1/action-plans/{id}` e `DELETE /api/v1/action-plans/{id}`: apenas quem criou a tarefa.
- `PATCH /api/v1/action-plans/{id}/toggle`: o cliente conclui ou reabre (registra `completedAt`).
- Regras: prazo não pode estar no passado; `overdue` = pendente com `dueDate` vencida (fuso `America/Sao_Paulo`).

---

## 🗓️ Módulo de Agenda (`agenda`)
- **Entidade `Availability`**: Faixas de horários (`startTime`, `endTime`) e dias da semana dos especialistas (`MONDAY..SUNDAY`).
- `GET /api/v1/agenda/specialists/{specialistId}`: Consulta pública dos horários de atendimento de um conselheiro.
- `GET /api/v1/agenda/specialists/{specialistId}/slots`: Início de cada sessão livre de 60 min.
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
- **Entidade `Review`**: nota de 1 a 5 e comentário (até 1000 caracteres) que o cliente dá ao conselheiro por mentoria **concluída**; uma por mentoria.
- `GET /api/v1/reviews/specialists/{specialistId}?page=&size=`: lista pública (mais recentes primeiro). O avaliador aparece só como `{ name, avatarUrl }`.
- `GET /api/v1/reviews/specialists/{specialistId}/summary`: média, total e distribuição de 1 a 5 estrelas.
- `GET /api/v1/reviews/me`: cliente vê as que fez; conselheiro vê as que recebeu.
- `GET /api/v1/reviews/pending`: mentorias concluídas do cliente ainda sem avaliação.
- `POST /api/v1/reviews`: cliente avalia (exige mentoria `COMPLETED`); recalcula a nota do especialista e notifica o conselheiro.
- `PATCH /api/v1/reviews/{id}` e `DELETE /api/v1/reviews/{id}`: o autor edita ou remove (o administrador também remove); a média é recalculada. Sem avaliações, a nota volta a `5.00` (padrão do cadastro).

---

## 🔔 Notificações In-App (`notifications`)
- `GET /api/v1/notifications?unreadOnly=&page=&size=`: lista de notificações do usuário logado.
- `GET /api/v1/notifications/unread-count`: quantidade de notificações não lidas.
- `PATCH /api/v1/notifications/{id}/read`: marca notificação específica como lida.
- `PATCH /api/v1/notifications/read-all`: marca todas como lidas.

---

## 📱 Notificações Push Mobile via FCM (`notification`)
- **Infraestrutura**: Google Firebase Admin SDK para envio de push notifications (`firebase-admin` 9.4.3).
- **Configuração (`FirebaseConfig`)**: Suporte a carregamento de credenciais via arquivo JSON local (`firebase-service-account.json`) ou via variável de ambiente `FIREBASE_CREDENTIALS_JSON` (JSON inline para produção/nuvem).
- **Gestão de Dispositivos (`/api/v1/devices`)**:
  - `POST /api/v1/devices/token`: registra ou atualiza o token FCM do dispositivo.
  - `DELETE /api/v1/devices/token`: desativa o token FCM (logout).
  - `GET /api/v1/devices/me`: lista os dispositivos ativos do usuário autenticado.
- **Disparo de Push (`/api/v1/push-notifications`)**:
  - `POST /api/v1/push-notifications/send`: envio direto para os dispositivos de um usuário específico (`ADMIN`).
  - `POST /api/v1/push-notifications/send/broadcast`: envio em massa para todos os dispositivos ativos, com batching de até 500 tokens (`ADMIN`).
  - `POST /api/v1/push-notifications/send/topic`: envio para tópicos de inscrição (`ADMIN`).

---

## 🛠️ Seeds e Cargas Iniciais
- **`AdminSeeder` (`CommandLineRunner`)**: Criação automática do usuário administrador padrão (`admin@sabios.com.br` com role `ADMIN`) na inicialização da aplicação, caso ainda não exista no banco de dados.

---

## 🗄️ Migrações do Banco de Dados (`Flyway`)
- **`V1__create_initial_schema.sql`**: Tabelas fundamentais (`users`, `specialists`, `mentorings`).
- **`V2__update_mentorings_and_create_action_plans.sql`**: Suporte a links de chamada e criação de `action_plans`.
- **`V3__create_agenda_messages_reviews.sql`**: Tabelas `availabilities`, `conversations`, `messages` e `reviews`.
- **`V4__password_reset_and_scheduling.sql`**: Tokens de recuperação de senha e normalização de agenda.
- **`V5__notifications_and_security_hardening.sql`**: Notificações in-app e invalidação de JWT (`password_changed_at`).
- **`V6__action_plan_links_and_review_updates.sql`**: Vínculos de planos de ação e atualizações em avaliações.
- **`V7__create_device_tokens.sql`**: Armazenamento de tokens FCM de dispositivos (`device_tokens`).

---

## 📋 Próximos Passos
- [x] Conectar os serviços de API do Frontend React (`axios`) apontando para `http://localhost:8080/api/v1` (Autenticação, Especialistas, Mentorias, Planos de Ação).
- [x] Conectar os serviços de API do Mobile Flutter (`http` e `shared_preferences`) apontando para o backend com suporte a emulador e web.
- [x] Validar a execução conjunta dos serviços (Backend Spring Boot + Web Frontend Vite + PostgreSQL).
