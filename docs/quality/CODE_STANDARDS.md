# Code Standards

# Enterprise Workflow Engine

**Versão:** 0.1
**Status:** Aprovado

---

# 1. Objetivo

Padrões de código do **Enterprise Workflow Engine**. Complementam os princípios de
`docs/PROJECT_GOVERNANCE.md` (§3) com as convenções concretas adotadas na base de código.

---

# 2. Idioma

| Elemento                                         | Idioma    |
| ------------------------------------------------ | --------- |
| Código: classes, métodos, variáveis, pacotes     | Inglês    |
| Mensagens da API e de exceções                   | Inglês    |
| Termos do domínio (Workflow, State, Transition…) | Inglês, conforme a linguagem ubíqua do `DOMAIN.md` |
| Comentários, Javadoc e documentação              | Português |
| Mensagens de commit                              | Português ou inglês, de forma consistente |

---

# 3. Organização

A estrutura de pacotes é definida em `docs/adr/ADR-002.md`:

```text
<módulo>/contract        contrato público do módulo
<módulo>/domain          aggregates, value objects, erros de domínio
<módulo>/application     casos de uso e portas
<módulo>/infrastructure  adapters
<módulo>/interfaces      controllers e DTOs
```

- Uma classe pertence ao pacote da sua responsabilidade, não ao da conveniência.
- Não existe pacote `util`, `common` ou `shared` genérico (`MODULES.md` §13).
- A visibilidade padrão é **package-private**. Só é `public` o que precisa ser visto de outro pacote.
  Controllers, entidades JPA, adapters e configurações não são públicos.

---

# 4. Domínio

- Java puro: sem anotações ou tipos de framework.
- Aggregates protegem suas invariantes. Não há setters; o estado muda por métodos com nome de negócio
  (`start`, `execute`, `cancel`, `activate`).
- Um método de domínio **valida tudo antes de alterar qualquer coisa**: uma operação recusada não deixa
  o aggregate parcialmente modificado (BR-022).
- Value objects são `record`s imutáveis, com cópias defensivas das coleções.
- Fábricas estáticas: `create(...)` aplica as regras de criação; `restore(...)` reconstitui a partir da
  persistência, sem revalidar.
- O domínio não lê o relógio: o instante é recebido por parâmetro.
- Violação de regra de negócio lança uma exceção específica do módulo (`WorkflowException`,
  `DefinitionException`). `IllegalArgumentException` e `IllegalStateException` indicam erro de programação.

---

# 5. Aplicação

- Um método público de application service corresponde a um caso de uso e define a transação
  (`@Transactional`; leituras com `readOnly = true`).
- O service orquestra: carrega, delega a regra ao domínio, grava e audita. Regra de negócio não mora no
  service.
- Portas (repositórios) são interfaces declaradas em `application` e implementadas em `infrastructure`.
- O relógio é injetado (`java.time.Clock`).

---

# 6. Interfaces (REST)

- Controllers não contêm regra de negócio (AC-004): convertem a requisição, chamam um caso de uso e
  convertem a resposta.
- Entidades de domínio nunca são serializadas diretamente; cada módulo tem seus DTOs (`record`s).
- Validação de formato com Bean Validation nos DTOs; validação de negócio no domínio.
- Cada módulo traduz seus próprios erros em um `@RestControllerAdvice`, no formato Problem Details.
- Convenções de URL, status e paginação: `docs/architecture/API_DESIGN.md`.

---

# 7. Persistência

- Entidades JPA vivem em `infrastructure/persistence` e não saem desse pacote.
- Associações `LAZY`; `open-in-view` desabilitado.
- Enumerações gravadas como texto (`EnumType.STRING`).
- Toda alteração de schema é uma nova migration Flyway; migrations aplicadas não são editadas.
- Nomes de tabelas e colunas em `snake_case`; constraints nomeadas (`pk_`, `fk_`, `uk_`, `ck_`, `ix_`).
- Instantes em UTC (`timestamptz`).

---

# 8. Estilo

- Java 25: `record`, expressões `switch`, `var` apenas quando o tipo é evidente na mesma linha.
- Injeção de dependência por construtor; campos `final`.
- `Optional` como retorno de busca; nunca como parâmetro ou campo.
- Coleções retornadas são imutáveis ou não modificáveis.
- Sem `null` como valor de retorno de coleção: retorne a coleção vazia.
- Linhas com até 120 colunas; indentação de 4 espaços (`.editorconfig`).
- Sem imports com curinga.

---

# 9. Comentários

- O código diz **o quê**; o comentário diz **por quê** — uma restrição, uma regra de negócio (citando o
  identificador, como `BR-020`) ou uma decisão não óbvia.
- Não comentar o que o nome já diz.
- Javadoc em contratos públicos de módulo e em aggregates.

---

# 10. Logs

- SLF4J com mensagens parametrizadas.
- `ERROR` para falhas inesperadas, `WARN` para situações anômalas tratadas, `INFO` para eventos
  relevantes, `DEBUG` para diagnóstico.
- Nunca registrar senhas, tokens ou dados sensíveis.
- Rastreabilidade de negócio é responsabilidade do Audit, não dos logs.

---

# 11. Testes

Convenções em `docs/quality/TEST_STRATEGY.md`. Em resumo:

- nome do teste descreve o comportamento (`rejectsActorsWithoutTheRequiredRole`);
- estrutura preparar / executar / verificar, separada por linhas em branco;
- asserções com AssertJ.

---

# 11A. Console Web (TypeScript/React)

- TypeScript estrito (`strict`, `noUncheckedIndexedAccess`); sem `any`.
- Componentes de função; estado do servidor com TanStack Query; filtros e paginação na URL.
- Uma pasta por área em `src/features/`; componentes reutilizáveis em `src/components/`.
- Chamadas à API apenas por `src/api/` (cliente com CSRF e tratamento de Problem Details).
- Nenhuma regra de negócio no cliente: ações disponíveis, permissões e validações definitivas vêm da API.
- Textos da interface em português; mensagens de erro da API traduzidas em `src/lib/format.ts`.
- Acessibilidade: todo campo com rótulo (`Field`), erros ligados por `aria-describedby`, foco visível,
  diálogos com `<dialog>` nativo, contraste AA nos dois temas.
- Sem `dangerouslySetInnerHTML` nem recursos de terceiros (fontes, scripts, imagens).

---

# 12. Verificação

| Regra                               | Como é verificada             |
| ----------------------------------- | ----------------------------- |
| Limites de módulos e camadas        | `ArchitectureTest` (ArchUnit) |
| Schema coerente com as entidades    | Hibernate `validate` na inicialização |
| Cobertura mínima                    | JaCoCo em CI                  |
| Tipos e estilo do console           | `tsc` estrito e ESLint em CI  |
| Acessibilidade do console           | axe nos testes end-to-end     |
| Demais convenções                   | revisão de código             |
