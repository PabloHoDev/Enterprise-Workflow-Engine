import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import './index.css'
import { applyTheme, readThemePreference } from './lib/theme'

applyTheme(readThemePreference())
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => applyTheme(readThemePreference()))

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
