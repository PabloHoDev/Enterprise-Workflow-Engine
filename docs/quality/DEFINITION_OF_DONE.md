# Definition of Done

# Enterprise Workflow Engine

**Versão:** 0.1
**Status:** Aprovado

---

# 1. Objetivo

Critérios objetivos para considerar uma entrega concluída. Detalha o que `docs/PROJECT_GOVERNANCE.md`
estabelece nas seções 9 (Quality Gates), 10 (Definition of Ready) e 11 (Definition of Done).

> Nenhuma entrega é considerada concluída apenas pela implementação.

---

# 2. Definition of Ready

Uma atividade pode ser iniciada quando:

- [ ] o objetivo está definido;
- [ ] o escopo é conhecido, inclusive o que fica de fora;
- [ ] os requisitos, casos de uso e regras de negócio afetados estão identificados;
- [ ] as dependências foram avaliadas;
- [ ] os critérios de aceitação estão definidos;
- [ ] decisões que merecem ADR foram identificadas.

---

# 3. Definition of Done — Fase

Uma fase (unidade menor de desenvolvimento) está concluída quando:

**Implementação**

- [ ] atende ao requisito ou caso de uso correspondente;
- [ ] respeita as regras de negócio e as invariantes do domínio;
- [ ] trata os fluxos alternativos e de erro relevantes;
- [ ] segue `docs/quality/CODE_STANDARDS.md`.

**Testes**

- [ ] regras de negócio novas têm teste unitário de domínio;
- [ ] endpoints novos têm teste de integração (fluxo principal, alternativos e autorização);
- [ ] `./mvnw verify` passa, incluindo os testes de arquitetura;
- [ ] a suíte foi executada **com** os testes de integração, e não apenas com eles ignorados.

**Documentação**

- [ ] documentos afetados atualizados (API, modelo de dados, regras de negócio);
- [ ] decisões relevantes registradas em ADR ou em `DECISION_LOG.md`;
- [ ] `CHANGELOG.md` atualizado;
- [ ] pendências registradas em `TECHNICAL_DEBT.md` ou `TECHNICAL_BACKLOG.md`.

---

# 4. Definition of Done — Etapa

Uma etapa (entrega completa do roadmap) está concluída quando, além de todas as suas fases:

- [ ] o checking da etapa foi executado (`docs/quality/CHECKLISTS.md`);
- [ ] as correções apontadas foram aplicadas ou registradas como dívida técnica;
- [ ] a pipeline de CI está verde, incluindo o quality gate de cobertura e o build da imagem;
- [ ] não há violação conhecida dos requisitos já atendidos;
- [ ] o `ROADMAP.md` reflete o estado real;
- [ ] a etapa foi validada e aprovada.

---

# 5. Definition of Done — Release

Uma versão pode ser publicada quando:

- [ ] todas as etapas incluídas estão concluídas;
- [ ] a versão foi definida conforme `docs/releases/VERSIONING.md`;
- [ ] `CHANGELOG.md` e `RELEASE_NOTES.md` descrevem a versão;
- [ ] migrations de banco foram verificadas a partir da versão anterior;
- [ ] a imagem Docker foi construída a partir da tag da versão.

---

# 6. Quality Gates Automatizados

| Gate                                  | Onde               | Falha quando                               |
| ------------------------------------- | ------------------ | ------------------------------------------ |
| Compilação                            | `./mvnw verify`    | o código não compila                       |
| Testes unitários e de integração      | `./mvnw verify`    | qualquer teste falha                       |
| Arquitetura                           | `ArchitectureTest` | um limite de módulo ou camada é violado    |
| Schema × entidades                    | inicialização      | o schema diverge do mapeamento JPA         |
| Cobertura de linhas ≥ 80%             | CI                 | a cobertura fica abaixo do mínimo          |
| Imagem Docker                         | CI                 | a imagem não é construída                  |

Os demais critérios dependem de revisão.

---

# 7. Fluxo

```text
Planejamento → Implementação → Review Técnica → Checking → Correções → Validação → Aprovação
```

Uma entrega no estado "implementada, aguardando validação" aparece como 🟡 no `ROADMAP.md`; só passa a 🟢
depois da aprovação.
