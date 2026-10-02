# Data Model

# Enterprise Workflow Engine

**Versão:** 0.2
**Status:** Aprovado

---

# 1. Objetivo

Este documento define o modelo de dados do **Enterprise Workflow Engine**: os conceitos persistentes,
seus relacionamentos e a estrutura física que os representa.

A estrutura física é propriedade das migrations em:

```text
src/main/resources/db/migration
```

Este documento explica o modelo; as migrations são a fonte da verdade sobre colunas e tipos.

---

# 2. Princípios do Modelo

- integridade dos dados;
- rastreabilidade;
- versionamento;
- preservação do histórico;
- independência entre definição e execução;
- consistência das relações;
- evolução controlada;
- persistência como detalhe de infraestrutura.

O modelo físico representa o domínio; limitações ou conveniências do banco não determinam as regras de
negócio. As decisões de persistência estão em `docs/adr/ADR-004.md`.

---

# 3. Visão Geral

O modelo possui três contextos, um por módulo de negócio que persiste dados:

```text
Workflow Definition      como o processo funciona
        │
        ▼
Workflow Execution       uma execução concreta do processo
        │
        ▼
Audit                    quem fez o quê, quando e com qual resultado
```

O módulo Rules não possui tabelas próprias: as Rules de uma Transition são parte da definição.

```text
workflow_definition 1 ─── N workflow_definition_version
                                   │ 1
                    ┌──────────────┼──────────────────┐
                    N              N                  N
             workflow_state   workflow_transition   workflow
                                   │ 1                │ 1
                                   N                  N
                         workflow_transition_rule   workflow_history

audit_record   (sem relacionamentos: referencia recursos por tipo e identificador)
```

---

# 4. Workflow Definition

## 4.1 `workflow_definition`

O modelo de um processo.

| Coluna           | Tipo            | Observação                                 |
| ---------------- | --------------- | ------------------------------------------ |
| `id`             | `uuid`          | chave primária                             |
| `definition_key` | `varchar(64)`   | identificação de negócio, única (BR-001)   |
| `name`           | `varchar(120)`  |                                            |
| `description`    | `varchar(1000)` | opcional                                   |
| `created_at`     | `timestamptz`   |                                            |
| `updated_at`     | `timestamptz`   |                                            |
| `lock_version`   | `bigint`        | controle de concorrência otimista          |

## 4.2 `workflow_definition_version`

Uma versão imutável da estrutura do processo.

| Coluna           | Tipo          | Observação                                       |
| ---------------- | ------------- | ------------------------------------------------ |
| `id`             | `uuid`        | chave primária; referenciada pelas execuções     |
| `definition_id`  | `uuid`        | FK para `workflow_definition`                    |
| `version_number` | `integer`     | sequencial por definição; único por definição    |
| `status`         | `varchar(16)` | `DRAFT`, `ACTIVE` ou `INACTIVE`                  |
| `created_at`     | `timestamptz` |                                                  |

Apenas `status` muda após a criação (ADR-006 §2.2).

## 4.3 `workflow_state`

| Coluna       | Tipo          | Observação                              |
| ------------ | ------------- | --------------------------------------- |
| `version_id` | `uuid`        | FK para a versão                        |
| `ordinal`    | `integer`     | ordem de declaração                     |
| `name`       | `varchar(64)` | único por versão                        |
| `state_type` | `varchar(16)` | `INITIAL`, `INTERMEDIATE` ou `TERMINAL` |

Chave primária: `(version_id, ordinal)`.

## 4.4 `workflow_transition`

| Coluna          | Tipo          | Observação                                 |
| --------------- | ------------- | ------------------------------------------ |
| `id`            | `uuid`        | chave primária                             |
| `version_id`    | `uuid`        | FK para a versão                           |
| `ordinal`       | `integer`     | ordem de declaração                        |
| `action`        | `varchar(64)` | única por `(version_id, from_state)`       |
| `from_state`    | `varchar(64)` | FK composta para `workflow_state`          |
| `to_state`      | `varchar(64)` | FK composta para `workflow_state`          |
| `required_role` | `varchar(64)` | opcional (BR-020)                          |

As FKs de `from_state` e `to_state` são `(version_id, name)`: uma Transition só referencia States da
própria versão (BR-017, BR-019). São adiadas para o commit porque States e Transitions de uma versão são
gravados na mesma transação.

## 4.5 `workflow_transition_rule`

| Coluna           | Tipo           | Observação                   |
| ---------------- | -------------- | ---------------------------- |
| `transition_id`  | `uuid`         | FK para a Transition         |
| `ordinal`        | `integer`      | ordem de declaração          |
| `field_name`     | `varchar(128)` | variável avaliada            |
| `rule_operator`  | `varchar(32)`  | operador de comparação       |
| `expected_value` | `varchar(255)` | opcional (operador `EXISTS`) |

---

# 5. Workflow Execution

## 5.1 `workflow`

| Coluna                      | Tipo          | Observação                                           |
| --------------------------- | ------------- | ---------------------------------------------------- |
| `id`                        | `uuid`        | chave primária                                       |
| `definition_key`            | `varchar(64)` | desnormalizado para consulta e filtro                |
| `definition_version_id`     | `uuid`        | FK para a versão utilizada; nunca muda (BR-010)      |
| `definition_version_number` | `integer`     | desnormalizado para consulta                         |
| `status`                    | `varchar(16)` | `CREATED`, `RUNNING`, `COMPLETED` ou `CANCELLED`     |
| `current_state`             | `varchar(64)` | State atual (BR-012)                                 |
| `variables`                 | `jsonb`       | dados da execução, usados pelas Rules                |
| `created_at`                | `timestamptz` |                                                      |
| `updated_at`                | `timestamptz` |                                                      |
| `lock_version`              | `bigint`      | controle de concorrência otimista (RNF-001)          |

A FK `(definition_version_id, current_state)` para `workflow_state` garante no banco que o State atual
existe na versão da execução (BR-014).

`definition_key` e `definition_version_number` são cópias imutáveis: a execução não pode trocar de versão,
portanto não há risco de divergência.

## 5.2 `workflow_history`

Registro append-only da evolução do Workflow (BR-029 a BR-031).

| Coluna            | Tipo           | Observação                                          |
| ----------------- | -------------- | --------------------------------------------------- |
| `id`              | `uuid`         | chave primária                                      |
| `workflow_id`     | `uuid`         | FK para `workflow`                                  |
| `sequence_number` | `integer`      | ordem cronológica; único por Workflow (BR-030)      |
| `event_type`      | `varchar(16)`  | `CREATED`, `STARTED`, `TRANSITIONED` ou `CANCELLED` |
| `action`          | `varchar(64)`  | ação executada, quando houver                       |
| `from_state`      | `varchar(64)`  | State anterior                                      |
| `to_state`        | `varchar(64)`  | novo State                                          |
| `actor_id`        | `varchar(128)` | responsável                                         |
| `occurred_at`     | `timestamptz`  | momento da operação                                 |
| `comment_text`    | `varchar(500)` | observação, como o motivo de um cancelamento        |

---

# 6. Audit

## 6.1 `audit_record`

Registro imutável de uma operação (BR-032, BR-033).

| Coluna          | Tipo            | Observação                           |
| --------------- | --------------- | ------------------------------------ |
| `id`            | `uuid`          | chave primária                       |
| `actor_id`      | `varchar(128)`  | quem                                 |
| `operation`     | `varchar(64)`   | o quê                                |
| `resource_type` | `varchar(64)`   | tipo do recurso afetado              |
| `resource_id`   | `varchar(128)`  | identificador do recurso afetado     |
| `outcome`       | `varchar(16)`   | `SUCCESS` ou `REJECTED`              |
| `detail`        | `varchar(1000)` | informação para investigação         |
| `occurred_at`   | `timestamptz`   | quando                               |

A tabela não possui chaves estrangeiras para os recursos: o módulo Audit não depende do modelo dos demais
módulos, e um registro de auditoria deve sobreviver ao recurso que descreve.

History e Audit são tabelas distintas com propósitos distintos (BR-034).

---

# 7. Versionamento e Imutabilidade

```text
workflow_definition "purchase-approval"
│
├── version 1 (INACTIVE) ◄── workflow #001, #002   continuam na versão 1
│
└── version 2 (ACTIVE)   ◄── workflow #003         novas execuções
```

- A estrutura de uma versão nunca é alterada (BR-006).
- Ativar ou desativar uma versão muda apenas `status` e não toca nas execuções (RF-005).
- No máximo uma versão `ACTIVE` por definição (BR-041), garantido pelo aggregate sob lock otimista.

---

# 8. Índices

Além das chaves primárias e únicas:

| Índice                          | Uso                                                  |
| ------------------------------- | ---------------------------------------------------- |
| `ix_definition_version_active`  | localizar a versão ativa ao criar um Workflow        |
| `ix_workflow_definition_key`    | listar Workflows de uma definição                    |
| `ix_workflow_status`            | listar Workflows por status                          |
| `ix_workflow_created_at`        | ordenação padrão da listagem                         |
| `ix_audit_record_resource`      | auditoria de um recurso                              |
| `ix_audit_record_actor`         | auditoria de um Actor                                |
| `ix_audit_record_occurred_at`   | ordenação e recorte temporal                         |

---

# 9. Migrations

- Ferramenta: Flyway, executado na inicialização da aplicação.
- Arquivos SQL versionados: `V<n>__<descricao>.sql`.
- **Uma migration aplicada nunca é editada**; toda alteração de schema é uma nova migration.
- O Hibernate apenas valida o schema (`ddl-auto: validate`): divergência entre entidades e banco impede
  a aplicação de subir.

| Versão | Conteúdo                                         |
| ------ | ------------------------------------------------ |
| `V1`   | módulo Workflow Definition                       |
| `V2`   | módulo Workflow Execution                        |
| `V3`   | módulo Audit                                     |

---

# 10. Relação com Outros Documentos

```text
docs/product/DOMAIN.md
docs/product/BUSINESS_RULES.md
docs/architecture/ARCHITECTURE.md
docs/architecture/MODULES.md
docs/adr/ADR-004.md
docs/adr/ADR-006.md
```
