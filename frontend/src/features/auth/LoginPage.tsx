import { LockKeyhole } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router'
import { safeRedirect, useAuth } from '../../auth/AuthProvider'
import { Button } from '../../components/ui/Button'
import { ErrorAlert, SuccessAlert } from '../../components/ui/Feedback'
import { Field, Input } from '../../components/ui/Form'
import { ApiError } from '../../api/client'

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)
  const redirect = safeRedirect(params.get('redirect'))

  if (user) {
    return <Navigate to={redirect} replace />
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      await login(username.trim(), password)
      navigate(redirect, { replace: true })
    } catch (caught) {
      setError(caught)
      setPassword('')
    } finally {
      setSubmitting(false)
    }
  }

  const invalidCredentials = error instanceof ApiError && error.status === 401

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <section className="hidden flex-col justify-between bg-indigo-700 p-12 text-indigo-50 lg:flex">
        <div className="flex items-center gap-3">
          <img src="/favicon.svg" alt="" className="size-10" />
          <span className="text-lg font-semibold text-white">Enterprise Workflow Engine</span>
        </div>
        <div className="max-w-md space-y-4">
          <h2 className="text-3xl font-semibold tracking-tight text-white">
            Processos corporativos executáveis, versionados e auditáveis.
          </h2>
          <p className="text-indigo-100">
            Modele aprovações e solicitações, acompanhe cada execução e saiba sempre quem fez o quê.
          </p>
        </div>
        <p className="text-sm text-indigo-200">Sessão protegida: cookie HttpOnly, CSRF e bloqueio por tentativas.</p>
      </section>

      <main className="flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-sm space-y-8">
          <div className="space-y-2 text-center lg:text-left">
            <span className="inline-flex rounded-full bg-indigo-50 p-3 text-indigo-600 dark:bg-indigo-950 dark:text-indigo-300">
              <LockKeyhole aria-hidden className="size-6" />
            </span>
            <h1 className="text-2xl font-semibold tracking-tight">Entrar no console</h1>
            <p className="text-sm text-slate-600 dark:text-slate-400">Use as credenciais fornecidas pelo administrador.</p>
          </div>

          <form onSubmit={submit} className="space-y-5" noValidate>
            {params.get('changed') === '1' && error === null && (
              <SuccessAlert>Senha alterada. Entre novamente com a nova senha.</SuccessAlert>
            )}
            {error !== null && (
              <ErrorAlert
                error={error}
                title={invalidCredentials ? 'Usuário ou senha inválidos.' : undefined}
              />
            )}
            <Field label="Usuário">
              {({ id, describedBy }) => (
                <Input
                  id={id}
                  aria-describedby={describedBy}
                  autoComplete="username"
                  autoCapitalize="none"
                  spellCheck={false}
                  required
                  autoFocus
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                />
              )}
            </Field>
            <Field label="Senha">
              {({ id, describedBy }) => (
                <Input
                  id={id}
                  aria-describedby={describedBy}
                  type="password"
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              )}
            </Field>
            <Button type="submit" className="w-full" loading={submitting} disabled={!username || !password}>
              Entrar
            </Button>
          </form>
        </div>
      </main>
    </div>
  )
}
