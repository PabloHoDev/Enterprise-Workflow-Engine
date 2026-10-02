# Checklists

# Enterprise Workflow Engine

**Versão:** 0.1
**Status:** Aprovado

---

# 1. Objetivo

Checklists usados no **checking** das entregas (`docs/PROJECT_GOVERNANCE.md` §6 e §8). Os critérios de
conclusão estão em `docs/quality/DEFINITION_OF_DONE.md`; estes checklists são o roteiro para verificá-los.

Para registrar o checking de uma etapa, copie `docs/templates/CHECKLIST_TEMPLATE.md`.

---

# 2. Validação Técnica

## 2.1 Domínio

- [ ] A regra está no aggregate ou value object que ela protege, não no service nem no controller.
- [ ] A operação valida tudo antes de alterar o estado.
- [ ] Toda mudança relevante de State gera entrada de History.
- [ ] Erros de negócio usam exceções do módulo, com mensagem compreensível para o consumidor da API.
- [ ] Nenhuma dependência de framework foi introduzida no domínio.

## 2.2 Arquitetura

- [ ] O código está no módulo e na camada corretos.
- [ ] Outros módulos são acessados apenas pelo pacote `contract`.
- [ ] `ArchitectureTest` passa sem regra afrouxada.
- [ ] Um módulo novo foi incluído em `ArchitectureTest`.
- [ ] Nenhuma tecnologia nova foi introduzida sem justificativa (AC-007).

## 2.3 Persistência

- [ ] Alteração de schema feita por nova migration; nenhuma migration aplicada foi editada.
- [ ] Constraints nomeadas e integridade garantida no banco quando possível.
- [ ] Consultas novas possuem índice adequado.
- [ ] A migration foi executada sobre um banco com dados da versão anterior.

## 2.4 API

- [ ] URL, verbo e status seguem `docs/architecture/API_DESIGN.md`.
- [ ] Entradas validadas; erros em Problem Details.
- [ ] Nenhuma entidade de domínio ou detalhe interno exposto.
- [ ] `docs/architecture/API_DESIGN.md` atualizado.

## 2.5 Segurança

- [ ] O endpoint novo tem regra de autorização explícita e teste que a verifica.
- [ ] Operações que alteram estado identificam o Actor e geram Audit.
- [ ] Nenhum segredo no código, em arquivos versionados ou em logs.
- [ ] Entradas externas que chegam a logs ou consultas são tratadas.

## 2.6 Testes

- [ ] Regras novas com teste unitário; endpoints novos com teste de integração.
- [ ] Fluxos alternativos e de erro cobertos, não apenas o fluxo principal.
- [ ] Suíte executada com os testes de integração ativos (Docker ou `EWE_TEST_DB_URL`).
- [ ] Cobertura acima do mínimo.

---

# 3. Validação Documental

- [ ] Requisitos, casos de uso e regras afetados atualizados.
- [ ] Decisões relevantes registradas em ADR; decisões menores em `DECISION_LOG.md`.
- [ ] `CHANGELOG.md` atualizado.
- [ ] `ROADMAP.md` reflete o estado real.
- [ ] Pendências registradas em `TECHNICAL_DEBT.md` ou `TECHNICAL_BACKLOG.md`.
- [ ] Referências entre documentos apontam para arquivos existentes.

---

# 4. Validação Final

- [ ] Critérios de aceitação da etapa atendidos.
- [ ] Pipeline de CI verde.
- [ ] Correções do checking aplicadas ou registradas.
- [ ] Etapa aprovada e roadmap atualizado para 🟢.

---

# 5. Checklist de Release

- [ ] Versão definida conforme `docs/releases/VERSIONING.md`.
- [ ] `pom.xml` com a versão final, sem `-SNAPSHOT`.
- [ ] `CHANGELOG.md`: seção "Não lançado" convertida na versão, com data.
- [ ] `RELEASE_NOTES.md` escrito para quem usa a API.
- [ ] Tag `vMAJOR.MINOR.PATCH` criada.
- [ ] Imagem Docker construída a partir da tag.
- [ ] `pom.xml` avançado para a próxima versão `-SNAPSHOT`.

---

# 6. Checklist de Revisão de Código

- [ ] O objetivo da mudança está claro e a mudança se limita a ele.
- [ ] Nomes dizem o que as coisas são.
- [ ] Não há código morto, comentado ou duplicado.
- [ ] Comentários explicam o porquê, não o quê.
- [ ] Erros são tratados no nível certo, sem engolir exceções.
- [ ] Concorrência considerada onde há estado compartilhado.
- [ ] Os testes falhariam se a mudança fosse revertida.
