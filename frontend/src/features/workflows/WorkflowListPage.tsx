import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { Link, useSearchParams } from 'react-router'
import { workflowsApi } from '../../api/endpoints'
import type { WorkflowStatus } from '../../api/types'
import { WorkflowStatusBadge } from '../../components/StatusBadge'
import { Button } from '../../components/ui/Button'
import { EmptyState, ErrorAlert, LoadingBlock } from '../../components/ui/Feedback'
import { Field, Input, Select } from '../../components/ui/Form'
import { Card, PageHeader, Table, Td, Th } from '../../components/ui/Layout'
import { Pagination } from '../../components/ui/Pagination'
import { formatDateTime, shortId, workflowStatusLabel } from '../../lib/format'
import { CreateWorkflowDialog } from './CreateWorkflowDialog'

const STATUSES: WorkflowStatus[] = ['CREATED', 'RUNNING', 'COMPLETED', 'CANCELLED']

export function WorkflowListPage() {
  // Filtros e página ficam na URL: a tela pode ser compartilhada e o "voltar" do navegador funciona.
  const [params, setParams] = useSearchParams()
  const status = (params.get('status') ?? '') as WorkflowStatus | ''
  const definitionKey = params.get('definition') ?? ''
  const page = Number(params.get('page') ?? 0)
  const creating = params.get('new') === '1'

  const update = (changes: Record<string, string | null>) => {
    const next = new URLSearchParams(params)
    for (const [key, value] of Object.entries(changes)) {
      if (value) {
        next.set(key, value)
      } else {
        next.delete(key)
      }
    }
    setParams(next, { replace: true })
  }

  const workflows = useQuery({
    queryKey: ['workflows', 'list', { status, definitionKey, page }],
    queryFn: () => workflowsApi.list({ status, definitionKey: definitionKey.trim(), page }),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <PageHeader
        title="Workflows"
        description="Execuções concretas dos processos definidos."
        actions={
          <Button onClick={() => update({ new: '1' })} icon={<Plus aria-hidden className="size-4" />}>
            Nova solicitação
          </Button>
        }
      />

      <Card>
        <div className="grid gap-4 border-b border-slate-200 p-4 sm:grid-cols-3 dark:border-slate-800">
          <Field label="Situação">
            {({ id }) => (
              <Select id={id} value={status} onChange={(event) => update({ status: event.target.value || null, page: null })}>
                <option value="">Todas</option>
                {STATUSES.map((value) => (
                  <option key={value} value={value}>
                    {workflowStatusLabel[value]}
                  </option>
                ))}
              </Select>
            )}
          </Field>
          <Field label="Chave da definição">
            {({ id }) => (
              <Input
                id={id}
                placeholder="ex.: purchase-approval"
                value={definitionKey}
                onChange={(event) => update({ definition: event.target.value || null, page: null })}
              />
            )}
          </Field>
        </div>

        {workflows.isError && (
          <div className="p-4">
            <ErrorAlert error={workflows.error} />
          </div>
        )}
        {workflows.isPending && <LoadingBlock />}
        {workflows.data && workflows.data.content.length === 0 && (
          <EmptyState
            title="Nenhum workflow encontrado"
            description={status || definitionKey ? 'Ajuste os filtros para ver outras execuções.' : 'Crie a primeira solicitação.'}
          />
        )}
        {workflows.data && workflows.data.content.length > 0 && (
          <>
            <Table label="Workflows">
              <thead>
                <tr>
                  <Th>Workflow</Th>
                  <Th>Processo</Th>
                  <Th>Estado atual</Th>
                  <Th>Situação</Th>
                  <Th>Criado em</Th>
                  <Th>Atualizado em</Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {workflows.data.content.map((workflow) => (
                  <tr key={workflow.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                    <Td>
                      <Link
                        to={`/workflows/${workflow.id}`}
                        className="font-mono font-medium text-indigo-600 hover:underline dark:text-indigo-400"
                      >
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
                    <Td className="whitespace-nowrap text-slate-600 dark:text-slate-400">{formatDateTime(workflow.createdAt)}</Td>
                    <Td className="whitespace-nowrap text-slate-600 dark:text-slate-400">{formatDateTime(workflow.updatedAt)}</Td>
                  </tr>
                ))}
              </tbody>
            </Table>
            <Pagination
              page={page}
              totalPages={workflows.data.page.totalPages}
              totalElements={workflows.data.page.totalElements}
              onChange={(next) => update({ page: next > 0 ? String(next) : null })}
            />
          </>
        )}
      </Card>

      <CreateWorkflowDialog open={creating} onClose={() => update({ new: null })} />
    </>
  )
}
