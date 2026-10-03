# API Design

# Enterprise Workflow Engine

**Versão:** 0.2
**Status:** Aprovado

---

# 1. Objetivo

Este documento define as convenções e os contratos da API REST do **Enterprise Workflow Engine**
(RF-024 a RF-026).

A referência exata de campos e schemas é o documento OpenAPI gerado pela própria aplicação:

```text
GET /v3/api-docs        documento OpenAPI (JSON)
GET /swagger-ui.html    interface interativa
```

Ambos ficam desabilitados no perfil `prod`.

---

# 2. Convenções

| Tema            | Convenção                                                                              |
| --------------- | -------------------------------------------------------------------------------------- |
| Base path       | `/api/v1`. Mudanças incompatíveis geram `/api/v2`                                      |
| Formato         | JSON (`application/json`); erros em `application/problem+json`                         |
| Nomes           | recursos no plural em kebab-case; campos em camelCase                                  |
| Identificadores | UUID para Workflows; `key` (kebab-case) para Workflow Definitions                      |
| Datas           | ISO-8601 em UTC (`2026-10-02T06:23:06.188864Z`)                                         |
| Enumerações     | texto em maiúsculas (`RUNNING`, `GREATER_THAN`)                                         |
| Autenticação    | sessão por cookie (console web) ou HTTP Basic (integrações); ver `SECURITY.md`          |
| CSRF            | requisições com sessão que alteram estado enviam `X-XSRF-TOKEN` (§8)                    |
| Tamanho         | corpo de requisição limitado a 256 KB                                                  |
| Correlação      | `X-Request-Id` aceito na requisição e sempre devolvido na resposta                     |
| Idioma          | mensagens da API em inglês, independentemente do locale                                |

Operações que mudam o estado de um recurso sem substituí-lo são modeladas como **sub-recursos de ação**
com `POST` (`/start`, `/cancel`, `/activate`), e não como `PATCH` de um campo de status: a mudança de
estado é uma operação de domínio com regras, não uma edição de atributo.

A API nunca expõe entidades do domínio diretamente; cada módulo possui seus DTOs de entrada e saída.

---

# 3. Paginação

Endpoints de listagem aceitam:

| Parâmetro | Padrão              | Observação                         |
| --------- | ------------------- | ---------------------------------- |
| `page`    | `0`                 | índice da página, iniciando em 0   |
| `size`    | `20`                | máximo `100`                       |
| `sort`    | depende do endpoint | `campo,asc` ou `campo,desc`        |

Resposta:

```json
{
  "content": [],
  "page": { "size": 20, "number": 0, "totalElements": 0, "totalPages": 0 }
}
```

---

# 4. Erros

Todos os erros seguem a RFC 9457 (Problem Details):

```json
{
  "title": "Rule not satisfied",
  "status": 422,
  "detail": "Action 'approve' was rejected because its rules are not satisfied",
  "instance": "/api/v1/workflows/bcd72a31-c0e4-4d7e-a851-38e379880cef/actions",
  "unsatisfiedRules": ["amount LESS_THAN_OR_EQUAL 10000"]
}
```

| Status | Quando                                                                                        | Campos extras      |
| ------ | --------------------------------------------------------------------------------------------- | ------------------ |
| `400`  | corpo malformado, campo obrigatório ausente, parâmetro com tipo ou valor inválido             | `errors[]`         |
| `401`  | credenciais ausentes ou inválidas                                                             |                    |
| `403`  | sem permissão para o endpoint, Actor sem o papel exigido pela Transition, ou token CSRF ausente/inválido (`title: Invalid CSRF token`) | |
| `404`  | recurso inexistente                                                                           |                    |
| `409`  | conflito com o estado atual: chave duplicada, status incompatível, ação indisponível no State atual, modificação concorrente, regra de administração de contas | `code` (contas) |
| `413`  | corpo da requisição acima de 256 KB                                                          |                    |
| `429`  | muitas falhas de login a partir da mesma origem                                             | header `Retry-After` |
| `422`  | requisição bem formada, mas recusada por regra: estrutura de definição inválida, definição sem versão ativa, Rule não satisfeita | `violations[]`, `unsatisfiedRules[]` |
| `500`  | erro inesperado; a resposta não expõe detalhes internos                                       |                    |

Um `409` por modificação concorrente indica que outra operação alterou o recurso primeiro: o consumidor
deve reler o recurso e decidir se repete a operação.

---

# 5. Workflow Definitions

Base: `/api/v1/workflow-definitions`. Escrita restrita ao papel `ADMIN`.

| Método | Caminho                                  | Caso de uso | Sucesso |
| ------ | ---------------------------------------- | ----------- | ------- |
| `POST` | `/`                                      | UC-001      | `201`   |
| `GET`  | `/`                                      | UC-005      | `200`   |
| `GET`  | `/{key}`                                 | UC-005      | `200`   |

A listagem informa `activeVersion` (número da versão ativa ou `null`) de cada definição.
| `POST` | `/{key}/versions`                        | UC-002      | `201`   |
| `GET`  | `/{key}/versions/{number}`               | UC-005      | `200`   |
| `POST` | `/{key}/versions/{number}/activate`      | UC-003      | `200`   |
| `POST` | `/{key}/versions/{number}/deactivate`    | UC-004      | `200`   |

Criação (a primeira versão nasce como `DRAFT`):

```json
{
  "key": "purchase-approval",
  "name": "Purchase Approval",
  "description": "Aprovação de compras",
  "states": [
    { "name": "REQUESTED", "type": "INITIAL" },
    { "name": "PENDING_APPROVAL", "type": "INTERMEDIATE" },
    { "name": "APPROVED", "type": "TERMINAL" },
    { "name": "REJECTED", "type": "TERMINAL" }
  ],
  "transitions": [
    { "action": "submit", "from": "REQUESTED", "to": "PENDING_APPROVAL" },
    {
      "action": "approve", "from": "PENDING_APPROVAL", "to": "APPROVED",
      "requiredRole": "MANAGER",
      "rules": [{ "field": "amount", "operator": "LESS_THAN_OR_EQUAL", "value": "10000" }]
    },
    { "action": "reject", "from": "PENDING_APPROVAL", "to": "REJECTED", "requiredRole": "MANAGER" }
  ]
}
```

`POST /{key}/versions` recebe apenas `states` e `transitions`.

Regras de formato:

- `key`: kebab-case minúsculo, até 64 caracteres;
- nomes de State e de ação: iniciam por letra; letras, dígitos, `_` e `-`; até 64 caracteres;
- `type`: `INITIAL`, `INTERMEDIATE` ou `TERMINAL`;
- `operator`: `EQUALS`, `NOT_EQUALS`, `GREATER_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN`,
  `LESS_THAN_OR_EQUAL`, `EXISTS` (o único que dispensa `value`);
- `field`: nome da variável; níveis aninhados separados por ponto (`purchase.amount`).

As violações estruturais (BR-002) retornam `422` com a lista completa em `violations`.

---

# 6. Workflows

Base: `/api/v1/workflows`. Exige autenticação; ações podem exigir papéis conforme a definição.

| Método | Caminho          | Caso de uso     | Sucesso |
| ------ | ---------------- | --------------- | ------- |
| `POST` | `/`              | UC-006          | `201`   |
| `GET`  | `/`              | UC-008          | `200`   |
| `GET`  | `/summary`       | —               | `200`   |
| `GET`  | `/{id}`          | UC-008          | `200`   |
| `POST` | `/{id}/start`    | UC-007          | `200`   |
| `GET`  | `/{id}/actions`  | —               | `200`   |
| `POST` | `/{id}/actions`  | UC-009          | `200`   |
| `POST` | `/{id}/cancel`   | UC-012          | `200`   |
| `GET`  | `/{id}/history`  | UC-010          | `200`   |

Criação — usa a versão ativa da definição:

```json
{ "definitionKey": "purchase-approval", "variables": { "amount": 15000 } }
```

Execução de ação — as variáveis enviadas são combinadas às existentes antes da avaliação das Rules:

```json
{ "action": "approve", "variables": { "note": "dentro do orçamento" } }
```

Cancelamento — corpo opcional:

```json
{ "reason": "solicitação duplicada" }
```

Representação do Workflow:

```json
{
  "id": "bcd72a31-c0e4-4d7e-a851-38e379880cef",
  "definitionKey": "purchase-approval",
  "definitionVersion": 1,
  "status": "RUNNING",
  "currentState": "PENDING_APPROVAL",
  "variables": { "amount": 15000 },
  "createdAt": "2026-10-02T06:23:06.188864Z",
  "updatedAt": "2026-10-02T06:23:07.091244Z"
}
```

`status` é o ciclo de vida da execução (`CREATED`, `RUNNING`, `COMPLETED`, `CANCELLED`); `currentState`
é o State do processo (ADR-006).

`GET /` aceita os filtros opcionais `definitionKey` e `status`.

`GET /summary` devolve a quantidade de Workflows por status, incluindo os status sem nenhum Workflow:

```json
{ "total": 12, "byStatus": { "CREATED": 2, "RUNNING": 5, "COMPLETED": 4, "CANCELLED": 1 } }
```

`GET /{id}/actions` lista as ações disponíveis a partir do State atual, com o State de destino e o papel
exigido. Não avalia Rules: uma ação listada ainda pode ser recusada.

---

# 7. Audit

Base: `/api/v1/audit-records`. Restrito ao papel `ADMIN` (UC-011).

| Método | Caminho | Filtros opcionais                                                |
| ------ | ------- | ---------------------------------------------------------------- |
| `GET`  | `/`     | `actorId`, `operation`, `resourceType`, `resourceId`, `outcome`  |

Operações registradas:

| `resourceType`        | `operation`                                                                                             |
| --------------------- | ------------------------------------------------------------------------------------------------------- |
| `WORKFLOW_DEFINITION` | `DEFINITION_CREATED`, `DEFINITION_VERSION_CREATED`, `DEFINITION_VERSION_ACTIVATED`, `DEFINITION_VERSION_DEACTIVATED` |
| `WORKFLOW`            | `WORKFLOW_CREATED`, `WORKFLOW_STARTED`, `WORKFLOW_ACTION_EXECUTED`, `WORKFLOW_CANCELLED`                |
| `USER`                | `LOGIN_SUCCEEDED`, `LOGIN_FAILED`, `ACCOUNT_LOCKED`, `LOGOUT`, `USER_CREATED`, `USER_UPDATED`, `USER_PASSWORD_RESET`, `USER_PASSWORD_CHANGED`, `USER_UNLOCKED` |
| `HTTP_ENDPOINT`       | `ACCESS_DENIED` (`resourceId` = método e caminho)                                                       |

`outcome` é `SUCCESS` ou `REJECTED`. Tentativas recusadas pelo domínio em Workflows (ação indisponível,
Actor sem papel, Rule não satisfeita, status incompatível) geram registro `REJECTED`.

---

# 8. Autenticação do console

Base: `/api/v1/auth`. Detalhes em `SECURITY.md` §4.

| Método | Caminho     | Acesso      | Uso                                                                  | Sucesso |
| ------ | ----------- | ----------- | -------------------------------------------------------------------- | ------- |
| `GET`  | `/csrf`     | público     | emite o cookie `XSRF-TOKEN`                                          | `204`   |
| `POST` | `/login`    | público     | `{ "username", "password" }` → abre a sessão e devolve o usuário     | `200`   |
| `GET`  | `/me`       | autenticado | `{ "username", "displayName", "roles" }`                             | `200`   |
| `POST` | `/password` | autenticado | `{ "currentPassword", "newPassword" }`; encerra as sessões do usuário | `204`   |
| `POST` | `/logout`   | autenticado | encerra a sessão                                                     | `204`   |

Sequência do console:

```text
GET  /api/v1/auth/csrf            → cookie XSRF-TOKEN
POST /api/v1/auth/login           → cookie EWE_SESSION (HttpOnly); token CSRF renovado
GET  /api/v1/auth/csrf            → novo XSRF-TOKEN
POST /api/v1/...                  → header X-XSRF-TOKEN = valor do cookie
```

---

# 9. Usuários

Base: `/api/v1/users`. Restrito ao papel `ADMIN`.

| Método | Caminho                  | Uso                                                          | Sucesso |
| ------ | ------------------------ | ------------------------------------------------------------ | ------- |
| `GET`  | `/`                      | lista paginada                                               | `200`   |
| `GET`  | `/{username}`            | consulta                                                     | `200`   |
| `POST` | `/`                      | `{ "username", "displayName", "password", "roles" }`         | `201`   |
| `PUT`  | `/{username}`            | `{ "displayName", "roles", "enabled" }`                      | `200`   |
| `POST` | `/{username}/password`   | `{ "newPassword" }`                                          | `204`   |
| `POST` | `/{username}/unlock`     | remove o bloqueio por tentativas                             | `200`   |

A resposta nunca contém a senha nem o hash. Campos: `id`, `username`, `displayName`, `roles`, `enabled`,
`locked`, `lockedUntil`, `failedLoginAttempts`, `lastLoginAt`, `passwordChangedAt`, `createdAt`,
`updatedAt`.

Erros específicos (`code`): `USER_NOT_FOUND` (404), `USERNAME_TAKEN`, `LAST_ADMINISTRATOR`,
`SELF_LOCKOUT` (409), `WRONG_CURRENT_PASSWORD` (400). Senha fora da política ou dados inválidos → `422`
com `violations`.

---

# 10. Operação

| Caminho                       | Acesso  | Uso                          |
| ----------------------------- | ------- | ---------------------------- |
| `/actuator/health`            | público | saúde geral                  |
| `/actuator/health/liveness`   | público | a aplicação está viva        |
| `/actuator/health/readiness`  | público | pronta para receber tráfego  |
| `/actuator/info`              | público | informações da aplicação     |
| `/actuator/metrics`           | `ADMIN` | métricas                     |

---

# 11. Documentos Relacionados

```text
docs/architecture/SECURITY.md
docs/product/USE_CASES.md
docs/product/BUSINESS_RULES.md
docs/adr/ADR-006.md
```
