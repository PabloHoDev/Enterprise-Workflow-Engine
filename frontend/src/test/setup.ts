import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Sem "globals" no Vitest, a limpeza automática do Testing Library precisa ser registrada aqui.
afterEach(() => cleanup())
