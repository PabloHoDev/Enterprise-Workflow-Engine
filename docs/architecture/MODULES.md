# Modules

# Enterprise Workflow Engine

**Versão:** 0.1
**Status:** Aprovado

---

# 1. Objetivo

Este documento define a organização modular do **Enterprise Workflow Engine**.

O sistema será desenvolvido como um **Modular Monolith**, conforme estabelecido em:

```text
docs/adr/ADR-001.md
```

A modularização tem como objetivo estabelecer limites claros entre as responsabilidades do sistema, reduzir acoplamento e permitir evolução incremental sem introduzir complexidade distribuída prematuramente.

Este documento define:

* os módulos iniciais;
* suas responsabilidades;
* seus limites;
* dependências permitidas;
* regras de comunicação;
* responsabilidades que não pertencem a cada módulo.

---

# 2. Visão Geral

A estrutura inicial do domínio será organizada nos seguintes módulos:

```text
Enterprise Workflow Engine
│
├── Workflow Definition
│
├── Workflow Execution
│
├── Rules
│
└── Audit
```

Além dos módulos de negócio, a aplicação possuirá componentes arquiteturais responsáveis por interfaces e infraestrutura.

```text
Enterprise Workflow Engine
│
├── Business Modules
│   │
│   ├── Workflow Definition
│   ├── Workflow Execution
│   ├── Rules
│   └── Audit
│
├── Interfaces
│
└── Infrastructure
```

---

# 3. Princípios de Modularização

A organização dos módulos deve respeitar os seguintes princípios.

## 3.1 Alta Coesão

Cada módulo deve concentrar responsabilidades relacionadas a um contexto específico.

Um módulo não deve assumir responsabilidades apenas porque outro módulo ainda não foi criado.

---

## 3.2 Baixo Acoplamento

Um módulo deve conhecer apenas os contratos necessários para interagir com outros módulos.

Detalhes internos não devem ser acessados diretamente.

---

## 3.3 Limites Explícitos

Cada módulo deve possuir responsabilidades claramente definidas.

Sempre que houver dúvida sobre onde determinado comportamento pertence, a decisão deve ser baseada na responsabilidade do módulo e não na conveniência da implementação.

---

## 3.4 Dependências Controladas

As dependências entre módulos devem ser explícitas e limitadas.

Dependências circulares não são permitidas.

---

## 3.5 Evolução Independente

Os módulos devem ser organizados de forma que possam evoluir com o menor impacto possível nos demais componentes.

Isso não significa que todos os módulos sejam completamente independentes.

---

# 4. Workflow Definition Module

## 4.1 Responsabilidade

O módulo **Workflow Definition** representa a definição estrutural de um processo.

Ele responde à pergunta:

> **Como este processo funciona?**

---

## 4.2 Responsabilidades Principais

Este módulo é responsável por:

* criação de Workflow Definitions;
* manutenção das versões;
* States definidos para o processo;
* Transitions permitidas;
* configuração estrutural do Workflow;
* ativação de versões;
* desativação de versões;
* validação da integridade estrutural.

---

## 4.3 Conceitos Principais

O módulo contém conceitos como:

```text
WorkflowDefinition
│
├── Version
│
├── State
│
└── Transition
```

A estrutura exata poderá evoluir conforme o modelo de domínio for detalhado.

---

## 4.4 Responsabilidades que Não Pertencem ao Módulo

O módulo não é responsável por:

* executar Workflows;
* controlar o estado atual de uma execução;
* registrar History de uma execução;
* registrar Audit;
* executar regras de negócio durante uma execução;
* autenticação de usuários.

---

# 5. Workflow Execution Module

## 5.1 Responsabilidade

O módulo **Workflow Execution** representa a execução concreta de um processo definido.

Ele responde à pergunta:

> **O que está acontecendo nesta execução específica?**

---

## 5.2 Responsabilidades Principais

Este módulo é responsável por:

* criação de Workflows;
* associação com uma versão específica da Workflow Definition;
* controle do State atual;
* execução de ações;
* solicitação e execução de Transitions;
* ciclo de vida do Workflow;
* cancelamento;
* finalização;
* preservação da integridade da execução;
* registro do History.

---

## 5.3 Conceitos Principais

```text
Workflow
│
├── Current State
│
├── Execution
└── History
```

`History` pertence ao módulo porque representa a evolução de uma execução específica.

---

## 5.4 Dependências

O módulo poderá depender de contratos públicos fornecidos pelo módulo:

```text
Workflow Definition
```

Essa dependência é necessária para que uma execução possa conhecer:

* a versão utilizada;
* States disponíveis;
* Transitions permitidas.

A implementação interna do módulo Workflow Definition não deve ser acessada diretamente.

---

# 6. Rules Module

## 6.1 Responsabilidade

O módulo **Rules** é responsável por avaliar regras aplicáveis ao contexto de execução de um Workflow.

Ele responde à pergunta:

> **Esta operação pode ser realizada?**

---

## 6.2 Responsabilidades Principais

O módulo poderá ser responsável por:

* avaliação de condições;
* validação de regras associadas a Transitions;
* validação de requisitos para determinadas ações;
* fornecimento de resultados de avaliação.

---

## 6.3 Limites

O módulo Rules não deve se tornar um repositório genérico para qualquer regra existente no sistema.

Regras fundamentais relacionadas diretamente a uma entidade ou Aggregate devem permanecer próximas ao comportamento que protegem.

O módulo Rules deve concentrar regras que:

* precisam ser avaliadas em determinado contexto;
* podem variar conforme o processo;
* participam da decisão sobre uma operação;
* possuem relação com a execução de Transitions ou ações.

---

## 6.4 Interação

O Workflow Execution poderá solicitar a avaliação de Rules antes de efetivar uma Transition.

Conceitualmente:

```text
Workflow Execution
        │
        ▼
 Rule Evaluation Contract
        │
        ▼
      Rules
        │
        ▼
 Evaluation Result
```

O Workflow Execution permanece responsável pela decisão sobre a mudança de estado.

O módulo Rules fornece o resultado da avaliação.

---

# 7. Audit Module

## 7.1 Responsabilidade

O módulo **Audit** é responsável pela rastreabilidade de operações relevantes realizadas no sistema.

Ele responde à pergunta:

> **Quem fez o quê, quando e qual foi o resultado?**

---

## 7.2 Responsabilidades Principais

O módulo poderá registrar:

* Actor;
* operação;
* recurso afetado;
* momento da operação;
* resultado;
* informações relevantes para investigação.

---

## 7.3 Limites

O Audit não é responsável por representar a evolução do Workflow.

Essa responsabilidade pertence ao:

```text
Workflow Execution
└── History
```

A separação deve ser preservada:

```text
History
    ↓
Evolução do Workflow

Audit
    ↓
Rastreabilidade de operações
```

---

# 8. Comunicação entre Módulos

A comunicação entre módulos deve ocorrer através de contratos explícitos.

Um módulo não deve depender diretamente de detalhes internos de outro módulo.

Modelo conceitual:

```text
Module A
    │
    ▼
Public Contract
    │
    ▼
Module B
```

Não deve ocorrer:

```text
Module A
    │
    └──────────► Internal Implementation
                 of Module B
```

---

# 9. Dependências Entre Módulos

A relação inicial esperada é:

```text
Workflow Execution
        │
        ├──────────────► Workflow Definition
        │
        └──────────────► Rules
```

O Audit poderá receber informações sobre operações relevantes realizadas por outros módulos através de mecanismos apropriados.

Conceitualmente:

```text
Workflow Definition ────┐
                        │
Workflow Execution ─────┼────► Audit
                        │
Rules ──────────────────┘
```

A estratégia concreta de comunicação com o módulo Audit será definida posteriormente.

---

# 10. Direção das Dependências

As dependências devem seguir uma direção controlada.

Inicialmente:

```text
Interfaces
    │
    ▼
Application
    │
    ▼
Business Modules
    │
    ├── Workflow Definition
    │
    ├── Workflow Execution
    │       │
    │       ├────► Workflow Definition Contract
    │       │
    │       └────► Rules Contract
    │
    ├── Rules
    │
    └── Audit
    │
    ▼
Infrastructure
```

A infraestrutura implementa os detalhes necessários para persistência, integração e execução da aplicação.

---

# 11. Regras de Dependência

As seguintes regras devem ser respeitadas.

### MR-001 — Sem dependências circulares

Um módulo não pode depender, direta ou indiretamente, de forma circular, de outro módulo.

---

### MR-002 — Acesso por contratos

Quando um módulo precisar utilizar uma capacidade fornecida por outro módulo, deve utilizar um contrato público apropriado.

---

### MR-003 — Proteção da implementação interna

Classes e componentes internos de um módulo não devem ser utilizados diretamente por outros módulos.

---

### MR-004 — Responsabilidade clara

Uma responsabilidade não deve existir simultaneamente em múltiplos módulos sem uma justificativa explícita.

---

### MR-005 — Domínio protegido

Detalhes técnicos não devem determinar a estrutura dos módulos de negócio.

---

### MR-006 — Dependência mínima

Um módulo deve depender apenas das capacidades necessárias para cumprir sua responsabilidade.

---

# 12. Interfaces e Infrastructure

`Interfaces` e `Infrastructure` não representam módulos de negócio do domínio.

Elas representam áreas arquiteturais transversais.

## Interfaces

Responsável por:

* REST API;
* entrada de requisições;
* transformação de dados externos;
* respostas;
* integração com consumidores.

---

## Infrastructure

Responsável por:

* persistência;
* banco de dados;
* implementação de adapters;
* mensageria;
* cache;
* integração externa;
* observabilidade;
* configurações técnicas.

---

# 13. Shared Components

Componentes compartilhados devem ser tratados com cuidado.

Não será criado um módulo `shared` genérico apenas para concentrar:

* utilitários;
* constantes;
* classes sem localização definida;
* dependências comuns.

Um componente compartilhado somente deverá existir quando possuir uma responsabilidade claramente identificada.

O princípio adotado é:

> **Um módulo compartilhado não deve se tornar um local para responsabilidades indefinidas.**

---

# 14. Futuras Evoluções

A estrutura modular poderá evoluir conforme novas necessidades forem identificadas.

Possíveis evoluções incluem:

* Notification Module;
* Integration Module;
* Identity and Access Module;
* Reporting Module;
* Task Management Module.

A criação de novos módulos dependerá da existência de uma responsabilidade clara e de limites justificáveis.

---

# 15. Possível Extração Futura

Os módulos não são definidos com o objetivo de serem automaticamente transformados em Microservices.

Entretanto, a definição explícita dos limites poderá facilitar uma futura extração quando existir justificativa.

Uma possível evolução:

```text
Modular Monolith
       │
       ▼
Module Boundaries Established
       │
       ▼
Independent Scaling / Deployment Requirement
       │
       ▼
Module Extraction
```

A decisão de extrair um módulo deverá ser tomada com base em necessidades reais.

---

# 16. Relação com a Arquitetura

A organização modular definida neste documento complementa:

```text
docs/architecture/ARCHITECTURE.md
```

Enquanto o `ARCHITECTURE.md` define:

> **Como o sistema é organizado arquiteturalmente.**

Este documento define:

> **Como as responsabilidades de negócio são divididas internamente.**

---

# 17. Decisões Arquiteturais

A arquitetura Modular Monolith foi formalmente adotada através de:

```text
docs/adr/ADR-001.md
```

Novas decisões relacionadas à divisão ou reorganização significativa dos módulos deverão ser avaliadas conforme:

```text
docs/PROJECT_GOVERNANCE.md
```

Quando aplicável:

> ⚠️ **Esta decisão merece um ADR.**

---

# 18. Status

**Status:** Aprovado

Esta é a estrutura modular inicial do Enterprise Workflow Engine.

A implementação concreta dos módulos será definida posteriormente durante a estruturação do projeto.
