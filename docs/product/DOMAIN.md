# Domain Definition

# Enterprise Workflow Engine

**Versão:** 0.1
**Status:** Em validação

---

# 1. Objetivo

Este documento define os principais conceitos, termos e relações que compõem o domínio do **Enterprise Workflow Engine**.

Seu objetivo é estabelecer uma linguagem comum para o projeto antes da definição da arquitetura e da implementação.

Os conceitos apresentados representam o **domínio de negócio** e não devem ser interpretados diretamente como classes Java, tabelas de banco de dados ou componentes de infraestrutura.

---

# 2. Linguagem Ubíqua

Os termos abaixo devem possuir significado consistente em todo o projeto.

| Termo                   | Definição                                                                       |
| ----------------------- | ------------------------------------------------------------------------------- |
| **Workflow Definition** | Modelo versionado que define como determinado workflow deve ser executado       |
| **Workflow**            | Execução concreta de uma versão de uma Workflow Definition                      |
| **Step**                | Unidade de processo definida dentro de uma Workflow Definition                  |
| **State**               | Estado possível de um workflow e estado atual de uma execução                   |
| **Transition**          | Movimento permitido entre estados durante a execução                            |
| **Rule**                | Regra de negócio que influencia o comportamento ou uma transição                |
| **Actor**               | Usuário, grupo ou sistema responsável por uma ação                              |
| **Execution**           | Registro conceitual da execução de uma ação ou transição                        |
| **History**             | Histórico das mudanças ocorridas durante a execução                             |
| **Audit**               | Registro de operações relevantes para segurança, conformidade e rastreabilidade |

> **Nota:** `Task` não faz parte do modelo inicial. Poderá ser introduzido posteriormente caso o domínio exija uma representação explícita de atividades atribuíveis.

---

# 3. Workflow Definition

Uma **Workflow Definition** representa o modelo de um processo de negócio.

Ela define a estrutura necessária para que um workflow possa ser executado, incluindo:

* identificação;
* nome;
* descrição;
* versão;
* steps;
* estados;
* transições;
* regras.

Uma Workflow Definition é **versionada**.

Cada versão representa uma definição específica e imutável para fins de execução.

Exemplo:

```text
Purchase Approval

Version 1
Version 2
Version 3
```

Uma alteração relevante no processo deve resultar em uma nova versão da definição.

Uma nova versão não deve alterar retroativamente workflows que já estejam sendo executados com uma versão anterior.

---

# 4. Workflow

Um **Workflow** representa uma execução concreta de uma Workflow Definition.

Exemplo:

```text
Workflow Definition
"Purchase Approval — Version 2"

        ↓

Workflow #001
Purchase Request: 5000
Current State: PENDING_APPROVAL

        ↓

Workflow #002
Purchase Request: 12000
Current State: FINANCIAL_VALIDATION
```

Cada Workflow possui:

* identificador;
* referência para uma Workflow Definition;
* referência para uma versão específica da definição;
* estado atual;
* histórico de execução;
* informações necessárias para sua execução.

Uma Workflow Definition pode originar múltiplos Workflows.

---

# 5. Step

Um **Step** representa uma unidade de processo dentro de uma Workflow Definition.

Exemplos:

* Solicitação;
* Aprovação do Gestor;
* Validação Financeira;
* Execução;
* Finalização.

Um Step pode representar uma atividade:

* executada por um usuário;
* executada por um sistema;
* processada automaticamente;
* condicionada a uma regra.

No modelo inicial, `Step` representa a unidade de processo.

O conceito de `Task` não será introduzido enquanto não houver uma necessidade concreta de representar atividades atribuíveis de forma independente.

---

# 6. State

O **State** representa uma situação possível dentro do ciclo de vida de um Workflow.

Exemplos:

```text
CREATED
PENDING_APPROVAL
APPROVED
REJECTED
COMPLETED
CANCELLED
FAILED
```

A Workflow Definition determina os estados e transições possíveis.

O Workflow mantém o seu **estado atual**.

Conceitualmente:

```text
Workflow Definition
        │
        └── States possíveis

Workflow
        │
        └── currentState
```

O conjunto definitivo de estados será refinado durante a definição dos requisitos e regras de negócio.

---

# 7. Transition

Uma **Transition** representa uma mudança permitida entre estados.

Exemplo:

```text
PENDING_APPROVAL
        │
        │ approve
        ↓
APPROVED
```

Uma Transition pode depender de:

* ação de um Actor;
* Rule;
* condição de negócio;
* evento;
* resultado de uma execução.

Uma transição inválida deve ser rejeitada pelo domínio.

---

# 8. Rule

Uma **Rule** representa uma regra de negócio que influencia o comportamento do Workflow.

Exemplo:

```text
Purchase Amount > 10,000
        ↓
Financial Approval Required
```

Outro exemplo:

```text
Actor belongs to Finance
        ↓
Approval permitted
```

As regras poderão inicialmente ser implementadas de forma determinística no domínio.

Mecanismos mais dinâmicos e configuráveis poderão ser considerados posteriormente.

---

# 9. Actor

Um **Actor** representa o responsável por uma ação dentro do Workflow.

Um Actor pode representar:

* usuário;
* grupo;
* sistema externo;
* processo automático.

Exemplo:

```text
Actor: manager-123
Action: APPROVE
```

A identificação e autorização técnica dos Actors serão tratadas posteriormente pela arquitetura de segurança.

---

# 10. Execution

Uma **Execution** representa conceitualmente a realização de uma ação ou transição durante a execução de um Workflow.

Exemplos:

* iniciar Workflow;
* executar Step;
* aprovar;
* rejeitar;
* avançar para outro State;
* cancelar Workflow.

Uma Execution deve fornecer informações suficientes para permitir a rastreabilidade da execução.

A representação técnica desse conceito será definida posteriormente durante a modelagem arquitetural.

---

# 11. History

O **History** representa a sequência de mudanças ocorridas durante a execução de um Workflow.

Exemplo:

```text
10:00 — CREATED
10:02 — PENDING_APPROVAL
10:15 — APPROVED
10:16 — FINANCIAL_VALIDATION
11:00 — COMPLETED
```

O History deve permitir compreender a evolução de um Workflow ao longo de sua execução.

Sua finalidade principal é responder:

> "O que aconteceu com este Workflow?"

---

# 12. Audit

O **Audit** representa registros destinados à rastreabilidade, segurança e conformidade.

Exemplo:

```text
Actor: manager-123
Action: APPROVE
Resource: workflow-456
Timestamp: ...
```

A auditoria pode registrar operações como:

* alteração de Workflow Definition;
* criação de Workflow;
* aprovação;
* rejeição;
* cancelamento;
* alteração de responsável.

Sua finalidade principal é responder:

> "Quem realizou determinada operação, quando e sobre qual recurso?"

`History` e `Audit` possuem responsabilidades diferentes e não devem ser tratados como o mesmo conceito.

---

# 13. Versionamento

O versionamento das Workflow Definitions é uma característica fundamental do domínio.

Cada Workflow deve estar associado a uma versão específica da Workflow Definition.

Exemplo:

```text
Workflow Definition
Purchase Approval

Version 1
    │
    ├── Workflow #001
    └── Workflow #002

Version 2
    │
    ├── Workflow #003
    └── Workflow #004
```

Uma alteração na definição não deve modificar o comportamento de Workflows já criados.

Isso garante:

* previsibilidade;
* consistência;
* rastreabilidade;
* integridade das execuções;
* possibilidade de evolução dos processos.

---

# 14. Relações Conceituais

A relação principal entre os conceitos pode ser representada da seguinte forma:

```text
                 Workflow Definition
                         │
              ┌──────────┼──────────┐
              │          │          │
              ↓          ↓          ↓
            Step       State     Transition
              │          │          │
              └──────────┼──────────┘
                         │
                    Version
                         │
                         ↓
                      Workflow
                         │
               ┌─────────┼─────────┐
               │         │         │
               ↓         ↓         ↓
             State    Execution   History
                                    │
                                    ↓
                                  Audit
                                    │
                                    ↓
                                  Actor
```

---

# 15. Fluxo Conceitual de Execução

O ciclo conceitual de um Workflow é:

```text
Workflow Definition
        ↓
Seleção da versão
        ↓
Criação do Workflow
        ↓
Estado inicial
        ↓
Execução do Step
        ↓
Avaliação das Rules
        ↓
Validação da Transition
        ↓
Mudança de State
        ↓
Registro no History
        ↓
Próximo Step / State
```

Esse ciclo continua até que o Workflow alcance um estado terminal.

---

# 16. Invariantes do Domínio

As seguintes regras fundamentais deverão ser preservadas pelo domínio.

## 16.1 Workflow Definition válida

Um Workflow somente pode ser criado a partir de uma Workflow Definition válida e versionada.

---

## 16.2 Versão imutável durante execução

Um Workflow não pode trocar arbitrariamente a versão da Workflow Definition durante sua execução.

---

## 16.3 Estado válido

Um Workflow não pode assumir um State inexistente na sua definição.

---

## 16.4 Transition válida

Um Workflow somente pode realizar Transitions permitidas pela sua Workflow Definition.

---

## 16.5 Regras satisfeitas

Uma Transition somente pode ocorrer quando suas condições e Rules forem satisfeitas.

---

## 16.6 Histórico rastreável

Mudanças relevantes de State devem ser registradas no History.

---

# 17. Limites do Domínio

O domínio será responsável por:

* regras de Workflow;
* States;
* Transitions;
* Rules;
* invariantes;
* execução das regras de negócio.

O domínio não será responsável diretamente por:

* HTTP;
* banco de dados;
* Docker;
* frameworks;
* mensageria;
* autenticação técnica;
* detalhes de infraestrutura.

Essas responsabilidades serão definidas posteriormente na arquitetura.

---

# 18. Conceitos Fora do Modelo Inicial

O conceito abaixo foi deliberadamente deixado fora do modelo inicial:

### Task

Uma Task poderá ser introduzida futuramente caso seja necessário representar uma atividade atribuível independentemente de um Step.

A introdução desse conceito deverá ocorrer somente quando existir uma necessidade de negócio ou arquitetural clara.

---

# 19. Evolução do Domínio

Este documento representa a primeira versão do modelo conceitual do Enterprise Workflow Engine.

O domínio poderá evoluir conforme:

* requisitos forem detalhados;
* casos de uso forem definidos;
* regras de negócio forem identificadas;
* novos cenários forem descobertos.

Alterações relevantes no modelo de domínio deverão ser avaliadas antes de serem incorporadas ao projeto.

Quando uma alteração representar uma decisão arquitetural ou de domínio significativa, deverá ser considerada para registro através de ADR.

---

# 20. Relação com os Próximos Documentos

Este documento servirá como base para:

```text
docs/product/REQUIREMENTS.md
docs/product/USE_CASES.md
docs/product/BUSINESS_RULES.md
```

Posteriormente, os conceitos de domínio serão utilizados como entrada para:

```text
docs/architecture/ARCHITECTURE.md
docs/architecture/MODULES.md
docs/architecture/DATA_MODEL.md
```

A implementação em Java será definida somente após essas decisões serem amadurecidas.

---

# 21. Status do Documento

**Status:** Em validação

Este documento representa a definição inicial e deliberadamente evolutiva do domínio do Enterprise Workflow Engine.
