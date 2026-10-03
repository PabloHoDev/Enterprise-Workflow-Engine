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
- **Usar pelo navegador.** Um console web cobre todo o ciclo: modelar e ativar processos, abrir
  solicitações, decidir, acompanhar o histórico, auditar e administrar usuários.
- **Confiar no acesso.** Login com sessão segura, bloqueio após tentativas erradas, troca de senha e
  revogação imediata de acesso.

### Como começar

Veja o `README.md` na raiz do repositório para subir o ambiente local e executar o exemplo de aprovação
de compras. A referência da API está em `docs/architecture/API_DESIGN.md` e, com a aplicação no ar, em
`/swagger-ui.html`.

### Requisitos

- Java 25 e PostgreSQL 18, ou Docker.

### Limitações conhecidas

- Sem MFA nem login único (SSO); a integração com um provedor de identidade está planejada (TD-001).
- O limite de falhas de login por IP vale por instância; em produção, aplique também limite no gateway
  (TD-009).
- Não há transições automáticas nem notificações; toda mudança de State parte de uma ação de um usuário.

A lista completa está em `docs/releases/TECHNICAL_DEBT.md`.

### Compatibilidade

Primeira versão: não há migração a partir de versões anteriores. Enquanto a versão for `0.x`, a API pode
sofrer mudanças incompatíveis entre versões `MINOR` (`docs/releases/VERSIONING.md`).
