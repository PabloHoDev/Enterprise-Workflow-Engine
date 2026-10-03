# Backend — Status e Plano de Ação

> Documento vivo. Atualize ao final de cada etapa.
> Front-end só começa quando o portão da seção 4 estiver 100% marcado.
> Última análise: 2026-10-03 — commit `5457eff`.

Stack: Java 25 · Spring Boot 4.1.1 · PostgreSQL 18 · Flyway. Não há Supabase/RLS: o controle de acesso
está no Spring Security e no domínio (`docs/architecture/SECURITY.md`).

## 1. ✅ O que já está OK

### Build e testes
- [x] `./mvnw clean verify -Pcoverage-gate` verde localmente: 96 testes, 0 falhas, 0 ignorados (PostgreSQL 18 real).
- [x] Cobertura: 98,5% de linhas, 91,9% de branches, 98,4% de métodos (gate: 80% de linhas).
- [x] CI no GitHub (run de 2026-10-02, commit `5457eff`): job de build/testes/quality gate e job de imagem Docker com sucesso.
- [x] Testes de arquitetura (ArchUnit) passando: limites entre módulos e camadas.

### Banco
- [x] Migrations `V1`–`V3` aplicadas do zero sem erro; Hibernate valida o schema na subida.
- [x] Integridade no banco: chaves únicas, checks de enumeração, FKs compostas (Transition → State da mesma versão; State atual do Workflow existe na versão).
- [x] Concorrência: 10 ações simultâneas no mesmo Workflow → 1 × `200`, 9 × `409`, History sem duplicidade.

### Domínio e API
- [x] UC-001 a UC-012 implementados e cobertos por teste de integração.
- [x] Regras BR-001 a BR-045 com teste de domínio (`docs/quality/TEST_STRATEGY.md` §4).
- [x] Erros em Problem Details; `400`/`405`/`415` tratados; ordenação inválida → `400`.
- [x] Paginação limitada a 100 itens; `page` negativo tratado como 0.
- [x] JSON aninhado em 2.000 níveis recusado com `400`.

### Segurança
- [x] Autenticação obrigatória em `/api/**`; papéis por endpoint e por Transition.
- [x] Cabeçalhos: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store`.
- [x] Actuator expõe só `health`, `info`, `metrics`; `env`, `heapdump`, `beans`, `configprops`, `loggers` inacessíveis.
- [x] Página de erro do Tomcat (URL malformada) não revela versão.
- [x] Auditoria de sucesso e de recusas de domínio; consulta restrita a `ADMIN`.
- [x] Segredos fora do código; springdoc desligado em `prod`.

## 2. ⏳ O que está pendente

### 2.1 🚧 Bloqueios (decisões do usuário)
- [ ] Aprovar a ETAPA 01 (roadmap em 🟡) ou apontar correções.
- [ ] Definir o limite de tamanho das variáveis de um Workflow (ver 2.2, item A1).
- [ ] Decidir se haverá front-end (ver `docs/frontend-status.md`).

### 2.2 🔐 Segurança
- [ ] **A1 — Payload sem limite (TD-008, confirmado):** um usuário comum criou um Workflow com 3 MB de variáveis (`201`). Risco de inflar banco e memória. Sugestão: limite de corpo (ex.: 256 KB) por filtro e limite de tamanho do `jsonb`.
- [ ] **A2 — Sem proteção contra força bruta (TD-003, confirmado):** 30 senhas erradas seguidas, nenhum bloqueio; a senha certa continua aceita em seguida.
- [ ] **A3 — Usuários estáticos com HTTP Basic (TD-001):** sem expiração/revogação; exige TLS.
- [ ] **A4 — Negações HTTP e conflitos por concorrência não auditados (TD-002):** `401`/`403` por endpoint e `409` por violação de unicidade não geram `audit_record`.
- [ ] Varredura de vulnerabilidades das dependências (OWASP Dependency-Check ou Dependabot) não configurada.

### 2.3 🗄️ Banco
- [ ] Backup e restauração não definidos (`DEPLOYMENT.md` §19).
- [ ] Teste de migration a partir de banco com dados de versão anterior (só existirá a partir da 0.2.0).

### 2.4 🧪 Testes
- [ ] Sem teste de carga nem metas de desempenho (TB-008).
- [ ] Conflito concorrente registra `WARN` com stack trace no log (ruído; não é erro).

### 2.5 🖥️ Camada de servidor
- [ ] Transições automáticas pelo sistema (TB-003) e eventos/notificações (TB-004) — fora do escopo da 0.1.0.

### 2.6 🚀 Prontidão
- [ ] Imagem não publicada em registry; sem ambiente de deploy (TB-009).
- [ ] Métricas não exportadas; sem tracing (TB-005).
- [ ] Release 0.1.0 não publicada.

## 3. 🗺️ Plano de ação

| # | Etapa | Entregas | Depende de |
|---|---|---|---|
| B-A | Endurecimento | Limite de payload (A1), Dependabot, auditoria de `401`/`403`/`409` (A4) | decisão do limite |
| B-B | Release 0.1.0 | Aprovação da ETAPA 01, changelog, tag, imagem | B-A |
| B-C | Identidade | OAuth2/OIDC (A3) + rate limiting (A2) no gateway | escolha do provedor |
| B-D | Operação | Registry, deploy, métricas, backup | ambiente de destino |

## 4. 🚦 Portão "Backend pronto"

- [x] Todas as tabelas do modelo aprovado criadas por migrations
- [x] Controle de acesso testado (autenticação, papel por endpoint, papel por Transition)
- [x] Fluxo principal de ponta a ponta testado com banco real
- [x] Regras (Rules) conferidas com valores de exemplo nos testes
- [x] Camada de servidor (API, validação, auditoria) pronta e testada
- [ ] Sem alertas de segurança abertos (A1–A4 pendentes)
- [ ] Aplicado em ambiente remoto (não existe ambiente ainda)
- [ ] `docs/backend-status.md` sem pendências de backend

## 5. Registro de etapas

| Data | Etapa | Resultado | Próximo passo |
|---|---|---|---|
| 2026-10-02 | Fundação técnica + núcleo do motor (ETAPA 01) | Implementada; CI verde | Validação |
| 2026-10-03 | Auditoria do backend | 96/96 testes; 4 achados de segurança (A1–A4) | Decidir limite de payload e iniciar B-A |
