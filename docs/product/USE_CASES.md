# Use Cases

# Enterprise Workflow Engine

**Versão:** 0.1
**Status:** 🟢 APROVADA

---

# 1. Objetivo

Este documento define os principais casos de uso do **Enterprise Workflow Engine**.

Os casos de uso descrevem como os diferentes atores interagem com o sistema para alcançar objetivos de negócio.

Este documento representa o comportamento esperado do produto e não define detalhes de implementação.

---

# 2. Atores

## 2.1 Administrator

Responsável pela configuração e administração das Workflow Definitions.

Principais responsabilidades:

* criar definições;
* criar versões;
* ativar definições;
* desativar definições;
* consultar configurações.

---

## 2.2 Workflow User

Usuário responsável por executar ações relacionadas aos Workflows.

Pode:

* consultar Workflows;
* executar ações permitidas;
* aprovar;
* rejeitar;
* acompanhar execuções.

---

## 2.3 System

Representa processos automáticos executados pelo próprio sistema.

Pode:

* iniciar execuções;
* avaliar regras;
* executar transições automáticas;
* registrar operações.

---

## 2.4 External System

Representa sistemas externos integrados ao Workflow Engine.

Pode:

* criar Workflows;
* consultar execuções;
* enviar informações;
* disparar determinadas operações.

As permissões dependerão da configuração de segurança.

---

# 3. Visão Geral dos Casos de Uso

| ID     | Caso de Uso                   | Ator Principal                  | Prioridade |
| ------ | ----------------------------- | ------------------------------- | ---------- |
| UC-001 | Criar Workflow Definition     | Administrator                   | MVP        |
| UC-002 | Criar nova versão             | Administrator                   | MVP        |
| UC-003 | Ativar Workflow Definition    | Administrator                   | MVP        |
| UC-004 | Desativar Workflow Definition | Administrator                   | MVP        |
| UC-005 | Consultar Workflow Definition | Administrator / Workflow User   | MVP        |
| UC-006 | Criar Workflow                | Workflow User / External System | MVP        |
| UC-007 | Iniciar Workflow              | Workflow User / System          | MVP        |
| UC-008 | Consultar Workflow            | Workflow User / External System | MVP        |
| UC-009 | Executar ação do Workflow     | Workflow User / System          | MVP        |
| UC-010 | Consultar histórico           | Workflow User / Administrator   | MVP        |
| UC-011 | Consultar auditoria           | Administrator                   | MVP        |
| UC-012 | Cancelar Workflow             | Workflow User / Administrator   | MVP        |

---

# 4. UC-001 — Criar Workflow Definition

## Objetivo

Permitir que um Administrator crie uma nova definição de workflow.

## Ator Principal

Administrator

## Pré-condições

* Administrator autenticado;
* Administrator autorizado a criar definições.

## Fluxo Principal

1. Administrator solicita a criação de uma Workflow Definition.
2. Sistema recebe os dados da definição.
3. Sistema valida os dados obrigatórios.
4. Sistema valida a estrutura do workflow.
5. Sistema cria a definição.
6. Sistema associa a primeira versão.
7. Sistema registra a operação.
8. Sistema retorna a definição criada.

## Fluxos Alternativos

### A1 — Dados inválidos

1. Sistema identifica dados inválidos.
2. Sistema rejeita a operação.
3. Sistema informa os erros encontrados.

### A2 — Nome ou identificador já utilizado

1. Sistema identifica conflito.
2. Sistema rejeita a criação.
3. Sistema informa o conflito.

## Resultado

Uma Workflow Definition válida é criada.

---

# 5. UC-002 — Criar Nova Versão

## Objetivo

Criar uma nova versão de uma Workflow Definition existente.

## Ator Principal

Administrator

## Pré-condições

* Workflow Definition existente;
* Administrator autorizado.

## Fluxo Principal

1. Administrator solicita nova versão.
2. Sistema recupera a definição existente.
3. Sistema cria uma nova versão.
4. Administrator fornece as alterações.
5. Sistema valida a nova estrutura.
6. Sistema registra a nova versão.
7. Sistema retorna a versão criada.

## Fluxos Alternativos

### A1 — Definição inválida

Sistema rejeita a nova versão.

### A2 — Versão em conflito

Sistema impede a criação de uma versão inconsistente.

## Resultado

Uma nova versão da Workflow Definition é criada sem alterar versões anteriores.

---

# 6. UC-003 — Ativar Workflow Definition

## Objetivo

Disponibilizar uma versão de Workflow Definition para novas execuções.

## Ator Principal

Administrator

## Pré-condições

* definição existente;
* versão válida;
* Administrator autorizado.

## Fluxo Principal

1. Administrator solicita ativação.
2. Sistema valida a definição.
3. Sistema verifica se todos os elementos necessários estão configurados.
4. Sistema altera o status da versão.
5. Sistema registra a operação.

## Resultado

A versão pode originar novos Workflows.

---

# 7. UC-004 — Desativar Workflow Definition

## Objetivo

Impedir que uma versão seja utilizada para novas execuções.

## Ator Principal

Administrator

## Pré-condições

* versão existente;
* Administrator autorizado.

## Fluxo Principal

1. Administrator solicita desativação.
2. Sistema valida a operação.
3. Sistema altera o status da versão.
4. Sistema registra a operação.

## Resultado

Novos Workflows não podem ser criados a partir da versão desativada.

Workflows existentes não são automaticamente interrompidos.

---

# 8. UC-005 — Consultar Workflow Definition

## Objetivo

Permitir consultar uma Workflow Definition e suas versões.

## Atores

Administrator / Workflow User

## Fluxo Principal

1. Ator solicita uma definição.
2. Sistema identifica a definição.
3. Sistema retorna seus dados.
4. Quando solicitado, sistema retorna suas versões.

## Resultado

O ator obtém informações sobre a definição.

---

# 9. UC-006 — Criar Workflow

## Objetivo

Criar uma execução concreta a partir de uma Workflow Definition ativa.

## Atores

Workflow User / External System

## Pré-condições

* Workflow Definition existente;
* versão ativa;
* ator autorizado.

## Fluxo Principal

1. Ator solicita criação do Workflow.
2. Sistema identifica a Workflow Definition.
3. Sistema seleciona a versão aplicável.
4. Sistema valida os dados de entrada.
5. Sistema cria o Workflow.
6. Sistema associa a versão utilizada.
7. Sistema define o estado inicial.
8. Sistema registra a criação.
9. Sistema retorna o Workflow.

## Fluxos Alternativos

### A1 — Definição indisponível

Sistema rejeita a criação.

### A2 — Dados inválidos

Sistema rejeita a operação.

## Resultado

Um Workflow é criado e associado a uma versão específica da definição.

---

# 10. UC-007 — Iniciar Workflow

## Objetivo

Iniciar a execução de um Workflow.

## Atores

Workflow User / System

## Pré-condições

* Workflow existente;
* Workflow não iniciado;
* definição válida.

## Fluxo Principal

1. Ator solicita o início.
2. Sistema valida o Workflow.
3. Sistema identifica o estado inicial.
4. Sistema verifica as regras aplicáveis.
5. Sistema inicia a execução.
6. Sistema registra a mudança.
7. Sistema atualiza o estado.
8. Sistema registra o histórico.

## Resultado

O Workflow passa para o estado inicial de execução.

---

# 11. UC-008 — Consultar Workflow

## Objetivo

Consultar o estado e informações de um Workflow.

## Atores

Workflow User / External System / Administrator

## Fluxo Principal

1. Ator solicita um Workflow.
2. Sistema localiza o Workflow.
3. Sistema retorna:

   * identificador;
   * definição;
   * versão;
   * estado atual;
   * datas relevantes;
   * informações da execução.

## Resultado

O ator obtém o estado atual do Workflow.

---

# 12. UC-009 — Executar Ação do Workflow

## Objetivo

Permitir que um ator execute uma ação disponível para o Workflow.

Exemplos:

* aprovar;
* rejeitar;
* avançar;
* executar;
* cancelar.

## Atores

Workflow User / System

## Pré-condições

* Workflow existente;
* ação disponível no estado atual;
* ator autorizado.

## Fluxo Principal

1. Ator solicita uma ação.
2. Sistema identifica o Workflow.
3. Sistema verifica o estado atual.
4. Sistema identifica a Transition correspondente.
5. Sistema valida a autorização do ator.
6. Sistema avalia as Rules aplicáveis.
7. Sistema valida a Transition.
8. Sistema executa a mudança.
9. Sistema atualiza o estado.
10. Sistema registra a Execution.
11. Sistema registra o History.
12. Sistema registra Audit quando aplicável.

## Fluxos Alternativos

### A1 — Ação não disponível

Sistema rejeita a operação.

### A2 — Actor não autorizado

Sistema rejeita a operação.

### A3 — Rule não satisfeita

Sistema impede a Transition.

### A4 — Workflow em estado terminal

Sistema rejeita a operação.

## Resultado

O Workflow é atualizado para o próximo estado permitido.

---

# 13. UC-010 — Consultar Histórico

## Objetivo

Permitir visualizar a evolução de um Workflow.

## Atores

Workflow User / Administrator

## Fluxo Principal

1. Ator solicita o histórico.
2. Sistema localiza o Workflow.
3. Sistema recupera os registros.
4. Sistema apresenta as mudanças em ordem cronológica.

## Resultado

O ator consegue compreender a evolução do Workflow.

---

# 14. UC-011 — Consultar Auditoria

## Objetivo

Permitir consultar operações registradas para fins de auditoria.

## Ator Principal

Administrator

## Pré-condições

* Administrator autenticado;
* permissão adequada.

## Fluxo Principal

1. Administrator solicita registros de auditoria.
2. Sistema valida a autorização.
3. Sistema aplica os filtros solicitados.
4. Sistema retorna os registros.

## Resultado

O Administrator consegue rastrear operações relevantes.

---

# 15. UC-012 — Cancelar Workflow

## Objetivo

Permitir cancelar um Workflow quando essa operação for permitida.

## Atores

Workflow User / Administrator

## Pré-condições

* Workflow existente;
* Workflow em estado que permita cancelamento;
* ator autorizado.

## Fluxo Principal

1. Ator solicita cancelamento.
2. Sistema identifica o Workflow.
3. Sistema verifica o estado atual.
4. Sistema verifica a autorização.
5. Sistema avalia as regras aplicáveis.
6. Sistema altera o estado para CANCELLED.
7. Sistema registra a Execution.
8. Sistema registra o History.
9. Sistema registra Audit quando aplicável.

## Fluxos Alternativos

### A1 — Workflow não pode ser cancelado

Sistema rejeita a operação.

### A2 — Actor não autorizado

Sistema rejeita a operação.

## Resultado

O Workflow entra em estado terminal `CANCELLED`.

---

# 16. Regras Transversais

Os seguintes comportamentos devem ser observados por todos os casos de uso relevantes:

### RT-001 — Autorização

Operações protegidas devem verificar se o Actor possui permissão.

### RT-002 — Integridade

Nenhuma operação deve deixar o Workflow em estado inconsistente.

### RT-003 — Versionamento

A execução deve permanecer associada à versão da Workflow Definition utilizada na criação.

### RT-004 — Transitions

Somente Transitions permitidas pelo estado atual podem ser executadas.

### RT-005 — Rules

Rules aplicáveis devem ser avaliadas antes da efetivação de uma Transition.

### RT-006 — Rastreabilidade

Mudanças relevantes devem gerar registros adequados de History e, quando aplicável, Audit.

### RT-007 — Estados terminais

Workflows em estados terminais não devem aceitar ações incompatíveis com seu ciclo de vida.

---

# 17. Relação com Requisitos

Os casos de uso deste documento derivam dos requisitos definidos em:

```text
docs/product/REQUIREMENTS.md
```

A relação conceitual é:

```text
VISION
   ↓
DOMAIN
   ↓
REQUIREMENTS
   ↓
USE CASES
   ↓
ARCHITECTURE
```

---

# 18. Fora do Escopo

Não fazem parte dos casos de uso iniciais:

* edição visual de workflows;
* BPMN;
* criação dinâmica de Rules por usuários de negócio;
* gerenciamento avançado de Tasks;
* inteligência artificial;
* funcionalidades completas de frontend.

Esses recursos poderão ser adicionados posteriormente.

---

# 19. Critérios Gerais de Aceitação

Um caso de uso será considerado implementado quando:

1. seu fluxo principal estiver funcional;
2. os fluxos alternativos relevantes forem tratados;
3. as regras do domínio forem respeitadas;
4. as permissões forem verificadas quando aplicável;
5. as mudanças relevantes forem rastreáveis;
6. existirem testes compatíveis com a criticidade do caso de uso.

---

# 20. Evolução

Os casos de uso representam a primeira versão do comportamento esperado do produto.

Novos casos de uso poderão ser adicionados conforme novos requisitos sejam aprovados.

Alterações relevantes nos casos de uso deverão ser avaliadas quanto ao impacto sobre:

* domínio;
* requisitos;
* arquitetura;
* roadmap.

---

# 21. Status do Documento

**Status:** 🟢 APROVADA

Este documento representa a primeira versão dos casos de uso do Enterprise Workflow Engine.
