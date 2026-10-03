import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { Link, useSearchParams } from 'react-router'
import { definitionsApi } from '../../api/endpoints'
import { useAuth } from '../../auth/AuthProvider'
import { ButtonLink } from '../../components/ui/Button'
import { EmptyState, ErrorAlert, LoadingBlock } from '../../components/ui/Feedback'
import { Badge, Card, PageHeader, Table, Td, Th } from '../../components/ui/Layout'
import { Pagination } from '../../components/ui/Pagination'
import { formatDateTime } from '../../lib/format'

export function DefinitionListPage() {
  const { hasRole } = useAuth()
  const [params, setParams] = useSearchParams()
  const page = Number(params.get('page') ?? 0)
  const definitions = useQuery({
    queryKey: ['definitions', 'list', page],
    queryFn: () => definitionsApi.list(page),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <PageHeader
        title="Definições de workflow"
        description="Modelos versionados dos processos. Cada alteração gera uma nova versão."
        actions={
          hasRole('ADMIN') && (
            <ButtonLink to="/definitions/new" variant="primary" icon={<Plus aria-hidden className="size-4" />}>
              Nova definição
            </ButtonLink>
          )
        }
      />
      <Card>
        {definitions.isError && (
          <div className="p-4">
            <ErrorAlert error={definitions.error} />
          </div>
        )}
        {definitions.isPending && <LoadingBlock />}
        {definitions.data && definitions.data.content.length === 0 && (
          <EmptyState
            title="Nenhuma definição cadastrada"
            description={hasRole('ADMIN') ? 'Comece modelando o primeiro processo.' : 'Peça a um administrador para cadastrar um processo.'}
          />
        )}
        {definitions.data && definitions.data.content.length > 0 && (
          <>
            <Table label="Definições">
              <thead>
                <tr>
                  <Th>Processo</Th>
                  <Th>Chave</Th>
                  <Th>Versão ativa</Th>
                  <Th>Atualizado em</Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {definitions.data.content.map((definition) => (
                  <tr key={definition.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50">
                    <Td>
                      <Link
                        to={`/definitions/${definition.key}`}
                        className="font-medium text-indigo-600 hover:underline dark:text-indigo-400"
                      >
                        {definition.name}
                      </Link>
                      {definition.description && (
                        <p className="mt-0.5 line-clamp-1 text-xs text-slate-500 dark:text-slate-400">{definition.description}</p>
                      )}
                    </Td>
                    <Td className="font-mono text-xs">{definition.key}</Td>
                    <Td>
                      {definition.activeVersion ? (
                        <Badge tone="emerald">v{definition.activeVersion}</Badge>
                      ) : (
                        <Badge tone="amber">Sem versão ativa</Badge>
                      )}
                    </Td>
                    <Td className="whitespace-nowrap text-slate-600 dark:text-slate-400">{formatDateTime(definition.updatedAt)}</Td>
                  </tr>
                ))}
              </tbody>
            </Table>
            <Pagination
              page={page}
              totalPages={definitions.data.page.totalPages}
              totalElements={definitions.data.page.totalElements}
              onChange={(next) => setParams(next > 0 ? { page: String(next) } : {})}
            />
          </>
        )}
      </Card>
    </>
  )
}
