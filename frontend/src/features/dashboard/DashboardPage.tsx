import { useQuery } from '@tanstack/react-query'
import { ArrowRight, Plus } from 'lucide-react'
import { Link } from 'react-router'
import { workflowsApi } from '../../api/endpoints'
import type { WorkflowStatus } from '../../api/types'
import { useAuth } from '../../auth/AuthProvider'
import { WorkflowStatusBadge } from '../../components/StatusBadge'
import { ButtonLink } from '../../components/ui/Button'
import { EmptyState, ErrorAlert, Skeleton } from '../../components/ui/Feedback'
import { Card, CardHeader, PageHeader, Table, Td, Th } from '../../components/ui/Layout'
import { formatNumber, formatRelative, shortId, workflowStatusLabel } from '../../lib/format'

const STATUS_ORDER: WorkflowStatus[] = ['CREATED', 'RUNNING', 'COMPLETED', 'CANCELLED']
const STATUS_ACCENT: Record<WorkflowStatus, string> = {
  CREATED: 'bg-sky-500',
  RUNNING: 'bg-indigo-500',
  COMPLETED: 'bg-emerald-500',
  CANCELLED: 'bg-rose-500',
}

export function DashboardPage() {
  const { user } = useAuth()
  const counts = useQuery({ queryKey: ['workflows', 'counts'], queryFn: workflowsApi.counts })
  const recent = useQuery({ queryKey: ['workflows', 'recent'], queryFn: () => workflowsApi.list({ size: 8 }) })

  return (
    <>
      <PageHeader
        title={`Olá, ${user?.displayName.split(' ')[0] ?? ''}`}
        description="Visão geral das execuções de workflow."
        actions={
          <ButtonLink to="/workflows?new=1" variant="primary" icon={<Plus aria-hidden className="size-4" />}>
            Nova solicitação
          </ButtonLink>
        }
      />

      {counts.isError && <ErrorAlert error={counts.error} />}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {STATUS_ORDER.map((status) => (
          <Link
            key={status}
            to={`/workflows?status=${status}`}
            className="group rounded-xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-indigo-300 dark:border-slate-800 dark:bg-slate-900 dark:hover:border-indigo-700"
          >
            <div className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
              <span className={`size-2 rounded-full ${STATUS_ACCENT[status]}`} aria-hidden />
              {workflowStatusLabel[status]}
            </div>
            {counts.isPending ? (
              <Skeleton className="mt-3 h-8 w-16" />
            ) : (
              <p className="mt-2 text-3xl font-semibold tabular-nums">
                {formatNumber(counts.data?.byStatus[status] ?? 0)}
              </p>
            )}
          </Link>
        ))}
      </div>

      <Card className="mt-8">
        <CardHeader
          title="Execuções recentes"
          actions={
            <Link to="/workflows" className="inline-flex items-center gap-1 text-sm font-medium text-indigo-600 hover:underline dark:text-indigo-400">
              Ver todas <ArrowRight aria-hidden className="size-4" />
            </Link>
          }
        />
        {recent.isError && (
          <div className="p-5">
            <ErrorAlert error={recent.error} />
          </div>
        )}
        {recent.isPending && (
          <div className="space-y-3 p-5">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-6 w-full" />
            ))}
          </div>
        )}
        {recent.data && recent.data.content.length === 0 && (
          <EmptyState title="Nenhuma execução ainda" description="Crie uma solicitação a partir de uma definição ativa." />
        )}
        {recent.data && recent.data.content.length > 0 && (
          <Table label="Execuções recentes">
            <thead>
              <tr>
                <Th>Workflow</Th>
                <Th>Processo</Th>
                <Th>Estado atual</Th>
                <Th>Situação</Th>
                <Th>Atualizado</Th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {recent.data.content.map((workflow) => (
                <tr key={workflow.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                  <Td>
                    <Link to={`/workflows/${workflow.id}`} className="font-mono font-medium text-indigo-600 hover:underline dark:text-indigo-400">
                      {shortId(workflow.id)}
                    </Link>
                  </Td>
                  <Td>
                    {workflow.definitionKey} <span className="text-slate-600 dark:text-slate-400">v{workflow.definitionVersion}</span>
                  </Td>
                  <Td className="font-mono text-xs">{workflow.currentState}</Td>
                  <Td>
                    <WorkflowStatusBadge status={workflow.status} />
                  </Td>
                  <Td className="whitespace-nowrap text-slate-600 dark:text-slate-400">{formatRelative(workflow.updatedAt)}</Td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card>
    </>
  )
}
