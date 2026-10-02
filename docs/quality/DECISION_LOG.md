# Decision Log

# Enterprise Workflow Engine

**Versão:** 0.1
**Status:** Em evolução

---

# 1. Objetivo

Registro cronológico de decisões menores tomadas durante o desenvolvimento: escolhas que não justificam um
ADR, mas cujo motivo não é evidente no código e seria perdido de outra forma.

Decisões estruturais ficam em `docs/adr/`; o resumo das decisões em vigor, em
`docs/architecture/DECISIONS.md`.

---

# 2. Registro

| ID     | Data       | Decisão                                                                                       | Motivo |
| ------ | ---------- | --------------------------------------------------------------------------------------------- | ------ |
| DL-001 | 2026-10-02 | ADRs renomeados para `ADR-NNN.md` e `PROJECT_GOVERNANCE.md` movido para `docs/`               | Todos os documentos já referenciavam esses caminhos; os arquivos estavam em outros nomes e locais. |
| DL-002 | 2026-10-02 | Maven Wrapper no modo `only-script`                                                           | Evita versionar um binário (`maven-wrapper.jar`); os scripts anteriores estavam vazios. |
| DL-003 | 2026-10-02 | `java.version` alterado de 21 para 25                                                         | Alinhamento com a stack consolidada (ADR-003). |
| DL-004 | 2026-10-02 | Erros da API no formato Problem Details (RFC 9457)                                            | Padrão suportado nativamente pelo Spring; evita um formato de erro próprio. |
| DL-005 | 2026-10-02 | Mapeamento de status: `409` para conflito com o estado atual, `422` para recusa por regra     | Distingue "tente de novo após reler" de "esta requisição não será aceita como está". |
| DL-006 | 2026-10-02 | Actor sem o papel exigido pela Transition retorna `403`                                       | É uma negação de autorização, ainda que decidida pelo domínio. |
| DL-007 | 2026-10-02 | Mensagens da API em inglês, com locale fixo                                                   | As mensagens de validação variavam conforme o locale do servidor. |
| DL-008 | 2026-10-02 | Relógio da aplicação com resolução de microssegundos                                          | Igual à do `timestamptz`: o instante devolvido numa escrita é idêntico ao lido depois. |
| DL-009 | 2026-10-02 | Variável ausente não satisfaz nenhuma Rule, inclusive `NOT_EQUALS`                            | Ausência de dado não é evidência; evita aprovação por omissão. |
| DL-010 | 2026-10-02 | Comparação numérica sempre que ambos os lados são números                                     | `10000.00` e `10000` devem ser iguais; variáveis JSON chegam como inteiro, decimal ou texto. |
| DL-011 | 2026-10-02 | `GET /workflows/{id}/actions` não avalia Rules                                                | Lista o que o processo permite a partir do State; avaliar exigiria as variáveis que só chegam com a ação. |
| DL-012 | 2026-10-02 | `ADMIN` não substitui o papel exigido por uma Transition                                      | Administrar a plataforma e decidir dentro de um processo são responsabilidades distintas. |
| DL-013 | 2026-10-02 | Auditoria de sucesso na mesma transação; de recusa, em transação própria                      | Sucesso só deve existir se a operação for efetivada; a recusa precisa sobreviver ao rollback. |
| DL-014 | 2026-10-02 | Colunas `definition_key` e `definition_version_number` duplicadas em `workflow`               | Permitem listar e filtrar sem join; são imutáveis, então não divergem. |
| DL-015 | 2026-10-02 | `audit_record` sem chaves estrangeiras                                                        | O módulo Audit não depende do modelo dos demais; o registro sobrevive ao recurso. |
| DL-016 | 2026-10-02 | Testes de integração são ignorados, e não falham, sem Docker nem banco externo                | Permite build local em máquina sem Docker; o quality gate de cobertura impede que isso passe despercebido em CI. |
| DL-017 | 2026-10-02 | Quality gate de cobertura em profile Maven ativado por `CI=true`                              | Consequência de DL-016: sem os testes de integração, a cobertura local não é representativa. |
| DL-018 | 2026-10-02 | ArchUnit usado como biblioteca em testes JUnit comuns, sem o engine `archunit-junit5`         | Evita depender da compatibilidade do engine com a versão do JUnit Platform. |
| DL-019 | 2026-10-02 | springdoc desabilitado no perfil `prod`                                                       | A documentação da API não deve ficar exposta publicamente por padrão. |
| DL-020 | 2026-10-02 | Colunas `ordinal`, `state_type`, `rule_operator`, `sequence_number`, `comment_text`           | Evitam palavras-chave do SQL (`position`, `type`, `operator`, `sequence`, `comment`) como nomes de coluna. |

---

# 3. Como Registrar

1. Acrescente uma linha ao final, com o próximo identificador `DL-NNN` e a data.
2. Escreva a decisão de forma afirmativa e o motivo em uma frase.
3. Se a decisão crescer — alternativas relevantes, impacto estrutural —, promova-a a ADR e referencie-o aqui.
4. Uma decisão revertida não é apagada: registre a nova decisão citando a anterior.
