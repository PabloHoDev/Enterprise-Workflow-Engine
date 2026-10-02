# Release Notes

Notas de versão do **Enterprise Workflow Engine**, escritas para quem usa ou opera a aplicação.

A lista detalhada de mudanças está em `docs/releases/CHANGELOG.md`.

---

## 0.1.0 — em preparação

**Status:** implementada, aguardando validação e publicação (`docs/product/ROADMAP.md`, FASE 14).

Primeira versão funcional: é possível modelar um processo de aprovação, executá-lo de ponta a ponta pela
API e auditar tudo o que aconteceu.

### O que você pode fazer

- **Modelar processos.** Defina States, Transitions, quem pode executar cada ação e sob quais condições.
- **Evoluir processos com segurança.** Cada alteração é uma nova versão; execuções em andamento continuam
  na versão em que começaram.
- **Executar workflows.** Crie, inicie, avance por ações e cancele execuções. O motor recusa qualquer
  ação que o processo não permita.
- **Aplicar regras.** Condicione uma Transition a dados da execução, como o valor de uma compra.
- **Rastrear.** Consulte o histórico de cada workflow e a trilha de auditoria, que inclui as tentativas
  recusadas.

### Como começar

Veja o `README.md` na raiz do repositório para subir o ambiente local e executar o exemplo de aprovação
de compras. A referência da API está em `docs/architecture/API_DESIGN.md` e, com a aplicação no ar, em
`/swagger-ui.html`.

### Requisitos

- Java 25 e PostgreSQL 18, ou Docker.

### Limitações conhecidas

- Os usuários são definidos por configuração e autenticados por HTTP Basic: use TLS e trate esta versão
  como não destinada a exposição pública (TD-001, TD-003).
- A imagem Docker e a pipeline de CI ainda não foram executadas (TD-006).
- Não há transições automáticas nem notificações; toda mudança de State parte de uma chamada à API.

A lista completa está em `docs/releases/TECHNICAL_DEBT.md`.

### Compatibilidade

Primeira versão: não há migração a partir de versões anteriores. Enquanto a versão for `0.x`, a API pode
sofrer mudanças incompatíveis entre versões `MINOR` (`docs/releases/VERSIONING.md`).
