import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router'
import { auditApi } from '../../api/endpoints'
import type { AuditOutcome } from '../../api/types'
import { OutcomeBadge } from '../../components/StatusBadge'
import { EmptyState, ErrorAlert, LoadingBlock } from '../../components/ui/Feedback'
import { Field, Input, Select } from '../../components/ui/Form'
import { Card, PageHeader, Table, Td, Th } from '../../components/ui/Layout'
import { Pagination } from '../../components/ui/Pagination'
import { auditOperations, formatDateTime, operationLabel } from '../../lib/format'

export function AuditPage() {
  const [params, setParams] = useSearchParams()
  const filters = {
    actorId: params.get('actor') ?? '',
    operation: params.get('operation') ?? '',
    resourceId: params.get('resource') ?? '',
    outcome: (params.get('outcome') ?? '') as AuditOutcome | '',
  }
  const page = Number(params.get('page') ?? 0)

  const update = (name: string, value: string) => {
    const next = new URLSearchParams(params)
    if (value) {
      next.set(name, value)
    } else {
      next.delete(name)
    }
    next.delete('page')
    setParams(next, { replace: true })
  }

  const records = useQuery({
    queryKey: ['audit', filters, page],
    queryFn: () =>
      auditApi.search({
        actorId: filters.actorId.trim(),
        operation: filters.operation,
        resourceId: filters.resourceId.trim(),
        outcome: filters.outcome,
        page,
      }),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <PageHeader title="Auditoria" description="Quem fez o quê, quando e com qual resultado — inclusive tentativas recusadas." />
      <Card>
        <div className="grid gap-4 border-b border-slate-200 p-4 sm:grid-cols-2 lg:grid-cols-4 dark:border-slate-800">
          <Field label="Usuário">
            {({ id }) => <Input id={id} value={filters.actorId} onChange={(event) => update('actor', event.target.value)} />}
          </Field>
          <Field label="Operação">
            {({ id }) => (
              <Select id={id} value={filters.operation} onChange={(event) => update('operation', event.target.value)}>
                <option value="">Todas</option>
                {auditOperations.map((operation) => (
                  <option key={operation} value={operation}>
                    {operationLabel(operation)}
                  </option>
                ))}
              </Select>
            )}
          </Field>
          <Field label="Recurso">
            {({ id }) => (
              <Input
                id={id}
                placeholder="id do workflow, chave…"
                value={filters.resourceId}
                onChange={(event) => update('resource', event.target.value)}
              />
            )}
          </Field>
          <Field label="Resultado">
            {({ id }) => (
              <Select id={id} value={filters.outcome} onChange={(event) => update('outcome', event.target.value)}>
                <option value="">Todos</option>
                <option value="SUCCESS">Sucesso</option>
                <option value="REJECTED">Recusado</option>
              </Select>
            )}
          </Field>
        </div>

        {records.isError && (
          <div className="p-4">
            <ErrorAlert error={records.error} />
          </div>
        )}
        {records.isPending && <LoadingBlock />}
        {records.data && records.data.content.length === 0 && <EmptyState title="Nenhum registro encontrado" />}
        {records.data && records.data.content.length > 0 && (
          <>
            <Table label="Registros de auditoria">
              <thead>
                <tr>
                  <Th>Quando</Th>
                  <Th>Usuário</Th>
                  <Th>Operação</Th>
                  <Th>Recurso</Th>
                  <Th>Resultado</Th>
                  <Th>Detalhe</Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {records.data.content.map((record) => (
                  <tr key={record.id}>
                    <Td className="whitespace-nowrap text-slate-600 dark:text-slate-400">{formatDateTime(record.occurredAt)}</Td>
                    <Td className="font-medium">{record.actorId}</Td>
                    <Td className="whitespace-nowrap">{operationLabel(record.operation)}</Td>
                    <Td>
                      <span className="text-xs text-slate-600 dark:text-slate-400">{record.resourceType}</span>
                      <p className="max-w-56 truncate font-mono text-xs" title={record.resourceId}>
                        {record.resourceId}
                      </p>
                    </Td>
                    <Td>
                      <OutcomeBadge outcome={record.outcome} />
                    </Td>
                    <Td className="max-w-md text-xs text-slate-600 dark:text-slate-400">{record.detail ?? '—'}</Td>
                  </tr>
                ))}
              </tbody>
            </Table>
            <Pagination
              page={page}
              totalPages={records.data.page.totalPages}
              totalElements={records.data.page.totalElements}
              onChange={(next) => {
                const nextParams = new URLSearchParams(params)
                if (next > 0) {
                  nextParams.set('page', String(next))
                } else {
                  nextParams.delete('page')
                }
                setParams(nextParams)
              }}
            />
          </>
        )}
      </Card>
    </>
  )
}
