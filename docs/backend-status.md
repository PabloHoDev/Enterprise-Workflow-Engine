# Backend — Status e Plano de Ação

> Documento vivo. Atualize ao final de cada etapa.
> Última análise: 2026-10-03 — após a etapa B-A (endurecimento) e o módulo Identity.

Stack: Java 25 · Spring Boot 4.1.1 · PostgreSQL 18 · Flyway. Não há Supabase/RLS: o controle de acesso
está no Spring Security, no módulo Identity e no domínio (`docs/architecture/SECURITY.md`).

## 1. ✅ O que já está OK

### Build e testes
- [x] `./mvnw clean verify -Pcoverage-gate`: 143 testes, 0 falhas, 0 ignorados (PostgreSQL 18 real).
- [x] Cobertura: 97,2% de linhas, 86,6% de branches (gate: 80% de linhas).
- [x] Testes de arquitetura cobrindo os 5 módulos (`definition`, `execution`, `rules`, `audit`, `identity`).
- [x] CI do commit `5457eff` verde (backend + imagem Docker). Os novos jobs (console, end-to-end, CodeQL)
      rodam no próximo push.

### Banco
- [x] Migrations `V1`–`V5` aplicadas do zero e sobre o banco existente (`V4` identity, `V5` sessões).
- [x] Integridade no banco: chaves únicas, checks, FKs compostas, remoção em cascata de papéis.
- [x] Concorrência: lock otimista nos aggregates; lock pessimista no contador de falhas de login.

### Segurança (achados da auditoria de 2026-10-03)
- [x] **A1 — payload sem limite:** corpo limitado a 256 KB → `413` (`RequestSizeLimitFilter`, testes unitário e de integração).
- [x] **A2 — força bruta:** bloqueio de conta após 5 falhas + limite de 10 falhas/min por IP → `429` (`AuthenticationIntegrationTest`).
- [x] **A3 — usuários estáticos:** módulo Identity com contas no banco, BCrypt, política de senha, gestão e revogação imediata (ADR-007).
- [x] **A4 — auditoria incompleta:** login, falha, bloqueio, logout, acesso negado e gestão de contas auditados.
- [x] Sessão do console: cookie HttpOnly no PostgreSQL, CSRF, rotação de sessão e de token a cada login.
- [x] Cabeçalhos: CSP, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`, `nosniff`.
- [x] CodeQL (Java e TypeScript) e Dependabot (Maven, npm, Actions, Docker) configurados.

### API
- [x] UC-001 a UC-012, autenticação (`/api/v1/auth`), usuários (`/api/v1/users`), resumo por status.
- [x] Erros em Problem Details; `400`, `401`, `403`, `404`, `409`, `413`, `422`, `429` documentados.

## 2. ⏳ O que está pendente

### 2.1 🚧 Bloqueios (decisões do usuário)
- [ ] Aprovar as ETAPAS 01 e 02 (roadmap em 🟡) ou apontar correções.
- [ ] Escolher o provedor de identidade para OIDC/MFA/SSO, quando for o momento (TB-013).

### 2.2 🔐 Segurança
- [ ] MFA e SSO (TD-001 → TB-013).
- [ ] Limite por IP é por instância; em produção, complementar no gateway (TD-009).
- [ ] Conflitos de concorrência (`409`) não auditados (TD-010).

### 2.3 🗄️ Banco
- [ ] Backup e restauração não definidos (`DEPLOYMENT.md` §19).

### 2.4 🧪 Testes
- [ ] Sem teste de carga nem metas de desempenho (TB-008).

### 2.5 🖥️ Camada de servidor
- [ ] Transições automáticas (TB-003) e eventos/notificações (TB-004) — fora do escopo da 0.1.0.

### 2.6 🚀 Prontidão
- [ ] Imagem não publicada em registry; sem ambiente de deploy (TB-009).
- [ ] Métricas não exportadas; sem tracing (TB-005).
- [ ] Release 0.1.0 não publicada.

## 3. 🗺️ Plano de ação

| # | Etapa | Entregas | Depende de |
|---|---|---|---|
| B-A | Endurecimento | ✅ concluída em 2026-10-03 | — |
| B-B | Release 0.1.0 | aprovação, changelog, tag, imagem | CI verde com os novos jobs |
| B-C | Identidade federada | OIDC + MFA + SSO (BFF) | escolha do provedor |
| B-D | Operação | registry, deploy, métricas, tracing, backup | ambiente de destino |

## 4. 🚦 Portão "Backend pronto"

- [x] Todas as tabelas do modelo aprovado criadas por migrations
- [x] Controle de acesso testado (sessão, Basic, papel por endpoint e por Transition, CSRF)
- [x] Fluxo principal de ponta a ponta testado com banco real e no navegador
- [x] Regras (Rules) conferidas com valores de exemplo nos testes
- [x] Camada de servidor (API, validação, auditoria, identidade) pronta e testada
- [x] Achados de segurança da auditoria resolvidos
- [ ] Aplicado em ambiente remoto (não existe ambiente ainda)

## 5. Registro de etapas

| Data | Etapa | Resultado | Próximo passo |
|---|---|---|---|
| 2026-10-02 | Fundação técnica + núcleo do motor (ETAPA 01) | Implementada; CI verde | Validação |
| 2026-10-03 | Auditoria do backend | 96/96 testes; 4 achados de segurança (A1–A4) | Endurecimento |
| 2026-10-03 | B-A Endurecimento + Identity (ADR-007) | 143/143 testes; A1–A4 resolvidos | Nova auditoria e release 0.1.0 |
