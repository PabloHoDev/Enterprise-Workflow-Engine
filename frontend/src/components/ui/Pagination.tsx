import { ChevronLeft, ChevronRight } from 'lucide-react'
import { formatNumber } from '../../lib/format'
import { Button } from './Button'

export function Pagination({
  page,
  totalPages,
  totalElements,
  onChange,
}: {
  page: number
  totalPages: number
  totalElements: number
  onChange: (page: number) => void
}) {
  if (totalElements === 0) {
    return null
  }
  return (
    <nav
      aria-label="Paginação"
      className="flex items-center justify-between gap-4 border-t border-slate-200 px-4 py-3 text-sm dark:border-slate-800"
    >
      <p className="text-slate-600 dark:text-slate-400">
        Página {page + 1} de {Math.max(totalPages, 1)} · {formatNumber(totalElements)} registros
      </p>
      <div className="flex gap-2">
        <Button
          variant="secondary"
          size="sm"
          onClick={() => onChange(page - 1)}
          disabled={page === 0}
          icon={<ChevronLeft aria-hidden className="size-4" />}
        >
          Anterior
        </Button>
        <Button variant="secondary" size="sm" onClick={() => onChange(page + 1)} disabled={page + 1 >= totalPages}>
          Próxima
          <ChevronRight aria-hidden className="size-4" />
        </Button>
      </div>
    </nav>
  )
}
