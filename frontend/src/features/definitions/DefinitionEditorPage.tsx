import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Plus, Trash2, Wand2 } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { definitionsApi } from '../../api/endpoints'
import type { RuleOperator, StateType } from '../../api/types'
import { StateDiagram } from '../../components/StateDiagram'
import { Button } from '../../components/ui/Button'
import { ErrorAlert, LoadingBlock } from '../../components/ui/Feedback'
import { Field, Input, Select, Textarea } from '../../components/ui/Form'
import { Card, CardHeader, PageHeader } from '../../components/ui/Layout'
import { useToast } from '../../components/ui/Toast'
import { operatorLabel, stateTypeLabel } from '../../lib/format'
import {
  EMPTY_STRUCTURE,
  fromStructure,
  PURCHASE_APPROVAL_TEMPLATE,
  toStructure,
  type StructureForm,
  type TransitionRow,
} from './structure'

const STATE_TYPES: StateType[] = ['INITIAL', 'INTERMEDIATE', 'TERMINAL']
const OPERATORS = Object.keys(operatorLabel) as RuleOperator[]

function StructureEditor({ form, onChange }: { form: StructureForm; onChange: (form: StructureForm) => void }) {
  const stateNames = form.states.map((state) => state.name.trim()).filter(Boolean)
  const setTransition = (index: number, patch: Partial<TransitionRow>) =>
    onChange({ ...form, transitions: form.transitions.map((t, i) => (i === index ? { ...t, ...patch } : t)) })

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader
          title="Estados"
          description="Exatamente um inicial e ao menos um final. Use maiúsculas, ex.: PENDING_APPROVAL."
        />
        <div className="space-y-3 p-5">
          {form.states.map((state, index) => (
            <div key={index} className="flex gap-2">
              <Input
                aria-label={`Nome do estado ${index + 1}`}
                value={state.name}
                maxLength={64}
                className="font-mono"
                onChange={(event) =>
                  onChange({
                    ...form,
                    states: form.states.map((s, i) => (i === index ? { ...s, name: event.target.value } : s)),
                  })
                }
              />
              <Select
                aria-label={`Tipo do estado ${index + 1}`}
                value={state.type}
                className="max-w-44"
                onChange={(event) =>
                  onChange({
                    ...form,
                    states: form.states.map((s, i) => (i === index ? { ...s, type: event.target.value as StateType } : s)),
                  })
                }
              >
                {STATE_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {stateTypeLabel[type]}
                  </option>
                ))}
              </Select>
              <Button
                variant="ghost"
                className="h-10 shrink-0"
                aria-label={`Remover estado ${index + 1}`}
                onClick={() => onChange({ ...form, states: form.states.filter((_, i) => i !== index) })}
                icon={<Trash2 aria-hidden className="size-4" />}
              />
            </div>
          ))}
          <Button
            variant="secondary"
            size="sm"
            onClick={() => onChange({ ...form, states: [...form.states, { name: '', type: 'INTERMEDIATE' }] })}
            icon={<Plus aria-hidden className="size-4" />}
          >
            Adicionar estado
          </Button>
        </div>
      </Card>

      <Card>
        <CardHeader title="Transições" description="Cada ação leva de um estado a outro; papel e regras são opcionais." />
        <div className="space-y-4 p-5">
          {form.transitions.map((transition, index) => (
            <fieldset key={index} className="space-y-3 rounded-lg border border-slate-200 p-4 dark:border-slate-700">
              <legend className="px-1 text-sm font-medium">Transição {index + 1}</legend>
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="Ação">
                  {({ id }) => (
                    <Input
                      id={id}
                      className="font-mono"
                      maxLength={64}
                      value={transition.action}
                      onChange={(event) => setTransition(index, { action: event.target.value })}
                    />
                  )}
                </Field>
                <Field label="De">
                  {({ id }) => (
                    <Select id={id} value={transition.from} onChange={(event) => setTransition(index, { from: event.target.value })}>
                      <option value="">—</option>
                      {stateNames.map((name) => (
                        <option key={name} value={name}>
                          {name}
                        </option>
                      ))}
                    </Select>
                  )}
                </Field>
                <Field label="Para">
                  {({ id }) => (
                    <Select id={id} value={transition.to} onChange={(event) => setTransition(index, { to: event.target.value })}>
                      <option value="">—</option>
                      {stateNames.map((name) => (
                        <option key={name} value={name}>
                          {name}
                        </option>
                      ))}
                    </Select>
                  )}
                </Field>
                <Field label="Papel exigido">
                  {({ id }) => (
                    <Input
                      id={id}
                      placeholder="opcional, ex.: MANAGER"
                      maxLength={32}
                      value={transition.requiredRole}
                      onChange={(event) => setTransition(index, { requiredRole: event.target.value.toUpperCase() })}
                    />
                  )}
                </Field>
              </div>

              {transition.rules.map((rule, ruleIndex) => (
                <div key={ruleIndex} className="flex flex-wrap items-end gap-2 rounded-md bg-slate-50 p-2 dark:bg-slate-800/50">
                  <Input
                    aria-label={`Variável da regra ${ruleIndex + 1}`}
                    placeholder="variável"
                    className="max-w-40 font-mono"
                    value={rule.field}
                    onChange={(event) =>
                      setTransition(index, {
                        rules: transition.rules.map((r, i) => (i === ruleIndex ? { ...r, field: event.target.value } : r)),
                      })
                    }
                  />
                  <Select
                    aria-label={`Operador da regra ${ruleIndex + 1}`}
                    className="max-w-48"
                    value={rule.operator}
                    onChange={(event) =>
                      setTransition(index, {
                        rules: transition.rules.map((r, i) =>
                          i === ruleIndex ? { ...r, operator: event.target.value as RuleOperator } : r,
                        ),
                      })
                    }
                  >
                    {OPERATORS.map((operator) => (
                      <option key={operator} value={operator}>
                        {operatorLabel[operator]}
                      </option>
                    ))}
                  </Select>
                  {rule.operator !== 'EXISTS' && (
                    <Input
                      aria-label={`Valor da regra ${ruleIndex + 1}`}
                      placeholder="valor"
                      className="max-w-40"
                      value={rule.value}
                      onChange={(event) =>
                        setTransition(index, {
                          rules: transition.rules.map((r, i) => (i === ruleIndex ? { ...r, value: event.target.value } : r)),
                        })
                      }
                    />
                  )}
                  <Button
                    variant="ghost"
                    aria-label={`Remover regra ${ruleIndex + 1}`}
                    onClick={() => setTransition(index, { rules: transition.rules.filter((_, i) => i !== ruleIndex) })}
                    icon={<Trash2 aria-hidden className="size-4" />}
                  />
                </div>
              ))}

              <div className="flex flex-wrap gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() =>
                    setTransition(index, { rules: [...transition.rules, { field: '', operator: 'EQUALS', value: '' }] })
                  }
                  icon={<Plus aria-hidden className="size-4" />}
                >
                  Adicionar regra
                </Button>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => onChange({ ...form, transitions: form.transitions.filter((_, i) => i !== index) })}
                  icon={<Trash2 aria-hidden className="size-4" />}
                >
                  Remover transição
                </Button>
              </div>
            </fieldset>
          ))}
          <Button
            variant="secondary"
            size="sm"
            onClick={() =>
              onChange({
                ...form,
                transitions: [...form.transitions, { action: '', from: '', to: '', requiredRole: '', rules: [] }],
              })
            }
            icon={<Plus aria-hidden className="size-4" />}
          >
            Adicionar transição
          </Button>
        </div>
      </Card>
    </div>
  )
}

/** Criação de definição (com a versão 1) ou de nova versão de uma definição existente. */
export function DefinitionEditorPage() {
  const { key } = useParams()
  const creating = !key
  const navigate = useNavigate()
  const notify = useToast()
  const queryClient = useQueryClient()

  const [definitionKey, setDefinitionKey] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [form, setForm] = useState<StructureForm | null>(creating ? EMPTY_STRUCTURE : null)

  const definition = useQuery({ queryKey: ['definition', key], queryFn: () => definitionsApi.get(key!), enabled: !creating })
  const latest = definition.data ? Math.max(...definition.data.versions.map((v) => v.number)) : null
  const baseVersion = useQuery({
    queryKey: ['version', key, latest],
    queryFn: () => definitionsApi.version(key!, latest!),
    enabled: latest !== null,
  })
  // A nova versão parte da estrutura mais recente.
  if (!creating && form === null && baseVersion.data) {
    setForm(fromStructure(baseVersion.data))
  }

  const save = useMutation({
    mutationFn: async () => {
      const structure = toStructure(form!)
      if (creating) {
        const created = await definitionsApi.create({
          key: definitionKey.trim(),
          name: name.trim(),
          description: description.trim() || null,
          ...structure,
        })
        return { key: created.key, version: 1 }
      }
      const version = await definitionsApi.addVersion(key!, structure)
      return { key: key!, version: version.number }
    },
    onSuccess: (result) => {
      void queryClient.invalidateQueries({ queryKey: ['definitions'] })
      void queryClient.invalidateQueries({ queryKey: ['definition', result.key] })
      notify(creating ? 'Definição criada como rascunho. Ative-a para permitir execuções.' : `Versão ${result.version} criada.`)
      navigate(`/definitions/${result.key}?version=${result.version}`)
    },
  })

  if (!creating && (definition.isPending || form === null)) {
    return definition.isError ? <ErrorAlert error={definition.error} /> : <LoadingBlock />
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    save.mutate()
  }

  const preview = toStructure(form!)
  const previewValid = preview.states.length > 0 && preview.states.every((state) => state.name)

  return (
    <form onSubmit={submit} noValidate>
      <PageHeader
        eyebrow={
          <Link to={creating ? '/definitions' : `/definitions/${key}`} className="inline-flex items-center gap-1 hover:underline">
            <ArrowLeft aria-hidden className="size-4" /> {creating ? 'Definições' : definition.data?.name}
          </Link>
        }
        title={creating ? 'Nova definição' : 'Nova versão'}
        description={
          creating
            ? 'A primeira versão nasce como rascunho e só origina workflows depois de ativada.'
            : 'A nova versão não altera as anteriores nem os workflows em andamento.'
        }
        actions={
          <>
            {creating && (
              <Button variant="secondary" onClick={() => setForm(PURCHASE_APPROVAL_TEMPLATE)} icon={<Wand2 aria-hidden className="size-4" />}>
                Usar exemplo
              </Button>
            )}
            <Button type="submit" loading={save.isPending}>
              {creating ? 'Criar definição' : 'Criar versão'}
            </Button>
          </>
        }
      />

      {save.isError && (
        <div className="mb-6">
          <ErrorAlert error={save.error} />
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 2xl:grid-cols-5">
        <div className="space-y-6 2xl:col-span-3">
          {creating && (
            <Card>
              <CardHeader title="Identificação" />
              <div className="grid gap-4 p-5 sm:grid-cols-2">
                <Field label="Chave" hint="Identificador único em kebab-case, ex.: purchase-approval">
                  {({ id, describedBy }) => (
                    <Input
                      id={id}
                      aria-describedby={describedBy}
                      className="font-mono"
                      maxLength={64}
                      value={definitionKey}
                      onChange={(event) => setDefinitionKey(event.target.value.toLowerCase())}
                    />
                  )}
                </Field>
                <Field label="Nome">
                  {({ id }) => <Input id={id} maxLength={120} value={name} onChange={(event) => setName(event.target.value)} />}
                </Field>
                <Field label="Descrição" className="sm:col-span-2">
                  {({ id }) => (
                    <Textarea id={id} rows={2} maxLength={1000} value={description} onChange={(event) => setDescription(event.target.value)} />
                  )}
                </Field>
              </div>
            </Card>
          )}
          <StructureEditor form={form!} onChange={setForm} />
        </div>

        <div className="2xl:col-span-2 2xl:order-none -order-1">
          <Card className="2xl:sticky 2xl:top-6">
            <CardHeader title="Pré-visualização" description="A validação completa é feita pelo servidor ao salvar." />
            <div className="p-5">
              {previewValid ? (
                <StateDiagram states={preview.states} transitions={preview.transitions.filter((t) => t.from && t.to && t.action)} />
              ) : (
                <p className="text-sm text-slate-600 dark:text-slate-400">Dê nome a todos os estados para ver o diagrama.</p>
              )}
            </div>
          </Card>
        </div>
      </div>
    </form>
  )
}
