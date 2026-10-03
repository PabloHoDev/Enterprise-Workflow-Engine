# Technical Debt

# Enterprise Workflow Engine

**Versão:** 0.2
**Status:** Em evolução

---

# 1. Objetivo

Decisões temporárias e limitações conhecidas do que já foi entregue (`docs/PROJECT_GOVERNANCE.md` §14).

Cada item possui descrição, motivo, impacto, prioridade e possível solução. Oportunidades que não são
limitações do que existe ficam em `docs/product/TECHNICAL_BACKLOG.md`.

---

# 2. Resumo

| ID     | Item                                                        | Prioridade | Situação  |
| ------ | ----------------------------------------------------------- | ---------- | --------- |
| TD-001 | Sem MFA nem SSO                                             | Média      | Aberto (reduzido em 2026-10-03) |
| TD-002 | Requisições anônimas recusadas (`401`) não são auditadas    | Baixa      | Aberto (reduzido em 2026-10-03) |
| TD-004 | History carregado por inteiro a cada operação               | Média      | Aberto    |
| TD-005 | Aggregate de definição carrega todas as versões             | Baixa      | Aberto    |
| TD-007 | Validação local feita em JDK 27, não no JDK 25 alvo         | Baixa      | Aberto    |
| TD-009 | Limite de falhas de login por IP local à instância          | Média      | Aberto    |
| TD-010 | Conflitos de concorrência (`409`) não são auditados         | Baixa      | Aberto    |
| TD-011 | Dados dos testes de integração e end-to-end se acumulam     | Baixa      | Aberto    |

Resolvidos em 2026-10-03:

| ID     | Item                                                        | Resolução                                              |
| ------ | ----------------------------------------------------------- | ------------------------------------------------------ |
| TD-003 | Sem limitação de taxa nem proteção contra força bruta       | bloqueio por conta + limite por IP (ADR-007)           |
| TD-006 | Imagem Docker e pipeline de CI nunca executadas             | primeira execução da CI concluída com sucesso          |
| TD-008 | Variáveis do Workflow sem limite de tamanho                 | corpo de requisição limitado a 256 KB                  |

---

# 3. Itens Abertos

## TD-001 — Sem MFA nem SSO

- **Descrição:** contas e senhas são geridas pela própria aplicação (ADR-007). Não há segundo fator nem
  login único com o diretório da empresa.
- **Motivo:** não há provedor de identidade disponível para desenvolvimento e demonstração.
- **Impacto:** a segurança da conta depende só da senha (mitigado por política de senha, bloqueio e
  limite por IP).
- **Prioridade:** Média; Alta antes de uso por uma organização real.
- **Possível solução:** OIDC com provedor externo (TB-013). O domínio depende apenas de
  `Actor(id, roles)` e o console só conhece `/api/v1/auth/*`.

## TD-002 — Requisições anônimas recusadas não são auditadas

- **Descrição:** `403` por papel ou CSRF e falhas de login são auditados; `401` de requisições sem
  credencial nenhuma não são.
- **Motivo:** seriam ruído (robôs, sessões expiradas), sem Actor a atribuir.
- **Impacto:** varreduras anônimas aparecem só nos logs de acesso.
- **Prioridade:** Baixa.
- **Possível solução:** métrica de `401` por origem, alertada no monitoramento.

## TD-004 — History carregado por inteiro a cada operação

- **Descrição:** o aggregate `Workflow` é reconstituído com todo o History, inclusive para executar uma ação.
- **Motivo:** simplicidade do mapeamento; o History faz parte do aggregate.
- **Impacto:** o custo de cada operação cresce com o número de mudanças do Workflow. Irrelevante para
  processos de aprovação; perceptível em execuções com milhares de transições.
- **Prioridade:** Média.
- **Possível solução:** o aggregate guardar apenas o tamanho do History e os registros novos; consulta do
  History paginada (TB-001).

## TD-005 — Aggregate de definição carrega todas as versões

- **Descrição:** operações administrativas sobre uma definição carregam a estrutura de todas as versões.
- **Motivo:** o aggregate protege a invariante de versão ativa única (BR-041).
- **Impacto:** restrito a operações administrativas; a execução de Workflows usa consulta direta à versão.
- **Prioridade:** Baixa.
- **Possível solução:** carregar apenas número e status das versões nas operações de ativação.

## TD-007 — Validação local feita em JDK 27, não no JDK 25 alvo

- **Descrição:** o projeto compila para Java 25 (`--release 25`), mas o build local roda sobre JDK 27.
- **Motivo:** único JDK disponível na máquina de desenvolvimento.
- **Impacto:** baixo; a CI usa JDK 25 e passou.
- **Prioridade:** Baixa.
- **Possível solução:** instalar o JDK 25 localmente.

## TD-009 — Limite de falhas de login por IP local à instância

- **Descrição:** o contador de falhas por IP (`LoginThrottle`) fica em memória.
- **Motivo:** evita uma dependência nova (Redis) para um controle que, em produção, cabe ao gateway.
- **Impacto:** com N instâncias, o limite efetivo é N vezes maior. O bloqueio por conta, que fica no banco,
  não é afetado.
- **Prioridade:** Média.
- **Possível solução:** limite no gateway/WAF; ou contador compartilhado no PostgreSQL ou Redis.

## TD-010 — Conflitos de concorrência não são auditados

- **Descrição:** quando duas operações disputam o mesmo Workflow, a perdedora recebe `409` pela violação
  detectada no commit, fora do caso de uso, e não gera registro de Audit.
- **Motivo:** a falha ocorre depois da lógica do application service.
- **Impacto:** a tentativa perdedora não aparece na trilha de auditoria (o History segue correto).
- **Prioridade:** Baixa.
- **Possível solução:** forçar o flush dentro do caso de uso e auditar a exceção, ou auditar no handler.

## TD-011 — Dados dos testes se acumulam

- **Descrição:** testes de integração e end-to-end criam dados com chaves únicas e não os removem.
- **Motivo:** independência entre testes sem limpeza do banco; com Testcontainers o banco é descartado.
- **Impacto:** com banco externo (`EWE_TEST_DB_URL`) ou no ambiente local, o volume cresce a cada execução.
- **Prioridade:** Baixa.
- **Possível solução:** usar um banco descartável também nesses casos, ou um script de limpeza.

---

# 4. Como Registrar um Item

1. Use o próximo identificador `TD-NNN`.
2. Preencha descrição, motivo, impacto, prioridade e possível solução.
3. Ao resolver, mova o item para a tabela de resolvidos e registre a mudança em `docs/releases/CHANGELOG.md`.
