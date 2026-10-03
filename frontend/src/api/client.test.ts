import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, query, UNAUTHORIZED_EVENT } from './client'

function jsonResponse(status: number, body?: unknown): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('api client', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
    document.cookie = 'XSRF-TOKEN=token-1; path=/'
  })

  afterEach(() => {
    fetchMock.mockReset()
    vi.unstubAllGlobals()
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
  })

  it('sends the CSRF token and the console marker on state-changing requests', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, { ok: true }))

    await api.post('/api/v1/workflows', { definitionKey: 'k' })

    const [, init] = fetchMock.mock.calls[0]!
    const headers = init!.headers as Record<string, string>
    expect(headers['X-XSRF-TOKEN']).toBe('token-1')
    expect(headers['X-Requested-With']).toBe('XMLHttpRequest')
    expect(init!.credentials).toBe('same-origin')
    expect(init!.body).toBe('{"definitionKey":"k"}')
  })

  it('does not send the CSRF token on reads', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, []))

    await api.get('/api/v1/workflows')

    const headers = fetchMock.mock.calls[0]![1]!.headers as Record<string, string>
    expect(headers['X-XSRF-TOKEN']).toBeUndefined()
  })

  it('renews the CSRF token once and retries when the server rejects it', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(403, { title: 'Invalid CSRF token', status: 403 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(jsonResponse(200, { id: '1' }))

    await expect(api.post('/api/v1/workflows/1/start')).resolves.toEqual({ id: '1' })
    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      '/api/v1/workflows/1/start',
      '/api/v1/auth/csrf',
      '/api/v1/workflows/1/start',
    ])
  })

  it('turns problem details into ApiError and signals an expired session', async () => {
    const listener = vi.fn()
    window.addEventListener(UNAUTHORIZED_EVENT, listener)
    fetchMock.mockResolvedValueOnce(jsonResponse(401, { title: 'Authentication required', status: 401 }))

    const error = await api.get('/api/v1/auth/me').catch((caught: unknown) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(401)
    expect(listener).toHaveBeenCalledOnce()
    window.removeEventListener(UNAUTHORIZED_EVENT, listener)
  })

  it('does not signal an expired session for a failed login attempt', async () => {
    const listener = vi.fn()
    window.addEventListener(UNAUTHORIZED_EVENT, listener)
    fetchMock.mockResolvedValueOnce(jsonResponse(401, { title: 'Authentication failed' }))

    await expect(api.post('/api/v1/auth/login', {}, { silentUnauthorized: true })).rejects.toBeInstanceOf(ApiError)
    expect(listener).not.toHaveBeenCalled()
    window.removeEventListener(UNAUTHORIZED_EVENT, listener)
  })

  it('returns undefined for empty successful responses', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }))

    await expect(api.post('/api/v1/auth/logout')).resolves.toBeUndefined()
  })

  it('builds query strings without empty filters', () => {
    expect(query({ status: '', definitionKey: 'a b', page: 0, size: undefined })).toBe('?definitionKey=a+b&page=0')
    expect(query({ status: null })).toBe('')
  })
})
