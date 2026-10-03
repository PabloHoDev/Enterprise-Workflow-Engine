import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { KeyRound, LockOpen, Pencil, UserPlus } from 'lucide-react'
import { useState } from 'react'
import { usersApi } from '../../api/endpoints'
import type { User } from '../../api/types'
import { Button } from '../../components/ui/Button'
import { Dialog } from '../../components/ui/Dialog'
import { EmptyState, ErrorAlert, LoadingBlock } from '../../components/ui/Feedback'
import { Checkbox, Field, Input } from '../../components/ui/Form'
import { Badge, Card, PageHeader, Table, Td, Th } from '../../components/ui/Layout'
import { useToast } from '../../components/ui/Toast'
import { formatDateTime, formatRelative } from '../../lib/format'

const KNOWN_ROLES = ['ADMIN', 'USER', 'MANAGER', 'FINANCE']

function parseRoles(text: string): string[] {
  return [...new Set(text.split(/[\s,]+/).map((role) => role.trim().toUpperCase()).filter(Boolean))]
}

function RolesInput({ roles, onChange }: { roles: string[]; onChange: (roles: string[]) => void }) {
  const extra = roles.filter((role) => !KNOWN_ROLES.includes(role))
  return (
    <fieldset className="space-y-2">
      <legend className="text-sm font-medium text-slate-700 dark:text-slate-300">Papéis</legend>
      <div className="flex flex-wrap gap-4">
        {KNOWN_ROLES.map((role) => (
          <Checkbox
            key={role}
            label={role}
            checked={roles.includes(role)}
            onChange={(event) => onChange(event.target.checked ? [...roles, role] : roles.filter((r) => r !== role))}
          />
        ))}
      </div>
      <Field label="Outros papéis" hint="Papéis exigidos por transições, separados por vírgula (ex.: LEGAL, HR).">
        {({ id, describedBy }) => (
          <Input
            id={id}
            aria-describedby={describedBy}
            defaultValue={extra.join(', ')}
            onBlur={(event) => onChange([...roles.filter((role) => KNOWN_ROLES.includes(role)), ...parseRoles(event.target.value)])}
          />
        )}
      </Field>
    </fieldset>
  )
}

function UserDialog({ user, open, onClose }: { user: User | null; open: boolean; onClose: () => void }) {
  const editing = user !== null
  const [username, setUsername] = useState('')
  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [password, setPassword] = useState('')
  const [roles, setRoles] = useState<string[]>(user?.roles ?? ['USER'])
  const [enabled, setEnabled] = useState(user?.enabled ?? true)
  const queryClient = useQueryClient()
  const notify = useToast()

  const save = useMutation({
    mutationFn: () =>
      editing
        ? usersApi.update(user.username, { displayName, roles, enabled })
        : usersApi.create({ username: username.trim(), displayName, password, roles }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['users'] })
      notify(editing ? 'Usuário atualizado. As sessões abertas foram encerradas se o acesso mudou.' : 'Usuário criado.')
      onClose()
    },
  })

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={editing ? `Editar ${user.username}` : 'Novo usuário'}
      onSubmit={() => save.mutate()}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" loading={save.isPending} disabled={roles.length === 0}>
            Salvar
          </Button>
        </>
      }
    >
      {save.isError && <ErrorAlert error={save.error} />}
      {!editing && (
        <Field label="Usuário" hint="3 a 64 caracteres: letras minúsculas, números, ponto, hífen ou sublinhado.">
          {({ id, describedBy }) => (
            <Input
              id={id}
              aria-describedby={describedBy}
              autoComplete="off"
              value={username}
              onChange={(event) => setUsername(event.target.value.toLowerCase())}
            />
          )}
        </Field>
      )}
      <Field label="Nome de exibição">
        {({ id }) => <Input id={id} maxLength={120} value={displayName} onChange={(event) => setDisplayName(event.target.value)} />}
      </Field>
      {!editing && (
        <Field label="Senha inicial" hint="Mínimo de 12 caracteres, sem conter o nome de usuário.">
          {({ id, describedBy }) => (
            <Input
              id={id}
              aria-describedby={describedBy}
              type="password"
              autoComplete="new-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          )}
        </Field>
      )}
      <RolesInput roles={roles} onChange={setRoles} />
      {editing && <Checkbox label="Conta ativa" checked={enabled} onChange={(event) => setEnabled(event.target.checked)} />}
    </Dialog>
  )
}

function ResetPasswordDialog({ user, onClose }: { user: User | null; onClose: () => void }) {
  const [password, setPassword] = useState('')
  const notify = useToast()
  const reset = useMutation({
    mutationFn: () => usersApi.resetPassword(user!.username, password),
    onSuccess: () => {
      notify('Senha redefinida. As sessões do usuário foram encerradas.')
      setPassword('')
      onClose()
    },
  })
  return (
    <Dialog
      open={user !== null}
      onClose={onClose}
      title={`Redefinir senha de ${user?.username ?? ''}`}
      description="Informe a nova senha ao usuário por um canal seguro."
      onSubmit={() => reset.mutate()}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" loading={reset.isPending} disabled={password.length === 0}>
            Redefinir
          </Button>
        </>
      }
    >
      {reset.isError && <ErrorAlert error={reset.error} />}
      <Field label="Nova senha" hint="Mínimo de 12 caracteres.">
        {({ id, describedBy }) => (
          <Input
            id={id}
            aria-describedby={describedBy}
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        )}
      </Field>
    </Dialog>
  )
}

export function UsersPage() {
  const queryClient = useQueryClient()
  const notify = useToast()
  const [editing, setEditing] = useState<User | null | undefined>(undefined)
  const [resetting, setResetting] = useState<User | null>(null)
  const users = useQuery({ queryKey: ['users'], queryFn: () => usersApi.list() })
  const unlock = useMutation({
    mutationFn: (username: string) => usersApi.unlock(username),
    onSuccess: (user) => {
      void queryClient.invalidateQueries({ queryKey: ['users'] })
      notify(`${user.username} desbloqueado.`)
    },
  })

  return (
    <>
      <PageHeader
        title="Usuários"
        description="Contas, papéis e situação de acesso. Alterações de acesso encerram as sessões abertas do usuário."
        actions={
          <Button onClick={() => setEditing(null)} icon={<UserPlus aria-hidden className="size-4" />}>
            Novo usuário
          </Button>
        }
      />
      {unlock.isError && (
        <div className="mb-4">
          <ErrorAlert error={unlock.error} />
        </div>
      )}
      <Card>
        {users.isError && (
          <div className="p-4">
            <ErrorAlert error={users.error} />
          </div>
        )}
        {users.isPending && <LoadingBlock />}
        {users.data && users.data.content.length === 0 && <EmptyState title="Nenhum usuário" />}
        {users.data && users.data.content.length > 0 && (
          <Table label="Usuários">
            <thead>
              <tr>
                <Th>Usuário</Th>
                <Th>Papéis</Th>
                <Th>Situação</Th>
                <Th>Último acesso</Th>
                <Th>
                  <span className="sr-only">Ações</span>
                </Th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {users.data.content.map((user) => (
                <tr key={user.id}>
                  <Td>
                    <p className="font-medium">{user.displayName}</p>
                    <p className="font-mono text-xs text-slate-600 dark:text-slate-400">{user.username}</p>
                  </Td>
                  <Td>
                    <div className="flex flex-wrap gap-1">
                      {user.roles.map((role) => (
                        <Badge key={role} tone={role === 'ADMIN' ? 'indigo' : 'slate'}>
                          {role}
                        </Badge>
                      ))}
                    </div>
                  </Td>
                  <Td>
                    {user.locked ? (
                      <Badge tone="rose">Bloqueado até {formatDateTime(user.lockedUntil)}</Badge>
                    ) : user.enabled ? (
                      <Badge tone="emerald">Ativo</Badge>
                    ) : (
                      <Badge tone="slate">Desativado</Badge>
                    )}
                  </Td>
                  <Td className="whitespace-nowrap text-slate-600 dark:text-slate-400">{formatRelative(user.lastLoginAt)}</Td>
                  <Td>
                    <div className="flex justify-end gap-1">
                      {user.locked && (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => unlock.mutate(user.username)}
                          icon={<LockOpen aria-hidden className="size-4" />}
                        >
                          Desbloquear
                        </Button>
                      )}
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setResetting(user)}
                        aria-label={`Redefinir senha de ${user.username}`}
                        icon={<KeyRound aria-hidden className="size-4" />}
                      />
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setEditing(user)}
                        aria-label={`Editar ${user.username}`}
                        icon={<Pencil aria-hidden className="size-4" />}
                      />
                    </div>
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </Card>
      {editing !== undefined && (
        <UserDialog key={editing?.username ?? 'new'} user={editing} open onClose={() => setEditing(undefined)} />
      )}
      <ResetPasswordDialog user={resetting} onClose={() => setResetting(null)} />
    </>
  )
}
