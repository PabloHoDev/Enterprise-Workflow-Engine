import type { RuleOperator, StateType, VersionStructure } from '../../api/types'

/** Linhas editáveis do formulário de estrutura (strings, para os campos controlados). */
export interface RuleRow {
  field: string
  operator: RuleOperator
  value: string
}

export interface TransitionRow {
  action: string
  from: string
  to: string
  requiredRole: string
  rules: RuleRow[]
}

export interface StateRow {
  name: string
  type: StateType
}

export interface StructureForm {
  states: StateRow[]
  transitions: TransitionRow[]
}

export function toStructure(form: StructureForm): VersionStructure {
  return {
    states: form.states.map((state) => ({ name: state.name.trim(), type: state.type })),
    transitions: form.transitions.map((transition) => ({
      action: transition.action.trim(),
      from: transition.from,
      to: transition.to,
      requiredRole: transition.requiredRole.trim() || null,
      rules: transition.rules.map((rule) => ({
        field: rule.field.trim(),
        operator: rule.operator,
        value: rule.operator === 'EXISTS' ? null : rule.value.trim(),
      })),
    })),
  }
}

export function fromStructure(structure: VersionStructure): StructureForm {
  return {
    states: structure.states.map((state) => ({ ...state })),
    transitions: structure.transitions.map((transition) => ({
      action: transition.action,
      from: transition.from,
      to: transition.to,
      requiredRole: transition.requiredRole ?? '',
      rules: transition.rules.map((rule) => ({ field: rule.field, operator: rule.operator, value: rule.value ?? '' })),
    })),
  }
}

/** Exemplo pronto: aprovação de compras com validação financeira acima de 10.000. */
export const PURCHASE_APPROVAL_TEMPLATE: StructureForm = {
  states: [
    { name: 'REQUESTED', type: 'INITIAL' },
    { name: 'PENDING_APPROVAL', type: 'INTERMEDIATE' },
    { name: 'FINANCIAL_VALIDATION', type: 'INTERMEDIATE' },
    { name: 'APPROVED', type: 'TERMINAL' },
    { name: 'REJECTED', type: 'TERMINAL' },
  ],
  transitions: [
    { action: 'submit', from: 'REQUESTED', to: 'PENDING_APPROVAL', requiredRole: '', rules: [] },
    {
      action: 'approve',
      from: 'PENDING_APPROVAL',
      to: 'APPROVED',
      requiredRole: 'MANAGER',
      rules: [{ field: 'amount', operator: 'LESS_THAN_OR_EQUAL', value: '10000' }],
    },
    {
      action: 'send-to-finance',
      from: 'PENDING_APPROVAL',
      to: 'FINANCIAL_VALIDATION',
      requiredRole: 'MANAGER',
      rules: [{ field: 'amount', operator: 'GREATER_THAN', value: '10000' }],
    },
    { action: 'reject', from: 'PENDING_APPROVAL', to: 'REJECTED', requiredRole: 'MANAGER', rules: [] },
    { action: 'approve', from: 'FINANCIAL_VALIDATION', to: 'APPROVED', requiredRole: 'FINANCE', rules: [] },
    { action: 'reject', from: 'FINANCIAL_VALIDATION', to: 'REJECTED', requiredRole: 'FINANCE', rules: [] },
  ],
}

export const EMPTY_STRUCTURE: StructureForm = {
  states: [
    { name: 'START', type: 'INITIAL' },
    { name: 'DONE', type: 'TERMINAL' },
  ],
  transitions: [{ action: 'finish', from: 'START', to: 'DONE', requiredRole: '', rules: [] }],
}
