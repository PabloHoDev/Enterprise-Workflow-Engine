import type { Problem } from './types'

const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'
const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

/** Disparado quando a sessão deixa de ser válida; o AuthProvider leva o usuário ao login. */
export const UNAUTHORIZED_EVENT = 'ewe:unauthorized'

export class ApiError extends Error {
  readonly status: number
  readonly problem: Problem

  constructor(status: number, problem: Problem) {
    super(problem.detail ?? problem.title ?? `HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

export function readCookie(name: string): string | null {
  const prefix = `${name}=`
  const cookie = document.cookie.split('; ').find((entry) => entry.startsWith(prefix))
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : null
}

/** Garante o cookie XSRF-TOKEN emitido pelo backend (proteção CSRF do console, ADR-007). */
export async function ensureCsrfToken(force = false): Promise<void> {
  if (!force && readCookie(CSRF_COOKIE)) {
    return
  }
  await fetch('/api/v1/auth/csrf', {
    credentials: 'same-origin',
    headers: { 'X-Requested-With': 'XMLHttpRequest' },
  })
}

interface RequestOptions {
  body?: unknown
  /** Não dispara o evento de sessão expirada (ex.: a própria tentativa de login). */
  silentUnauthorized?: boolean
}

async function parseProblem(response: Response): Promise<Problem> {
  const text = await response.text()
  if (!text) {
    return { status: response.status }
  }
  try {
    return JSON.parse(text) as Problem
  } catch {
    return { status: response.status, detail: text.slice(0, 200) }
  }
}

async function send<T>(method: string, path: string, options: RequestOptions, retryCsrf: boolean): Promise<T> {
  const unsafe = UNSAFE_METHODS.has(method)
  if (unsafe) {
    await ensureCsrfToken()
  }
  const headers: Record<string, string> = {
    Accept: 'application/json, application/problem+json',
    // Identifica a chamada como do console: o backend não envia o desafio Basic que abriria o diálogo do navegador.
    'X-Requested-With': 'XMLHttpRequest',
  }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  const csrf = unsafe ? readCookie(CSRF_COOKIE) : null
  if (csrf) {
    headers[CSRF_HEADER] = csrf
  }

  const response = await fetch(path, {
    method,
    headers,
    credentials: 'same-origin',
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })

  if (response.ok) {
    if (response.status === 204) {
      return undefined as T
    }
    const text = await response.text()
    return (text ? JSON.parse(text) : undefined) as T
  }

  const problem = await parseProblem(response)
  // O token CSRF muda a cada login: renova uma vez e repete a requisição.
  if (response.status === 403 && unsafe && retryCsrf && problem.title === 'Invalid CSRF token') {
    await ensureCsrfToken(true)
    return send<T>(method, path, options, false)
  }
  if (response.status === 401 && !options.silentUnauthorized) {
    window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
  }
  throw new ApiError(response.status, problem)
}

export const api = {
  get: <T>(path: string) => send<T>('GET', path, {}, true),
  post: <T>(path: string, body?: unknown, options: Omit<RequestOptions, 'body'> = {}) =>
    send<T>('POST', path, { ...options, body }, true),
  put: <T>(path: string, body?: unknown) => send<T>('PUT', path, { body }, true),
}

/** Monta a query string ignorando filtros vazios. */
export function query(params: Record<string, string | number | null | undefined>): string {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== null && value !== undefined && value !== '') {
      search.set(key, String(value))
    }
  }
  const text = search.toString()
  return text ? `?${text}` : ''
}
