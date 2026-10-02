# Security

# Enterprise Workflow Engine

**Versão:** 0.1
**Status:** Aprovado

---

# 1. Objetivo

Este documento descreve como o **Enterprise Workflow Engine** atende aos requisitos de segurança
(RF-021 a RF-023, RNF-006) e às regras de Actor (BR-026 a BR-028).

A decisão sobre o mecanismo de autenticação está em `docs/adr/ADR-005.md`.

---

# 2. Visão Geral

```text
Client
   ↓  HTTP Basic (sobre TLS)
Autenticação            Spring Security: usuário e senha
   ↓
Autorização por rota    Spring Security: papel exigido pelo endpoint
   ↓
Application             Actor(id, roles) derivado da autenticação
   ↓
Autorização por regra   Domínio: papel exigido pela Transition (BR-020)
```

A aplicação é **stateless**: não há sessão, cookie ou estado de autenticação no servidor.

---

# 3. Autenticação

- Mecanismo: HTTP Basic.
- Usuários definidos por configuração externa em `workflow-engine.security.users`.
- Senhas no formato do `DelegatingPasswordEncoder`; fora dos perfis `local` e `test`, use `{bcrypt}`.
- A aplicação **não inicia** sem ao menos um usuário configurado.

Exemplo por variáveis de ambiente (perfil `prod`):

```text
WORKFLOW_ENGINE_SECURITY_USERS_0_USERNAME=admin
WORKFLOW_ENGINE_SECURITY_USERS_0_PASSWORD={bcrypt}$2a$10$...
WORKFLOW_ENGINE_SECURITY_USERS_0_ROLES=ADMIN
```

Falhas de autenticação retornam `401` com `WWW-Authenticate: Basic` e corpo em Problem Details.

---

# 4. Autorização

## 4.1 Por endpoint

| Recurso                                             | Regra               |
| --------------------------------------------------- | ------------------- |
| `/actuator/health/**`, `/actuator/info`             | público             |
| `/v3/api-docs/**`, `/swagger-ui/**`                 | público (desabilitado em `prod`) |
| demais `/actuator/**`                               | papel `ADMIN`       |
| `POST /api/v1/workflow-definitions/**`              | papel `ADMIN`       |
| `/api/v1/audit-records/**`                          | papel `ADMIN`       |
| qualquer outra rota                                 | autenticado         |

## 4.2 Por Transition

Cada Transition pode declarar um `requiredRole`. O aggregate `Workflow` recusa a ação quando o Actor não
possui o papel (BR-020), retornando `403`. Estar autenticado não implica estar autorizado (BR-027).

Os papéis são livres: além de `ADMIN`, cada instalação define os que seus processos exigem (`MANAGER`,
`FINANCE`...). O papel `ADMIN` **não** substitui o papel exigido por uma Transition: administrar a
plataforma e decidir dentro de um processo são responsabilidades distintas.

---

# 5. Rastreabilidade

- Toda entrada de History e todo registro de Audit guardam o identificador do Actor (BR-026, BR-028).
- Operações recusadas pelo domínio em Workflows geram Audit com resultado `REJECTED`, inclusive
  tentativas de Actors sem o papel exigido.
- A consulta de auditoria é restrita a `ADMIN` (RF-018).

---

# 6. Proteções Aplicadas

| Risco                               | Tratamento                                                                 |
| ----------------------------------- | -------------------------------------------------------------------------- |
| Injeção de SQL                      | acesso exclusivamente por JPA/Criteria, com parâmetros vinculados          |
| Vazamento de detalhes internos      | erros inesperados retornam mensagem genérica; o detalhe vai apenas ao log  |
| Entrada inválida                    | Bean Validation na borda + validação estrutural no domínio                 |
| Segredos no código                  | banco e usuários vêm do ambiente (DP-003); `.env` é ignorado pelo Git      |
| CSRF                                | não aplicável: sem sessão nem cookies; credencial enviada em header        |
| Forja de linhas de log              | `X-Request-Id` recebido só é aceito se casar com `[A-Za-z0-9._-]{1,64}`    |
| Atualização perdida por concorrência| lock otimista por aggregate (ADR-004)                                      |
| Execução como root no container     | a imagem roda com usuário sem privilégios                                  |
| Exposição da documentação da API    | springdoc desabilitado no perfil `prod`                                    |

---

# 7. Requisitos do Ambiente

- **TLS obrigatório** fora do ambiente local: HTTP Basic envia a credencial a cada requisição. A terminação
  TLS fica a cargo do proxy ou balanceador à frente da aplicação.
- O usuário de banco da aplicação deve ter permissão apenas sobre o seu próprio schema.
- As credenciais do perfil `local` existem só para desenvolvimento e não devem ser reutilizadas.

---

# 8. Limitações Conhecidas

Registradas em `docs/releases/TECHNICAL_DEBT.md`:

- TD-001: usuários estáticos, sem expiração ou revogação; evolução prevista para OAuth2/OIDC;
- TD-002: negações de acesso na camada HTTP (`401`/`403` por endpoint) não geram registro de Audit;
- TD-003: não há limitação de taxa de requisições nem bloqueio por tentativas de autenticação.

---

# 9. Documentos Relacionados

```text
docs/adr/ADR-005.md
docs/architecture/API_DESIGN.md
docs/architecture/DEPLOYMENT.md
docs/product/BUSINESS_RULES.md
```
