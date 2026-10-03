/** Linha editável de variável: chave + valor digitado. */
export interface VariableRow {
  key: string
  value: string
}

/**
 * Converte o texto digitado no tipo JSON mais adequado: números e booleanos viram número/booleano,
 * o resto permanece texto. As Rules do backend comparam números por valor.
 */
export function parseValue(raw: string): unknown {
  const text = raw.trim()
  if (text === 'true' || text === 'false') {
    return text === 'true'
  }
  if (text !== '' && /^-?\d+(\.\d+)?$/.test(text)) {
    return Number(text)
  }
  return raw
}

export function rowsToVariables(rows: VariableRow[]): Record<string, unknown> {
  const variables: Record<string, unknown> = {}
  for (const row of rows) {
    const key = row.key.trim()
    if (key) {
      variables[key] = parseValue(row.value)
    }
  }
  return variables
}

export function formatValue(value: unknown): string {
  if (value === null || value === undefined) {
    return '—'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}
