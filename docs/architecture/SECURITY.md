# Security

# Enterprise Workflow Engine

**Versão:** 0.2
**Status:** Aprovado

---

# 1. Objetivo

Este documento descreve como o **Enterprise Workflow Engine** atende aos requisitos de segurança
(RF-021 a RF-023, RNF-006) e às regras de Actor (BR-026 a BR-028).

Decisões: `docs/adr/ADR-007.md` (identidade e sessão) e `docs/adr/ADR-008.md` (console web).
Referência de verificação: OWASP ASVS 4 nível 2 e OWASP Top 10.

---

# 2. Visão Geral

```text
Navegador (console)                         Sistema integrado
   │ cookie de sessão HttpOnly                 │ HTTP Basic
   │ + token CSRF no header                    │ (sem sessão, sem CSRF)
   └──────────────────┬────────────────────────┘
                      ▼
          Limite de tamanho (256 KB)
                      ▼
          Autenticação (módulo Identity: contas no banco, BCrypt, bloqueio)
                      ▼
          Autorização por rota (papel exigido pelo endpoint)
                      ▼
          Application → Actor(id, roles)
                      ▼
          Autorização por regra (papel exigido pela Transition, BR-020)
                      ▼
          Auditoria (sucesso, recusa, login, logout, acesso negado)
```

---

# 3. Contas

| Item                  | Regra                                                                                  |
| --------------------- | -------------------------------------------------------------------------------------- |
| Armazenamento         | tabela `app_user`; senha apenas como hash BCrypt (`{bcrypt}…`)                          |
| Nome de usuário       | 3 a 64 caracteres: minúsculas, dígitos, `.`, `_`, `-`                                  |
| Política de senha     | mínimo 12 e máximo 128 caracteres; sem repetição trivial; sem conter o usuário (NIST SP 800-63B) |
| Bloqueio              | 5 senhas erradas consecutivas → 15 minutos bloqueada; desbloqueio pelo administrador    |
| Administração         | sempre existe ao menos um administrador ativo; ninguém remove o próprio acesso de administrador |
| Revogação             | mudança de papéis, desativação, redefinição ou troca de senha encerram todas as sessões |
| Contas iniciais       | `workflow-engine.identity.seed-users`; a aplicação não inicia sem um administrador ativo |

Contas iniciais em produção:

```text
WORKFLOW_ENGINE_IDENTITY_SEED_USERS_0_USERNAME=admin
WORKFLOW_ENGINE_IDENTITY_SEED_USERS_0_PASSWORD=<senha forte ou {bcrypt}…>
WORKFLOW_ENGINE_IDENTITY_SEED_USERS_0_ROLES=ADMIN,USER
```

A senha em texto é cifrada na primeira inicialização; depois disso, a variável pode ser removida.

---

# 4. Autenticação

## 4.1 Console web — sessão

| Item              | Implementação                                                                 |
| ----------------- | ----------------------------------------------------------------------------- |
| Login / logout    | `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`                          |
| Cookie de sessão  | `EWE_SESSION`: `HttpOnly`, `SameSite=Lax`, `Secure` no perfil `prod`           |
| Armazenamento     | PostgreSQL (Spring Session JDBC); instâncias sem estado local                  |
| Expiração         | 30 minutos de inatividade                                                      |
| Fixação de sessão | novo identificador de sessão a cada login                                      |
| CSRF              | cookie `XSRF-TOKEN` (`SameSite=Strict`) devolvido no header `X-XSRF-TOKEN`; renovado a cada login |
| Falha de login    | sempre `401 Invalid username or password`, sem revelar se a conta existe, está bloqueada ou desativada |
| Força bruta       | bloqueio por conta (§3) + limite de 10 falhas por minuto por IP (`429` com `Retry-After`) |

O console identifica suas chamadas com `X-Requested-With: XMLHttpRequest`; para elas, o `401` não traz
`WWW-Authenticate: Basic`, evitando o diálogo nativo de login do navegador.

## 4.2 Integrações — HTTP Basic

- Cada requisição envia `Authorization: Basic …`; não há sessão.
- Requisições com `Authorization` não carregam credencial ambiente e por isso dispensam CSRF.
- Contas, bloqueio e auditoria são os mesmos do console.
- `401` inclui `WWW-Authenticate: Basic`, como exige o protocolo.

---

# 5. Autorização

## 5.1 Por endpoint

| Recurso                                              | Regra                              |
| ---------------------------------------------------- | ---------------------------------- |
| `/actuator/health/**`, `/actuator/info`              | público                            |
| `/v3/api-docs/**`, `/swagger-ui/**`                  | público (desabilitado em `prod`)   |
| `/api/v1/auth/csrf`, `/api/v1/auth/login`            | público                            |
| demais `/actuator/**`                                | `ADMIN`                            |
| `POST /api/v1/workflow-definitions/**`               | `ADMIN`                            |
| `/api/v1/audit-records/**`, `/api/v1/users/**`       | `ADMIN`                            |
| demais `/api/**`                                     | autenticado                        |
| demais caminhos (arquivos e rotas do console)        | público — não contêm dados         |

## 5.2 Por Transition

Cada Transition pode exigir um papel (`requiredRole`), verificado pelo aggregate `Workflow` (BR-020). O
papel `ADMIN` **não** substitui o papel exigido: administrar a plataforma e decidir dentro de um processo
são responsabilidades distintas (DL-012).

## 5.3 No console

O console esconde menus e desabilita ações que o usuário não pode executar, mas isso é apenas
conveniência: toda proteção efetiva está na API, que recusa com `403`.

---

# 6. Cabeçalhos e proteção do navegador

| Cabeçalho                   | Valor                                                                 |
| --------------------------- | --------------------------------------------------------------------- |
| `Content-Security-Policy`   | `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'` |
| `X-Frame-Options`           | `DENY`                                                                |
| `X-Content-Type-Options`    | `nosniff`                                                             |
| `Referrer-Policy`           | `strict-origin-when-cross-origin`                                     |
| `Permissions-Policy`        | câmera, microfone, geolocalização e pagamento desabilitados           |
| `Cache-Control`             | `no-store` em respostas da API e no `index.html`                      |
| `Strict-Transport-Security` | enviado automaticamente em requisições HTTPS                          |

O console não usa `dangerouslySetInnerHTML`, não carrega recursos de terceiros e trata todo dado da API
como texto (o React escapa por padrão). O destino após o login só aceita caminhos internos (sem
redirecionamento aberto).

---

# 7. Rastreabilidade

| Operação                                   | Registro                                      |
| ------------------------------------------ | --------------------------------------------- |
| Login pelo console                         | `LOGIN_SUCCEEDED`                             |
| Falha de autenticação (console ou Basic)   | `LOGIN_FAILED` (`REJECTED`), com o motivo técnico |
| Conta bloqueada por tentativas             | `ACCOUNT_LOCKED` (actor `system`)             |
| Logout                                     | `LOGOUT`                                      |
| Acesso negado por papel ou CSRF            | `ACCESS_DENIED` (`REJECTED`), com método e caminho |
| Gestão de contas                           | `USER_CREATED`, `USER_UPDATED`, `USER_PASSWORD_RESET`, `USER_PASSWORD_CHANGED`, `USER_UNLOCKED` |
| Operações de negócio e recusas de domínio  | ver `docs/architecture/API_DESIGN.md` §7       |

O nome digitado em uma falha de login é registrado sem caracteres de controle e truncado em 64 caracteres.

---

# 8. Demais proteções

| Risco                                    | Tratamento                                                              |
| ---------------------------------------- | ----------------------------------------------------------------------- |
| Injeção de SQL                           | JPA/Criteria com parâmetros vinculados; nenhum SQL concatenado          |
| Vazamento de detalhes internos           | erro inesperado → mensagem genérica; detalhe apenas no log              |
| Corpo de requisição excessivo            | 256 KB (`workflow-engine.http.max-request-size`) → `413`                |
| JSON malicioso (aninhamento extremo)     | limites do Jackson → `400`                                              |
| Atualização perdida por concorrência     | lock otimista por aggregate (ADR-004) → `409`                           |
| Forja de linhas de log                   | `X-Request-Id` só é aceito se casar com `[A-Za-z0-9._-]{1,64}`          |
| Segredos no repositório                  | banco e contas iniciais vêm do ambiente; `.env` ignorado pelo Git       |
| Dependências vulneráveis                 | Dependabot (Maven, npm, Actions, Docker) e `npm audit` na CI            |
| Vulnerabilidades no código               | CodeQL (`security-extended`) para Java e TypeScript                     |
| Execução privilegiada no container       | imagem roda com usuário sem privilégios                                 |
| Documentação da API exposta              | springdoc desabilitado no perfil `prod`                                 |

---

# 9. Requisitos do Ambiente

- **TLS obrigatório** fora do ambiente local. O perfil `prod` marca o cookie de sessão como `Secure`.
- Atrás de proxy ou balanceador, configure `server.forward-headers-strategy=native` (ou `framework`) **e**
  garanta que só o proxy confiável alcance a aplicação; caso contrário o limite por IP veria o IP do proxy
  ou um IP forjado.
- Aplique também limite de requisições no gateway: o limite da aplicação é por instância (TD-009).
- O usuário de banco da aplicação deve ter permissão apenas sobre o próprio schema; o banco guarda hashes
  de senha e deve ter backup cifrado.
- As credenciais do perfil `local` existem só para desenvolvimento.

---

# 10. Limitações Conhecidas

Registradas em `docs/releases/TECHNICAL_DEBT.md`:

- TD-001: sem MFA nem SSO (evolução: OIDC, TB-013);
- TD-002: requisições anônimas a rotas protegidas (`401`) não são auditadas, por serem ruído;
- TD-009: limite de falhas de login por IP mantido em memória, por instância.

---

# 11. Documentos Relacionados

```text
docs/adr/ADR-007.md
docs/adr/ADR-008.md
docs/architecture/API_DESIGN.md
docs/architecture/DEPLOYMENT.md
docs/product/BUSINESS_RULES.md
```
