import { describe, expect, it } from 'vitest'
import { ApiError } from '../api/client'
import { safeRedirect } from '../auth/AuthProvider'
import { layoutStates } from '../components/StateDiagram'
import { describeRule, errorDetails, errorMessage, formatRelative, operationLabel } from './format'
import { formatValue, parseValue, rowsToVariables } from './variables'

describe('variables', () => {
  it('keeps numbers and booleans typed so the backend rules compare them by value', () => {
    expect(parseValue('15000')).toBe(15000)
    expect(parseValue('-2.5')).toBe(-2.5)
    expect(parseValue('true')).toBe(true)
    expect(parseValue('12a')).toBe('12a')
    expect(parseValue('')).toBe('')
  })

  it('ignores rows without a name', () => {
    expect(rowsToVariables([{ key: ' amount ', value: '10' }, { key: '', value: 'x' }])).toEqual({ amount: 10 })
  })

  it('formats any variable for display', () => {
    expect(formatValue(null)).toBe('—')
    expect(formatValue({ a: 1 })).toBe('{"a":1}')
    expect(formatValue(3)).toBe('3')
  })
})

describe('format', () => {
  it('translates API errors into Portuguese messages with technical details', () => {
    const error = new ApiError(422, { title: 'Rule not satisfied', unsatisfiedRules: ['amount LESS_THAN 10'] })

    expect(errorMessage(error)).toBe('As regras desta ação não foram atendidas.')
    expect(errorDetails(error)).toEqual(['amount LESS_THAN 10'])
    expect(errorMessage(new ApiError(409, {}))).toContain('conflita')
    expect(errorMessage(new TypeError('Failed to fetch'))).toContain('servidor')
    expect(errorDetails(new ApiError(400, { errors: [{ field: 'key', message: 'must not be blank' }] }))).toEqual([
      'key: must not be blank',
    ])
  })

  it('describes rules and audit operations in Portuguese', () => {
    expect(describeRule({ field: 'amount', operator: 'GREATER_THAN', value: '10000' })).toBe('amount maior que 10000')
    expect(describeRule({ field: 'note', operator: 'EXISTS', value: null })).toBe('note preenchido')
    expect(operationLabel('ACCOUNT_LOCKED')).toBe('Conta bloqueada')
    expect(operationLabel('UNKNOWN_OP')).toBe('UNKNOWN_OP')
  })

  it('formats relative times', () => {
    const now = new Date('2026-01-01T12:00:00Z')
    expect(formatRelative('2026-01-01T11:00:00Z', now)).toBe('há 1 hora')
    expect(formatRelative(null, now)).toBe('—')
  })
})

describe('safeRedirect', () => {
  it('only accepts internal paths to prevent open redirects', () => {
    expect(safeRedirect('/workflows/1?x=1')).toBe('/workflows/1?x=1')
    expect(safeRedirect('https://evil.example')).toBe('/dashboard')
    expect(safeRedirect('//evil.example')).toBe('/dashboard')
    expect(safeRedirect('/\\evil.example')).toBe('/dashboard')
    expect(safeRedirect(null)).toBe('/dashboard')
  })
})

describe('layoutStates', () => {
  it('places each state in the column of its distance from the initial state', () => {
    const positions = layoutStates(
      [
        { name: 'A', type: 'INITIAL' },
        { name: 'B', type: 'INTERMEDIATE' },
        { name: 'C', type: 'TERMINAL' },
        { name: 'D', type: 'TERMINAL' },
      ],
      [
        { action: 'go', from: 'A', to: 'B', requiredRole: null, rules: [] },
        { action: 'ok', from: 'B', to: 'C', requiredRole: null, rules: [] },
        { action: 'no', from: 'B', to: 'D', requiredRole: null, rules: [] },
        { action: 'back', from: 'B', to: 'A', requiredRole: null, rules: [] },
      ],
    )

    expect(positions.get('A')!.x).toBeLessThan(positions.get('B')!.x)
    expect(positions.get('C')!.x).toBe(positions.get('D')!.x)
    expect(positions.get('C')!.y).toBeLessThan(positions.get('D')!.y)
  })
})
