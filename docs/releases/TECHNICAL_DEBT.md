# Technical Debt

# Enterprise Workflow Engine

**Versão:** 0.1
**Status:** Em evolução

---

# 1. Objetivo

Decisões temporárias e limitações conhecidas do que já foi entregue (`docs/PROJECT_GOVERNANCE.md` §14).

Cada item possui descrição, motivo, impacto, prioridade e possível solução. Oportunidades que não são
limitações do que existe ficam em `docs/product/TECHNICAL_BACKLOG.md`.

---

# 2. Resumo

| ID     | Item                                                        | Prioridade |
| ------ | ----------------------------------------------------------- | ---------- |
| TD-001 | Usuários estáticos com HTTP Basic                           | Alta       |
| TD-002 | Negações de acesso HTTP não são auditadas                   | Média      |
| TD-003 | Sem limitação de taxa nem proteção contra força bruta       | Média      |
| TD-004 | History carregado por inteiro a cada operação               | Média      |
| TD-005 | Aggregate de definição carrega todas as versões             | Baixa      |
| TD-006 | Imagem Docker e pipeline de CI ainda não executadas         | Alta       |
| TD-007 | Validação local feita em JDK 27, não no JDK 25 alvo         | Baixa      |
| TD-008 | Variáveis do Workflow sem limite de tamanho próprio         | Baixa      |

---

# 3. Itens

## TD-001 — Usuários estáticos com HTTP Basic

- **Descrição:** os usuários da API vêm da configuração e são carregados na inicialização.
- **Motivo:** o módulo de identidade está fora do escopo inicial (ADR-005).
- **Impacto:** sem expiração, revogação ou troca de senha sem reinício; credencial enviada a cada
  requisição, exigindo TLS; custo de BCrypt por requisição.
- **Prioridade:** Alta — é o principal item antes de um uso real.
- **Possível solução:** OAuth2 Resource Server com JWT de um provedor OIDC. O domínio depende apenas de
  `Actor(id, roles)`, então a mudança fica restrita a `infrastructure/security`.

## TD-002 — Negações de acesso HTTP não são auditadas

- **Descrição:** respostas `401` e `403` decididas pelo Spring Security (por endpoint) não geram registro
  de Audit. Recusas decididas pelo domínio, como o papel exigido por uma Transition, são auditadas.
- **Motivo:** a auditoria é feita nos application services, que não chegam a ser chamados nesses casos.
- **Impacto:** tentativas de acesso a endpoints administrativos não ficam na trilha de auditoria (BR-032),
  apenas nos logs de acesso.
- **Prioridade:** Média.
- **Possível solução:** ouvir os eventos de autorização do Spring Security e registrá-los pelo
  `AuditRecorder`.

## TD-003 — Sem limitação de taxa nem proteção contra força bruta

- **Descrição:** não há limite de requisições por cliente nem bloqueio após falhas de autenticação.
- **Motivo:** normalmente tratado no gateway ou proxy à frente da aplicação, ainda não definido.
- **Impacto:** exposição a tentativa de senha por repetição e a abuso da API.
- **Prioridade:** Média; passa a Alta se a aplicação for exposta sem gateway.
- **Possível solução:** rate limiting no gateway; resolver junto com TD-001.

## TD-004 — History carregado por inteiro a cada operação

- **Descrição:** o aggregate `Workflow` é reconstituído com todo o History, inclusive para executar uma
  ação.
- **Motivo:** simplicidade do mapeamento; o History faz parte do aggregate.
- **Impacto:** o custo de cada operação cresce com o número de mudanças do Workflow. Irrelevante para
  processos de aprovação; perceptível em execuções com milhares de transições.
- **Prioridade:** Média.
- **Possível solução:** o aggregate guardar apenas o tamanho do History e os registros novos; consulta do
  History paginada (TB-001).

## TD-005 — Aggregate de definição carrega todas as versões

- **Descrição:** operações administrativas sobre uma definição carregam a estrutura de todas as versões.
- **Motivo:** o aggregate protege a invariante de versão ativa única (BR-041).
- **Impacto:** restrito a operações administrativas, pouco frequentes. A execução de Workflows não é
  afetada: usa uma consulta direta à versão.
- **Prioridade:** Baixa.
- **Possível solução:** carregar apenas número e status das versões nas operações de ativação.

## TD-006 — Imagem Docker e pipeline de CI ainda não executadas

- **Descrição:** o `Dockerfile`, o `docker-compose.yml` e o workflow do GitHub Actions foram escritos, mas
  nunca executados: a máquina de desenvolvimento não possui Docker e a pipeline ainda não rodou.
- **Motivo:** restrição do ambiente em que a fundação técnica foi construída.
- **Impacto:** podem conter erros só detectáveis na execução. A extração do jar em camadas usada pelo
  `Dockerfile` e a suíte completa de testes foram verificadas localmente; a integração com Testcontainers
  não.
- **Prioridade:** Alta — resolver na primeira execução da pipeline.
- **Possível solução:** acompanhar a primeira execução da pipeline e corrigir o que falhar.

## TD-007 — Validação local feita em JDK 27, não no JDK 25 alvo

- **Descrição:** o projeto compila para Java 25 (`--release 25`), mas build e testes locais rodaram sobre
  um JDK 27.
- **Motivo:** único JDK disponível na máquina de desenvolvimento.
- **Impacto:** baixo; o bytecode gerado é Java 25 e a pipeline usa JDK 25. Bibliotecas de instrumentação
  emitem avisos em JDKs mais novos.
- **Prioridade:** Baixa.
- **Possível solução:** a pipeline de CI já cobre o JDK alvo; opcionalmente instalar o JDK 25 localmente.

## TD-008 — Variáveis do Workflow sem limite de tamanho próprio

- **Descrição:** as variáveis de um Workflow têm formato e tamanho livres, limitados apenas pelo tamanho
  máximo de requisição do servidor.
- **Motivo:** o conteúdo das variáveis depende de cada processo e ainda não há um esquema declarado.
- **Impacto:** um consumidor pode armazenar documentos grandes em `jsonb`, encarecendo cada operação.
- **Prioridade:** Baixa.
- **Possível solução:** limite configurável e esquema de variáveis por definição (TB-012).

---

# 4. Como Registrar um Item

1. Use o próximo identificador `TD-NNN`.
2. Preencha descrição, motivo, impacto, prioridade e possível solução.
3. Ao resolver, remova o item e registre a mudança em `docs/releases/CHANGELOG.md`.
