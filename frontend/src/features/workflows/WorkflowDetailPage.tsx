import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Ban, CircleDot, Play, Zap } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { definitionsApi, workflowsApi } from '../../api/endpoints'
import type { AvailableAction, TransitionDefinition, Workflow } from '../../api/types'
import { useAuth } from '../../auth/AuthProvider'
import { StateDiagram } from '../../components/StateDiagram'
import { WorkflowStatusBadge } from '../../components/StatusBadge'
import { VariablesEditor } from '../../components/VariablesEditor'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { EmptyState, ErrorAlert, LoadingBlock } from '../../components/ui/Feedback'
import { Field, Textarea } from '../../components/ui/Form'
import { Card, CardHeader, DescriptionList, PageHeader } from '../../components/ui/Layout'
import { useToast } from '../../components/ui/Toast'
import { describeRule, formatDateTime, historyTypeLabel, shortId } from '../../lib/format'
import { formatValue, rowsToVariables, type VariableRow } from '../../lib/variables'

function ActionDialog({
  workflow,
  action,
  transition,
  onClose,
}: {
  workflow: Workflow
  action: AvailableAction | null
  transition?: TransitionDefinition
  onClose: () => void
}) {
  const [rows, setRows] = useState<VariableRow[]>([])
  const queryClient = useQueryClient()
  const notify = useToast()
  const execute = useMutation({
    mutationFn: () => workflowsApi.execute(workflow.id, action!.action, rowsToVariables(rows)),
    onSuccess: (updated) => {
      queryClient.setQueryData(['workflow', workflow.id], updated)
      void queryClient.invalidateQueries({ queryKey: ['workflow', workflow.id] })
      void queryClient.invalidateQueries({ queryKey: ['workflows'] })
      notify(`Ação "${action!.action}" executada. Novo estado: ${updated.currentState}.`)
      onClose()
    },
  })

  return (
    <Dialog
      open={action !== null}
      onClose={() => {
        execute.reset()
        setRows([])
        onClose()
      }}
      title={`Executar "${action?.action ?? ''}"`}
      description={action ? `O workflow passará de ${workflow.currentState} para ${action.targetState}.` : undefined}
      onSubmit={() => execute.mutate()}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" loading={execute.isPending}>
            Confirmar
          </Button>
        </>
      }
    >
      {execute.isError && <ErrorAlert error={execute.error} />}
      {transition && transition.rules.length > 0 && (
        <div className="rounded-lg bg-slate-50 p-3 text-sm dark:bg-slate-800/60">
          <p className="font-medium">Regras desta transição</p>
          <ul className="mt-1 list-inside list-disc font-mono text-xs text-slate-600 dark:text-slate-300">
            {transition.rules.map((rule) => (
              <li key={describeRule(rule)}>{describeRule(rule)}</li>
            ))}
          </ul>
        </div>
      )}
      <VariablesEditor rows={rows} onChange={setRows} />
    </Dialog>
  )
}

function CancelDialog({ workflow, open, onClose }: { workflow: Workflow; open: boolean; onClose: () => void }) {
  const [reason, setReason] = useState('')
  const queryClient = useQueryClient()
  const notify = useToast()
  const cancel = useMutation({
    mutationFn: () => workflowsApi.cancel(workflow.id, reason.trim() || null),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['workflow', workflow.id] })
      void queryClient.invalidateQueries({ queryKey: ['workflows'] })
      notify('Workflow cancelado.')
      onClose()
    },
  })
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Cancelar workflow"
      description="O cancelamento é definitivo e fica registrado no histórico e na auditoria."
      onSubmit={() => cancel.mutate()}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Voltar
          </Button>
          <Button type="submit" variant="danger" loading={cancel.isPending}>
            Cancelar workflow
          </Button>
        </>
      }
    >
      {cancel.isError && <ErrorAlert error={cancel.error} />}
      <Field label="Motivo" hint={`${reason.length}/500 caracteres`}>
        {({ id, describedBy }) => (
          <Textarea
            id={id}
            aria-describedby={describedBy}
            rows={3}
            maxLength={500}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
        )}
      </Field>
    </Dialog>
  )
}

export function WorkflowDetailPage() {
  const { id = '' } = useParams()
  const { hasRole } = useAuth()
  const queryClient = useQueryClient()
  const notify = useToast()
  const [selectedAction, setSelectedAction] = useState<AvailableAction | null>(null)
  const [cancelling, setCancelling] = useState(false)

  const workflow = useQuery({ queryKey: ['workflow', id], queryFn: () => workflowsApi.get(id) })
  const history = useQuery({ queryKey: ['workflow', id, 'history'], queryFn: () => workflowsApi.history(id) })
  const actions = useQuery({
    queryKey: ['workflow', id, 'actions'],
    queryFn: () => workflowsApi.availableActions(id),
    enabled: workflow.data?.status === 'RUNNING',
  })
  const version = useQuery({
    queryKey: ['version', workflow.data?.definitionKey, workflow.data?.definitionVersion],
    queryFn: () => definitionsApi.version(workflow.data!.definitionKey, workflow.data!.definitionVersion),
    enabled: Boolean(workflow.data),
    staleTime: Infinity, // A estrutura de uma versão nunca muda.
  })
  const start = useMutation({
    mutationFn: () => workflowsApi.start(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['workflow', id] })
      void queryClient.invalidateQueries({ queryKey: ['workflows'] })
      notify('Workflow iniciado.')
    },
  })

  if (workflow.isPending) {
    return <LoadingBlock />
  }
  if (workflow.isError) {
    return <ErrorAlert error={workflow.error} />
  }
  const data = workflow.data
  const finished = data.status === 'COMPLETED' || data.status === 'CANCELLED'
  const variables = Object.entries(data.variables)

  return (
    <>
      <PageHeader
        eyebrow={
          <Link to="/workflows" className="inline-flex items-center gap-1 hover:underline">
            <ArrowLeft aria-hidden className="size-4" /> Workflows
          </Link>
        }
        title={
          <span className="flex flex-wrap items-center gap-3">
            <span>
              Workflow <span className="font-mono">{shortId(data.id)}</span>
            </span>
            <WorkflowStatusBadge status={data.status} />
          </span>
        }
        description={
          <>
            {data.definitionKey} · versão {data.definitionVersion} · estado atual{' '}
            <strong className="font-mono">{data.currentState}</strong>
          </>
        }
        actions={
          <>
            {data.status === 'CREATED' && (
              <Button onClick={() => start.mutate()} loading={start.isPending} icon={<Play aria-hidden className="size-4" />}>
                Iniciar
              </Button>
            )}
            {!finished && (
              <Button variant="secondary" onClick={() => setCancelling(true)} icon={<Ban aria-hidden className="size-4" />}>
                Cancelar
              </Button>
            )}
          </>
        }
      />
      {start.isError && (
        <div className="mb-6">
          <ErrorAlert error={start.error} />
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <div className="space-y-6 xl:col-span-2">
          {data.status === 'RUNNING' && (
            <Card>
              <CardHeader title="Ações disponíveis" description="Transições permitidas a partir do estado atual." />
              <div className="p-5">
                {actions.isError && <ErrorAlert error={actions.error} />}
                {actions.isPending && <LoadingBlock label="Carregando ações…" />}
                {actions.data && actions.data.length === 0 && <p className="text-sm text-slate-600 dark:text-slate-400">Nenhuma ação disponível.</p>}
                <div className="flex flex-wrap gap-3">
                  {actions.data?.map((action) => {
                    const allowed = !action.requiredRole || hasRole(action.requiredRole)
                    return (
                      <div key={action.action} className="flex flex-col gap-1">
                        <Button
                          variant={allowed ? 'primary' : 'secondary'}
                          disabled={!allowed}
                          onClick={() => setSelectedAction(action)}
                          icon={<Zap aria-hidden className="size-4" />}
                        >
                          {action.action}
                        </Button>
                        <span className="text-xs text-slate-500 dark:text-slate-400">
                          → {action.targetState}
                          {action.requiredRole && ` · exige ${action.requiredRole}`}
                        </span>
                      </div>
                    )
                  })}
                </div>
              </div>
            </Card>
          )}
          {data.status === 'CREATED' && (
            <Card className="p-5 text-sm text-slate-600 dark:text-slate-400">
              O workflow foi criado e está aguardando o início para aceitar ações.
            </Card>
          )}

          <Card>
            <CardHeader title="Fluxo do processo" description={`Versão ${data.definitionVersion} de ${data.definitionKey}`} />
            <div className="p-5">
              {version.isError && <ErrorAlert error={version.error} />}
              {version.isPending && <LoadingBlock label="Carregando diagrama…" />}
              {version.data && (
                <StateDiagram
                  states={version.data.states}
                  transitions={version.data.transitions}
                  currentState={data.currentState}
                />
              )}
            </div>
          </Card>

          <Card>
            <CardHeader title="Histórico" description="Toda mudança relevante, em ordem cronológica." />
            <div className="p-5">
              {history.isError && <ErrorAlert error={history.error} />}
              {history.isPending && <LoadingBlock />}
              <ol className="relative space-y-6 border-l border-slate-200 pl-6 dark:border-slate-700">
                {history.data?.map((entry) => (
                  <li key={entry.sequence} className="relative">
                    <CircleDot aria-hidden className="absolute top-0.5 -left-[2.05rem] size-4 bg-white text-indigo-500 dark:bg-slate-900" />
                    <p className="text-sm font-medium">
                      {historyTypeLabel[entry.type]}
                      {entry.action && (
                        <>
                          : <span className="font-mono">{entry.action}</span>
                        </>
                      )}
                    </p>
                    <p className="text-sm text-slate-600 dark:text-slate-400">
                      {entry.fromState && entry.fromState !== entry.toState ? (
                        <>
                          <span className="font-mono">{entry.fromState}</span> → <span className="font-mono">{entry.toState}</span>
                        </>
                      ) : (
                        <span className="font-mono">{entry.toState}</span>
                      )}{' '}
                      · por <strong>{entry.actorId}</strong>
                    </p>
                    {entry.comment && <p className="mt-1 text-sm italic text-slate-600 dark:text-slate-400">“{entry.comment}”</p>}
                    <time dateTime={entry.occurredAt} className="text-xs text-slate-600 dark:text-slate-400">
                      {formatDateTime(entry.occurredAt)}
                    </time>
                  </li>
                ))}
              </ol>
            </div>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader title="Detalhes" />
            <div className="p-5">
              <DescriptionList
                items={[
                  { label: 'Identificador', value: <span className="font-mono text-xs">{data.id}</span> },
                  {
                    label: 'Definição',
                    value: (
                      <Link className="text-indigo-600 hover:underline dark:text-indigo-400" to={`/definitions/${data.definitionKey}`}>
                        {data.definitionKey}
                      </Link>
                    ),
                  },
                  { label: 'Criado em', value: formatDateTime(data.createdAt) },
                  { label: 'Atualizado em', value: formatDateTime(data.updatedAt) },
                ]}
              />
            </div>
          </Card>
          <Card>
            <CardHeader title="Variáveis" />
            {variables.length === 0 ? (
              <EmptyState title="Sem variáveis" />
            ) : (
              <dl className="divide-y divide-slate-100 dark:divide-slate-800">
                {variables.map(([key, value]) => (
                  <div key={key} className="flex justify-between gap-4 px-5 py-3 text-sm">
                    <dt className="font-mono text-slate-600 dark:text-slate-400">{key}</dt>
                    <dd className="truncate text-right font-medium" title={formatValue(value)}>
                      {formatValue(value)}
                    </dd>
                  </div>
                ))}
              </dl>
            )}
          </Card>
        </div>
      </div>

      <ActionDialog
        workflow={data}
        action={selectedAction}
        transition={version.data?.transitions.find(
          (transition) => transition.from === data.currentState && transition.action === selectedAction?.action,
        )}
        onClose={() => setSelectedAction(null)}
      />
      <CancelDialog workflow={data} open={cancelling} onClose={() => setCancelling(false)} />
    </>
  )
}
