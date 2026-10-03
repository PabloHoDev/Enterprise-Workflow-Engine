# Front-end — Status e Plano de Ação

> Documento vivo. Atualize ao final de cada etapa.
> O front só começa quando o portão do backend (`docs/backend-status.md`) estiver fechado.
> Última análise: 2026-10-03 — commit `5457eff`.

## 1. ✅ O que já está OK

### Situação atual
- [x] **Não existe front-end no repositório** (nenhum `package.json`, componente ou página).
- [x] Isso é coerente com o escopo aprovado: "frontend completo" está fora dos requisitos iniciais
      (`docs/product/REQUIREMENTS.md` §8 e `USE_CASES.md` §18).
- [x] A interface disponível hoje é o **Swagger UI** (`/swagger-ui.html`), que permite exercitar toda a API.

## 2. ⏳ O que está pendente

### 2.1 🚧 Bloqueios (decisões do usuário)
- [ ] O projeto terá front-end? Opções:
  1. **Manter só API** (Swagger como interface) — alinhado ao escopo atual.
  2. **Console administrativo enxuto** — definições, workflows, ações, histórico e auditoria.
  3. **Aplicação completa** — inclui editor visual de workflows (hoje explicitamente fora do escopo).
- [ ] Se sim: stack (sugestão: Next.js + React + Tailwind), repositório (mesmo ou separado) e identidade visual.
- [ ] Mudança de escopo exige atualizar `REQUIREMENTS.md`, `ROADMAP.md` e registrar ADR.

### 2.2 🎨 Design system
- [ ] Não iniciado.
### 2.3 🧭 Rotas e navegação
- [ ] Não iniciado.
### 2.4 📄 Telas
- [ ] Não iniciado.
### 2.5 🧪 Qualidade
- [ ] Não iniciado.
### 2.6 🚀 Produção
- [ ] Não iniciado. Pré-requisito do backend: CORS e autenticação adequada a navegador (HTTP Basic não serve; depende de OAuth2/OIDC).

## 3. 🗺️ Plano de ação (se a opção 2 for escolhida)

| # | Etapa | Entregas | Depende de |
|---|---|---|---|
| F0 | Decisões e protótipo | Mapa de telas, wireframes, marca, libs | decisão de escopo + ADR |
| F1 | Design system | Tokens, componentes base, formatadores | F0 |
| F2 | Estrutura | Layouts, navegação, erro/carregamento, acesso por papel | F1 |
| F3 | Autenticação | Login via OIDC | backend B-C |
| F4 | Workflows | Lista, detalhe, ações disponíveis, histórico | F3 |
| F5 | Administração | Definições, versões, ativação, auditoria | F4 |
| F6 | Qualidade | Responsividade, acessibilidade AA, E2E | F5 |

## 4. 🚦 Portão "Front-end pronto"

- [ ] Escopo de front-end aprovado
- [ ] Todos os fluxos do MVP funcionando de ponta a ponta com dados reais
- [ ] Nenhum cálculo ou regra de negócio na interface
- [ ] Responsivo, AA e com todos os estados tratados
- [ ] E2E dos fluxos principais passando
- [ ] `docs/frontend-status.md` sem pendências

## 5. Registro de etapas

| Data | Etapa | Resultado | Próximo passo |
|---|---|---|---|
| 2026-10-03 | Auditoria do front-end | Inexistente, por escopo | Decidir se haverá front-end |
