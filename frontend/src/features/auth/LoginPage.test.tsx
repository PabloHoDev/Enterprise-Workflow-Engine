import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { AuthProvider } from '../../auth/AuthProvider'
import { LoginPage } from './LoginPage'

function renderLogin() {
  const router = createMemoryRouter(
    [
      { path: '/login', element: <LoginPage /> },
      { path: '/workflows', element: <p>workflows page</p> },
    ],
    { initialEntries: ['/login?redirect=%2Fworkflows'] },
  )
  render(
    <QueryClientProvider client={new QueryClient()}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>,
  )
}

describe('LoginPage', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    document.cookie = 'XSRF-TOKEN=t; path=/'
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
  })

  it('shows a generic message for invalid credentials and clears the password', async () => {
    fetchMock.mockImplementation(async (input) => {
      const url = String(input)
      if (url.endsWith('/auth/me') || url.endsWith('/auth/login')) {
        return new Response(JSON.stringify({ title: 'Authentication failed', status: 401 }), { status: 401 })
      }
      return new Response(null, { status: 204 })
    })
    renderLogin()
    const user = userEvent.setup()

    await user.type(await screen.findByLabelText('Usuário'), 'ana')
    await user.type(screen.getByLabelText('Senha'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Usuário ou senha inválidos.')
    expect(screen.getByLabelText('Senha')).toHaveValue('')
  })

  it('goes to the requested page after a successful login', async () => {
    fetchMock.mockImplementation(async (input) => {
      const url = String(input)
      if (url.endsWith('/auth/me')) {
        return new Response(null, { status: 401 })
      }
      if (url.endsWith('/auth/login')) {
        return new Response(JSON.stringify({ username: 'ana', displayName: 'Ana', roles: ['USER'] }), { status: 200 })
      }
      return new Response(null, { status: 204 })
    })
    renderLogin()
    const user = userEvent.setup()

    await user.type(await screen.findByLabelText('Usuário'), 'ana')
    await user.type(screen.getByLabelText('Senha'), 'correct-password')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('workflows page')).toBeInTheDocument()
  })
})
