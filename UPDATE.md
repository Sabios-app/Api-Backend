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

## 🔔 Módulo de Notificações e Push (`notification`)

### 1. Infraestrutura e Firebase Admin SDK
- **Dependência**: Adicionado `firebase-admin` (v9.4.3) ao `pom.xml`.
- **Configuração (`FirebaseConfig`)**: Suporte a carregamento de credenciais via arquivo JSON local (`src/main/resources/firebase-service-account.json`) ou via variável de ambiente `FIREBASE_CREDENTIALS_JSON` (JSON inline para ambiente de nuvem/produção).
- **Segurança de Métodos**: Habilitado `@EnableMethodSecurity` no `SecurityConfig` para proteger endpoints restritos a administradores com `@PreAuthorize("hasRole('ADMIN')")`.

### 2. Modelo de Domínio e Banco de Dados
- **Migration Flyway (`V2__create_device_tokens.sql`)**: Criação da tabela `device_tokens` com suporte a múltiplos dispositivos por usuário, status de ativação (`active`) e índices otimizados para busca por `fcm_token` e `user_id`.
- **Entidade `DeviceToken`**: Mapeada com JPA contendo relacionamento `@ManyToOne` com `User`, campos `device_info`, `fcm_token` e timestamps de auditoria.
- **Repositório `DeviceTokenRepository`**: Métodos para consulta por token, tokens ativos por usuário e todos os tokens ativos do sistema.

### 3. Regras de Negócio e Serviços
- **Gestão de Tokens (`DeviceTokenService`)**:
  - Registro e atualização de tokens FCM vinculados ao usuário autenticado (reatribui automaticamente caso o dispositivo mude de usuário).
  - Desativação de tokens sob demanda (ex: logout).
  - Consulta de tokens ativos com logs detalhados de auditoria.
- **Disparo de Notificações (`NotificationService`)**:
  - Envio individual para os aparelhos de um usuário específico (`sendToUser`).
  - Envio em massa/broadcast (`sendBroadcast`): particionamento automático em lotes de até 500 tokens (respeitando os limites do Firebase Multicast API).
  - Envio para tópicos do Firebase (`sendToTopic`).
  - Limpeza automática: desativação transparente de tokens expirados/desinstalados quando o Firebase reporta erro `UNREGISTERED`.

### 4. Endpoints Disponíveis
- `POST /api/v1/devices/token`: Registra ou atualiza o token FCM do dispositivo do usuário logado.
- `DELETE /api/v1/devices/token`: Desativa o token FCM especificado (logout).
- `GET /api/v1/devices/me`: Lista os dispositivos ativos registrados do usuário logado.
- `POST /api/v1/notifications/send`: Disparo de notificação para um usuário específico (restrito a `ADMIN`).
- `POST /api/v1/notifications/send/broadcast`: Disparo em massa para todos os dispositivos ativos (restrito a `ADMIN`).
- `POST /api/v1/notifications/send/topic`: Disparo para tópicos de inscrição (restrito a `ADMIN`).

---

## 🛠️ Seeds e Cargas Iniciais

- **`AdminSeeder` (`CommandLineRunner`)**: Criação automática do usuário administrador padrão (`admin@sabios.com.br` com role `ADMIN`) na inicialização da aplicação, caso ainda não exista no banco de dados.

---

## 📋 Próximos Passos
- [ ] **Módulo `agenda`**: Tabela de disponibilidades e horários livres dos conselheiros.
- [ ] **Módulo `mentorings`**: Solicitação, aceite e recusa de mentorias.
- [ ] **Seed de Dados**: Migração `V3__seed_initial_data.sql` com conselheiros e dados de teste para consumo do Frontend e Mobile.
