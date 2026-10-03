import { Plus, Trash2 } from 'lucide-react'
import type { VariableRow } from '../lib/variables'
import { Button } from './ui/Button'
import { Input } from './ui/Form'

/** Editor de variáveis do Workflow em pares chave/valor. Números e true/false são enviados com o tipo certo. */
export function VariablesEditor({ rows, onChange }: { rows: VariableRow[]; onChange: (rows: VariableRow[]) => void }) {
  const update = (index: number, patch: Partial<VariableRow>) =>
    onChange(rows.map((row, i) => (i === index ? { ...row, ...patch } : row)))

  return (
    <fieldset className="space-y-2">
      <legend className="text-sm font-medium text-slate-700 dark:text-slate-300">Variáveis</legend>
      <p className="text-xs text-slate-500 dark:text-slate-400">
        Dados da execução usados pelas regras das transições (ex.: <code>amount</code> = <code>15000</code>).
      </p>
      {rows.map((row, index) => (
        <div key={index} className="flex gap-2">
          <Input
            aria-label={`Nome da variável ${index + 1}`}
            placeholder="nome"
            value={row.key}
            maxLength={64}
            onChange={(event) => update(index, { key: event.target.value })}
          />
          <Input
            aria-label={`Valor da variável ${index + 1}`}
            placeholder="valor"
            value={row.value}
            maxLength={500}
            onChange={(event) => update(index, { value: event.target.value })}
          />
          <Button
            variant="ghost"
            size="sm"
            className="h-10 shrink-0"
            aria-label={`Remover variável ${index + 1}`}
            onClick={() => onChange(rows.filter((_, i) => i !== index))}
            icon={<Trash2 aria-hidden className="size-4" />}
          />
        </div>
      ))}
      <Button
        variant="secondary"
        size="sm"
        onClick={() => onChange([...rows, { key: '', value: '' }])}
        icon={<Plus aria-hidden className="size-4" />}
      >
        Adicionar variável
      </Button>
    </fieldset>
  )
}
