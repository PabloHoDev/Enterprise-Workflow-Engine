import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CopyPlus, Power, PowerOff } from 'lucide-react'
import { Link, useParams, useSearchParams } from 'react-router'
import { definitionsApi } from '../../api/endpoints'
import type { Definition } from '../../api/types'
import { useAuth } from '../../auth/AuthProvider'
import { StateDiagram } from '../../components/StateDiagram'
import { VersionStatusBadge } from '../../components/StatusBadge'
import { Button, ButtonLink } from '../../components/ui/Button'
import { ErrorAlert, LoadingBlock } from '../../components/ui/Feedback'
import { Badge, Card, CardHeader, PageHeader, Table, Td, Th } from '../../components/ui/Layout'
import { useToast } from '../../components/ui/Toast'
import { describeRule, formatDateTime, stateTypeLabel } from '../../lib/format'

function defaultVersion(definition: Definition): number {
  return definition.activeVersion ?? Math.max(...definition.versions.map((version) => version.number))
}

export function DefinitionDetailPage() {
  const { key = '' } = useParams()
  const [params, setParams] = useSearchParams()
  const { hasRole } = useAuth()
  const isAdmin = hasRole('ADMIN')
  const queryClient = useQueryClient()
  const notify = useToast()

  const definition = useQuery({ queryKey: ['definition', key], queryFn: () => definitionsApi.get(key) })
  const selected = definition.data ? Number(params.get('version') ?? defaultVersion(definition.data)) : null
  const version = useQuery({
    queryKey: ['version', key, selected],
    queryFn: () => definitionsApi.version(key, selected!),
    enabled: selected !== null,
  })

  const changeStatus = useMutation({
    mutationFn: ({ number, activate }: { number: number; activate: boolean }) =>
      activate ? definitionsApi.activate(key, number) : definitionsApi.deactivate(key, number),
    onSuccess: (updated, { activate }) => {
      void queryClient.invalidateQueries({ queryKey: ['definition', key] })
      void queryClient.invalidateQueries({ queryKey: ['version', key] })
      void queryClient.invalidateQueries({ queryKey: ['definitions'] })
      notify(activate ? `Versão ${updated.number} ativada.` : `Versão ${updated.number} desativada.`)
    },
  })

  if (definition.isPending) {
    return <LoadingBlock />
  }
  if (definition.isError) {
    return <ErrorAlert error={definition.error} />
  }
  const data = definition.data

  return (
    <>
      <PageHeader
        eyebrow={
          <Link to="/definitions" className="inline-flex items-center gap-1 hover:underline">
            <ArrowLeft aria-hidden className="size-4" /> Definições
          </Link>
        }
        title={data.name}
        description={
          <>
            <span className="font-mono">{data.key}</span>
            {data.description && <> · {data.description}</>}
          </>
        }
        actions={
          isAdmin && (
            <ButtonLink to={`/definitions/${key}/versions/new`} icon={<CopyPlus aria-hidden className="size-4" />}>
              Nova versão
            </ButtonLink>
          )
        }
      />
      {changeStatus.isError && (
        <div className="mb-6">
          <ErrorAlert error={changeStatus.error} />
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <Card className="xl:col-span-1">
          <CardHeader title="Versões" description="Só uma versão fica ativa; execuções em andamento não mudam de versão." />
          <ul className="divide-y divide-slate-100 dark:divide-slate-800">
            {[...data.versions].reverse().map((item) => (
              <li key={item.number} className={`px-5 py-4 ${item.number === selected ? 'bg-indigo-50/60 dark:bg-indigo-950/30' : ''}`}>
                <div className="flex items-center justify-between gap-3">
                  <button
                    type="button"
                    onClick={() => setParams({ version: String(item.number) })}
                    className="text-left font-medium hover:underline"
                    aria-current={item.number === selected ? 'true' : undefined}
                  >
                    Versão {item.number}
                  </button>
                  <VersionStatusBadge status={item.status} />
                </div>
                <p className="mt-1 text-xs text-slate-600 dark:text-slate-400">Criada em {formatDateTime(item.createdAt)}</p>
                {isAdmin && (
                  <div className="mt-3">
                    {item.status === 'ACTIVE' ? (
                      <Button
                        size="sm"
                        variant="secondary"
                        loading={changeStatus.isPending && changeStatus.variables?.number === item.number}
                        onClick={() => changeStatus.mutate({ number: item.number, activate: false })}
                        icon={<PowerOff aria-hidden className="size-4" />}
                      >
                        Desativar
                      </Button>
                    ) : (
                      <Button
                        size="sm"
                        loading={changeStatus.isPending && changeStatus.variables?.number === item.number}
                        onClick={() => changeStatus.mutate({ number: item.number, activate: true })}
                        icon={<Power aria-hidden className="size-4" />}
                      >
                        Ativar
                      </Button>
                    )}
                  </div>
                )}
              </li>
            ))}
          </ul>
        </Card>

        <div className="space-y-6 xl:col-span-2">
          {version.isError && <ErrorAlert error={version.error} />}
          {version.isPending && <LoadingBlock />}
          {version.data && (
            <>
              <Card>
                <CardHeader
                  title={`Fluxo da versão ${version.data.number}`}
                  actions={<VersionStatusBadge status={version.data.status} />}
                />
                <div className="p-5">
                  <StateDiagram states={version.data.states} transitions={version.data.transitions} />
                </div>
              </Card>
              <Card>
                <CardHeader title="Estados" />
                <div className="flex flex-wrap gap-2 p-5">
                  {version.data.states.map((state) => (
                    <Badge key={state.name} tone={state.type === 'INITIAL' ? 'sky' : state.type === 'TERMINAL' ? 'emerald' : 'slate'}>
                      <span className="font-mono">{state.name}</span>
                      <span className="ml-1.5 font-normal">· {stateTypeLabel[state.type]}</span>
                    </Badge>
                  ))}
                </div>
              </Card>
              <Card>
                <CardHeader title="Transições" />
                <Table label="Transições">
                  <thead>
                    <tr>
                      <Th>Ação</Th>
                      <Th>De → Para</Th>
                      <Th>Papel exigido</Th>
                      <Th>Regras</Th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {version.data.transitions.map((transition) => (
                      <tr key={`${transition.from}-${transition.action}`}>
                        <Td className="font-mono font-medium">{transition.action}</Td>
                        <Td className="font-mono text-xs whitespace-nowrap">
                          {transition.from} → {transition.to}
                        </Td>
                        <Td>{transition.requiredRole ? <Badge tone="indigo">{transition.requiredRole}</Badge> : '—'}</Td>
                        <Td className="font-mono text-xs">
                          {transition.rules.length === 0 ? '—' : transition.rules.map(describeRule).join(' e ')}
                        </Td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              </Card>
            </>
          )}
        </div>
      </div>
    </>
  )
}
