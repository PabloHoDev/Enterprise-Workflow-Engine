# Architecture Decision Records

Decisões relevantes de arquitetura, tecnologia e domínio do **Enterprise Workflow Engine**, conforme
`docs/PROJECT_GOVERNANCE.md` (§12).

## Índice

| ADR                   | Título                                                       | Status   | Data       |
| --------------------- | ------------------------------------------------------------ | -------- | ---------- |
| [ADR-000](ADR-000.md) | PROJECT_GOVERNANCE como documento fundador                   | Accepted | 2026-08-07 |
| [ADR-001](ADR-001.md) | Modular Monolith como arquitetura inicial                    | Accepted | 2026-08-11 |
| [ADR-002](ADR-002.md) | Estrutura de pacotes: módulo primeiro, camadas dentro        | Accepted | 2026-10-02 |
| [ADR-003](ADR-003.md) | Stack tecnológica                                            | Accepted | 2026-10-02 |
| [ADR-004](ADR-004.md) | Estratégia de persistência e consistência                    | Accepted | 2026-10-02 |
| [ADR-005](ADR-005.md) | Autenticação HTTP Basic com usuários configurados externamente | Accepted | 2026-10-02 |
| [ADR-006](ADR-006.md) | Decisões de modelagem do domínio na primeira implementação   | Accepted | 2026-10-02 |

## Quando escrever um ADR

Quando a decisão:

- escolhe ou troca uma tecnologia;
- altera a estrutura dos módulos ou a direção das dependências;
- fecha um ponto que os documentos de produto deixaram em aberto;
- seria difícil ou cara de reverter.

Decisões menores e do dia a dia vão para `docs/quality/DECISION_LOG.md`.

## Como escrever

1. Copie `docs/templates/ADR_TEMPLATE.md` para `docs/adr/ADR-NNN.md`, usando o próximo número.
2. Preencha contexto, decisão, alternativas e consequências.
3. Acrescente a linha correspondente no índice acima.

## Ciclo de vida

`Proposed` → `Accepted` → (`Deprecated` | `Superseded by ADR-NNN`)

Um ADR aceito não é reescrito: uma mudança de decisão gera um novo ADR que substitui o anterior.
