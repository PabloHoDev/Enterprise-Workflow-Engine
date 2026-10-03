// Contratos da API REST (docs/architecture/API_DESIGN.md). Espelham os DTOs do backend.

export type WorkflowStatus = 'CREATED' | 'RUNNING' | 'COMPLETED' | 'CANCELLED'
export type VersionStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE'
export type StateType = 'INITIAL' | 'INTERMEDIATE' | 'TERMINAL'
export type RuleOperator =
  | 'EQUALS'
  | 'NOT_EQUALS'
  | 'GREATER_THAN'
  | 'GREATER_THAN_OR_EQUAL'
  | 'LESS_THAN'
  | 'LESS_THAN_OR_EQUAL'
  | 'EXISTS'
export type HistoryEventType = 'CREATED' | 'STARTED' | 'TRANSITIONED' | 'CANCELLED'
export type AuditOutcome = 'SUCCESS' | 'REJECTED'

export interface Page<T> {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export interface Me {
  username: string
  displayName: string
  roles: string[]
}

export interface Rule {
  field: string
  operator: RuleOperator
  value: string | null
}

export interface StateDefinition {
  name: string
  type: StateType
}

export interface TransitionDefinition {
  action: string
  from: string
  to: string
  requiredRole: string | null
  rules: Rule[]
}

export interface DefinitionSummary {
  id: string
  key: string
  name: string
  description: string | null
  createdAt: string
  updatedAt: string
  activeVersion: number | null
}

export interface Definition extends DefinitionSummary {
  versions: { number: number; status: VersionStatus; createdAt: string }[]
}

export interface Version {
  id: string
  definitionKey: string
  number: number
  status: VersionStatus
  createdAt: string
  states: StateDefinition[]
  transitions: TransitionDefinition[]
}

export interface VersionStructure {
  states: StateDefinition[]
  transitions: TransitionDefinition[]
}

export interface WorkflowSummary {
  id: string
  definitionKey: string
  definitionVersion: number
  status: WorkflowStatus
  currentState: string
  createdAt: string
  updatedAt: string
}

export interface Workflow extends WorkflowSummary {
  variables: Record<string, unknown>
}

export interface HistoryEntry {
  sequence: number
  type: HistoryEventType
  action: string | null
  fromState: string | null
  toState: string
  actorId: string
  occurredAt: string
  comment: string | null
}

export interface AvailableAction {
  action: string
  targetState: string
  requiredRole: string | null
}

export interface WorkflowCounts {
  total: number
  byStatus: Record<WorkflowStatus, number>
}

export interface AuditRecord {
  id: string
  actorId: string
  operation: string
  resourceType: string
  resourceId: string
  outcome: AuditOutcome
  detail: string | null
  occurredAt: string
}

export interface User {
  id: string
  username: string
  displayName: string
  roles: string[]
  enabled: boolean
  locked: boolean
  lockedUntil: string | null
  failedLoginAttempts: number
  lastLoginAt: string | null
  passwordChangedAt: string
  createdAt: string
  updatedAt: string
}

/** Erro no formato Problem Details (RFC 9457), com as extensões usadas pela API. */
export interface Problem {
  title?: string
  status?: number
  detail?: string
  code?: string
  violations?: string[]
  unsatisfiedRules?: string[]
  errors?: { field: string; message: string }[]
}
