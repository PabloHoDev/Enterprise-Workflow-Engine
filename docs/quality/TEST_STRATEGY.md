# Test Strategy

# Enterprise Workflow Engine

**Versão:** 0.2
**Status:** Aprovado

---

# 1. Objetivo

Define como o **Enterprise Workflow Engine** é verificado por testes automatizados (RNF-007), quais níveis
de teste existem e o que cada um deve provar.

---

# 2. Princípios

- **Regras de negócio são testadas sem infraestrutura.** O domínio é Java puro; seus testes não sobem
  Spring nem banco e rodam em milissegundos.
- **Integração é testada contra o real.** Os testes de integração usam PostgreSQL de verdade e a cadeia de
  segurança real, sem mocks.
- **O teste descreve o comportamento.** O nome do teste diz a regra verificada, não o método chamado.
- **Testes independentes.** Cada teste cria seus próprios dados com chaves únicas; nenhum depende de
  ordem de execução nem de limpeza do banco.

---

# 3. Níveis de Teste

| Nível        | O que prova                                                         | Como                                 | Onde                                          |
| ------------ | ------------------------------------------------------------------- | ------------------------------------ | --------------------------------------------- |
| Unitário     | regras e invariantes do domínio                                     | JUnit + AssertJ, sem Spring          | `*/domain/*Test`                              |
| Arquitetura  | limites entre módulos e camadas                                     | ArchUnit                             | `architecture/ArchitectureTest`               |
| Integração   | casos de uso de ponta a ponta: HTTP, segurança, transação, banco    | Spring Boot + MockMvc + PostgreSQL   | `*IntegrationTest`, `WorkflowEngineApplicationTests` |
| Console      | cliente da API (CSRF, erros), formatação, telas                     | Vitest + Testing Library (jsdom)     | `frontend/src/**/*.test.ts(x)`                |
| End-to-end   | jornadas reais no navegador, acessibilidade, responsividade         | Playwright + axe contra a aplicação  | `frontend/e2e/`                               |

Não há, por ora, testes de application services com mocks: os services apenas orquestram domínio e
portas, e esse caminho é exercitado pelos testes de integração. Testes com dublês devem ser adicionados
quando um service ganhar lógica própria que seja cara de alcançar pela API.

Testes de carga estão no backlog (TB-008).

---

# 4. O que Cada Suíte Cobre

| Suíte                                 | Cobertura                                                                                      |
| ------------------------------------- | ---------------------------------------------------------------------------------------------- |
| `DeterministicRuleEvaluatorTest`      | operadores, comparação numérica e textual, variável ausente, caminhos aninhados (BR-023 a BR-025, BR-045) |
| `WorkflowDefinitionTest`              | validação estrutural, versionamento, ativação e desativação (BR-001 a BR-008, BR-041, BR-043, BR-044) |
| `WorkflowTest`                        | ciclo de vida, Transitions, papéis, Rules, History, cancelamento, estados terminais (BR-009 a BR-022, BR-029 a BR-040, BR-042) |
| `ArchitectureTest`                    | AC-001 a AC-005, MR-001 a MR-003                                                                |
| `DefinitionApiIntegrationTest`        | UC-001 a UC-005, autorização administrativa, erros de validação                                 |
| `WorkflowApiIntegrationTest`          | UC-006 a UC-012, versionamento em execução, auditoria de sucesso e de recusa                    |
| `WorkflowConcurrencyIntegrationTest`  | RNF-001: uma operação baseada em leitura desatualizada falha em vez de sobrescrever             |
| `WorkflowEngineApplicationTests`      | inicialização, health checks, autenticação, correlação, OpenAPI                                 |
| `UserAccountTest`, `PasswordPolicyTest`, `LoginThrottleTest` | contas, bloqueio por tentativas, política de senha, limite por origem (BR-046 a BR-049) |
| `AuthenticationIntegrationTest`       | login por sessão, CSRF, logout, falhas genéricas, bloqueio, limite por IP, troca de senha, cabeçalhos, limite de tamanho, auditoria de acesso negado |
| `UserManagementIntegrationTest`       | gestão de contas, revogação imediata de sessões, proteção do último administrador |
| `WebConsoleIntegrationTest`           | entrega da SPA e cache dos arquivos, resumo por status                                          |
| `RequestSizeLimitFilterTest`          | corpo declarado e não declarado acima do limite                                                 |
| `frontend/e2e/console.spec.ts`        | jornada completa com 4 perfis (modelar, ativar, solicitar, regra recusando, aprovar, auditar), áreas restritas, celular de 360 px em tema escuro, axe WCAG 2 AA em 6 telas |

---

# 5. Banco de Dados nos Testes de Integração

Os testes de integração precisam de um PostgreSQL, iniciado uma vez e compartilhado por toda a suíte
(`support/TestDatabase`). A origem é escolhida nesta ordem:

1. **Banco externo**, se `EWE_TEST_DB_URL` estiver definida (com `EWE_TEST_DB_USERNAME` e
   `EWE_TEST_DB_PASSWORD`). O banco deve ser descartável.
2. **Testcontainers**, se houver Docker: um container `postgres:18-alpine`. É o caminho do CI.
3. **PostgreSQL embutido** (`io.zonky.test:embedded-postgres`, binários 18.x), sem Docker, ou quando
   `EWE_TEST_DB=embedded` está definida. É um processo nativo iniciado pela JVM, com UTF-8 e locale `C`.

Os testes de integração nunca são ignorados por falta de banco: `./mvnw verify` executa a suíte completa
em qualquer máquina com JDK.

As migrations Flyway rodam no banco de teste; os testes não limpam dados.

---

# 5A. Console Web

```bash
cd frontend
npm run lint && npm run typecheck && npm test      # estáticos e unitários
npm run build                                      # build de produção em dist/
```

Os testes end-to-end rodam contra a aplicação completa servindo o build do console, com o perfil `local`
e suas contas de demonstração:

```bash
./mvnw -DskipTests package
java -jar target/*.jar --spring.profiles.active=local --workflow-engine.web.console-location=file:frontend/dist/
cd frontend && npx playwright install chromium && npm run e2e
```

Cada execução cria uma definição com chave única; os dados permanecem no banco local.

---

# 6. Cobertura

- Ferramenta: JaCoCo. Relatório em `target/site/jacoco/index.html` após `./mvnw verify`.
- **Quality gate: mínimo de 80% de linhas.**
- O gate é aplicado no profile Maven `coverage-gate`, ativado automaticamente em CI (`CI=true`) e sob
  demanda com `-Pcoverage-gate`. Fica fora do build padrão porque, sem banco, os testes de integração são
  ignorados e a cobertura medida não representa a suíte completa.
- O gate também protege contra o caso em que a CI deixa de executar os testes de integração: a cobertura
  cairia abaixo do mínimo e o build falharia.

Cobertura é um piso, não um objetivo: um teste só vale pelo comportamento que verifica.

---

# 7. Execução

```bash
./mvnw test                      # testes
./mvnw verify                    # testes + relatório de cobertura
./mvnw verify -Pcoverage-gate    # idem, exigindo a cobertura mínima
./mvnw test -Dtest=WorkflowTest  # uma suíte
```

---

# 8. Regras para Novos Testes

1. Toda regra de negócio nova (`BR-NNN`) tem ao menos um teste unitário de domínio.
2. Todo endpoint novo tem teste de integração para o fluxo principal, para os fluxos alternativos do caso
   de uso e para a autorização.
3. Toda correção de defeito começa por um teste que reproduz o defeito.
4. Um módulo novo é acrescentado à lista de módulos de `ArchitectureTest`.
5. Testes de integração estendem `AbstractIntegrationTest`.

---

# 9. Documentos Relacionados

```text
docs/quality/DEFINITION_OF_DONE.md
docs/quality/CODE_STANDARDS.md
docs/architecture/ARCHITECTURE.md
```
