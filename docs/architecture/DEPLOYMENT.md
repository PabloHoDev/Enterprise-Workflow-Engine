# Deployment

# Enterprise Workflow Engine

**Versão:** 0.2
**Status:** Aprovado

---

# 1. Objetivo

Este documento define a estratégia de deployment do **Enterprise Workflow Engine**: como a aplicação é
empacotada, executada, configurada, conectada às dependências externas, monitorada e evoluída
operacionalmente.

A estratégia permanece coerente com a decisão arquitetural registrada em:

```text
docs/adr/ADR-001.md
```

---

# 2. Visão Geral

```text
┌─────────────────────────────────────────────┐
│                 Environment                 │
│                                             │
│  ┌───────────────────────────────────────┐  │
│  │           Application Container       │  │
│  │                                       │  │
│  │      Enterprise Workflow Engine       │  │
│  │                                       │  │
│  │  ├── REST API                         │  │
│  │  ├── Workflow Definition              │  │
│  │  ├── Workflow Execution               │  │
│  │  ├── Rules                            │  │
│  │  └── Audit                            │  │
│  │                                       │  │
│  └───────────────────┬───────────────────┘  │
│                      │                      │
│                      ▼                      │
│  ┌───────────────────────────────────────┐  │
│  │             PostgreSQL                │  │
│  └───────────────────────────────────────┘  │
│                                             │
└─────────────────────────────────────────────┘
```

A aplicação e o banco de dados são componentes independentes do ponto de vista operacional. O banco de
dados não faz parte do mesmo container da aplicação.

---

# 3. Unidade de Deployment

O Enterprise Workflow Engine é distribuído como uma única aplicação.

```text
Enterprise Workflow Engine
            │
            ▼
      Application Artifact      (jar executável do Spring Boot)
            │
            ▼
      Docker Image
            │
            ▼
       Container Instance
```

Os módulos internos permanecem separados arquiteturalmente, porém não são implantados individualmente.

---

# 4. Containerization

A aplicação é empacotada com Docker, pelo `Dockerfile` na raiz do repositório.

```text
Source Code
    │
    ▼
Console Stage      node:24-alpine — build estático do console web (ADR-008)
    │
    ▼
Build Stage        eclipse-temurin:25-jdk — inclui o console em /static, compila e extrai o jar em camadas
    │
    ▼
Runtime Image      eclipse-temurin:25-jre — apenas JRE e aplicação (API + console)
```

Características da imagem:

- build separado da imagem final (multi-stage): a imagem de runtime não contém JDK, Maven nem código-fonte;
- jar extraído em camadas (dependências, loader, aplicação), para que uma mudança de código não invalide
  a camada de dependências;
- execução com usuário sem privilégios;
- porta `8080`.

```bash
docker build -t enterprise-workflow-engine .
```

Os testes não rodam dentro do build da imagem: são executados antes, na pipeline de CI.

---

# 5. External Dependencies

Dependências externas não são incorporadas à aplicação. A única dependência atual é o **PostgreSQL**,
acessado por configuração externa.

Outras dependências (mensageria, cache, serviços externos) só serão introduzidas com justificativa
técnica (DP-008).

---

# 6. Environment Configuration

O princípio adotado é:

> **Build once, configure externally.**

O mesmo artefato roda em qualquer ambiente; o que muda é a configuração, selecionada por perfil do Spring
(`SPRING_PROFILES_ACTIVE`) e por variáveis de ambiente.

| Perfil  | Uso                         | Banco                               | Usuários                        |
| ------- | --------------------------- | ----------------------------------- | ------------------------------- |
| `local` | desenvolvimento individual  | padrão `localhost:5432` (Compose)   | fixos, apenas para uso local    |
| `test`  | testes automatizados        | Testcontainers, embutido ou externo | fixos, apenas para testes       |
| `prod`  | execução real               | exclusivamente por variáveis        | exclusivamente por variáveis    |

Variáveis de ambiente:

| Variável                                           | Descrição                                   |
| -------------------------------------------------- | ------------------------------------------- |
| `SPRING_PROFILES_ACTIVE`                           | perfil ativo                                |
| `DB_URL`                                           | URL JDBC do PostgreSQL                      |
| `DB_USERNAME` / `DB_PASSWORD`                      | credenciais do banco                        |
| `WORKFLOW_ENGINE_IDENTITY_SEED_USERS_<n>_USERNAME` | conta criada na inicialização, se não existir (ex.: o primeiro administrador) |
| `WORKFLOW_ENGINE_IDENTITY_SEED_USERS_<n>_PASSWORD` | senha em texto (cifrada ao criar) ou `{bcrypt}...` |
| `WORKFLOW_ENGINE_IDENTITY_SEED_USERS_<n>_ROLES`    | papéis separados por vírgula                |
| `WORKFLOW_ENGINE_HTTP_MAX_REQUEST_SIZE`            | tamanho máximo do corpo (padrão `256KB`)    |
| `WORKFLOW_ENGINE_WEB_CONSOLE_LOCATION`             | local do build do console (padrão `classpath:/static/`) |
| `SERVER_PORT`                                      | porta HTTP (padrão `8080`)                  |

Sem perfil e sem variáveis de banco, a aplicação falha na inicialização em vez de assumir valores.

---

# 7. Sensitive Configuration

Informações sensíveis não são armazenadas no código-fonte (DP-003): senhas, tokens, chaves e credenciais
de banco chegam por variáveis de ambiente.

- O arquivo `.env` é ignorado pelo Git; `.env.example` documenta as variáveis usadas pelo Compose.
- As credenciais presentes em `application-local.yml` e `application-test.yml` existem apenas para
  desenvolvimento e teste, e esses perfis não devem ser usados em outros ambientes.
- Em produção, os segredos devem vir do mecanismo de segredos da plataforma de deployment.

---

# 8. Environment Isolation

Cada ambiente possui configuração independente, para evitar que:

- ambientes de desenvolvimento utilizem dados de produção;
- credenciais sejam compartilhadas indevidamente;
- alterações locais afetem outros ambientes;
- configurações específicas sejam acopladas ao código.

---

# 9. Database Deployment

O PostgreSQL é um componente independente; a aplicação não gerencia sua operação. A aplicação controla,
porém, a evolução do **schema**, por migrations Flyway executadas na inicialização:

```text
Application Startup
        │
        ▼
Database Migration        Flyway aplica as migrations pendentes
        │
        ▼
Schema Validation         Hibernate valida o schema contra as entidades
        │
        ▼
Application Execution
```

Se a migration ou a validação falhar, a aplicação não sobe. Detalhes em `docs/architecture/DATA_MODEL.md`.

Com múltiplas instâncias iniciando ao mesmo tempo, o Flyway serializa a migração por lock no banco.

---

# 10. Application Instances

A arquitetura permite a execução de múltiplas instâncias:

```text
                Load Balancer
                      │
           ┌──────────┴──────────┐
           ▼                     ▼
    Application 1         Application 2
           │                     │
           └──────────┬──────────┘
                      ▼
                  PostgreSQL
```

Isso é possível porque não há estado local entre requisições e a consistência das execuções é garantida
no banco, por lock otimista (ADR-004). A estratégia concreta de balanceamento não faz parte da
implementação inicial.

---

# 11. Stateless Application Principle

A aplicação é stateless entre requisições: não há sessão HTTP, e todo estado relevante está no banco de
dados. Isso facilita reinicialização, escalabilidade horizontal, recuperação de falhas e substituição de
containers.

As sessões do console ficam no PostgreSQL (Spring Session JDBC), e não na memória da instância: qualquer
instância atende qualquer usuário, e encerrar a sessão de um usuário vale para todas. O único estado local
é o contador de falhas de login por IP (TD-009).

---

# 12. Health Checks

Disponibilizados pelo Spring Boot Actuator:

| Endpoint                       | Significado                                             |
| ------------------------------ | ------------------------------------------------------- |
| `/actuator/health`             | saúde geral, incluindo o banco                          |
| `/actuator/health/liveness`    | a aplicação está viva (reiniciar se falhar)             |
| `/actuator/health/readiness`   | pronta para receber tráfego (remover do balanceamento)  |

```text
Application Availability  ≠  Application Readiness
```

Os endpoints de saúde são públicos; o detalhamento dos componentes só é exibido a usuários autorizados.

---

# 13. Logging

- Cada requisição recebe um identificador (`X-Request-Id`), aceito do cliente ou gerado, incluído em
  todas as linhas de log da requisição e devolvido na resposta.
- No perfil `prod`, os logs são emitidos em formato estruturado (ECS/JSON) na saída padrão, prontos para
  coleta pela plataforma.
- Erros inesperados são registrados com stack trace; a resposta ao cliente não os expõe.
- Informações sensíveis não são registradas nos logs.

---

# 14. Observability

A implementação inicial contempla health checks, métricas (`/actuator/metrics`, restrito a `ADMIN`) e
logs correlacionados.

Tracing distribuído e exportação de métricas para um sistema externo serão introduzidos quando houver a
necessidade operacional (ver `docs/product/TECHNICAL_BACKLOG.md`).

---

# 15. Deployment Environments

| Ambiente    | Objetivo                                        |
| ----------- | ----------------------------------------------- |
| Local       | Desenvolvimento individual                      |
| Development | Integração contínua e validação compartilhada   |
| Test        | Execução de testes e validações automatizadas   |
| Production  | Execução da aplicação para usuários             |

A infraestrutura concreta desses ambientes será definida progressivamente. Não é objetivo reproduzir toda
a infraestrutura de produção localmente.

---

# 16. Local Development

O caminho padrão não usa Docker. A classe `LocalWorkflowEngineApplication` (em `src/test`) sobe um PostgreSQL
embutido e depois a aplicação com o perfil `local`:

```bash
./mvnw spring-boot:test-run                      # PostgreSQL embutido em :5433 + API em :8080
cd frontend && npm install && npm run dev        # http://localhost:5173 (proxy para a API)
```

- Os dados ficam em `.local/postgres` (ignorada pelo Git) e persistem entre execuções.
- Se a execução anterior foi encerrada à força, o servidor órfão é encerrado e o PostgreSQL se recupera
  pelo WAL.
- `LOCAL_DB_PORT` troca a porta. Com `DB_URL` definida, o banco embutido não sobe e a aplicação usa aquele
  banco.
- Argumentos extras vão por `-Dspring-boot.run.arguments=...`, por exemplo
  `--workflow-engine.web.console-location=file:frontend/dist/` para servir o build do console nos testes E2E.

O `docker-compose.yml` continua disponível como alternativa com containers:

```bash
docker compose up -d                             # apenas o PostgreSQL (:5432)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
docker compose --profile app up --build          # PostgreSQL + aplicação, http://localhost:8080
```

As duas opções são ferramentas de desenvolvimento, não a definição da infraestrutura de produção.

---

# 17. CI/CD

A pipeline está em `.github/workflows/ci.yml` e roda a cada push em `main` e a cada pull request:

```text
Source Code
     │
     ├──► Backend           ./mvnw verify: unitários, arquitetura, integração; cobertura mínima
     ├──► Console           lint, tipos, testes unitários, build, npm audit
     ├──► CodeQL            análise de segurança de Java e TypeScript
     │
     ▼
End-to-end                 aplicação real + PostgreSQL + navegador (Playwright, axe)
Docker Image               docker build
```

Dependabot abre pull requests semanais de atualização para Maven, npm, GitHub Actions e imagens Docker.

A publicação da imagem em um registry e o deployment automatizado serão adicionados quando houver um
ambiente de destino definido.

---

# 18. Failure Considerations

- **Encerramento:** `server.shutdown=graceful` — ao receber o sinal de parada, a aplicação conclui as
  requisições em andamento antes de encerrar.
- **Falha no meio de uma operação:** cada caso de uso é uma transação; uma falha desfaz a operação inteira
  e o Workflow permanece no estado anterior (RNF-005).
- **Indisponibilidade do banco:** o readiness passa a falhar e as requisições retornam erro; não há
  dados parciais.
- **Concorrência:** operações conflitantes sobre o mesmo recurso retornam `409` em vez de sobrescrever.

Retry, timeout e circuit breaker serão introduzidos quando houver dependências externas que os
justifiquem.

---

# 19. Backup and Data Recovery

A responsabilidade operacional pelo backup do banco depende do ambiente onde a aplicação estiver
implantada. A aplicação não assume que o banco possui backup automático.

Em produção devem ser considerados: backup periódico, retenção, recuperação, integridade dos backups e
procedimentos de restore. O detalhamento não faz parte do escopo inicial.

---

# 20. Future Deployment Evolution

```text
Docker
    │
    ▼
Container Registry
    │
    ▼
Managed Container Platform
    │
    ▼
Horizontal Scaling
```

Ou, quando justificável:

```text
Modular Monolith
        │
        ▼
Selective Module Extraction
        │
        ▼
Independent Service Deployment
```

A evolução para uma arquitetura distribuída não é um objetivo automático.

---

# 21. Deployment Principles

### DP-001 — Single Deployable Unit

A aplicação é distribuída como uma única unidade de deployment.

### DP-002 — Externalized Configuration

Configurações permanecem externas ao artefato da aplicação.

### DP-003 — No Sensitive Data in Source Code

Informações sensíveis não são armazenadas no código-fonte.

### DP-004 — Independent Dependencies

Banco de dados e demais dependências externas permanecem operacionalmente independentes da aplicação.

### DP-005 — Reproducibility

O ambiente de execução é reproduzível.

### DP-006 — Stateless Preference

A aplicação evita estado local persistente.

### DP-007 — Observable Operation

A aplicação permite evolução adequada dos mecanismos de observabilidade.

### DP-008 — Incremental Complexity

Novos componentes de infraestrutura são introduzidos apenas com justificativa técnica.

---

# 22. Relationship with Other Documents

```text
docs/architecture/ARCHITECTURE.md
docs/architecture/MODULES.md
docs/architecture/DATA_MODEL.md
docs/architecture/SECURITY.md
docs/adr/ADR-001.md
docs/adr/ADR-003.md
docs/adr/ADR-004.md
```

Decisões futuras de infraestrutura e deployment devem ser avaliadas conforme `docs/PROJECT_GOVERNANCE.md`.

Quando aplicável:

> ⚠️ **Esta decisão merece um ADR.**

---

# 23. Status

**Status:** Aprovado

Este documento representa a estratégia de deployment do Enterprise Workflow Engine, já refletindo a
implementação de Docker, Docker Compose e CI.
