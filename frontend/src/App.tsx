import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Compass } from 'lucide-react'
import { createBrowserRouter, Navigate, RouterProvider } from 'react-router'
import { ApiError } from './api/client'
import { AuthProvider } from './auth/AuthProvider'
import { RequireAuth, RequireRole } from './auth/guards'
import { AppShell } from './components/layout/AppShell'
import { ButtonLink } from './components/ui/Button'
import { ToastProvider } from './components/ui/Toast'
import { AuditPage } from './features/audit/AuditPage'
import { LoginPage } from './features/auth/LoginPage'
import { DashboardPage } from './features/dashboard/DashboardPage'
import { DefinitionDetailPage } from './features/definitions/DefinitionDetailPage'
import { DefinitionEditorPage } from './features/definitions/DefinitionEditorPage'
import { DefinitionListPage } from './features/definitions/DefinitionListPage'
import { ProfilePage } from './features/profile/ProfilePage'
import { UsersPage } from './features/users/UsersPage'
import { WorkflowDetailPage } from './features/workflows/WorkflowDetailPage'
import { WorkflowListPage } from './features/workflows/WorkflowListPage'

function NotFoundPage() {
  return (
    <div className="flex flex-col items-center gap-4 py-20 text-center">
      <Compass aria-hidden className="size-10 text-slate-400" />
      <div>
        <h1 className="text-xl font-semibold">Página não encontrada</h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">O endereço acessado não existe no console.</p>
      </div>
      <ButtonLink to="/dashboard">Ir para o painel</ButtonLink>
    </div>
  )
}

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 15_000,
      refetchOnWindowFocus: true,
      // Erros do cliente (4xx) não melhoram com nova tentativa.
      retry: (failureCount, error) => !(error instanceof ApiError && error.status < 500) && failureCount < 2,
    },
  },
})

const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: (
      <RequireAuth>
        <AppShell />
      </RequireAuth>
    ),
    children: [
      { path: '/', element: <Navigate to="/dashboard" replace /> },
      { path: '/dashboard', element: <DashboardPage /> },
      { path: '/workflows', element: <WorkflowListPage /> },
      { path: '/workflows/:id', element: <WorkflowDetailPage /> },
      { path: '/definitions', element: <DefinitionListPage /> },
      {
        path: '/definitions/new',
        element: (
          <RequireRole role="ADMIN">
            <DefinitionEditorPage />
          </RequireRole>
        ),
      },
      { path: '/definitions/:key', element: <DefinitionDetailPage /> },
      {
        path: '/definitions/:key/versions/new',
        element: (
          <RequireRole role="ADMIN">
            <DefinitionEditorPage />
          </RequireRole>
        ),
      },
      {
        path: '/audit',
        element: (
          <RequireRole role="ADMIN">
            <AuditPage />
          </RequireRole>
        ),
      },
      {
        path: '/users',
        element: (
          <RequireRole role="ADMIN">
            <UsersPage />
          </RequireRole>
        ),
      },
      { path: '/profile', element: <ProfilePage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ToastProvider>
          <RouterProvider router={router} />
        </ToastProvider>
      </AuthProvider>
    </QueryClientProvider>
  )
}
