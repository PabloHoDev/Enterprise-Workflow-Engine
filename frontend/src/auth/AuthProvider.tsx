import { useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError, ensureCsrfToken, UNAUTHORIZED_EVENT } from '../api/client'
import { authApi } from '../api/endpoints'
import type { Me } from '../api/types'

interface AuthValue {
  user: Me | null
  loading: boolean
  hasRole: (role: string) => boolean
  login: (username: string, password: string) => Promise<Me>
  logout: () => Promise<void>
  /** Esquece a sessão no cliente quando o servidor já a encerrou (ex.: troca de senha). */
  signOutLocally: () => void
}

const AuthContext = createContext<AuthValue | null>(null)

/**
 * Estado da sessão do console. A sessão vive no servidor (cookie HttpOnly): aqui só se guarda quem é o
 * usuário, para exibir a interface certa. A autorização real é sempre feita pela API.
 *
 * <p>O usuário fica em estado do React (e não no cache de consultas) para que login, logout e a navegação
 * seguinte sejam aplicados na mesma renderização, sem uma tela intermediária com o usuário anterior.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<Me | null>(null)
  const [loading, setLoading] = useState(true)

  const signOutLocally = useCallback(() => {
    queryClient.clear()
    setUser(null)
  }, [queryClient])

  useEffect(() => {
    let active = true
    authApi
      .me()
      .then((me) => active && setUser(me))
      .catch((error: unknown) => {
        if (active && !(error instanceof ApiError && error.status === 401)) {
          console.warn('Could not load the current session', error)
        }
      })
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    window.addEventListener(UNAUTHORIZED_EVENT, signOutLocally)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, signOutLocally)
  }, [signOutLocally])

  const login = useCallback(
    async (username: string, password: string) => {
      const me = await authApi.login(username, password)
      // O backend troca o token CSRF a cada login.
      await ensureCsrfToken(true)
      queryClient.clear()
      setUser(me)
      return me
    },
    [queryClient],
  )

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      signOutLocally()
    }
  }, [signOutLocally])

  const value = useMemo<AuthValue>(
    () => ({
      user,
      loading,
      hasRole: (role: string) => Boolean(user?.roles.includes(role)),
      login,
      logout,
      signOutLocally,
    }),
    [user, loading, login, logout, signOutLocally],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}

/** Aceita apenas caminhos internos como destino após o login (evita redirecionamento aberto). */
export function safeRedirect(target: string | null): string {
  if (!target || !target.startsWith('/') || target.startsWith('//') || target.startsWith('/\\')) {
    return '/dashboard'
  }
  return target
}
