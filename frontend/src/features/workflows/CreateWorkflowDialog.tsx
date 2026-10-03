import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { definitionsApi, workflowsApi } from '../../api/endpoints'
import { VariablesEditor } from '../../components/VariablesEditor'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { ErrorAlert, Spinner } from '../../components/ui/Feedback'
import { Field, Select } from '../../components/ui/Form'
import { useToast } from '../../components/ui/Toast'
import { rowsToVariables, type VariableRow } from '../../lib/variables'

export function CreateWorkflowDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const navigate = useNavigate()
  const notify = useToast()
  const queryClient = useQueryClient()
  const [definitionKey, setDefinitionKey] = useState('')
  const [rows, setRows] = useState<VariableRow[]>([{ key: 'amount', value: '' }])

  const definitions = useQuery({
    queryKey: ['definitions', 'all'],
    queryFn: () => definitionsApi.list(0, 100),
    enabled: open,
  })
  const executable = definitions.data?.content.filter((definition) => definition.activeVersion !== null) ?? []

  const create = useMutation({
    mutationFn: () => workflowsApi.create(definitionKey, rowsToVariables(rows)),
    onSuccess: (workflow) => {
      void queryClient.invalidateQueries({ queryKey: ['workflows'] })
      notify('Workflow criado.')
      onClose()
      navigate(`/workflows/${workflow.id}`)
    },
  })

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Nova solicitação"
      description="O workflow usa a versão ativa da definição escolhida e permanece nela até o fim."
      onSubmit={() => create.mutate()}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" loading={create.isPending} disabled={!definitionKey}>
            Criar workflow
          </Button>
        </>
      }
    >
      {create.isError && <ErrorAlert error={create.error} />}
      {definitions.isPending ? (
        <Spinner label="Carregando definições…" />
      ) : (
        <Field
          label="Processo"
          hint={executable.length === 0 ? 'Nenhuma definição com versão ativa. Peça a um administrador para ativar uma.' : undefined}
        >
          {({ id, describedBy }) => (
            <Select
              id={id}
              aria-describedby={describedBy}
              value={definitionKey}
              required
              onChange={(event) => setDefinitionKey(event.target.value)}
            >
              <option value="">Selecione…</option>
              {executable.map((definition) => (
                <option key={definition.key} value={definition.key}>
                  {definition.name} (v{definition.activeVersion})
                </option>
              ))}
            </Select>
          )}
        </Field>
      )}
      <VariablesEditor rows={rows} onChange={setRows} />
    </Dialog>
  )
}
