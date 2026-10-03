import { api, query } from './client'
import type {
  AuditOutcome,
  AuditRecord,
  AvailableAction,
  Definition,
  DefinitionSummary,
  HistoryEntry,
  Me,
  Page,
  User,
  Version,
  VersionStructure,
  Workflow,
  WorkflowCounts,
  WorkflowStatus,
  WorkflowSummary,
} from './types'

const V1 = '/api/v1'

export const authApi = {
  me: () => api.get<Me>(`${V1}/auth/me`),
  login: (username: string, password: string) =>
    api.post<Me>(`${V1}/auth/login`, { username, password }, { silentUnauthorized: true }),
  logout: () => api.post<void>(`${V1}/auth/logout`),
  changePassword: (currentPassword: string, newPassword: string) =>
    api.post<void>(`${V1}/auth/password`, { currentPassword, newPassword }),
}

export const definitionsApi = {
  list: (page = 0, size = 20) => api.get<Page<DefinitionSummary>>(`${V1}/workflow-definitions${query({ page, size })}`),
  get: (key: string) => api.get<Definition>(`${V1}/workflow-definitions/${encodeURIComponent(key)}`),
  version: (key: string, number: number) =>
    api.get<Version>(`${V1}/workflow-definitions/${encodeURIComponent(key)}/versions/${number}`),
  create: (body: { key: string; name: string; description: string | null } & VersionStructure) =>
    api.post<Definition>(`${V1}/workflow-definitions`, body),
  addVersion: (key: string, body: VersionStructure) =>
    api.post<Version>(`${V1}/workflow-definitions/${encodeURIComponent(key)}/versions`, body),
  activate: (key: string, number: number) =>
    api.post<Version>(`${V1}/workflow-definitions/${encodeURIComponent(key)}/versions/${number}/activate`),
  deactivate: (key: string, number: number) =>
    api.post<Version>(`${V1}/workflow-definitions/${encodeURIComponent(key)}/versions/${number}/deactivate`),
}

export interface WorkflowFilters {
  definitionKey?: string
  status?: WorkflowStatus | ''
  page?: number
  size?: number
}

export const workflowsApi = {
  list: ({ definitionKey, status, page = 0, size = 20 }: WorkflowFilters) =>
    api.get<Page<WorkflowSummary>>(`${V1}/workflows${query({ definitionKey, status, page, size, sort: 'createdAt,desc' })}`),
  counts: () => api.get<WorkflowCounts>(`${V1}/workflows/summary`),
  get: (id: string) => api.get<Workflow>(`${V1}/workflows/${id}`),
  history: (id: string) => api.get<HistoryEntry[]>(`${V1}/workflows/${id}/history`),
  availableActions: (id: string) => api.get<AvailableAction[]>(`${V1}/workflows/${id}/actions`),
  create: (definitionKey: string, variables: Record<string, unknown>) =>
    api.post<Workflow>(`${V1}/workflows`, { definitionKey, variables }),
  start: (id: string) => api.post<Workflow>(`${V1}/workflows/${id}/start`),
  execute: (id: string, action: string, variables: Record<string, unknown>) =>
    api.post<Workflow>(`${V1}/workflows/${id}/actions`, { action, variables }),
  cancel: (id: string, reason: string | null) => api.post<Workflow>(`${V1}/workflows/${id}/cancel`, { reason }),
}

export interface AuditFilters {
  actorId?: string
  operation?: string
  resourceType?: string
  resourceId?: string
  outcome?: AuditOutcome | ''
  page?: number
  size?: number
}

export const auditApi = {
  search: ({ page = 0, size = 25, ...filters }: AuditFilters) =>
    api.get<Page<AuditRecord>>(`${V1}/audit-records${query({ ...filters, page, size, sort: 'occurredAt,desc' })}`),
}

export const usersApi = {
  list: (page = 0, size = 50) => api.get<Page<User>>(`${V1}/users${query({ page, size })}`),
  create: (body: { username: string; displayName: string; password: string; roles: string[] }) =>
    api.post<User>(`${V1}/users`, body),
  update: (username: string, body: { displayName: string; roles: string[]; enabled: boolean }) =>
    api.put<User>(`${V1}/users/${encodeURIComponent(username)}`, body),
  resetPassword: (username: string, newPassword: string) =>
    api.post<void>(`${V1}/users/${encodeURIComponent(username)}/password`, { newPassword }),
  unlock: (username: string) => api.post<User>(`${V1}/users/${encodeURIComponent(username)}/unlock`),
}
