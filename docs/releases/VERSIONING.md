# Versioning

# Enterprise Workflow Engine

**Versão:** 0.1
**Status:** Aprovado

---

# 1. Objetivo

Define como o **Enterprise Workflow Engine** é versionado e como uma release é produzida
(`docs/PROJECT_GOVERNANCE.md` §15).

---

# 2. Versionamento Semântico

```text
MAJOR.MINOR.PATCH
```

| Parte   | Incrementa quando                                                              |
| ------- | ------------------------------------------------------------------------------ |
| `MAJOR` | há mudança incompatível para quem consome a API ou opera a aplicação           |
| `MINOR` | há funcionalidade nova, compatível com a versão anterior                       |
| `PATCH` | há apenas correções compatíveis                                                |

Enquanto a versão for `0.x`, o produto está em desenvolvimento inicial: mudanças incompatíveis podem
ocorrer em uma `MINOR`. A versão `1.0.0` marca o primeiro contrato estável (Milestone 5).

Entre releases, o `pom.xml` usa o sufixo `-SNAPSHOT` (`0.1.0-SNAPSHOT`).

---

# 3. O que é Mudança Incompatível

- remover ou renomear endpoint, campo de resposta ou valor de enumeração;
- tornar obrigatório um campo de requisição antes opcional;
- alterar o significado de um status HTTP ou de um campo;
- alterar regra de negócio de modo que uma requisição antes aceita passe a ser recusada;
- exigir nova configuração obrigatória para a aplicação iniciar;
- migration que não possa ser aplicada sobre o banco da versão anterior.

Acrescentar endpoint, campo opcional de requisição ou campo de resposta é compatível.

---

# 4. O que é Versionado Separadamente

| Elemento                 | Versão                          | Regra                                                         |
| ------------------------ | ------------------------------- | ------------------------------------------------------------- |
| Aplicação                | `MAJOR.MINOR.PATCH`             | este documento                                                |
| API REST                 | `/api/v1`, `/api/v2`…           | o prefixo só muda em mudança incompatível da API              |
| Schema do banco          | `V1`, `V2`… (Flyway)            | sequencial; uma migration aplicada nunca é editada            |
| Workflow Definitions     | `1`, `2`… por definição         | regra de negócio (BR-005 a BR-008), gerida pelos usuários     |
| Documentos               | `Versão` no cabeçalho           | incrementa em revisão relevante do conteúdo                   |

Uma nova versão da aplicação não altera as versões das Workflow Definitions existentes, nem o comportamento
dos Workflows em execução.

---

# 5. Processo de Release

1. Confirmar que as etapas incluídas atendem ao `DEFINITION_OF_DONE.md`.
2. Definir o número da versão conforme as seções 2 e 3.
3. Em `CHANGELOG.md`, converter "Não lançado" na nova versão, com a data.
4. Escrever a entrada correspondente em `RELEASE_NOTES.md`.
5. Remover `-SNAPSHOT` do `pom.xml` e fazer o commit da release.
6. Criar a tag `vMAJOR.MINOR.PATCH`.
7. Construir a imagem Docker a partir da tag.
8. Avançar o `pom.xml` para a próxima versão `-SNAPSHOT`.

O roteiro de verificação está em `docs/quality/CHECKLISTS.md` (§5).

---

# 6. Branches e Commits

- `main` contém sempre código que compila e passa nos testes.
- Mudanças chegam por pull request, validadas pela pipeline de CI.
- Correções de uma versão já publicada partem da tag correspondente e geram uma `PATCH`.
- Mensagens de commit descrevem a mudança e seu motivo, não o arquivo alterado.

---

# 7. Documentos Relacionados

```text
docs/releases/CHANGELOG.md
docs/releases/RELEASE_NOTES.md
docs/quality/DEFINITION_OF_DONE.md
```
