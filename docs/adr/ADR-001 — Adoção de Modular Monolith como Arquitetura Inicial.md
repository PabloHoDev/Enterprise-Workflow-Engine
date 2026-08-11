# ADR-001 — Adoção de Modular Monolith como Arquitetura Inicial

* **Status:** Accepted
* **Date:** 2026-08-11
* **Decision Type:** Architecture
* **Related:** `docs/product/DOMAIN.md`, `docs/product/REQUIREMENTS.md`, `docs/product/USE_CASES.md`, `docs/product/BUSINESS_RULES.md`

---

## 1. Context

O Enterprise Workflow Engine tem como objetivo demonstrar capacidade de projetar e desenvolver um sistema backend corporativo utilizando Java, com foco em:

* arquitetura de software;
* domínio de negócio;
* APIs REST;
* persistência;
* segurança;
* testes;
* observabilidade;
* mensageria;
* escalabilidade;
* práticas de engenharia utilizadas em ambientes enterprise.

O sistema possui diferentes responsabilidades relacionadas ao gerenciamento e execução de workflows, incluindo:

* definição de workflows;
* versionamento;
* execução;
* transições;
* regras;
* atores;
* histórico;
* auditoria.

A arquitetura precisa permitir evolução futura sem criar complexidade distribuída antes que ela seja necessária.

---

## 2. Problem

Precisamos definir o estilo arquitetural inicial do sistema.

As principais alternativas consideradas são:

1. monólito tradicional;
2. microservices;
3. modular monolith.

A escolha deve considerar:

* complexidade do domínio;
* facilidade de desenvolvimento;
* isolamento de responsabilidades;
* testabilidade;
* possibilidade de evolução;
* custo operacional;
* potencial de futura distribuição dos componentes.

---

## 3. Decision

Será adotado um **Modular Monolith** como arquitetura inicial do Enterprise Workflow Engine.

O sistema será executado inicialmente como uma única aplicação, porém organizado internamente em módulos com responsabilidades e limites bem definidos.

A arquitetura também seguirá princípios de **Clean Architecture**, mantendo o núcleo de domínio independente de frameworks e detalhes de infraestrutura.

Conceitualmente:

```text
Enterprise Workflow Engine
│
├── Domain
│
├── Application
│
├── Modules
│   ├── Workflow Definition
│   ├── Workflow Execution
│   ├── Rules
│   └── Audit
│
├── Infrastructure
│
└── Interfaces
    └── REST API
```

A estrutura exata dos módulos será definida posteriormente em:

```text
docs/architecture/MODULES.md
```

---

## 4. Architectural Principles

A decisão implica os seguintes princípios:

### 4.1 Domain Independence

O domínio não deve depender diretamente de:

* Spring;
* JPA;
* banco de dados;
* HTTP;
* mensageria;
* infraestrutura externa.

---

### 4.2 Module Boundaries

Cada módulo deverá possuir responsabilidades claramente definidas.

Um módulo não deve acessar diretamente detalhes internos de outro módulo sem passar por uma interface ou contrato apropriado.

---

### 4.3 Dependency Direction

As dependências devem apontar para as regras de negócio e abstrações, evitando que o domínio dependa de detalhes externos.

Conceitualmente:

```text
Infrastructure
      ↓
Application
      ↓
Domain
```

As dependências concretas de infraestrutura devem ser conectadas através de abstrações definidas em camadas apropriadas.

---

### 4.4 Single Deployable Unit

Inicialmente, os módulos serão executados como uma única aplicação.

Não haverá necessidade de:

* múltiplos deployments;
* service discovery;
* comunicação de rede entre módulos;
* infraestrutura distribuída.

---

### 4.5 Evolutionary Architecture

A arquitetura deve permitir que determinados módulos sejam posteriormente extraídos para serviços independentes caso exista uma justificativa técnica ou de negócio.

A possibilidade de extração futura não significa que a distribuição será antecipada.

---

## 5. Alternatives Considered

### 5.1 Traditional Monolith

#### Vantagens

* menor complexidade inicial;
* desenvolvimento simples;
* deployment simples.

#### Desvantagens

* limites internos podem ficar pouco definidos;
* maior risco de acoplamento;
* menor demonstração de organização arquitetural;
* evolução pode se tornar mais difícil conforme o sistema cresce.

**Decisão:** Rejeitado.

---

### 5.2 Microservices

#### Vantagens

* independência de deployment;
* possibilidade de escalar serviços individualmente;
* isolamento operacional;
* adequado para determinados cenários distribuídos.

#### Desvantagens

* complexidade operacional significativamente maior;
* comunicação distribuída;
* consistência distribuída;
* observabilidade mais complexa;
* necessidade de infraestrutura adicional;
* maior custo de desenvolvimento e manutenção.

Para o estágio atual do projeto, essa complexidade não é justificada.

**Decisão:** Rejeitado como arquitetura inicial.

---

### 5.3 Modular Monolith

#### Vantagens

* baixo custo operacional inicial;
* deployment simples;
* limites arquiteturais explícitos;
* possibilidade de evolução incremental;
* facilidade de testes;
* menor complexidade distribuída;
* possibilidade de futura extração de módulos.

#### Desvantagens

* exige disciplina arquitetural;
* módulos podem ser acoplados indevidamente se os limites não forem respeitados;
* extração futura para microservices não é automática.

**Decisão:** Adotado.

---

## 6. Consequences

### 6.1 Positive Consequences

A decisão proporciona:

* arquitetura organizada desde o início;
* domínio desacoplado;
* menor complexidade operacional;
* facilidade de desenvolvimento local;
* facilidade de testes;
* possibilidade de evolução incremental;
* possibilidade de futura distribuição;
* melhor demonstração de maturidade arquitetural.

---

### 6.2 Negative Consequences

A decisão também exige:

* disciplina para preservar limites entre módulos;
* definição clara das responsabilidades;
* testes arquiteturais quando necessário;
* atenção para evitar dependências indevidas;
* documentação das decisões relevantes.

---

## 7. Future Evolution

A adoção de Modular Monolith não impede uma futura evolução para uma arquitetura distribuída.

A extração de módulos para serviços independentes somente deverá ocorrer quando houver justificativa baseada em fatores como:

* necessidade de escala independente;
* isolamento operacional;
* requisitos de disponibilidade;
* autonomia de deployment;
* volume de processamento;
* integração externa;
* necessidade de diferentes ciclos de evolução.

A evolução para Microservices não será considerada um objetivo obrigatório do projeto.

---

## 8. Rejected Principle

Não será adotado o princípio:

> "Microservices porque o sistema é enterprise."

A arquitetura será orientada pelas necessidades do sistema, e não pela complexidade aparente da tecnologia.

---

## 9. Related Documents

Esta decisão influencia principalmente:

```text
docs/architecture/ARCHITECTURE.md
docs/architecture/MODULES.md
docs/architecture/DEPLOYMENT.md
docs/architecture/DATA_MODEL.md
```

Também está relacionada aos documentos de produto:

```text
docs/product/DOMAIN.md
docs/product/REQUIREMENTS.md
docs/product/USE_CASES.md
docs/product/BUSINESS_RULES.md
```

---

## 10. Status

**Accepted**

Esta decisão estabelece o estilo arquitetural inicial do Enterprise Workflow Engine.

Alterações futuras que representem mudança significativa dessa decisão deverão ser registradas através de um novo ADR ou da atualização formal deste ADR, conforme as regras de governança do projeto.
