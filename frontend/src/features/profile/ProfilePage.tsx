import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { authApi } from '../../api/endpoints'
import { useAuth } from '../../auth/AuthProvider'
import { Button } from '../../components/ui/Button'
import { ErrorAlert } from '../../components/ui/Feedback'
import { Field, Input } from '../../components/ui/Form'
import { Badge, Card, CardHeader, DescriptionList, PageHeader } from '../../components/ui/Layout'

export function ProfilePage() {
  const { user, signOutLocally } = useAuth()
  const navigate = useNavigate()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const mismatch = confirmation.length > 0 && next !== confirmation

  const change = useMutation({
    mutationFn: () => authApi.changePassword(current, next),
    onSuccess: () => {
      // O backend encerra todas as sessões do usuário ao trocar a senha.
      signOutLocally()
      navigate('/login?changed=1', { replace: true })
    },
  })

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!mismatch) {
      change.mutate()
    }
  }

  if (!user) {
    return null
  }

  return (
    <>
      <PageHeader title="Meu perfil" />
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader title="Conta" />
          <div className="p-5">
            <DescriptionList
              items={[
                { label: 'Nome', value: user.displayName },
                { label: 'Usuário', value: <span className="font-mono">{user.username}</span> },
                {
                  label: 'Papéis',
                  value: (
                    <span className="flex flex-wrap gap-1">
                      {user.roles.map((role) => (
                        <Badge key={role}>{role}</Badge>
                      ))}
                    </span>
                  ),
                },
              ]}
            />
          </div>
        </Card>

        <Card>
          <CardHeader title="Trocar senha" description="Por segurança, todas as suas sessões serão encerradas." />
          <form onSubmit={submit} noValidate className="space-y-4 p-5">
            {change.isError && <ErrorAlert error={change.error} />}
            <Field label="Senha atual">
              {({ id }) => (
                <Input id={id} type="password" autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} />
              )}
            </Field>
            <Field label="Nova senha" hint="Mínimo de 12 caracteres. Frases longas são mais seguras e fáceis de lembrar.">
              {({ id, describedBy }) => (
                <Input
                  id={id}
                  aria-describedby={describedBy}
                  type="password"
                  autoComplete="new-password"
                  value={next}
                  onChange={(e) => setNext(e.target.value)}
                />
              )}
            </Field>
            <Field label="Confirme a nova senha" error={mismatch ? 'As senhas não conferem.' : undefined}>
              {({ id, describedBy, invalid }) => (
                <Input
                  id={id}
                  aria-describedby={describedBy}
                  aria-invalid={invalid}
                  type="password"
                  autoComplete="new-password"
                  value={confirmation}
                  onChange={(e) => setConfirmation(e.target.value)}
                />
              )}
            </Field>
            <Button type="submit" loading={change.isPending} disabled={!current || !next || mismatch || !confirmation}>
              Trocar senha
            </Button>
          </form>
        </Card>
      </div>
    </>
  )
}
