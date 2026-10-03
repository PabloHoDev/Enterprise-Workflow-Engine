import {
  GitBranch,
  LayoutDashboard,
  LogOut,
  Menu,
  Monitor,
  Moon,
  ScrollText,
  Sun,
  UserCircle,
  Users,
  Workflow,
  X,
} from 'lucide-react'
import { useState, type ComponentType } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../../auth/AuthProvider'
import { readThemePreference, saveThemePreference, type ThemePreference } from '../../lib/theme'

interface NavItem {
  to: string
  label: string
  icon: ComponentType<{ className?: string; 'aria-hidden'?: boolean }>
  role?: string
}

const NAV: NavItem[] = [
  { to: '/dashboard', label: 'Painel', icon: LayoutDashboard },
  { to: '/workflows', label: 'Workflows', icon: Workflow },
  { to: '/definitions', label: 'Definições', icon: GitBranch },
  { to: '/audit', label: 'Auditoria', icon: ScrollText, role: 'ADMIN' },
  { to: '/users', label: 'Usuários', icon: Users, role: 'ADMIN' },
]

function Logo() {
  return (
    <div className="flex items-center gap-2.5">
      <img src="/favicon.svg" alt="" className="size-8" />
      <div className="leading-tight">
        <p className="text-sm font-semibold">Workflow Engine</p>
        <p className="text-xs text-slate-500 dark:text-slate-400">Console</p>
      </div>
    </div>
  )
}

function Navigation({ onNavigate }: { onNavigate?: () => void }) {
  const { hasRole } = useAuth()
  return (
    <nav aria-label="Principal" className="flex flex-col gap-1">
      {NAV.filter((item) => !item.role || hasRole(item.role)).map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          onClick={onNavigate}
          className={({ isActive }) =>
            `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
              isActive
                ? 'bg-indigo-50 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300'
                : 'text-slate-700 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800'
            }`
          }
        >
          <Icon aria-hidden className="size-5" />
          {label}
        </NavLink>
      ))}
    </nav>
  )
}

const THEMES: { value: ThemePreference; label: string; icon: ComponentType<{ className?: string }> }[] = [
  { value: 'light', label: 'Claro', icon: Sun },
  { value: 'dark', label: 'Escuro', icon: Moon },
  { value: 'system', label: 'Sistema', icon: Monitor },
]

function ThemeSwitcher() {
  const [theme, setTheme] = useState<ThemePreference>(readThemePreference)
  return (
    <div role="radiogroup" aria-label="Tema" className="flex rounded-lg bg-slate-100 p-1 dark:bg-slate-800">
      {THEMES.map(({ value, label, icon: Icon }) => (
        <button
          key={value}
          type="button"
          role="radio"
          aria-checked={theme === value}
          title={label}
          onClick={() => {
            setTheme(value)
            saveThemePreference(value)
          }}
          className={`flex flex-1 items-center justify-center rounded-md py-1.5 ${
            theme === value ? 'bg-white shadow-sm dark:bg-slate-700' : 'text-slate-500'
          }`}
        >
          <Icon className="size-4" />
          <span className="sr-only">{label}</span>
        </button>
      ))}
    </div>
  )
}

function UserPanel({ onNavigate }: { onNavigate?: () => void }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  if (!user) {
    return null
  }
  return (
    <div className="space-y-3 border-t border-slate-200 pt-4 dark:border-slate-800">
      <ThemeSwitcher />
      <NavLink
        to="/profile"
        onClick={onNavigate}
        className="flex items-center gap-3 rounded-lg px-2 py-2 hover:bg-slate-100 dark:hover:bg-slate-800"
      >
        <UserCircle aria-hidden className="size-8 text-slate-400" />
        <div className="min-w-0 leading-tight">
          <p className="truncate text-sm font-medium">{user.displayName}</p>
          <p className="truncate text-xs text-slate-500 dark:text-slate-400">{user.roles.join(' · ')}</p>
        </div>
      </NavLink>
      <button
        type="button"
        onClick={async () => {
          await logout()
          navigate('/login', { replace: true })
        }}
        className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
      >
        <LogOut aria-hidden className="size-5" />
        Sair
      </button>
    </div>
  )
}

/** Estrutura das telas autenticadas: barra lateral no desktop e menu recolhível no celular. */
export function AppShell() {
  const location = useLocation()
  // O menu móvel fica aberto apenas na página em que foi aberto: navegar o fecha.
  const [menuOpenAt, setMenuOpenAt] = useState<string | null>(null)
  const menuOpen = menuOpenAt === location.pathname
  const setMenuOpen = (open: boolean) => setMenuOpenAt(open ? location.pathname : null)

  return (
    <div className="min-h-screen lg:flex">
      <a
        href="#conteudo"
        className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50 focus:rounded-md focus:bg-white focus:px-3 focus:py-2 focus:shadow"
      >
        Ir para o conteúdo
      </a>

      <aside className="hidden w-64 shrink-0 flex-col justify-between border-r border-slate-200 bg-white px-4 py-5 lg:sticky lg:top-0 lg:flex lg:h-screen dark:border-slate-800 dark:bg-slate-900">
        <div className="space-y-6">
          <Logo />
          <Navigation />
        </div>
        <UserPanel />
      </aside>

      <header className="sticky top-0 z-30 flex items-center justify-between border-b border-slate-200 bg-white/90 px-4 py-3 backdrop-blur lg:hidden dark:border-slate-800 dark:bg-slate-900/90">
        <Logo />
        <button
          type="button"
          onClick={() => setMenuOpen(!menuOpen)}
          aria-expanded={menuOpen}
          aria-controls="mobile-menu"
          className="rounded-lg p-2 hover:bg-slate-100 dark:hover:bg-slate-800"
        >
          {menuOpen ? <X aria-hidden className="size-6" /> : <Menu aria-hidden className="size-6" />}
          <span className="sr-only">{menuOpen ? 'Fechar menu' : 'Abrir menu'}</span>
        </button>
      </header>
      {menuOpen && (
        <div id="mobile-menu" className="space-y-4 border-b border-slate-200 bg-white px-4 py-4 lg:hidden dark:border-slate-800 dark:bg-slate-900">
          <Navigation onNavigate={() => setMenuOpen(false)} />
          <UserPanel onNavigate={() => setMenuOpen(false)} />
        </div>
      )}

      <main id="conteudo" className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 sm:px-6 lg:px-10 lg:py-10">
        <Outlet />
      </main>
    </div>
  )
}
