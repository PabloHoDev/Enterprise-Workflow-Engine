# Architectural Decisions

# Enterprise Workflow Engine

**Versão:** 0.1
**Status:** Aprovado

---

# 1. Objetivo

Visão consolidada das decisões arquiteturais em vigor. Cada linha resume uma decisão e aponta para o
registro completo, onde estão o contexto, as alternativas e as consequências.

- Decisões estruturais: `docs/adr/`
- Decisões menores do dia a dia: `docs/quality/DECISION_LOG.md`

---

# 2. Decisões em Vigor

| Tema                     | Decisão                                                                    | Registro |
| ------------------------ | -------------------------------------------------------------------------- | -------- |
| Governança               | `PROJECT_GOVERNANCE.md` como documento fundador; decisões em ADRs          | ADR-000  |
| Estilo arquitetural      | Modular Monolith + Clean Architecture, uma única unidade de deployment     | ADR-001  |
| Estrutura de pacotes     | módulo primeiro, camadas dentro; módulos só se falam por `contract`        | ADR-002  |
| Verificação dos limites  | testes de arquitetura (ArchUnit) a cada build                              | ADR-002  |
| Stack                    | Java 25, Spring Boot 4.1, PostgreSQL 18, Flyway, Maven                     | ADR-003  |
| Modelo de persistência   | domínio puro; entidades JPA e adapters por módulo                          | ADR-004  |
| Transações               | uma transação por caso de uso, na camada de aplicação                      | ADR-004  |
| Concorrência             | lock otimista por aggregate; conflito retorna `409`                        | ADR-004  |
| Integridade              | States, Transitions e State atual protegidos por chaves estrangeiras       | ADR-004  |
| Autenticação             | HTTP Basic stateless, usuários por configuração externa                    | ADR-005  |
| Autorização              | por endpoint (Spring Security) e por Transition (domínio)                  | ADR-005  |
| Step                     | representado por State + Transitions, sem entidade própria                 | ADR-006  |
| Versões                  | estrutura imutável; no máximo uma versão ativa por definição               | ADR-006  |
| Ciclo de vida            | `status` do Workflow separado do `currentState` do processo                | ADR-006  |
| Cancelamento             | operação do motor, não uma Transition da definição                         | ADR-006  |
| Rules                    | condições declarativas e determinísticas sobre as variáveis do Workflow    | ADR-006  |

---

# 3. Decisões Adiadas

Avaliadas e conscientemente não adotadas agora. O critério para retomá-las está no registro indicado.

| Tema                               | Situação                                               | Registro                 |
| ---------------------------------- | ------------------------------------------------------ | ------------------------ |
| Microservices                      | rejeitado como arquitetura inicial                     | ADR-001                  |
| Módulos Maven separados            | adiado; ArchUnit cobre a necessidade atual             | ADR-002, TB-006          |
| OAuth2 / OIDC                      | adiado até existir um provedor de identidade           | ADR-005, TD-001          |
| Mensageria e eventos de integração | sem necessidade concreta                               | `ARCHITECTURE.md` §19, TB-004 |
| Cache                              | sem evidência de necessidade                           | `ARCHITECTURE.md` §20    |
| Event sourcing                     | complexidade não justificada pelos requisitos          | ADR-004                  |
| Rascunhos editáveis de versão      | adiado; versões nascem completas                       | ADR-006, TB-002          |

---

# 4. Restrições Arquiteturais Verificadas Automaticamente

`ArchitectureTest` falha o build quando:

- `domain` ou `contract` dependem de Spring, Jakarta, Hibernate, Jackson ou Swagger (AC-001);
- `domain` depende de `application`, `infrastructure` ou `interfaces`;
- `application` depende de `infrastructure` ou `interfaces`;
- `interfaces` depende de `infrastructure` (AC-004);
- um `contract` expõe classes internas do seu módulo (MR-003);
- um módulo acessa outro fora do pacote `contract` (MR-002);
- existe ciclo entre módulos (MR-001).
