import { X } from 'lucide-react'
import { useEffect, useId, useRef, type FormEvent, type ReactNode } from 'react'

interface DialogProps {
  open: boolean
  onClose: () => void
  title: string
  description?: ReactNode
  children: ReactNode
  footer?: ReactNode
  onSubmit?: (event: FormEvent<HTMLFormElement>) => void
  wide?: boolean
}

/**
 * Diálogo modal sobre o elemento nativo {@code <dialog>}: o navegador cuida do foco preso no modal,
 * da tecla Esc e da camada de fundo.
 */
export function Dialog({ open, onClose, title, description, children, footer, onSubmit, wide = false }: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const titleId = useId()

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) {
      return
    }
    if (open && !dialog.open) {
      dialog.showModal()
    } else if (!open && dialog.open) {
      dialog.close()
    }
  }, [open])

  const content = (
    <>
      <div className="flex items-start justify-between gap-4 border-b border-slate-200 px-6 py-4 dark:border-slate-800">
        <div>
          <h2 id={titleId} className="text-lg font-semibold">
            {title}
          </h2>
          {description && <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">{description}</p>}
        </div>
        <button
          type="button"
          onClick={onClose}
          className="rounded-md p-1 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800"
          aria-label="Fechar"
        >
          <X aria-hidden className="size-5" />
        </button>
      </div>
      <div className="max-h-[70vh] space-y-4 overflow-y-auto px-6 py-5">{children}</div>
      {footer && (
        <div className="flex flex-col-reverse gap-2 border-t border-slate-200 px-6 py-4 sm:flex-row sm:justify-end dark:border-slate-800">
          {footer}
        </div>
      )}
    </>
  )

  return (
    <dialog
      ref={ref}
      aria-labelledby={titleId}
      onClose={onClose}
      onCancel={(event) => {
        event.preventDefault()
        onClose()
      }}
      className={`m-auto w-[calc(100%-2rem)] rounded-xl border border-slate-200 bg-white p-0 text-slate-900 shadow-xl dark:border-slate-800 dark:bg-slate-900 dark:text-slate-100 ${
        wide ? 'max-w-3xl' : 'max-w-lg'
      }`}
    >
      {open &&
        (onSubmit ? (
          <form
            noValidate
            onSubmit={(event) => {
              event.preventDefault()
              onSubmit(event)
            }}
          >
            {content}
          </form>
        ) : (
          content
        ))}
    </dialog>
  )
}
