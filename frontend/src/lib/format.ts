import { ApiError } from '../api/client'
import type {
  AuditOutcome,
  HistoryEventType,
  RuleOperator,
  StateType,
  VersionStatus,
  WorkflowStatus,
} from '../api/types'

const dateTime = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'medium' })
const relative = new Intl.RelativeTimeFormat('pt-BR', { numeric: 'auto' })
const integer = new Intl.NumberFormat('pt-BR')

export function formatDateTime(value: string | null | undefined): string {
  return value ? dateTime.format(new Date(value)) : '—'
}

export function formatRelative(value: string | null | undefined, now: Date = new Date()): string {
  if (!value) {
    return '—'
  }
  const seconds = Math.round((new Date(value).getTime() - now.getTime()) / 1000)
  const units: [Intl.RelativeTimeFormatUnit, number][] = [
    ['day', 86_400],
    ['hour', 3_600],
    ['minute', 60],
  ]
  for (const [unit, size] of units) {
    if (Math.abs(seconds) >= size) {
      return relative.format(Math.round(seconds / size), unit)
    }
  }
  return relative.format(seconds, 'second')
}

export function formatNumber(value: number): string {
  return integer.format(value)
}

export function shortId(id: string): string {
  return id.slice(0, 8)
}

export const workflowStatusLabel: Record<WorkflowStatus, string> = {
  CREATED: 'Criado',
  RUNNING: 'Em andamento',
  COMPLETED: 'Concluído',
  CANCELLED: 'Cancelado',
}

export const versionStatusLabel: Record<VersionStatus, string> = {
  DRAFT: 'Rascunho',
  ACTIVE: 'Ativa',
  INACTIVE: 'Inativa',
}

export const stateTypeLabel: Record<StateType, string> = {
  INITIAL: 'Inicial',
  INTERMEDIATE: 'Intermediário',
  TERMINAL: 'Final',
}

export const operatorLabel: Record<RuleOperator, string> = {
  EQUALS: 'igual a',
  NOT_EQUALS: 'diferente de',
  GREATER_THAN: 'maior que',
  GREATER_THAN_OR_EQUAL: 'maior ou igual a',
  LESS_THAN: 'menor que',
  LESS_THAN_OR_EQUAL: 'menor ou igual a',
  EXISTS: 'preenchido',
}

export const historyTypeLabel: Record<HistoryEventType, string> = {
  CREATED: 'Criado',
  STARTED: 'Iniciado',
  TRANSITIONED: 'Transição',
  CANCELLED: 'Cancelado',
}

export const outcomeLabel: Record<AuditOutcome, string> = {
  SUCCESS: 'Sucesso',
  REJECTED: 'Recusado',
}

const operationLabels: Record<string, string> = {
  DEFINITION_CREATED: 'Definição criada',
  DEFINITION_VERSION_CREATED: 'Versão criada',
  DEFINITION_VERSION_ACTIVATED: 'Versão ativada',
  DEFINITION_VERSION_DEACTIVATED: 'Versão desativada',
  WORKFLOW_CREATED: 'Workflow criado',
  WORKFLOW_STARTED: 'Workflow iniciado',
  WORKFLOW_ACTION_EXECUTED: 'Ação executada',
  WORKFLOW_CANCELLED: 'Workflow cancelado',
  LOGIN_SUCCEEDED: 'Login',
  LOGIN_FAILED: 'Falha de login',
  LOGOUT: 'Logout',
  ACCOUNT_LOCKED: 'Conta bloqueada',
  ACCESS_DENIED: 'Acesso negado',
  USER_CREATED: 'Usuário criado',
  USER_UPDATED: 'Usuário alterado',
  USER_PASSWORD_RESET: 'Senha redefinida',
  USER_PASSWORD_CHANGED: 'Senha alterada',
  USER_UNLOCKED: 'Usuário desbloqueado',
}

export const auditOperations = Object.keys(operationLabels)

export function operationLabel(operation: string): string {
  return operationLabels[operation] ?? operation
}

export function describeRule(rule: { field: string; operator: RuleOperator; value: string | null }): string {
  return rule.operator === 'EXISTS'
    ? `${rule.field} ${operatorLabel.EXISTS}`
    : `${rule.field} ${operatorLabel[rule.operator]} ${rule.value ?? ''}`.trim()
}

const statusMessages: Record<number, string> = {
  400: 'A requisição tem dados inválidos.',
  401: 'Sua sessão expirou. Entre novamente.',
  403: 'Você não tem permissão para esta operação.',
  404: 'O recurso não foi encontrado.',
  409: 'A operação conflita com o estado atual. Atualize a página e tente de novo.',
  413: 'O conteúdo enviado é grande demais.',
  422: 'A operação foi recusada pelas regras do sistema.',
  429: 'Muitas tentativas. Aguarde um pouco antes de tentar de novo.',
}

const titleMessages: Record<string, string> = {
  'Rule not satisfied': 'As regras desta ação não foram atendidas.',
  'Actor not authorized': 'Seu perfil não tem o papel exigido para esta ação.',
  'Action not available': 'Esta ação não está disponível no estado atual.',
  'Operation incompatible with workflow status': 'A operação não é permitida na situação atual do workflow.',
  'Workflow definition unavailable': 'A definição não tem versão ativa para novas execuções.',
  'Invalid workflow definition': 'A estrutura do processo não é válida.',
  'Workflow definition key already in use': 'Já existe uma definição com esta chave.',
  'Invalid version status': 'A versão já está nessa situação.',
  'Weak password': 'A senha não atende à política de senhas.',
  'Username already in use': 'Este nome de usuário já está em uso.',
  'Incorrect current password': 'A senha atual está incorreta.',
  'Operation not allowed on own account': 'Você não pode remover o próprio acesso de administrador.',
  'Last administrator': 'O sistema precisa manter ao menos um administrador ativo.',
  'Concurrent modification': 'Outra pessoa alterou este registro. Atualize e tente de novo.',
}

/** Mensagem para o usuário, em português, a partir de qualquer erro. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return (
      (error.problem.title && titleMessages[error.problem.title]) ??
      statusMessages[error.status] ??
      'Ocorreu um erro inesperado. Tente novamente.'
    )
  }
  return 'Não foi possível falar com o servidor. Verifique sua conexão.'
}

/** Detalhes técnicos devolvidos pela API (regras, violações, campos), para exibir abaixo da mensagem. */
export function errorDetails(error: unknown): string[] {
  if (!(error instanceof ApiError)) {
    return []
  }
  const { problem } = error
  return [
    ...(problem.unsatisfiedRules ?? []),
    ...(problem.violations ?? []),
    ...(problem.errors ?? []).map((e) => `${e.field}: ${e.message}`),
  ]
}
