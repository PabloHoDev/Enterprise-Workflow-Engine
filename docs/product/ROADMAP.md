# Product Roadmap

# Enterprise Workflow Engine

**Versão:** 0.2
**Status:** Em desenvolvimento

---

# 1. Objetivo

Este documento apresenta a evolução planejada do **Enterprise Workflow Engine**: os marcos do produto, o
que já foi entregue e o que vem a seguir.

Legenda:

| Símbolo | Significado                                                     |
| ------- | --------------------------------------------------------------- |
| 🟢      | Concluído e aprovado                                            |
| 🟡      | Implementado, aguardando validação e aprovação (governança §6)  |
| 🔲      | Próximo                                                         |
| ⚪      | Planejado                                                       |

---

# 2. Milestones

## Milestone 0 — Foundation

**Status:** 🟢 Concluído

Base inicial do projeto: estrutura, documentação base, modelo de governança.

---

## Milestone 1 — Product Definition

**Status:** 🟢 Concluído

Visão, domínio, requisitos, casos de uso e regras de negócio.

---

## Milestone 2 — Architecture Foundation

**Status:** 🟡 Implementado, aguardando aprovação

Base arquitetural e técnica do sistema.

- 🟢 Arquitetura, módulos, modelo de dados e deployment documentados;
- 🟡 Stack, estrutura de pacotes, persistência e segurança decididas (ADR-002 a ADR-005);
- 🟡 Projeto Maven, perfis de ambiente, migrations, base de testes, Docker e CI.

---

## Milestone 3 — Workflow Engine Core

**Status:** 🟡 Implementado, aguardando aprovação

Núcleo do motor de workflow.

- 🟡 Workflow Definitions versionadas, com validação estrutural (UC-001 a UC-005);
- 🟡 Execução: criação, início, ações, cancelamento e estados terminais (UC-006 a UC-009, UC-012);
- 🟡 Rules declarativas por Transition;
- 🟡 History da execução (UC-010);
- 🟡 API REST documentada (OpenAPI).

---

## Milestone 4 — Enterprise Features

**Status:** 🟡 Parcialmente implementado

Capacidades que aproximam o sistema de um cenário corporativo real.

- 🟡 Autenticação e autorização por papel, por endpoint e por Transition;
- 🟡 Identidade: contas no banco, política de senha, bloqueio, sessão segura, revogação (ADR-007);
- 🟡 Console web: definições, execuções, auditoria e usuários (ADR-008);
- 🟡 Auditoria de operações, inclusive das recusadas e de autenticação (UC-011);
- 🟡 Controle de concorrência otimista;
- 🟡 Health checks, métricas e logs correlacionados;
- ⚪ Login por OIDC com MFA e SSO (TB-013);
- ⚪ Eventos de domínio e notificações (TB-004);
- ⚪ Transições automáticas executadas pelo sistema (TB-003).

---

## Milestone 5 — Production Ready

**Status:** 🟡 Parcialmente implementado

Preparação para uma versão estável e próxima de ambiente produtivo.

- 🟡 Imagem Docker e ambiente local com Docker Compose;
- 🟢 Pipeline de CI com testes, quality gate de cobertura e build da imagem (executada com sucesso);
- 🟡 CI com testes do console e end-to-end em navegador real; CodeQL; Dependabot;
- ⚪ Publicação da imagem em registry e deployment automatizado;
- ⚪ Exportação de métricas e tracing (TB-005);
- ⚪ Testes de carga e definição de objetivos de desempenho (RNF-011);
- ⚪ Release 1.0.0.

---

# 3. Etapas

```text
ETAPA 00 — Visão e Definição do Produto
│
├── FASE 01 — Product Vision                 🟢
├── FASE 02 — Domain Definition              🟢
├── FASE 03 — Requirements                   🟢
├── FASE 04 — Use Cases                      🟢
├── FASE 05 — Business Rules                 🟢
├── FASE 06 — Architecture Definition        🟢
└── FASE 07 — Technical Foundation           🟡

ETAPA 01 — Núcleo do Motor
│
├── FASE 08 — Domínio (Definition, Execution, Rules)   🟡
├── FASE 09 — Persistência e migrations                🟡
├── FASE 10 — API REST                                 🟡
├── FASE 11 — Segurança                                🟡
├── FASE 12 — Auditoria                                🟡
└── FASE 13 — Qualidade (testes, arquitetura, CI)      🟡

ETAPA 02 — Segurança e Console
│
├── FASE 14 — Identidade e sessão segura (ADR-007)     🟡
├── FASE 15 — Console web (ADR-008)                    🟡
└── FASE 16 — Qualidade do console (unitários, E2E, acessibilidade, CI)   🟡

ETAPA 03 — Evolução Corporativa
│
├── FASE 17 — Validação e release 0.1.0                🔲
├── FASE 18 — OIDC, MFA e SSO                          ⚪
├── FASE 19 — Transições automáticas e eventos         ⚪
└── FASE 20 — Operação (métricas, tracing, deploy)     ⚪
```

A FASE 07 definiu: stack tecnológica, Java/Spring Boot, Maven, estrutura inicial do projeto, estrutura de
pacotes, configuração base, perfis de ambiente, dependências, banco de dados, estratégia de migration,
base de testes, base Docker e base de qualidade.

---

# 4. Próximos Passos

1. **Validar as ETAPAS 01 e 02** conforme `docs/quality/CHECKLISTS.md` e aprovar ou registrar correções.
2. **Acompanhar a CI** com os novos jobs (console, end-to-end, CodeQL).
3. **Publicar a release 0.1.0** conforme `docs/releases/VERSIONING.md`.
4. Priorizar os itens de `docs/product/TECHNICAL_BACKLOG.md` para a ETAPA 03.

---

# 5. Histórico de Evolução

## 2026-10-03

- Auditoria do backend em execução: 4 achados de segurança.
- Módulo Identity e sessão segura do console (ADR-007), resolvendo os achados.
- Console web (ADR-008) com testes unitários, end-to-end e de acessibilidade.
- CI ampliada com console, end-to-end, CodeQL e Dependabot.

## 2026-10-02

- Technical Foundation implementada: Maven Wrapper, perfis, migrations, Docker, CI.
- Núcleo do motor implementado: módulos Workflow Definition, Workflow Execution, Rules e Audit.
- API REST, segurança e auditoria implementadas.
- ADR-002 a ADR-006 registrados.
- Documentação de arquitetura, qualidade e releases completada.
- Status dos milestones corrigidos para refletir o estado real do projeto.

## 2026-08-11

- Arquitetura inicial definida (ADR-001).

## 2026-08-07

- Projeto iniciado.
- Estrutura documental definida.
- Modelo de governança estabelecido.
- Roadmap inicial criado.

---

# 6. Atualização

Este documento deve ser atualizado conforme milestones e fases forem definidos ou concluídos.

Alterações de implementação são registradas em `docs/releases/CHANGELOG.md`.
