# Front-end — Status e Plano de Ação

> Documento vivo. Atualize ao final de cada etapa.
> Última análise: 2026-10-03 — console web implementado (ADR-008).

Stack: React 19 · TypeScript (estrito) · Vite 8 · TanStack Query · React Router · Tailwind CSS 4.
Código em `frontend/`; servido pela aplicação Spring na mesma origem da API.

## 1. ✅ O que já está OK

### Decisões (F0)
- [x] Escopo aprovado: console administrativo (REQUIREMENTS RF-027, RF-028); editor de arrastar e soltar fora do escopo.
- [x] Arquitetura: SPA estática na mesma origem, sessão HttpOnly + CSRF (ADR-007, ADR-008).
- [x] Identidade visual própria e neutra (índigo/ardósia, fontes do sistema), sem kit de marca externo.

### Design system (F1)
- [x] Componentes: botão, campos com rótulo/dica/erro, diálogo (`<dialog>`), badges, cards, tabela responsiva, paginação, avisos, toasts.
- [x] Tema claro, escuro e do sistema; formatação pt-BR de datas, números e tempos relativos.

### Estrutura (F2)
- [x] Layout com barra lateral (desktop) e menu recolhível (celular); link "Ir para o conteúdo".
- [x] Rotas protegidas, áreas restritas a `ADMIN`, página 404, redirecionamento seguro após o login.

### Autenticação (F3)
- [x] Login, logout, troca de senha, sessão expirada leva ao login; mensagem genérica em falhas.

### Fluxos (F4/F5)
- [x] Painel com contagem por status e execuções recentes.
- [x] Workflows: lista com filtros na URL, criação com variáveis, detalhe com ações, regras da transição, diagrama com estado atual, histórico, início e cancelamento.
- [x] Definições: lista, detalhe com versões e diagrama, ativar/desativar, editor de definição e de nova versão com pré-visualização.
- [x] Auditoria com filtros; gestão de usuários (criar, papéis, ativar, redefinir senha, desbloquear); perfil.

### Qualidade (F6)
- [x] `tsc` estrito e ESLint sem erros.
- [x] 17 testes unitários (Vitest + Testing Library): cliente da API com CSRF, erros, formatação, redirecionamento seguro, layout do diagrama, tela de login.
- [x] 8 testes end-to-end (Playwright) contra a aplicação real: jornada completa com 4 perfis, regra de negócio recusando, auditoria, área restrita, celular 360 px em tema escuro.
- [x] Acessibilidade verificada pelo axe (WCAG 2 A/AA) em 6 telas; problemas de contraste e de rolagem por teclado encontrados e corrigidos.
- [x] Build de produção: ~133 KB de JavaScript comprimido; `npm audit` sem vulnerabilidades.

## 2. ⏳ O que está pendente

### 2.1 🚧 Bloqueios (decisões do usuário)
- [ ] Validar o visual e os textos do console (capturas em `docs/images/`).

### 2.2 🎨 Design system
- [ ] Sem vitrine de componentes (Storybook) — não necessária no tamanho atual.

### 2.3 🧭 Rotas e navegação
- [ ] —

### 2.4 📄 Telas
- [ ] Edição visual de arrastar e soltar (TB-015), fora do escopo.
- [ ] Internacionalização (TB-014).

### 2.5 🧪 Qualidade
- [ ] Os jobs de console e end-to-end da CI ainda não rodaram no GitHub (rodam no próximo push).
- [ ] Lighthouse/performance não medidos.

### 2.6 🚀 Produção
- [ ] Depende do ambiente de deploy (TB-009).

## 3. 🗺️ Plano de ação

| # | Etapa | Entregas | Depende de |
|---|---|---|---|
| F0–F6 | Console | ✅ concluídas em 2026-10-03 | — |
| F7 | Produção | domínio, TLS, monitoramento de erros do cliente | ambiente de deploy |
| F8 | Login federado | redirecionamento OIDC no lugar do formulário | backend B-C |

## 4. 🚦 Portão "Front-end pronto"

- [x] Escopo de front-end aprovado
- [x] Todos os fluxos do MVP funcionando de ponta a ponta com dados reais
- [x] Nenhum cálculo ou regra de negócio na interface
- [x] Responsivo, AA e com todos os estados tratados (carregando, vazio, erro, sucesso)
- [x] E2E dos fluxos principais passando
- [ ] Validação visual pelo responsável do projeto

## 5. Registro de etapas

| Data | Etapa | Resultado | Próximo passo |
|---|---|---|---|
| 2026-10-03 | Auditoria do front-end | Inexistente, por escopo | Decidir se haverá front-end |
| 2026-10-03 | F0–F6 Console web | Implementado; 17 unitários + 8 E2E passando; axe sem violações | Validação e CI |
