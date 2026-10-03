import type { AuditOutcome, VersionStatus, WorkflowStatus } from '../api/types'
import { outcomeLabel, versionStatusLabel, workflowStatusLabel } from '../lib/format'
import { Badge } from './ui/Layout'

const workflowTone = {
  CREATED: 'sky',
  RUNNING: 'indigo',
  COMPLETED: 'emerald',
  CANCELLED: 'rose',
} as const

const versionTone = {
  DRAFT: 'amber',
  ACTIVE: 'emerald',
  INACTIVE: 'slate',
} as const

export function WorkflowStatusBadge({ status }: { status: WorkflowStatus }) {
  return <Badge tone={workflowTone[status]}>{workflowStatusLabel[status]}</Badge>
}

export function VersionStatusBadge({ status }: { status: VersionStatus }) {
  return <Badge tone={versionTone[status]}>{versionStatusLabel[status]}</Badge>
}

export function OutcomeBadge({ outcome }: { outcome: AuditOutcome }) {
  return <Badge tone={outcome === 'SUCCESS' ? 'emerald' : 'rose'}>{outcomeLabel[outcome]}</Badge>
}
