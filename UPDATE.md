# Atualizações do Backend

Registro resumido das mudanças realizadas no backend. A entrada mais recente deve ficar no início da página.

> Antes de abrir um Pull Request, adicione nesta página uma breve descrição das alterações relevantes e mantenha o texto direto e fácil de consultar pelo time.

## Como registrar uma atualização

Copie o modelo abaixo e substitua os dados pela versão correspondente:

```markdown
## YYYY-MM-DD - Título curto da entrega

- **Adicionado:** funcionalidade nova entregue.
- **Alterado:** comportamento ou configuração que mudou.
- **Corrigido:** problema resolvido.
- **Banco de dados:** migração, tabela ou coluna criada ou alterada.
- **Segurança:** ajuste de autenticação, autorização ou proteção.
- **Impacto:** endpoint, DTO, variável de ambiente ou ação necessária para Web/Mobile.
- **Relacionados:** #numero-da-pr ou nome da issue.
```

Use apenas as linhas aplicáveis à entrega. Evite detalhes internos irrelevantes para Web/Mobile e não informe senhas, tokens ou dados sensíveis.


---

## 2026-11-25 - Plano de ação completo e avaliações dos conselheiros

- **Adicionado (plano de ação):** tarefa ligada à mentoria e ao conselheiro; o conselheiro atribui tarefas ao cliente (`POST /action-plans/mentorings/{id}`) e o cliente é notificado; prazo estruturado `dueDate`; editar (`PATCH /action-plans/{id}`) e remover (`DELETE`); histórico (`?completed=true`), vencidas (`?overdue=true`) e por mentoria (`GET /action-plans/mentorings/{id}`); momento da conclusão (`completedAt`).
- **Adicionado (avaliações):** resumo com média e distribuição (`GET /reviews/specialists/{id}/summary`); editar e remover a própria avaliação; `GET /reviews/me`; `GET /reviews/pending` (mentorias concluídas ainda sem avaliação); notificação `REVIEW_RECEIVED` ao conselheiro.
- **Corrigido:** `POST /reviews` agora exige mentoria **concluída** (antes aceitava qualquer status); a lista pública de avaliações não expõe mais e-mail, telefone nem perfil do cliente.
- **Alterado:** `ActionPlanResponse` ganhou campos (`mentoringId`, `specialistId`, `dueDate`, `completedAt`, `overdue`, `assignedByCounselor`); os antigos continuam iguais. O `client` das avaliações passou a ser `{ name, avatarUrl }`. A lista de avaliações aceita `page` e `size` (máx. 50, padrão 50).
- **Banco de dados:** migração `V6` (colunas novas em `action_plans` e `reviews.updated_at`).
- **Impacto:** Web/Mobile não devem mais ler `client.email`, `client.phone`, `client.role` nem `client.id` das avaliações. Novos tipos de notificação: `ACTION_PLAN_ASSIGNED` e `REVIEW_RECEIVED`.

## 2026-11-25 - MVP de agendamento, recuperação de senha, notificações e segurança

- **Adicionado:** recuperação de senha (`/auth/forgot-password`, `/auth/reset-password`); filtros de busca (nota, preço, dia atendido); horários livres (`/agenda/specialists/{id}/slots`); cancelar, concluir e informar link das mentorias; histórico (`?scope=`); notificações (`/notifications`); expiração automática de solicitações vencidas (`EXPIRED`); rate limit no login e na recuperação de senha.
- **Alterado:** `dayOfWeek` da agenda agora é `MONDAY..SUNDAY`; a solicitação de mentoria valida disponibilidade e conflitos; `meetingLink` vem `null` até o conselheiro informar um link real; sem token a API responde `401` (antes `403`).
- **Segurança:** `register` recusa `role: ADMIN`; redefinir a senha invalida os JWT antigos.
- **Banco de dados:** migrações `V4` e `V5`.
- **Impacto:** Web/Mobile devem usar `dayOfWeek` em inglês, tratar `401`, `429` e o status `EXPIRED`, e o Web precisa da tela `/redefinir-senha?token=`. Detalhes na seção "MVP" abaixo.
- **Documentação:** `update.md` e `UPDATE.md` eram arquivos distintos no git (colidem no Windows); unificados neste arquivo.

## 2026-11-23 - Padronização do registro de mudanças

- **Adicionado:** criado este arquivo para centralizar as atualizações do backend.
- **Documentação:** inclusão de um acesso direto às atualizações na página inicial do README.

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
- **Entidade `Review`**: nota de 1 a 5 e comentário (até 1000 caracteres) que o cliente dá ao conselheiro por mentoria **concluída**; uma por mentoria.
- `GET /api/v1/reviews/specialists/{specialistId}?page=&size=`: lista pública (mais recentes primeiro). O avaliador aparece só como `{ name, avatarUrl }`.
- `GET /api/v1/reviews/specialists/{specialistId}/summary`: média, total e distribuição de 1 a 5 estrelas.
- `GET /api/v1/reviews/me`: cliente vê as que fez; conselheiro vê as que recebeu.
- `GET /api/v1/reviews/pending`: mentorias concluídas do cliente ainda sem avaliação.
- `POST /api/v1/reviews`: cliente avalia (exige mentoria `COMPLETED`); recalcula a nota do especialista e notifica o conselheiro.
- `PATCH /api/v1/reviews/{id}` e `DELETE /api/v1/reviews/{id}`: o autor edita ou remove (o administrador também remove); a média é recalculada. Sem avaliações, a nota volta a `5.00` (padrão do cadastro).

---

## 🔑 MVP: recuperação de senha, agendamento, busca, notificações e segurança

### Recuperação de senha
- `POST /api/v1/auth/forgot-password` `{ email }`: sempre responde `202`, exista ou não o e-mail (sem enumeração de usuários).
- `POST /api/v1/auth/reset-password` `{ token, newPassword }`: token de uso único, validade de 60 min (`400` se inválido/expirado).
- O token é aleatório e só o **hash SHA-256** fica no banco (`password_reset_tokens`).
- Envio por SMTP quando `MAIL_ENABLED=true` (+ `spring.mail.*`). Com `MAIL_ENABLED=false` (padrão), o link é escrito no log da aplicação (**somente dev**).
- O link aponta para `PASSWORD_RESET_URL_TEMPLATE` (padrão `http://localhost:5173/redefinir-senha?token=%s`); **o Web precisa criar essa tela**.
- **Segurança:** `POST /auth/register` agora recusa `role: ADMIN` (antes qualquer pessoa podia se cadastrar como administrador).

### Agenda
- `dayOfWeek` agora é o enum `MONDAY..SUNDAY` (antes texto livre). **Impacto Web/Mobile:** enviar/ler em inglês maiúsculo. A migração V4 converte nomes em português e remove linhas irreconhecíveis.
- Faixas de horário que se sobrepõem no mesmo dia retornam `409`.
- `GET /api/v1/agenda/specialists/{id}/slots?from=YYYY-MM-DD&to=YYYY-MM-DD` (público, até 31 dias): início de cada sessão livre, descontando mentorias confirmadas e horários passados.
- Sessão com duração fixa de 60 min (`SCHEDULING_SESSION_MINUTES`) no fuso `America/Sao_Paulo` (`SCHEDULING_ZONE`).

### Mentorias
- A solicitação agora exige que `scheduledAt` caiba na disponibilidade do conselheiro (`400` se não couber) e recusa conflitos (`409`): horário já confirmado para o conselheiro, ou cliente com solicitação/sessão ativa sobreposta. Não é possível solicitar para si mesmo.
- Pedidos pendentes não bloqueiam o horário; o conflito é verificado de novo no aceite. Solicitações e aceites do mesmo conselheiro são serializados por lock no banco.
- `PATCH /mentorings/{id}/cancel`: cliente ou conselheiro da mentoria cancelam `REQUESTED` ou `CONFIRMED` futura.
- `PATCH /mentorings/{id}/complete`: conselheiro conclui uma `CONFIRMED` cujo horário já começou. Libera a criação de avaliações.
- `GET /mentorings?status=&scope=`: `scope=UPCOMING` (pendentes/confirmadas futuras, da mais próxima) ou `HISTORY` (demais, da mais recente).
- **Link da reunião:** o link inventado foi removido. `meetingLink` é `null` até o conselheiro informar um link `https://` no aceite (corpo opcional `{ meetingLink }`) ou em `PATCH /mentorings/{id}/meeting-link`.

### Busca de especialistas
- `GET /specialists` aceita também `minRating`, `minPrice`, `maxPrice` e `availableDay` (`MONDAY..SUNDAY`), além de `specialty` e `search`. Ordenação via `sort=rating,desc`, `hourlyRate,asc`, `user.name,asc`. Tamanho máximo de página: 50.
- Valores inválidos retornam `400` (código `BAD_REQUEST` ou `INVALID_PARAMETER`).

### Segurança e robustez
- **Rate limit (429 + `Retry-After`):** `POST /auth/login` bloqueia após 5 falhas por e-mail (30 por IP) em 15 min; um login correto zera o contador. `POST /auth/forgot-password` aceita 3 pedidos por e-mail e 10 por IP por hora (conta mesmo para e-mail inexistente). Os limites ficam em memória: com mais de uma réplica, cada uma conta separado. Configurável por `RATE_LIMIT_*`. Em produção atrás de proxy, o IP vem de `X-Forwarded-For` (`server.forward-headers-strategy=native`).
- **JWT x troca de senha:** redefinir a senha invalida todos os tokens emitidos antes (`users.password_changed_at` + claim `iat`). O usuário precisa logar de novo.
- **401 padronizado:** sem token, token inválido ou expirado agora responde `401` com `{ status, code: "UNAUTHORIZED", message }`. Antes respondia `403`, contrariando o contrato. **Impacto Web/Mobile:** tratar `401` como sessão expirada.
- **Mentorias vencidas:** um job (a cada 5 min, `MENTORING_EXPIRATION_CHECK_MS`) marca `REQUESTED` com horário passado como `EXPIRED`. **Impacto Web/Mobile:** novo valor no enum `status` (aparece em `scope=HISTORY`).

### Notificações
- Geradas junto com o fato: nova solicitação → conselheiro (`MENTORING_REQUESTED`); aceite/recusa → cliente (`MENTORING_ACCEPTED`/`MENTORING_REJECTED`); cancelamento → a outra parte (`MENTORING_CANCELED`).
- `GET /api/v1/notifications?unreadOnly=&page=&size=`: paginado, mais recentes primeiro. Campos: `id, type, title, message, mentoringId, read, createdAt`.
- `GET /api/v1/notifications/unread-count` → `{ "count": n }` (contador do sino).
- `PATCH /api/v1/notifications/{id}/read` e `PATCH /api/v1/notifications/read-all`. Notificação de outro usuário responde `404`.

### Banco de dados
- **`V4__password_reset_and_scheduling.sql`**: tabela `password_reset_tokens`, normalização de `availabilities.day_of_week` e índice `mentorings(specialist_id, scheduled_at)`.
- **`V5__notifications_and_security_hardening.sql`**: tabela `notifications` e coluna `users.password_changed_at`.
- **`V6__action_plan_links_and_review_updates.sql`**: colunas novas em `action_plans` e `reviews.updated_at`.

### Testes
- Novas suítes `SchedulingAndRecoveryIntegrationTests` e `HardeningAndNotificationsIntegrationTests` e `ActionPlanAndReviewIntegrationTests` (recuperação de senha, agenda, conflitos, cancelar/concluir, histórico, filtros, rate limit, JWT, notificações, expiração). O teste fim a fim existente passou a cadastrar disponibilidade antes de agendar.

---

## 🗄️ Migração do Banco de Dados (`Flyway`)
- **`V1__create_initial_schema.sql`**: Tabelas fundamentais (`users`, `specialists`, `mentorings`) com UUIDs nativos e índices.
- **`V2__update_mentorings_and_create_action_plans.sql`**: Adiciona suporte a link de chamada (`meeting_link`) e dúvidas (`doubts`) em `mentorings`, além da criação da tabela `action_plans`.
- **`V3__create_agenda_messages_reviews.sql`**: Criação das tabelas `availabilities`, `conversations`, `messages` e `reviews`. Schema 100% limpo, sem dados fictícios.
- **`V4__password_reset_and_scheduling.sql`**: tokens de recuperação de senha, `day_of_week` padronizado e índice de conflito de horário.
- **`V5__notifications_and_security_hardening.sql`**: notificações e `password_changed_at` (invalidação de JWT).
- **`V6__action_plan_links_and_review_updates.sql`**: vínculos, prazo em data e `completed_at` no plano de ação; `reviews.updated_at`.

---

## 📋 Próximos Passos
- [ ] Conectar os serviços de API do Frontend React (`axios` / `fetch`) apontando para `http://localhost:8080/api/v1`.
- [ ] Conectar os serviços de API do Mobile Flutter (`dio` / `http`).
