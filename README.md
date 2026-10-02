# 🚀 Enterprise Workflow Engine

> Plataforma backend para modelagem, execução e gerenciamento de workflows corporativos.

[![CI](https://github.com/PabloHoDev/Enterprise-Workflow-Engine/actions/workflows/ci.yml/badge.svg)](../../actions/workflows/ci.yml)

## 📌 Visão Geral

O **Enterprise Workflow Engine** é um motor de workflow: transforma processos de negócio — aprovações,
solicitações, validações — em fluxos executáveis, versionados e auditáveis, expostos por uma API REST.

Você descreve o processo (estados, transições, quem pode agir e sob quais condições) e o motor garante que
cada execução siga exatamente o que foi definido, registrando tudo o que acontece.

```text
REQUESTED ──submit──► PENDING_APPROVAL ──approve (MANAGER, amount ≤ 10000)──────────► APPROVED
                              │                                                          ▲
                              ├──send-to-finance (MANAGER, amount > 10000)──► FINANCIAL_VALIDATION
                              │                                                │         │
                              └──reject (MANAGER)──► REJECTED ◄──reject (FINANCE)        └─approve (FINANCE)
```

---

## ✨ O que o motor faz

| Capacidade                  | Descrição                                                                                      |
| --------------------------- | ---------------------------------------------------------------------------------------------- |
| **Definições versionadas**  | Cada alteração de processo é uma nova versão; execuções em andamento não mudam de versão       |
| **Validação estrutural**    | Recusa processos sem estado inicial, com estados inalcançáveis, becos sem saída ou ações ambíguas |
| **Execução controlada**     | Só acontecem transições previstas a partir do estado atual                                     |
| **Regras declarativas**     | Transições condicionadas a dados da execução (`amount > 10000`)                                |
| **Autorização por papel**   | Por endpoint e por transição                                                                   |
| **Histórico**               | O que aconteceu com cada workflow, em ordem cronológica                                        |
| **Auditoria**               | Quem fez o quê, quando e com qual resultado — inclusive tentativas recusadas                   |
| **Consistência**            | Uma transação por operação e controle de concorrência otimista                                 |

---

## ▶️ Como executar

**Pré-requisitos:** JDK 25 ou superior e Docker (para o PostgreSQL).

```bash
# 1. PostgreSQL local
docker compose up -d

# 2. Aplicação (as migrations rodam na inicialização)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

No Windows (cmd ou PowerShell), use `mvnw.cmd` no lugar de `./mvnw`.

| Recurso      | Endereço                                |
| ------------ | --------------------------------------- |
| Swagger UI   | http://localhost:8080/swagger-ui.html   |
| OpenAPI      | http://localhost:8080/v3/api-docs       |
| Health check | http://localhost:8080/actuator/health   |

Para rodar tudo em containers: `docker compose --profile app up --build`.

Sem Docker, aponte para um PostgreSQL existente com as variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.

### Usuários do perfil `local`

| Usuário   | Senha     | Papéis            |
| --------- | --------- | ----------------- |
| `admin`   | `admin`   | `ADMIN`           |
| `manager` | `manager` | `USER`, `MANAGER` |
| `finance` | `finance` | `USER`, `FINANCE` |
| `user`    | `user`    | `USER`            |

Credenciais apenas para desenvolvimento. Em outros ambientes, os usuários vêm da configuração externa
([SECURITY.md](docs/architecture/SECURITY.md)).

---

## 🧪 Exemplo: aprovação de compras

```bash
API=http://localhost:8080/api/v1

# 1. O administrador define o processo (a versão 1 nasce como rascunho)
curl -u admin:admin -X POST $API/workflow-definitions -H "Content-Type: application/json" -d '{
  "key": "purchase-approval",
  "name": "Purchase Approval",
  "states": [
    {"name": "REQUESTED", "type": "INITIAL"},
    {"name": "PENDING_APPROVAL", "type": "INTERMEDIATE"},
    {"name": "FINANCIAL_VALIDATION", "type": "INTERMEDIATE"},
    {"name": "APPROVED", "type": "TERMINAL"},
    {"name": "REJECTED", "type": "TERMINAL"}
  ],
  "transitions": [
    {"action": "submit", "from": "REQUESTED", "to": "PENDING_APPROVAL"},
    {"action": "approve", "from": "PENDING_APPROVAL", "to": "APPROVED", "requiredRole": "MANAGER",
     "rules": [{"field": "amount", "operator": "LESS_THAN_OR_EQUAL", "value": "10000"}]},
    {"action": "send-to-finance", "from": "PENDING_APPROVAL", "to": "FINANCIAL_VALIDATION", "requiredRole": "MANAGER",
     "rules": [{"field": "amount", "operator": "GREATER_THAN", "value": "10000"}]},
    {"action": "reject", "from": "PENDING_APPROVAL", "to": "REJECTED", "requiredRole": "MANAGER"},
    {"action": "approve", "from": "FINANCIAL_VALIDATION", "to": "APPROVED", "requiredRole": "FINANCE"},
    {"action": "reject", "from": "FINANCIAL_VALIDATION", "to": "REJECTED", "requiredRole": "FINANCE"}
  ]
}'

# 2. ...e a disponibiliza para execução
curl -u admin:admin -X POST $API/workflow-definitions/purchase-approval/versions/1/activate

# 3. Um usuário cria e inicia uma solicitação de 15.000 (guarde o "id" da resposta)
curl -u user:user -X POST $API/workflows -H "Content-Type: application/json" \
  -d '{"definitionKey": "purchase-approval", "variables": {"amount": 15000}}'
ID=<id retornado>
curl -u user:user -X POST $API/workflows/$ID/start
curl -u user:user -X POST $API/workflows/$ID/actions -H "Content-Type: application/json" -d '{"action": "submit"}'

# 4. O gestor tenta aprovar direto: recusado (422), a regra exige amount <= 10000
curl -u manager:manager -X POST $API/workflows/$ID/actions -H "Content-Type: application/json" -d '{"action": "approve"}'

# 5. O gestor encaminha ao financeiro, que aprova
curl -u manager:manager -X POST $API/workflows/$ID/actions -H "Content-Type: application/json" -d '{"action": "send-to-finance"}'
curl -u finance:finance -X POST $API/workflows/$ID/actions -H "Content-Type: application/json" -d '{"action": "approve"}'

# 6. O que aconteceu, e quem fez o quê (incluindo a tentativa recusada)
curl -u user:user $API/workflows/$ID/history
curl -u admin:admin "$API/audit-records?resourceId=$ID"
```

Referência completa da API: [API_DESIGN.md](docs/architecture/API_DESIGN.md).

---

## 🧠 Arquitetura

**Modular Monolith + Clean Architecture**: uma única aplicação, organizada em módulos de negócio com
limites explícitos, cada um com suas camadas.

```text
com.pablohenrique.workflowengine
├── definition/      Workflow Definition — como o processo funciona
├── execution/       Workflow Execution  — o que acontece em cada execução
├── rules/           Rules               — esta operação pode ser realizada?
├── audit/           Audit               — quem fez o quê, quando e com qual resultado
└── infrastructure/  segurança, tratamento de erros, OpenAPI

Dentro de cada módulo:
contract/  →  domain/  →  application/  →  infrastructure/  +  interfaces/rest/
```

- O **domínio é Java puro**: não conhece Spring, JPA nem HTTP, e é testado sem infraestrutura.
- Módulos só se comunicam por **contratos públicos** (`contract`).
- Esses limites são **verificados a cada build** por testes de arquitetura (ArchUnit).

Detalhes: [ARCHITECTURE.md](docs/architecture/ARCHITECTURE.md) ·
[MODULES.md](docs/architecture/MODULES.md) · [decisões em vigor](docs/architecture/DECISIONS.md) ·
[ADRs](docs/adr/README.md)

---

## 🛠️ Tecnologias

| Área            | Tecnologia                                              |
| --------------- | ------------------------------------------------------- |
| Linguagem       | Java 25                                                 |
| Framework       | Spring Boot 4.1 (Web MVC, Data JPA, Security, Actuator) |
| Banco de dados  | PostgreSQL 18, migrations com Flyway                    |
| API             | REST, OpenAPI (springdoc), Problem Details (RFC 9457)   |
| Testes          | JUnit, AssertJ, MockMvc, Testcontainers, ArchUnit       |
| Qualidade       | JaCoCo (mínimo de 80% de linhas em CI)                  |
| Infraestrutura  | Docker, Docker Compose, GitHub Actions                  |

---

## ✅ Testes

```bash
./mvnw verify
```

Executa testes unitários de domínio, testes de arquitetura e testes de integração (API, segurança e
PostgreSQL reais), e gera o relatório de cobertura em `target/site/jacoco/index.html`.

Os testes de integração usam Testcontainers e precisam de Docker. Sem Docker, eles são **ignorados** — ou
podem usar um PostgreSQL existente:

```bash
export EWE_TEST_DB_URL=jdbc:postgresql://localhost:5432/workflow_engine_test
export EWE_TEST_DB_USERNAME=...
export EWE_TEST_DB_PASSWORD=...
./mvnw verify
```

Detalhes: [TEST_STRATEGY.md](docs/quality/TEST_STRATEGY.md).

---

## 📚 Documentação

| Área         | Documentos                                                                                              |
| ------------ | ------------------------------------------------------------------------------------------------------- |
| Produto      | [Visão](docs/product/VISION.md) · [Domínio](docs/product/DOMAIN.md) · [Requisitos](docs/product/REQUIREMENTS.md) · [Casos de uso](docs/product/USE_CASES.md) · [Regras de negócio](docs/product/BUSINESS_RULES.md) |
| Arquitetura  | [Arquitetura](docs/architecture/ARCHITECTURE.md) · [Módulos](docs/architecture/MODULES.md) · [Modelo de dados](docs/architecture/DATA_MODEL.md) · [API](docs/architecture/API_DESIGN.md) · [Segurança](docs/architecture/SECURITY.md) · [Deployment](docs/architecture/DEPLOYMENT.md) |
| Decisões     | [ADRs](docs/adr/README.md) · [Decisões em vigor](docs/architecture/DECISIONS.md) · [Log de decisões](docs/quality/DECISION_LOG.md) |
| Qualidade    | [Estratégia de testes](docs/quality/TEST_STRATEGY.md) · [Padrões de código](docs/quality/CODE_STANDARDS.md) · [Definition of Done](docs/quality/DEFINITION_OF_DONE.md) · [Checklists](docs/quality/CHECKLISTS.md) |
| Evolução     | [Roadmap](docs/product/ROADMAP.md) · [Backlog técnico](docs/product/TECHNICAL_BACKLOG.md) · [Dívida técnica](docs/releases/TECHNICAL_DEBT.md) · [Changelog](docs/releases/CHANGELOG.md) · [Versionamento](docs/releases/VERSIONING.md) |
| Governança   | [Project Governance](docs/PROJECT_GOVERNANCE.md)                                                        |

---

## 📍 Status do Projeto

🚧 **Em desenvolvimento** — versão `0.1.0` implementada, aguardando validação e publicação.

| Milestone                       | Status                         |
| ------------------------------- | ------------------------------ |
| 0 — Foundation                  | 🟢 Concluído                   |
| 1 — Product Definition          | 🟢 Concluído                   |
| 2 — Architecture Foundation     | 🟡 Implementado, em validação  |
| 3 — Workflow Engine Core        | 🟡 Implementado, em validação  |
| 4 — Enterprise Features         | 🟡 Parcial                     |
| 5 — Production Ready            | 🟡 Parcial                     |

Próximos passos: autenticação por OAuth2/OIDC, transições automáticas, eventos de domínio e operação
(métricas, tracing, deployment). Veja o [Roadmap](docs/product/ROADMAP.md) e as
[limitações conhecidas](docs/releases/TECHNICAL_DEBT.md).

---

## 🎯 Objetivo do Projeto

Demonstrar, em um sistema completo, práticas de engenharia backend usadas em ambientes corporativos:
arquitetura modular, Domain-Driven Design, Clean Architecture, APIs REST, persistência, segurança, testes
automatizados, observabilidade, containers e CI — com decisões registradas e justificadas.

O projeto é construído de forma incremental: cada etapa é planejada, implementada, validada e documentada
antes da seguinte, conforme o [modelo de governança](docs/PROJECT_GOVERNANCE.md).

---

## 👨‍💻 Motivação

O Enterprise Workflow Engine faz parte de uma jornada de desenvolvimento profissional focada em arquitetura
backend e construção de sistemas corporativos.

> Desenvolvimento de plataformas backend escaláveis orientadas a processos de negócio.

---

## 📄 Licença

Projeto desenvolvido para fins de estudo, evolução profissional e demonstração de habilidades técnicas.
