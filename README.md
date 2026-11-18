# Sábios App - Backend

Backend do Sábios App desenvolvido em **Spring Boot**, responsável pela API compartilhada pelas aplicações web e mobile, pela autenticação e autorização, pelas regras de negócio e pela persistência dos dados no **PostgreSQL**.

Este documento define o contrato técnico inicial da equipe. As decisões aqui registradas devem ser seguidas por todos os membros do backend.

## Índice

- [Visão geral](#visão-geral)
- [Funcionalidades cobertas pelo sistema](#funcionalidades-cobertas-pelo-sistema)
- [Stack recomendada](#stack-recomendada)
- [Arquitetura](#arquitetura)
- [Estrutura obrigatória](#estrutura-obrigatória)
- [Regras de desenvolvimento](#regras-de-desenvolvimento)
- [API REST](#api-rest)
- [Autenticação e autorização](#autenticação-e-autorização)
- [PostgreSQL e persistência](#postgresql-e-persistência)
- [Validação e tratamento de erros](#validação-e-tratamento-de-erros)
- [Testes](#testes)
- [Logs, observabilidade e segurança](#logs-observabilidade-e-segurança)
- [Ambientes e configuração](#ambientes-e-configuração)
- [Integração com Web e Mobile](#integração-com-web-e-mobile)
- [Colaboração com 6 pessoas](#colaboração-com-6-pessoas)
- [Fluxo Git e Pull Requests](#fluxo-git-e-pull-requests)
- [Comandos iniciais](#comandos-iniciais)
- [Definição de pronto](#definição-de-pronto)

---

## Visão geral

O backend deve ser uma API única, estável e segura, consumida pelo frontend web e pelo aplicativo mobile. Os dois clientes devem receber os mesmos dados e obedecer às mesmas regras. A interface nunca deve conter a única validação de uma ação.

Principais responsabilidades:

- autenticação, cadastro e recuperação de senha;
- gerenciamento de perfis de cliente e conselheiro;
- busca e apresentação de especialistas;
- agenda, disponibilidade e solicitações de mentoria;
- planos de ação e tarefas;
- mensagens e conversas;
- avaliações e histórico de mentorias;
- configurações, comunicação e preferências;
- notificações e estados operacionais da aplicação.

## Funcionalidades cobertas pelo sistema

O backend deve implementar os seguintes domínios, mantendo consistência entre web e mobile:

| Domínio | Funcionalidades |
|---|---|
| Conta e identidade | cadastro, login, logout, redefinição de senha, perfil e papel |
| Especialistas | busca, filtros, ordenação, detalhes e avaliações |
| Cliente | visão geral, próximos atendimentos, histórico e plano de ação |
| Conselheiro | métricas, agenda, sessões, solicitações e avaliações |
| Agenda | disponibilidade, agendamento, cancelamento e reagendamento |
| Mentorias | solicitação, aceite, recusa, execução e conclusão |
| Plano de ação | tarefas, progresso e histórico |
| Mensagens | conversas, histórico, envio e marcação de leitura |
| Configurações | dados da conta, comunicação e acessibilidade |
| Notificações | contadores e avisos pendentes |

Qualquer campo exibido pelo frontend deve vir de uma API ou seguir uma constante definida pela API. Dados mockados não podem ser tratados como contrato definitivo.

## Stack recomendada

- Java 21 LTS;
- Spring Boot 3;
- Spring Web;
- Spring Validation;
- Spring Security;
- Spring Data JPA;
- PostgreSQL;
- Flyway para migrações de banco;
- Lombok somente quando realmente necessário;
- MapStruct ou mappers explícitos para DTO/entity;
- Springdoc OpenAPI para documentação da API;
- JUnit 5, Mockito e Testcontainers;
- Maven ou Gradle, escolhido uma única vez para todo o projeto;
- Docker e Docker Compose para ambiente local.

Não utilizar um framework ou biblioteca adicional sem justificativa técnica registrada em Pull Request.

## Arquitetura

Adotar arquitetura em camadas dentro de módulos por domínio:

```text
src/main/java/com/sabios/
├── config/
├── shared/
├── identity/
├── specialists/
├── agenda/
├── mentorship/
├── actionplan/
├── messaging/
├── review/
└── notification/
```

Cada domínio deve separar, sempre que necessário:

```text
domain/
application/
infra/
api/
```

Responsabilidade das camadas:

- `api`: controllers, DTOs de entrada/saída e mapeamento HTTP;
- `application`: casos de uso, serviços e regras orquestradas;
- `domain`: entidades, agregados, objetos de valor e regras internas;
- `infra`: repositórios JPA, integrações externas, bancos e serviços técnicos.

### Fluxo obrigatório

```text
Controller -> use case/service -> domain/repository -> database
```

Regras:

- controllers não acessam entidades JPA diretamente;
- repositórios não implementam lógica de apresentação;
- entidades não retornam diretamente pela API;
- regras de negócio não ficam em controllers;
- dependências apontam para dentro da aplicação, não para o framework;
- código deve poder ser testado sem iniciar o servidor HTTP.

## Estrutura obrigatória

Exemplo para um módulo de mentorias:

```text
mentorship/
├── api/
│   ├── MentorshipController.java
│   ├── MentorshipRequest.java
│   └── MentorshipResponse.java
├── application/
│   ├── MentorshipService.java
│   └── dto/
├── domain/
│   ├── Mentorship.java
│   ├── MentorshipStatus.java
│   └── MentorshipRepository.java
└── infra/
    ├── MentorshipEntity.java
    ├── MentorshipJpaRepository.java
    └── MentorshipRepositoryImpl.java
```

### Convenções de nome

- classes em `PascalCase`;
- métodos e variáveis em `camelCase`;
- constantes em `UPPER_Snake_Case`;
- pacotes em inglês e em letras minúsculas;
- controllers terminam com `Controller`;
- casos de uso terminam com `UseCase` ou `Service`;
- repositórios terminam com `Repository`;
- DTOs possuem nomes claros: `CreateUserRequest`, `UserResponse`, `UpdateProfileRequest`;
- nomes devem expressar comportamento, não o tipo técnico.

Evitar classes genéricas como `Manager`, `Helper`, `Utils`, `Common` e serviços gigantes.

## Regras de desenvolvimento

### Arquitetura e qualidade

- aplicar responsabilidade única;
- preferir composição e interfaces simples;
- não reutilizar entidades JPA como DTO;
- não criar métodos públicos sem necessidade;
- não ocultar exceções;
- não capturar `Exception` ou `Throwable` de forma genérica;
- não usar `System.out.println`;
- não fazer chamadas externas dentro de transações longas;
- não usar consultas SQL desnecessariamente complexas no controller ou service;
- evitar anotações que façam a classe acumular regras de apresentação, negócio e infraestrutura.

### Código legível

- métodos pequenos e com uma responsabilidade;
- nomes explícitos em português ou inglês: não misturar idiomas no mesmo módulo;
- no máximo uma abstração diferente por mesmo problema;
- comentários apenas para decisões não óbvias;
- documentação de API gerada pelo Springdoc;
- códigos de status sem magic numbers;
- exclusão lógica preferida quando o registro tiver histórico ou vínculo externo;
- exclusão física somente quando não houver dependência nem requisito de auditoria.

### Transações

- `@Transactional` deve ficar no caso de uso ou serviço de aplicação;
- operações de leitura devem usar `@Transactional(readOnly = true)`;
- não usar transações em métodos que apenas retornam dados serializáveis;
- toda operação que altera dados deve definir claramente onde começa e termina a transação;
- não manter transações abertas durante chamadas HTTP externas, filas ou aguardamento de entrada humana.

### Paginação e ordenação

Todas as listas externas devem ser paginadas:

```text
GET /api/v1/specialists?page=0&size=20&sort=displayName,asc
```

- limite máximo configurável para `size`;
- ordenação padrão documentada;
- respostas contendo `content`, `page`, `size`, `totalElements` e `totalPages`;
- nunca retornar uma coleção inteira sem necessidade.

### Datas e fusos

- persistir datas instantâneas em UTC;
- usar `Instant` ou `OffsetDateTime` para eventos;
- usar `LocalDate` e `LocalTime` somente para conceitos locais, como disponibilidade;
- nunca enviar datas sem indicar o formato ou o fuso;
- representar horários da agenda de forma inequívoca no contrato da API;
- calcular diferenças considerando o fuso do usuário.

## API REST

### Padrão de rotas

Usar versionamento por URL:

```text
/api/v1/users
/api/v1/auth
/api/v1/specialists
/api/v1/mentorings
/api/v1/messages
/api/v1/action-plans
/api/v1/reviews
/api/v1/notifications
```

Evitar ações em verbos:

```text
POST /api/v1/mentorings/{id}/accept
```

É aceitável quando a operação representa uma ação de domínio, mas o estado deve ser validado no backend.

### Códigos HTTP

| Situação | Status |
|---|---|
| Recurso criado | `201 Created` com `Location` |
| Busca/lista concluída | `200 OK` |
| Atualização concluída | `200 OK` |
| Remoção concluída | `204 No Content` |
| Recurso não encontrado | `404 Not Found` |
| Entrada inválida | `400 Bad Request` |
| Não autenticado | `401 Unauthorized` |
| Sem autorização | `403 Forbidden` |
| Conflito de estado ou recurso duplicado | `409 Conflict` |
| Limite excedido | `429 Too Many Requests` |

### Contrato de resposta

Erro padrão:

```json
{
  "timestamp": "2026-01-10T12:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Os dados enviados são inválidos.",
  "path": "/api/v1/users",
  "errors": [
    {
      "field": "email",
      "message": "Informe um email válido."
    }
  ],
  "traceId": "identificador-correlacionado"
}
```

Não expor stack trace, SQL, nome de arquivo, segredo ou detalhes internos em respostas HTTP.

### Idempotência

Operações de criação sensíveis devem validar duplicidade, especialmente:

- cadastro;
- solicitação de mentoria;
- envio de mensagem;
- avaliação;
- pagamento, caso seja implementado;
- operações de webhook.

Para ações críticas, usar chave de idempotência ou chave única no banco. Não permitir que a mesma solicitação gere dois registros ao repetir o request.

## Autenticação e autorização

- usar Spring Security;
- usar JWT acessível pelo cliente ou OAuth 2/OIDC, caso um provedor seja adotado;
- senhas com BCrypt ou Argon2;
- senhas nunca retornadas por endpoints;
- todos os endpoints privados devem exigir autenticação;
- `cliente` e `conselheiro` são papéis diferentes;
- autorização deve ser aplicada no backend, não apenas no frontend;
- proprietário do recurso deve ser validado no caso de uso;
- tokens devem ter expiração curta;
- refresh token deve ser protegido, rotacionado e revogado no logout;
- recuperação de senha deve usar token temporário de uso único;
- bloquear ou limitar tentativas de autenticação;
- configurar CORS apenas para origens reais de web e mobile.

Exemplos de regras:

- cliente não pode visualizar agenda privada de outro cliente;
- conselheiro não pode alterar sessão de outro conselheiro;
- somente participantes podem enviar ou ler mensagens da conversa;
- somente cliente pode solicitar uma mentoria;
- somente conselheiro pode aceitar ou recusar sua solicitação;
- somente participantes confirmados podem avaliar uma mentoria concluída.

Os papéis não devem ser enviados pelo cliente como prova de autorização. Eles devem ser derivados da sessão autenticada.

## PostgreSQL e persistência

### Modelagem

- nomes de tabelas e colunas em inglês, por padrão;
- usar `snake_case`;
- identificar entidades com `UUID` ou `BIGSERIAL`, de forma consistente;
- definir `PRIMARY KEY`, `FOREIGN KEY`, `UNIQUE`, `NOT NULL` e índices;
- não usar cascata para dados financeiros, auditoria ou históricos;
- evitar JSONB salvo como substituto de um modelo relacional sem justificativa;
- todo relacionamento com cardinalidade importante deve estar representado no banco;
- usar auditoria com `createdAt`, `createdBy`, `updatedAt` e `updatedBy` quando necessário;
- soft delete não deve impedir integridade referencial.

### JPA

- usar `@EntityGraph` ou `fetch join` quando evitar N+1;
- evitar `FetchType.EAGER`;
- preferir relacionamentos simples e bem definidos;
- usar consultas nomeadas ou derivadas curtas;
- usar JPQL ou QueryDSL para consultas complexas;
- toda query administrativa ou analítica deve ser indexada e testada com volume representativo;
- usar transações para operações concorrentes;
- não usar `saveAndFlush` fora de um cenário necessário.

### Migrações

- toda alteração de banco deve usar Flyway;
- arquivos em `src/main/resources/db/migration`;
- nomes sequenciais e únicos, por exemplo:

```text
V1__create_identity_tables.sql
V2__create_specialist_tables.sql
V3__create_mentoring_tables.sql
```

- nunca alterar uma migração já aplicada em ambiente compartilhado;
- criar uma nova migração para corrigir a anterior;
- testar migrações em banco vazio e em banco já populado;
- dados de seed devem ser separados por ambiente e não alterar produção.

### Concorrência

- evitar perda de atualização em agenda e solicitações;
- usar `@Version` em entidades que podem ser editadas em paralelo;
- converter conflitos de concorrência em `409 Conflict`;
- impedir dois atendimentos no mesmo horário, conforme a regra do produto;
- utilizar restrições no banco para conflitos quando possível.

## Validação e tratamento de erros

- validar entrada com Bean Validation;
- retornar todos os erros relevantes em uma única resposta;
- separar erros de validação, recurso inexistente, conflito e integração externa;
- usar exceções específicas no domínio;
- mapear exceções em `@RestControllerAdvice`;
- não usar `Optional.get()` sem garantia de presença;
- não confiar em IDs enviados pelo cliente sem confirmar propriedade;
- não permitir dados com espaços vazios ou campos fora do contrato;
- validar tamanho máximo de texto, lista, arquivo e upload;
- não sanitizar dados antes da validação sem explicar o efeito no valor original.

Exemplos Bean Validation:

```java
@NotBlank
@Email
@Size(max = 254)
private String email;
```

## Testes

### Pirâmide de testes

- testes unitários para regras de domínio;
- testes de integração para JPA, Flyway e PostgreSQL;
- testes de serviço/caso de uso com mocks limitados;
- testes de controller com `@WebMvcTest` ou MockMvc;
- testes de contrato para integrações externas;
- cenários de ponta a ponta apenas para fluxos críticos.

Ferramenta recomendada:

- JUnit 5;
- Mockito;
- AssertJ;
- Testcontainers com PostgreSQL;
- MockMvc;
- WireMock ou cliente HTTP mockado para integrações externas.

### Cenários obrigatórios

Cada funcionalidade deve testar:

- sucesso;
- entrada inválida;
- não autenticado;
- sem autorização;
- recurso inexistente;
- conflito;
- repetição da operação;
- transação concorrente quando aplicável;
- limites e paginação.

Métodos de teste devem ter nomes que expressem o cenário:

```text
shouldRejectMentoringRequestWhenUserIsNotClient
shouldNotDoubleBookCounselorAtTheSameTime
shouldReturnNotFoundWhenConversationDoesNotExist
```

Não testar implementação privada. Testar regras, resultado e contratos.

## Logs, observabilidade e segurança

- usar SLF4J;
- incluir `traceId` ou `correlationId` em logs;
- registrar eventos importantes de login, alteração de senha e mudança de estado;
- não registrar senha, token, documento, cartão ou payload completo sensível;
- mascarar dados pessoais em logs;
- configurar níveis por ambiente;
- usar métricas de aplicação com Micrometer;
- monitorar latência, taxa de erro e uso de recursos;
- health checks separados para aplicação e dependências;
- não deixar endpoint de atuadores administrativos público;
- aplicar rate limit em login, recuperação de senha e mensagens;
- validar arquivos por tipo, tamanho e conteúdo;
- armazenar arquivos em serviço externo ou diretório configurado, nunca no banco;
- manter dependências atualizadas e executar análise de vulnerabilidades.

## Ambientes e configuração

Ambientes mínimos:

- `local`;
- `test`;
- `staging`;
- `production`.

- usar variáveis de ambiente para secrets;
- não adicionar `.env` ou credenciais ao repositório;
- usar Spring Profiles;
- manter `.env.example` ou um modelo seguro de configuração;
- não usar credenciais reais em testes;
- separar banco, chaves e logs por ambiente;
- produção deve ter configuração de memória, pool de conexões e timeouts.

Exemplo de variáveis:

```text
SPRING_PROFILES_ACTIVE=local
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sabios
SPRING_DATASOURCE_USERNAME=sabios
SPRING_DATASOURCE_PASSWORD=<secret>
JWT_SECRET=<secret>
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

## Integração com Web e Mobile

Web e mobile devem consumir exatamente a mesma API. Nenhuma regra crítica pode existir apenas em um dos clientes.

Contrato mínimo:

- JSON como formato padrão;
- UTF-8;
- camelCase em propriedades JSON;
- ISO-8601 para datas;
- paginação padronizada;
- estados de API padronizados;
- códigos de erro estáveis;
- política de CORS configurada no backend;
- suporte a internacionalização na resposta, quando necessário;
- versionamento da API para mudanças incompatíveis.

Estados que o frontend deve receber ou representar:

- carregando;
- vazio;
- sucesso;
- erro;
- não autorizado;
- recurso indisponível;
- sessão expirada.

Mudanças incompatíveis em endpoints exigem uma nova versão ou migração coordenada com Web e Mobile.

## Colaboração com 6 pessoas

Como o time possui seis pessoas, os trabalhos devem ser divididos por domínio para reduzir conflitos:

| Área sugerida | Responsável primário |
|---|---|
| Identidade e segurança | 1 pessoa |
| Especialistas e perfis | 1 pessoa |
| Agenda e mentorias | 1 pessoa |
| Plano de ação e avaliações | 1 pessoa |
| Mensagens e notificações | 1 pessoa |
| Integrações, infraestrutura e qualidade | 1 pessoa |

As áreas são referências de ownership, não silos: todos devem revisar código de outros módulos e compartilhar conhecimento.

Boas práticas do time:

- um PR deve resolver uma única tarefa;
- não iniciar duas implementações concorrentes no mesmo domínio sem coordenação;
- abrir a tarefa com contrato de API antes da implementação;
- manter a documentação OpenAPI atualizada;
- atualizar o README ou os ADRs quando uma decisão técnica mudar;
- registrar decisões relevantes em ADR (`docs/adr`);
- pair programming ou revisão cruzada em migrações críticas;
- nenhum membro deve manter um trabalho local por mais de uma ou duas semanas sem integração;
- bugs críticos devem ser corrigidos em branch curta e cobertos por regressão;
- reuniões técnicas devem produzir decisões escritas, não apenas acordos orais.

## Fluxo Git e Pull Requests

Branches:

- `main`: código estável;
- `develop`: integração, quando o processo do projeto já utilizar essa branch;
- `feat/nome-da-funcionalidade`;
- `fix/nome-do-ajuste`;
- `chore/nome-da-configuracao`;
- `docs/nome-da-documentacao`;
- `refactor/nome-da-refatoracao`.

Regras:

- nunca enviar código diretamente para `main`;
- criar branch a partir da branch base atualizada;
- manter commits pequenos e funcionais;
- usar Conventional Commits:

```text
feat: create mentoring request endpoint
fix: prevent duplicate agenda booking
test: cover expired password reset token
docs: document authentication contract
chore: configure Flyway
```

- manter o PR atualizado com a branch base;
- solicitar pelo menos duas revisões em mudanças de segurança, banco ou arquitetura;
- para PRs comuns, exigir pelo menos uma revisão;
- o autor não pode aprovar o próprio código;
- PR deve indicar endpoint, migração, feature flags e impactos em web/mobile;
- comentários de revisão devem ser resolvidos antes do merge;
- merge preferencialmente por squash ou merge strategy definida pelo time.

## Comandos iniciais

Os comandos podem variar conforme Maven ou Gradle.

```bash
# Maven
./mvnw spring-boot:run
./mvnw test
./mvnw verify

# Gradle
./gradlew bootRun
./gradlew test
./gradlew check
```

Ambiente local com Docker:

```bash
docker compose up -d
```

Swagger/OpenAPI local, após iniciar a aplicação:

```text
/swagger-ui.html
/v3/api-docs
```

## Definição de pronto

Uma entrega somente está pronta quando:

- [ ] a funcionalidade segue o contrato acordado com Web e Mobile;
- [ ] cadastro, consulta, alteração e exclusão possuem regras consistentes;
- [ ] autenticação e autorização foram validadas;
- [ ] entrada inválida gera resposta padronizada;
- [ ] PostgreSQL e migrações Flyway foram testados;
- [ ] testes unitários e de integração passaram;
- [ ] não existem dados sensíveis em logs ou respostas;
- [ ] endpoints estão documentados no OpenAPI;
- [ ] consultas críticas foram analisadas quanto a N+1 e índices;
- [ ] migrações foram testadas em banco vazio e existente;
- [ ] o PR foi revisado por outro membro;
- [ ] os impactos para Web e Mobile estão descritos;
- [ ] o build e os testes passaram no CI;
- [ ] o commit não contém secrets, arquivos locais ou credenciais.