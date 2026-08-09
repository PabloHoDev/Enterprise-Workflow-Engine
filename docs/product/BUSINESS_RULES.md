# Business Rules

# Enterprise Workflow Engine

**Versão:** 0.1
**Status:** 🟢 APROVADA

---

# 1. Objetivo

Este documento define as principais regras de negócio que governam o comportamento do **Enterprise Workflow Engine**.

As regras aqui descritas representam condições, restrições e invariantes que devem ser respeitadas independentemente da interface ou tecnologia utilizada para executar uma operação.

Este documento complementa:

```text
docs/product/DOMAIN.md
docs/product/REQUIREMENTS.md
docs/product/USE_CASES.md
```

Não é objetivo deste documento definir detalhes de implementação.

---

# 2. Princípios

As regras do domínio devem garantir:

* consistência das execuções;
* integridade das definições;
* previsibilidade das transições;
* rastreabilidade;
* versionamento;
* respeito às permissões;
* preservação das invariantes do domínio.

---

# 3. Regras de Workflow Definition

## BR-001 — Identificação única

Cada Workflow Definition deve possuir uma identificação única dentro do sistema.

---

## BR-002 — Definição válida

Uma Workflow Definition somente poderá ser utilizada quando possuir estrutura válida para execução.

---

## BR-003 — Versões independentes

Cada versão de uma Workflow Definition representa uma definição específica do processo.

Uma versão não deve alterar o comportamento de outra versão.

---

## BR-004 — Integridade da definição

Uma versão não poderá ser ativada enquanto possuir elementos obrigatórios inválidos ou incompletos.

---

# 4. Regras de Versionamento

## BR-005 — Versionamento obrigatório

Toda execução deve estar associada a uma versão específica de uma Workflow Definition.

---

## BR-006 — Imutabilidade da versão utilizada

Uma versão de Workflow Definition que já esteja sendo utilizada por um Workflow não poderá ser alterada de forma que modifique o comportamento daquela execução.

---

## BR-007 — Nova versão para alterações relevantes

Alterações que modifiquem o comportamento de uma Workflow Definition devem resultar em uma nova versão.

---

## BR-008 — Histórico de versões

Versões anteriores devem permanecer identificáveis e rastreáveis.

---

# 5. Regras de Workflow

## BR-009 — Definição obrigatória

Todo Workflow deve estar associado a uma Workflow Definition válida.

---

## BR-010 — Versão determinada na criação

Ao ser criado, o Workflow deve possuir uma referência à versão da Workflow Definition utilizada.

---

## BR-011 — Estado inicial

Todo Workflow deve iniciar em um State válido definido pela sua Workflow Definition.

---

## BR-012 — Estado atual único

Um Workflow deve possuir um único State atual em determinado momento da execução.

---

## BR-013 — Integridade da execução

Uma operação não pode deixar o Workflow em um estado parcialmente atualizado ou inconsistente.

---

# 6. Regras de State

## BR-014 — State válido

Um Workflow somente pode assumir States definidos pela sua Workflow Definition.

---

## BR-015 — State atual

O State atual deve representar o estágio efetivo da execução do Workflow.

---

## BR-016 — Estados terminais

Estados definidos como terminais não devem permitir novas Transitions incompatíveis com o ciclo de vida do Workflow.

---

# 7. Regras de Transition

## BR-017 — Transition definida

Uma Transition somente pode ser executada quando estiver definida para a Workflow Definition associada ao Workflow.

---

## BR-018 — Origem válida

Uma Transition somente pode ser executada quando o Workflow estiver no State de origem correspondente.

---

## BR-019 — Destino válido

Uma Transition deve possuir um State de destino válido.

---

## BR-020 — Transition autorizada

Uma Transition que exija ação de um Actor somente poderá ser executada quando o Actor estiver autorizado.

---

## BR-021 — Rules satisfeitas

Quando uma Transition possuir Rules associadas, todas as condições obrigatórias devem ser satisfeitas antes de sua efetivação.

---

## BR-022 — Atomicidade conceitual

Uma Transition deve ser tratada como uma operação única do ponto de vista do domínio:

```text
State atual
     ↓
Validações
     ↓
Transition
     ↓
Novo State
```

Não deve existir um estado intermediário considerado válido pelo domínio.

---

# 8. Regras de Rule

## BR-023 — Aplicabilidade

Uma Rule somente deve ser avaliada quando estiver associada ao contexto da operação correspondente.

---

## BR-024 — Resultado determinístico

Para um mesmo contexto e conjunto de dados, uma Rule deve produzir resultado consistente.

---

## BR-025 — Falha de Rule

Quando uma Rule obrigatória não for satisfeita, a operação dependente dela deve ser rejeitada.

---

# 9. Regras de Actor

## BR-026 — Identificação do Actor

Ações relevantes executadas por usuários ou sistemas devem possuir um Actor identificável.

---

## BR-027 — Autorização

A existência de um Actor autenticado não implica automaticamente autorização para executar determinada operação.

---

## BR-028 — Responsabilidade

Quando uma operação exigir um Actor específico, a identidade do responsável deve ser preservada para fins de rastreabilidade.

---

# 10. Regras de History

## BR-029 — Registro de mudanças relevantes

Mudanças relevantes no ciclo de vida de um Workflow devem ser registradas no History.

---

## BR-030 — Ordem cronológica

Os registros de History devem permitir determinar a sequência das mudanças ocorridas.

---

## BR-031 — Rastreabilidade

O histórico deve permitir identificar, quando aplicável:

* estado anterior;
* ação realizada;
* novo estado;
* Actor;
* momento da operação.

---

# 11. Regras de Audit

## BR-032 — Operações auditáveis

Operações relevantes para segurança, administração ou conformidade devem gerar registros de Audit.

---

## BR-033 — Integridade da auditoria

Registros de Audit devem preservar informações suficientes para identificar:

* Actor;
* operação;
* recurso afetado;
* momento da operação;
* resultado.

---

## BR-034 — Separação entre History e Audit

History e Audit possuem objetivos diferentes.

```text
History
→ evolução do Workflow

Audit
→ rastreabilidade das operações
```

Um registro de History não substitui automaticamente um registro de Audit.

---

# 12. Regras de Cancelamento

## BR-035 — Cancelamento permitido

Um Workflow somente poderá ser cancelado quando seu State atual permitir essa operação.

---

## BR-036 — Cancelamento autorizado

O Actor responsável pelo cancelamento deve possuir autorização para executar a operação.

---

## BR-037 — Cancelamento rastreável

O cancelamento deve gerar registro no History e, quando aplicável, no Audit.

---

# 13. Regras de Estados Terminais

## BR-038 — Finalização

Um Workflow que alcançar um State terminal deve ser considerado encerrado.

---

## BR-039 — Bloqueio de novas Transitions

Um Workflow em State terminal não poderá executar Transitions incompatíveis com seu ciclo de vida.

---

## BR-040 — Rastreabilidade do encerramento

A entrada em um State terminal deve ser registrada no History.

---

# 14. Invariantes do Domínio

As seguintes condições devem permanecer verdadeiras durante toda a vida de um Workflow:

```text
1. Todo Workflow possui uma Workflow Definition.
2. Todo Workflow possui uma versão específica da definição.
3. Todo Workflow possui um State atual válido.
4. Toda Transition executada pertence à definição utilizada.
5. Toda Transition parte do State atual.
6. Toda Transition possui destino válido.
7. Rules obrigatórias devem ser satisfeitas.
8. Estados terminais não permitem operações incompatíveis.
9. Mudanças relevantes são rastreáveis.
```

---

# 15. Evolução das Regras

As regras definidas neste documento representam a primeira versão do modelo de negócio.

Novas regras poderão surgir durante:

* detalhamento dos casos de uso;
* definição da arquitetura;
* implementação;
* testes;
* descoberta de novos cenários de negócio.

Uma nova regra deve:

1. possuir identificação própria;
2. possuir descrição clara;
3. possuir justificativa;
4. ser avaliada quanto ao impacto no domínio;
5. ser refletida no roadmap quando alterar o escopo.

---

# 16. Decisões que Podem Exigir ADR

Nem toda regra de negócio exige um ADR.

Entretanto, quando uma regra representar uma decisão relevante com impacto significativo no domínio ou na arquitetura, ela deverá ser avaliada para registro em `docs/adr/`.

Exemplo:

> ⚠️ **Esta decisão merece um ADR.**

A decisão deve então ser registrada antes de sua implementação quando houver risco relevante de divergência ou impacto futuro.

---

# 17. Relação com Outros Documentos

Este documento deve permanecer consistente com:

```text
docs/product/VISION.md
docs/product/DOMAIN.md
docs/product/REQUIREMENTS.md
docs/product/USE_CASES.md
docs/PROJECT_GOVERNANCE.md
```

As regras aqui definidas servem como referência para futuras decisões de arquitetura e implementação.

---

# 18. Status do Documento

**Status:** 🟢 APROVADA

Este documento representa a primeira versão das regras de negócio do Enterprise Workflow Engine.
