import { ShieldAlert } from 'lucide-react'
import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { ButtonLink } from '../components/ui/Button'
import { LoadingBlock } from '../components/ui/Feedback'
import { useAuth } from './AuthProvider'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) {
    return (
      <div className="grid min-h-screen place-items-center">
        <LoadingBlock label="Verificando sua sessão…" />
      </div>
    )
  }
  if (!user) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  return children
}

/** Esconde telas de quem não tem o papel. A proteção efetiva está na API, que recusa a operação com 403. */
export function RequireRole({ role, children }: { role: string; children: ReactNode }) {
  const { hasRole } = useAuth()
  if (!hasRole(role)) {
    return (
      <div className="flex flex-col items-center gap-4 py-20 text-center">
        <ShieldAlert aria-hidden className="size-10 text-amber-500" />
        <div>
          <h1 className="text-xl font-semibold">Acesso restrito</h1>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
            Esta área é exclusiva para administradores.
          </p>
        </div>
        <ButtonLink to="/dashboard">Voltar ao painel</ButtonLink>
      </div>
    )
  }
  return children
}
