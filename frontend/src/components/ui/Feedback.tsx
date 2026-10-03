import { AlertTriangle, CheckCircle2, Inbox, Loader2 } from 'lucide-react'
import type { ReactNode } from 'react'
import { errorDetails, errorMessage } from '../../lib/format'

export function Spinner({ small = false, label }: { small?: boolean; label?: string }) {
  return (
    <span role={label ? 'status' : undefined} className="inline-flex items-center gap-2">
      <Loader2 aria-hidden className={`animate-spin ${small ? 'size-4' : 'size-5'}`} />
      {label && <span className="text-sm text-slate-600 dark:text-slate-400">{label}</span>}
    </span>
  )
}

export function LoadingBlock({ label = 'Carregando…' }: { label?: string }) {
  return (
    <div className="flex justify-center py-12 text-slate-600 dark:text-slate-400">
      <Spinner label={label} />
    </div>
  )
}

export function Skeleton({ className = '' }: { className?: string }) {
  return <div aria-hidden className={`animate-pulse rounded-md bg-slate-200 dark:bg-slate-800 ${className}`} />
}

/** Erro de uma operação: mensagem em português e, quando houver, os detalhes devolvidos pela API. */
export function ErrorAlert({ error, title }: { error: unknown; title?: string }) {
  const details = errorDetails(error)
  return (
    <div
      role="alert"
      className="flex gap-3 rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-900 dark:border-rose-900/60 dark:bg-rose-950/40 dark:text-rose-100"
    >
      <AlertTriangle aria-hidden className="mt-0.5 size-5 shrink-0" />
      <div className="space-y-1">
        <p className="font-medium">{title ?? errorMessage(error)}</p>
        {title && <p>{errorMessage(error)}</p>}
        {details.length > 0 && (
          <ul className="list-inside list-disc text-rose-800 dark:text-rose-200">
            {details.map((detail) => (
              <li key={detail} className="font-mono text-xs">
                {detail}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}

export function SuccessAlert({ children }: { children: ReactNode }) {
  return (
    <div
      role="status"
      className="flex gap-3 rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900 dark:border-emerald-900/60 dark:bg-emerald-950/40 dark:text-emerald-100"
    >
      <CheckCircle2 aria-hidden className="mt-0.5 size-5 shrink-0" />
      <div>{children}</div>
    </div>
  )
}

export function EmptyState({ title, description, action }: { title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 px-6 py-14 text-center">
      <span className="rounded-full bg-slate-100 p-3 text-slate-500 dark:bg-slate-800 dark:text-slate-400">
        <Inbox aria-hidden className="size-6" />
      </span>
      <div>
        <p className="font-medium">{title}</p>
        {description && <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{description}</p>}
      </div>
      {action}
    </div>
  )
}
