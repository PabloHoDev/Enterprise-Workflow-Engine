# Changelog

Mudanças relevantes do **Enterprise Workflow Engine**, por versão.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o versionamento segue
`docs/releases/VERSIONING.md`.

---

## [Não lançado]

Conteúdo previsto para a versão `0.1.0`.

### Adicionado — 2026-10-03

**Ambiente sem Docker**

- PostgreSQL 18 embutido (`io.zonky.test:embedded-postgres`) para os testes de integração quando não há
  Docker. A suíte completa roda em qualquer máquina, e nenhum teste é mais ignorado por falta de banco.
- `./mvnw spring-boot:test-run` sobe o PostgreSQL embutido, com dados persistentes em `.local/postgres`, e a
  aplicação com o perfil `local`. O Docker Compose passa a ser opcional.

**Console web** (ADR-008)

- SPA em React 19 + TypeScript, servida pela própria aplicação na mesma origem da API.
- Painel com contagem por status e execuções recentes.
- Workflows: lista com filtros na URL, criação com variáveis, detalhe com ações disponíveis, regras de cada
  transição, diagrama do processo com o estado atual, histórico e cancelamento.
- Definições: lista, detalhe com versões e diagrama, ativação/desativação, editor de definição e de nova
  versão com pré-visualização e exemplo pronto.
- Auditoria com filtros; gestão de usuários; perfil com troca de senha.
- Tema claro/escuro, layout responsivo e verificação automática de acessibilidade (WCAG 2 AA).

**Identidade e segurança** (ADR-007)

- Módulo Identity: contas no PostgreSQL com BCrypt, política de senha, bloqueio após 5 falhas, gestão
  pelo administrador, troca da própria senha e proteção do último administrador.
- Login do console por sessão em cookie HttpOnly guardada no PostgreSQL, com CSRF e rotação a cada login.
- Revogação imediata: mudança de acesso encerra as sessões do usuário.
- Limite de falhas de login por IP (`429`), limite de 256 KB por requisição (`413`).
- Cabeçalhos CSP, Referrer-Policy e Permissions-Policy.
- Auditoria de login, falha de login, bloqueio, logout, acesso negado e gestão de contas.
- `GET /api/v1/workflows/summary` e `activeVersion` na listagem de definições.

**Engenharia**

- Testes unitários do console (Vitest), testes end-to-end com Playwright e axe, testes de identidade e
  segurança no backend.
- CI com jobs de console e end-to-end; CodeQL; Dependabot.
- ADR-007 e ADR-008.

### Alterado — 2026-10-03

- ADR-005 (HTTP Basic com usuários na configuração) substituído pelo ADR-007. HTTP Basic continua
  disponível para integrações, com as contas do banco.
- `workflow-engine.security.users` substituído por `workflow-engine.identity.seed-users`.
- `Dockerfile` ganhou um estágio Node para o build do console.

### Adicionado — 2026-10-02

**Workflow Definition**

- Criação de Workflow Definitions com States, Transitions, papéis exigidos e Rules.
- Versionamento: novas versões não alteram as anteriores; estrutura imutável após a criação.
- Ativação e desativação de versões, com no máximo uma versão ativa por definição.
- Validação estrutural: State inicial único, State terminal obrigatório, States alcançáveis, sem becos
  sem saída e sem ações ambíguas.

**Workflow Execution**

- Criação de Workflows a partir da versão ativa, presos a ela durante toda a execução.
- Início, execução de ações, cancelamento e encerramento em States terminais.
- Autorização por papel em cada Transition.
- History cronológico de toda mudança relevante.
- Consulta de Workflows com filtros e das ações disponíveis no State atual.
- Controle de concorrência otimista: operações conflitantes retornam `409`.

**Rules**

- Rules declarativas por Transition, com os operadores `EQUALS`, `NOT_EQUALS`, `GREATER_THAN`,
  `GREATER_THAN_OR_EQUAL`, `LESS_THAN`, `LESS_THAN_OR_EQUAL` e `EXISTS`.
- Avaliação determinística sobre as variáveis do Workflow, com suporte a campos aninhados.

**Audit**

- Registro das operações sobre definições e Workflows, com Actor, recurso, momento e resultado.
- Registro de operações recusadas pelo domínio.
- Consulta de auditoria com filtros, restrita a administradores.

**Plataforma**

- API REST versionada (`/api/v1`) com documentação OpenAPI e Swagger UI.
- Erros no formato Problem Details (RFC 9457).
- Autenticação HTTP Basic stateless, com usuários definidos por configuração externa.
- Perfis `local`, `test` e `prod`; configuração por variáveis de ambiente.
- Migrations Flyway para PostgreSQL.
- Health checks (liveness e readiness), métricas e correlação de requisições por `X-Request-Id`.
- Logs estruturados no perfil `prod`.
- `Dockerfile` multi-stage, `docker-compose.yml` para ambiente local e pipeline de CI no GitHub Actions.

**Qualidade**

- Testes unitários de domínio, testes de arquitetura (ArchUnit) e testes de integração com PostgreSQL.
- Relatório de cobertura (JaCoCo) e quality gate de 80% de linhas em CI.

**Documentação**

- ADR-002 a ADR-006.
- Documentos de API, segurança, decisões, estratégia de testes, padrões de código, Definition of Done,
  checklists, versionamento, dívida técnica e backlog técnico.

### Alterado

- Java alvo de 21 para 25.
- `spring-boot-starter-web` substituído por `spring-boot-starter-webmvc`.
- ADRs renomeados para `ADR-NNN.md`; `PROJECT_GOVERNANCE.md` movido para `docs/`.
- `ROADMAP.md`: status dos milestones corrigidos para refletir o estado real.
- `DEPLOYMENT.md`, `DATA_MODEL.md`, `VISION.md` e `ADR-000.md`: conteúdo truncado completado.
- `BUSINESS_RULES.md`: acrescentadas BR-041 a BR-045.

### Corrigido

- Maven Wrapper: `mvnw` e `mvnw.cmd` estavam vazios.
- `.gitignore` estava vazio e um arquivo de `target/` estava versionado.

---

## Histórico anterior

Antes desta versão, o repositório continha apenas a documentação de produto e de arquitetura e o esqueleto
da aplicação Spring Boot. A evolução desse período está em `docs/product/ROADMAP.md`.
