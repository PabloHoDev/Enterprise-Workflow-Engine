# Domain Definition

# Enterprise Workflow Engine

**Versão:** 0.1  
**Status:** Em definição

---

# 1. Objetivo

Este documento define os principais conceitos, termos e relações que compõem o domínio do **Enterprise Workflow Engine**.

O objetivo é estabelecer uma linguagem comum para o projeto antes da definição da arquitetura de software e da implementação.

Os conceitos apresentados neste documento representam o domínio de negócio e não devem ser confundidos diretamente com classes, tabelas ou componentes de infraestrutura.

---

# 2. Linguagem Ubíqua

Os termos abaixo devem possuir significado consistente em todo o projeto.

| Termo | Definição |
|---|---|
| **Workflow** | Processo estruturado composto por etapas e transições |
| **Workflow Definition** | Modelo que define como um workflow deve ser executado |
| **Workflow Instance** | Execução concreta de uma Workflow Definition |
| **Step** | Etapa pertencente a um workflow |
| **Transition** | Regra que permite a passagem entre etapas ou estados |
| **State** | Estado atual de uma instância durante sua execução |
| **Rule** | Regra de negócio utilizada para determinar comportamentos ou transições |
| **Actor** | Usuário ou sistema responsável por uma ação |
| **Task** | Atividade que precisa ser executada durante um workflow |
| **Execution** | Processo de execução de uma etapa ou transição |
| **History** | Registro das mudanças ocorridas durante a execução |
| **Audit** | Registro destinado à rastreabilidade das operações relevantes |

---

# 3. Workflow

Um **Workflow** representa um processo de negócio estruturado.

Um workflow é composto por:

- etapas;
- estados;
- transições;
- regras;
- responsáveis;
- condições de execução.

Exemplo:

```text
Solicitação
    ↓
Aprovação do Gestor
    ↓
Validação Financeira
    ↓
Execução
    ↓
Finalização
```

O workflow representa a definição do processo, não uma execução específica.

---

# 4. Workflow Definition

A **Workflow Definition** representa o modelo de um workflow.

Ela define:

- nome;
- descrição;
- versão;
- etapas;
- transições;
- regras;
- configurações necessárias para execução.

Uma definição pode possuir múltiplas versões ao longo do tempo.

Exemplo:

```text
Purchase Approval Workflow
    Version 1
    Version 2
    Version 3
```

Uma alteração na definição não deve modificar retroativamente instâncias que já estejam sendo executadas.

---

# 5. Workflow Instance

Uma **Workflow Instance** representa uma execução concreta de uma Workflow Definition.

Exemplo:

```text
Workflow Definition

"Purchase Approval"

        ↓

Workflow Instance #001
Solicitação: R$ 5.000
Solicitante: User A
Status: Waiting Approval

        ↓

Workflow Instance #002
Solicitação: R$ 12.000
Solicitante: User B
Status: Financial Validation
```

Cada instância possui seu próprio:

- identificador;
- estado;
- contexto;
- histórico;
- responsáveis;
- timestamps.

Uma mesma definição pode gerar diversas instâncias.

---

# 6. Step

Um **Step** representa uma etapa definida dentro de um workflow.

Exemplos:

- Solicitação;
- Aprovação;
- Validação;
- Execução;
- Finalização.

Uma etapa pode exigir:

- ação de um usuário;
- ação de um sistema;
- avaliação de uma regra;
- processamento automático.

---

# 7. Task

Uma **Task** representa uma atividade concreta que precisa ser executada dentro de uma etapa.

Exemplo:

```text
Step:
Manager Approval

Task:
Approve Purchase Request #1234
```

Uma etapa pode gerar uma ou mais tarefas dependendo das regras do workflow.

---

# 8. State

O **State** representa a situação atual de uma Workflow Instance.

Exemplos:

```text
CREATED
RUNNING
WAITING
COMPLETED
REJECTED
CANCELLED
FAILED
```

O conjunto definitivo de estados será definido posteriormente durante a especificação dos requisitos e regras de negócio.

Uma instância deve possuir um estado consistente durante toda sua execução.

---

# 9. Transition

Uma **Transition** representa uma mudança permitida entre estados ou etapas.

Exemplo:

```text
WAITING_APPROVAL
        |
        | approve
        ↓
APPROVED
```

Uma transição pode depender de:

- ação de um ator;
- regra de negócio;
- condição;
- evento;
- resultado de uma execução.

Transições inválidas devem ser rejeitadas pelo domínio.

---

# 10. Rule

Uma **Rule** representa uma condição ou regra de negócio que influencia o comportamento do workflow.

Exemplos:

```text
Valor > R$ 10.000
        ↓
Exigir aprovação financeira
```

ou:

```text
Aprovador pertence ao departamento financeiro
        ↓
Permitir aprovação
```

As regras poderão inicialmente ser implementadas de forma estática e posteriormente evoluir para mecanismos configuráveis.

---

# 11. Actor

Um **Actor** representa quem executa uma ação dentro do workflow.

Um actor pode representar:

- usuário;
- grupo;
- sistema externo;
- processo automático.

Exemplo:

```text
Actor: Manager
Action: APPROVE
```

A identificação e autorização dos atores serão tratadas posteriormente pela camada de segurança.

---

# 12. Execution

Uma **Execution** representa a realização de uma ação dentro de uma Workflow Instance.

Exemplos:

- iniciar workflow;
- executar tarefa;
- aprovar etapa;
- rejeitar solicitação;
- avançar para próxima etapa;
- cancelar execução.

A execução deve produzir informações suficientes para permitir rastreabilidade.

---

# 13. History

O **History** representa o histórico das alterações ocorridas durante a execução de um workflow.

Exemplo:

```text
10:00 — Workflow created
10:02 — Manager assigned
10:15 — Request approved
10:16 — Financial validation started
```

O histórico deve permitir reconstruir a evolução de uma Workflow Instance.

---

# 14. Audit

A **Audit** representa registros relacionados a operações relevantes para segurança, conformidade e rastreabilidade.

Exemplos:

- alteração de uma Workflow Definition;
- criação de uma instância;
- mudança de responsável;
- aprovação;
- rejeição;
- cancelamento.

A estratégia completa de auditoria será definida posteriormente na arquitetura e nos requisitos de segurança.

---

# 15. Relações Conceituais

A relação principal entre os conceitos pode ser representada da seguinte forma:

```text
Workflow Definition
        │
        ├── Step
        │
        ├── Transition
        │
        └── Rule
                │
                ↓
        Workflow Instance
                │
                ├── State
                │
                ├── Task
                │
                ├── Execution
                │
                ├── History
                │
                └── Audit
                        │
                        ↓
                      Actor
```

---

# 16. Modelo Conceitual Simplificado

O fluxo conceitual do sistema é:

```text
Definition
     │
     ↓
Workflow
     │
     ↓
Instance
     │
     ↓
State
     │
     ↓
Task / Action
     │
     ↓
Rule Evaluation
     │
     ↓
Transition
     │
     ↓
Next State
```

Cada mudança relevante deve produzir informações suficientes para manter a rastreabilidade da execução.

---

# 17. Invariantes do Domínio

Algumas regras fundamentais deverão ser preservadas pelo domínio.

## 17.1 Estado válido

Uma Workflow Instance não pode assumir um estado inexistente ou inválido.

---

## 17.2 Transição válida

Uma instância somente pode realizar transições permitidas pela definição do workflow.

---

## 17.3 Integridade da execução

Uma transição não deve ocorrer sem que as condições necessárias tenham sido satisfeitas.

---

## 17.4 Histórico

Mudanças relevantes de estado devem ser rastreáveis.

---

## 17.5 Versionamento

Alterações em uma Workflow Definition não devem alterar retroativamente uma instância já criada com uma versão anterior.

---

# 18. Limites do Domínio

O domínio será responsável por:

- regras de workflow;
- estados;
- transições;
- execução;
- invariantes;
- regras de negócio.

O domínio não será responsável diretamente por:

- banco de dados;
- HTTP;
- autenticação técnica;
- mensageria;
- Docker;
- frameworks;
- detalhes de infraestrutura.

Essas responsabilidades serão definidas posteriormente na arquitetura.

---

# 19. Evolução do Domínio

Os conceitos definidos neste documento representam a primeira versão do modelo de domínio.

Novos conceitos poderão surgir conforme:

- requisitos forem detalhados;
- casos de uso forem definidos;
- regras de negócio forem identificadas;
- decisões arquiteturais forem tomadas.

Alterações relevantes no modelo conceitual devem ser avaliadas antes de serem incorporadas ao projeto.

---

# 20. Status do Documento

Este documento representa a definição inicial do domínio do Enterprise Workflow Engine.

**Status atual:** Em validação.

Próximas referências:

```text
docs/product/REQUIREMENTS.md
docs/product/USE_CASES.md
docs/product/BUSINESS_RULES.md
```