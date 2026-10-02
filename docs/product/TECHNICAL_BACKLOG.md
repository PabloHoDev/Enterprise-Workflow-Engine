# Technical Backlog

# Enterprise Workflow Engine

**Versão:** 0.1
**Status:** Em evolução

---

# 1. Objetivo

Oportunidades futuras: itens que podem melhorar o sistema, mas não impedem a evolução atual
(`docs/PROJECT_GOVERNANCE.md` §13).

Limitações de algo já entregue não ficam aqui, e sim em `docs/releases/TECHNICAL_DEBT.md`.

Prioridade: **Alta** (próxima etapa), **Média** (quando houver demanda), **Baixa** (ideia registrada).

---

# 2. Itens

| ID     | Item                                             | Prioridade | Origem                         |
| ------ | ------------------------------------------------ | ---------- | ------------------------------ |
| TB-001 | Consulta do History com paginação                | Média      | RF-016                         |
| TB-002 | Rascunhos editáveis de versão                    | Baixa      | ADR-006 §2.2                   |
| TB-003 | Transições automáticas executadas pelo sistema   | Alta       | UC-009 (ator System)           |
| TB-004 | Eventos de domínio e notificações                | Média      | `ARCHITECTURE.md` §19          |
| TB-005 | Exportação de métricas e tracing                 | Média      | RNF-004, `DEPLOYMENT.md` §14   |
| TB-006 | Módulos Maven separados                          | Baixa      | ADR-002 §3.2                   |
| TB-007 | Novos tipos de Rule                              | Média      | RNF-009                        |
| TB-008 | Objetivos e testes de desempenho                 | Média      | RNF-011                        |
| TB-009 | Publicação da imagem e deployment automatizado   | Alta       | `DEPLOYMENT.md` §17            |
| TB-010 | Idempotência na criação de Workflows             | Média      | UC-006 (External System)       |
| TB-011 | Prazos e expiração por State                     | Baixa      | —                              |
| TB-012 | Validação das variáveis do Workflow              | Média      | UC-006 A2                      |

---

# 3. Detalhamento

## TB-001 — Consulta do History com paginação

O History é devolvido inteiro e carregado junto com o Workflow a cada operação. É adequado para processos
com dezenas de mudanças; para execuções longas, paginar a consulta e deixar de carregar o History nas
operações de escrita.

## TB-002 — Rascunhos editáveis de versão

Permitir salvar uma versão incompleta e editá-la até a ativação, movendo a validação estrutural para a
ativação (BR-004). Hoje uma versão nasce completa e imutável.

## TB-003 — Transições automáticas executadas pelo sistema

Transitions sem ação de Actor, disparadas quando suas Rules passam a ser satisfeitas (por exemplo,
aprovação automática abaixo de um valor). Exige definir o Actor de sistema e o gatilho de reavaliação.

## TB-004 — Eventos de domínio e notificações

Publicar eventos (`WorkflowCompleted`, `WorkflowCancelled`...) para consumidores internos e, depois,
externos. Avaliar o padrão outbox antes de introduzir mensageria. Merece ADR.

## TB-005 — Exportação de métricas e tracing

Registry de métricas (Prometheus/OTLP), métricas de negócio (Workflows criados, concluídos, recusas por
Rule) e tracing distribuído.

## TB-006 — Módulos Maven separados

Converter os pacotes de módulo em módulos Maven, obtendo isolamento em tempo de compilação. Só se
justifica se os testes de arquitetura deixarem de ser suficientes ou se um módulo for extraído.

## TB-007 — Novos tipos de Rule

Operadores adicionais (`IN`, `MATCHES`, comparação de datas) e Rules sobre o Actor (por exemplo, quem
aprova não pode ser quem solicitou). O contrato `RuleEvaluator` já isola o ponto de extensão.

## TB-008 — Objetivos e testes de desempenho

Definir metas quantitativas (RNF-011) e medi-las com testes de carga, incluindo o cenário de contenção
sobre um mesmo Workflow.

## TB-009 — Publicação da imagem e deployment automatizado

Publicar a imagem em um registry a cada versão e automatizar o deployment em um ambiente de destino.

## TB-010 — Idempotência na criação de Workflows

Aceitar uma chave de idempotência para que um sistema externo possa repetir uma criação sem duplicar a
execução.

## TB-011 — Prazos e expiração por State

Permitir que um State tenha prazo, com ação automática ao expirar. Depende de TB-003.

## TB-012 — Validação das variáveis do Workflow

Permitir que a definição declare as variáveis esperadas (nome, tipo, obrigatoriedade) e validar a entrada
na criação. Hoje as variáveis têm formato livre.

---

# 4. Como Adicionar um Item

1. Use o próximo identificador `TB-NNN`.
2. Descreva o que é e por que melhoraria o sistema.
3. Indique a origem (requisito, ADR, documento) e a prioridade.
4. Ao iniciar o trabalho, mova o item para o roadmap; ao concluir, remova-o daqui e registre no changelog.
