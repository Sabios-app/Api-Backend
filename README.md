# Sábios App - Backend

Backend do projeto acadêmico **Sábios App**, desenvolvido em **Spring Boot** com banco de dados **PostgreSQL**.

O backend é responsável pela API utilizada pelas versões Web e Mobile, pelos dados persistidos e pelas regras principais do sistema.

Este README apresenta uma proposta organizada de desenvolvimento para o time de seis pessoas, sem exigir práticas complexas desde o início do projeto.

---

## 📚 Documentação relacionada

- [Frontend Web](https://github.com/Sabios-app/Web-Front/blob/main/README.md) — aplicação web do ecossistema Sábios.
- [Frontend Moble](https://github.com/Sabios-app/Mobile/blob/main/README.md) — serviço responsável pelas
  integrações de backend.

---

## Tecnologias

- Java 21;
- Spring Boot;
- Spring Web;
- Spring Validation;
- Spring Security;
- Spring Data JPA;
- PostgreSQL;
- Flyway;
- Maven ou Gradle;
- JUnit 5 e Mockito;
- Docker para facilitar o ambiente local.

O time deve escolher **Maven ou Gradle** antes do início da implementação e utilizar apenas essa ferramenta em todo o projeto.

---

## Funcionalidades

As principais áreas que o backend deverá atender são:

- Cadastro, login e recuperação de senha;
- Perfis de cliente e conselheiro;
- Cadastro e busca de especialistas;
- Agenda e disponibilidade;
- Solicitações e mentorias;
- Planos de ação e tarefas;
- Mensagens entre usuários;
- Avaliações;
- Configurações da conta;
- Notificações.

As telas Web e Mobile devem consumir a mesma API. Regras importantes, como permissões e validações, devem ser implementadas no backend e não dependem da interface.

---

## Arquitetura sugerida

O projeto deve ser organizado por responsabilidade. Uma estrutura simples e recomendada é:

```text
src/main/java/com/sabios/
├── config/
├── shared/
├── auth/
├── users/
├── specialists/
├── agenda/
├── mentoring/
├── actionplan/
├── messages/
└── reviews/
```

Cada módulo pode conter:

```text
module/
├── controller/
├── dto/
├── service/
├── domain/
└── repository/
```

Responsabilidades básicas:

- **controller**: recebe e responde requisições HTTP;
- **dto**: representa os dados enviados ou recebidos pela API;
- **service**: contém regras de negócio e coordena o funcionamento;
- **domain**: representa as entidades principais do sistema;
- **repository**: acessa os dados no PostgreSQL;
- **config**: contém configurações da aplicação.

Fluxo esperado:

```text
Controller -> Service -> Repository -> Banco de dados
```

Também é correto reutilizar entidades JPA apenas internamente. A resposta da API deve preferencialmente usar DTOs.

---

## Organização das classes

Nomes recomendados:

- classes em `PascalCase`;
- métodos e atributos em `camelCase`;
- constantes em `UPPER_Snake_Case`;
- pacotes em letras minúsculas;
- nomes em português ou inglês, sem misturar dentro do mesmo módulo.

Exemplos:

```text
MentoringController
MentoringService
CreateMentoringRequest
MentoringResponse
MentoringRepository
```

Evite classes muito grandes e métodos sem nomes claros. Quando uma classe tiver muitas responsabilidades, separe o código em classes menores.

---

## API REST

### Endpoints

Use o padrão REST e o prefixo `/api/v1`:

```text
/api/v1/auth
/api/v1/users
/api/v1/specialists
/api/v1/agenda
/api/v1/mentorings
/api/v1/action-plans
/api/v1/messages
/api/v1/reviews
/api/v1/notifications
```

Exemplos:

```text
GET    /api/v1/specialists
GET    /api/v1/specialists/{id}
POST   /api/v1/mentorings
PATCH  /api/v1/mentorings/{id}/accept
GET    /api/v1/users/me
```

### Métodos HTTP

| Operação | Método |
|---|---|
| Buscar dados | `GET` |
| Criar um recurso | `POST` |
| Alterar tudo | `PUT` |
| Alterar parte | `PATCH` |
| Remover | `DELETE` |

### Códigos HTTP

| Situação | Código |
|---|---|
| Requisição concluída | `200 OK` |
| Recurso criado | `201 Created` |
| Recurso removido | `204 No Content` |
| Dados inválidos | `400 Bad Request` |
| Usuário não autenticado | `401 Unauthorized` |
| Usuário sem permissão | `403 Forbidden` |
| Recurso não encontrado | `404 Not Found` |
| Conflito ou duplicidade | `409 Conflict` |

---

## Contrato das respostas

As respostas devem usar JSON em `camelCase`.

Exemplo de sucesso:

```json
{
  "id": "b7e2a7ec-1a22-4ee7-8f5a-9189b218df11",
  "name": "Maria Silva",
  "role": "CLIENT"
}
```

Exemplo de erro:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Os dados enviados são inválidos.",
  "errors": [
    {
      "field": "email",
      "message": "Informe um e-mail válido."
    }
  ]
}
```

Sempre retorne uma mensagem compreensível. Não retorne SQL, stack trace ou informações internas da aplicação.

---

## Autenticação e autorização

Recomendações iniciais:

- usar Spring Security;
- proteger endpoints privados;
- usar senha criptografada com BCrypt;
- não salvar ou retornar a senha em texto puro;
- usar JWT ou OAuth/OIDC, caso o time decida implementar OAuth;
- definir a duração do token;
- validar o perfil do usuário no backend;
- impedir acesso de um perfil às áreas privadas de outro perfil.

Exemplos de regras:

- cliente não pode visualizar a agenda privada de outro cliente;
- conselheiro não pode alterar uma sessão de outro conselheiro;
- somente participantes podem enviar e ler mensagens de uma conversa;
- somente cliente pode solicitar uma mentoria;
- somente o conselheiro indicado pode aceitar ou recusar uma solicitação;
- uma avaliação só deve ser criada após uma mentoria concluída.

O frontend pode esconder botões ou bloquear telas, mas o backend deve fazer a validação real.

---

## Validação

Utilize Bean Validation nos DTOs de entrada:

```java
@NotBlank
@Email
@Size(max = 254)
private String email;
```

Validações importantes:

- campos obrigatórios;
- formato de e-mail;
- tamanho mínimo e máximo;
- datas válidas;
- perfil autorizado;
- existência dos recursos informados;
- duplicidade de cadastro ou solicitação;
- conflito de horários na agenda.

Use `@RestControllerAdvice` para centralizar o tratamento de erros e manter todas as respostas consistentes.

---

## PostgreSQL e banco de dados

### Boas práticas

- usar nomes de tabelas e colunas em `snake_case`;
- definir chave primária em todas as tabelas;
- criar chaves estrangeiras;
- impedir campos obrigatórios como nulos;
- criar índices em campos usados em filtros e consultas frequentes;
- não salvar arquivos ou imagens diretamente em tabelas;
- registrar `created_at` e `updated_at` quando forem necessários.

Exemplos de entidades iniciais:

```text
users
specialists
availabilities
mentorings
action_plans
tasks
conversations
messages
reviews
notifications
```

A modelagem final deve seguir os requisitos e evitar tabelas desnecessárias.

### Flyway

Todas as alterações do banco devem ser feitas por migrações Flyway.

```text
src/main/resources/db/migration/
├── V1__create_users.sql
├── V2__create_specialists.sql
├── V3__create_mentorings.sql
└── V4__create_messages.sql
```

Regras:

- não alterar uma migração já executada;
- criar uma nova migração para correções;
- testar migrações antes de enviar o Pull Request;
- não adicionar dados reais ou credenciais em scripts.

### JPA

- não usar `FetchType.EAGER`;
- observar consultas que podem causar problema N+1;
- usar transações apenas onde são necessárias;
- evitar retornar entidades diretamente pelos controllers;
- usar UUID ou uma estratégia de ID definida pelo time.

---

## Paginação

Listas grandes devem ser paginadas.

```text
GET /api/v1/specialists?page=0&size=20&sort=name,asc
```

Defina um tamanho máximo para evitar consultas excessivas.

Exemplo de resposta:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

---

## Datas e horários

- utilizar padrão ISO-8601;
- usar `Instant` ou `OffsetDateTime` para eventos;
- usar `LocalDate` para datas sem horário;
- guardar horários da agenda de forma que não gere ambiguidades;
- considerar o fuso horário dos usuários.

Exemplo:

```text
2026-11-18T14:30:00-03:00
```

---

## Integração Web e Mobile

Web e Mobile devem consumir o mesmo backend e seguir os mesmos contratos.

### Regras comuns

- usar JSON;
- usar propriedades em `camelCase`;
- usar datas no padrão ISO-8601;
- utilizar os mesmos códigos HTTP;
- utilizar os mesmos códigos de erro;
- utilizar paginação no mesmo formato;
- usar `/api/v1` no endereço das APIs;
- enviar o token no header `Authorization: Bearer <token>`;
- configurar CORS para os endereços oficiais do Web;
- implementar mudanças incompatíveis em uma nova versão da API.

O backend deve fornecer estados necessários às interfaces:

- sucesso;
- carregamento;
- lista vazia;
- erro;
- não autenticado;
- sem permissão;
- sessão expirada;
- recurso indisponível.

Qualquer mudança em endpoint ou DTO deve ser comunicada aos responsáveis pelo Web e pelo Mobile antes do merge.

---

## Testes

Priorize inicialmente testes para as regras mais importantes.

Usar:

- JUnit 5;
- Mockito;
- Testcontainers para PostgreSQL;
- MockMvc para testes de controllers.

Cenários básicos:

- cadastro realizado;
- dados inválidos;
- usuário não autenticado;
- usuário sem permissão;
- recurso não encontrado;
- conflito de horário;
- duplicidade de solicitação;
- acesso permitido ao proprietário dos dados.

Exemplo de nome de teste:

```text
shouldRejectMentoringRequestWhenUserIsNotClient
shouldNotAllowDoubleBookingAtTheSameTime
shouldReturnNotFoundWhenConversationDoesNotExist
```

Os testes podem começar menores e evoluir conforme o projeto avança.

---

## Segurança e logs

- não registrar senha ou token nos logs;
- não retornar detalhes internos em respostas HTTP;
- validar tamanho e tipo de arquivos enviados;
- aplicar limite de tentativas no login;
- configurar variáveis sensíveis por ambiente;
- não enviar credenciais para o Git;
- manter dependências atualizadas;
- permitir acesso aos endpoints administrativos apenas em ambientes controlados.

Exemplo de variáveis de ambiente:

```text
SPRING_PROFILES_ACTIVE=local
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/sabios
SPRING_DATASOURCE_USERNAME=sabios
SPRING_DATASOURCE_PASSWORD=<senha>
JWT_SECRET=<chave-secreta>
```

---

## Ambientes

Ambientes sugeridos:

- `local`: desenvolvimento da equipe;
- `test`: testes automatizados;
- `staging`: apresentação ou homologação;
- `production`: aplicação publicada.

Não use senhas reais em testes ou arquivos versionados. Um arquivo `.env.example` pode servir como modelo sem valores secretos.

---

## Organização do time

O time de seis pessoas pode dividir os módulos da seguinte forma:

| Responsabilidade | Desenvolvedor |
|---|---|
| Login, cadastro e segurança | Pessoa 1 |
| Usuários e especialistas | Pessoa 2 |
| Agenda e mentorias | Pessoa 3 |
| Planos de ação e avaliações | Pessoa 4 |
| Mensagens e notificações | Pessoa 5 |
| Testes, Docker e integração | Pessoa 6 |

A divisão ajuda na organização, mas não impede revisões cruzadas. Todos devem participar da validação dos principais contratos da API.

---

## Colaboração no Git

### Branches

- `main`: versão estável;
- `feat/nome-da-funcionalidade`;
- `fix/nome-do-ajuste`;
- `chore/nome-da-configuracao`;
- `docs/nome-da-documentacao`.

Não enviar commits diretamente para `main`.

### Commits

Cada commit deve conter:

1. **Título obrigatório**: informar de forma curta o que foi alterado;
2. **Descrição breve**: explicar o que foi adicionado ou corrigido.

Exemplo:

```text
Adicionar endpoint de login

Cria o endpoint de autenticação e valida o e-mail e a senha do usuário.
```

O título não deve ser genérico, como `update`, `ajustes` ou `commit`. A descrição pode conter uma ou duas frases objetivas.

### Pull Requests

Antes de abrir um Pull Request:

- atualizar a branch com a base;
- executar os testes;
- validar a migração do banco;
- verificar o contrato utilizado por Web e Mobile;
- solicitar pelo menos uma revisão de outra pessoa;
- resolver comentários antes do merge;
- não incluir arquivos locais, secrets ou credenciais.

Mudanças em banco, segurança ou contratos externos devem ter pelo menos duas revisões.

---

## Documentação da API

Utilizar Springdoc OpenAPI para disponibilizar a documentação.

Após iniciar o projeto:

```text
/swagger-ui/index.html
/v3/api-docs
```

Cada endpoint novo deve possuir uma descrição suficiente para permitir que Web e Mobile entendam:

- parâmetros;
- dados de entrada;
- dados de retorno;
- possíveis códigos de erro;
- regras de autorização.

---

## Comandos úteis

Com Maven:

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw verify
```

Com Docker:

```bash
docker compose up -d
docker compose down
```

Os comandos disponíveis podem mudar dependendo da ferramenta escolhida pelo time.

---

## Critérios de conclusão

Uma funcionalidade pode ser considerada concluída quando:

- o endpoint está documentado;
- os dados são validados;
- Web e Mobile conseguem utilizar o contrato;
- usuários sem autorização não conseguem executar a ação;
- a alteração foi aplicada no PostgreSQL por Flyway;
- existe pelo menos um teste relacionado;
- o build ou os testes passaram;
- outra pessoa revisou o Pull Request;
- nenhum segredo foi enviado ao repositório.
