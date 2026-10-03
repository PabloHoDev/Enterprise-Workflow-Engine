import type { StateDefinition, TransitionDefinition } from '../api/types'

const NODE_WIDTH = 176
const NODE_HEIGHT = 46
const COLUMN_GAP = 190
const ROW_GAP = 56
const PADDING = 24

interface Positioned {
  state: StateDefinition
  x: number
  y: number
}

/** Coluna de cada State = distância (em transições) a partir do State inicial. */
export function layoutStates(states: StateDefinition[], transitions: TransitionDefinition[]): Map<string, Positioned> {
  const level = new Map<string, number>()
  const initial = states.find((state) => state.type === 'INITIAL') ?? states[0]
  if (initial) {
    level.set(initial.name, 0)
    const queue = [initial.name]
    while (queue.length > 0) {
      const current = queue.shift()!
      for (const transition of transitions.filter((t) => t.from === current)) {
        if (!level.has(transition.to)) {
          level.set(transition.to, (level.get(current) ?? 0) + 1)
          queue.push(transition.to)
        }
      }
    }
  }
  const maxLevel = Math.max(0, ...level.values())
  const rowsPerColumn = new Map<number, number>()
  const positions = new Map<string, Positioned>()
  for (const state of states) {
    const column = level.get(state.name) ?? maxLevel + 1
    const row = rowsPerColumn.get(column) ?? 0
    rowsPerColumn.set(column, row + 1)
    positions.set(state.name, {
      state,
      x: PADDING + column * (NODE_WIDTH + COLUMN_GAP),
      y: PADDING + row * (NODE_HEIGHT + ROW_GAP),
    })
  }
  return positions
}

/** Distribui as pontas de várias setas ao longo da borda do nó, em vez de saírem do mesmo ponto. */
function anchor(index: number, count: number): number {
  return (NODE_HEIGHT * (index + 1)) / (count + 1)
}

const nodeStyles = {
  INITIAL: 'fill-sky-50 stroke-sky-500 dark:fill-sky-950',
  INTERMEDIATE: 'fill-white stroke-slate-400 dark:fill-slate-900 dark:stroke-slate-500',
  TERMINAL: 'fill-emerald-50 stroke-emerald-500 dark:fill-emerald-950',
} as const

/**
 * Diagrama dos States e Transitions de uma versão, destacando o State atual de uma execução.
 * A mesma informação está disponível em texto (tabela de transições) para quem não enxerga o diagrama.
 */
export function StateDiagram({
  states,
  transitions,
  currentState,
}: {
  states: StateDefinition[]
  transitions: TransitionDefinition[]
  currentState?: string
}) {
  const positions = layoutStates(states, transitions)
  const all = [...positions.values()]
  const width = Math.max(...all.map((p) => p.x + NODE_WIDTH), 0) + PADDING
  const height = Math.max(...all.map((p) => p.y + NODE_HEIGHT), 0) + PADDING + 56

  const forwardOut = new Map<string, TransitionDefinition[]>()
  const forwardIn = new Map<string, TransitionDefinition[]>()
  for (const transition of transitions) {
    const from = positions.get(transition.from)
    const to = positions.get(transition.to)
    if (from && to && to.x > from.x) {
      forwardOut.set(transition.from, [...(forwardOut.get(transition.from) ?? []), transition])
      forwardIn.set(transition.to, [...(forwardIn.get(transition.to) ?? []), transition])
    }
  }

  return (
    // Focável para que a rolagem horizontal funcione também pelo teclado.
    <div className="overflow-x-auto rounded-md" tabIndex={0} role="region" aria-label="Diagrama do processo (role para os lados)">
      <svg
        role="img"
        aria-label={`Diagrama do processo com ${states.length} estados e ${transitions.length} transições`}
        viewBox={`0 0 ${width} ${height}`}
        // Ocupa a largura do card; abaixo de 70% do tamanho natural o texto ficaria ilegível, então passa a rolar.
        style={{ width: '100%', minWidth: Math.round(width * 0.7), height: 'auto' }}
        className="max-w-none text-slate-500"
      >
        <defs>
          <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
            <path d="M0 0 10 5 0 10z" className="fill-slate-400 dark:fill-slate-500" />
          </marker>
        </defs>

        {transitions.map((transition, index) => {
          const from = positions.get(transition.from)
          const to = positions.get(transition.to)
          if (!from || !to) {
            return null
          }
          const forward = to.x > from.x
          let path: string
          let labelX: number
          let labelY: number
          if (forward) {
            const outgoing = forwardOut.get(transition.from) ?? []
            const incoming = forwardIn.get(transition.to) ?? []
            const startX = from.x + NODE_WIDTH
            const startY = from.y + anchor(outgoing.indexOf(transition), outgoing.length)
            const endX = to.x
            const endY = to.y + anchor(incoming.indexOf(transition), incoming.length)
            const midX = (startX + endX) / 2
            path = `M${startX} ${startY} C${midX} ${startY} ${midX} ${endY} ${endX} ${endY}`
            // Rótulo perto da origem, onde as setas ainda estão separadas.
            labelX = startX + 12
            labelY = startY - 6
          } else {
            // Laços e retornos passam por baixo dos nós para não cruzar o fluxo principal.
            const startX = from.x + NODE_WIDTH / 2
            const startY = from.y + NODE_HEIGHT
            const endX = to.x + NODE_WIDTH / 2
            const endY = to.y + NODE_HEIGHT
            const dip = Math.max(startY, endY) + 30 + (index % 3) * 10
            path = `M${startX} ${startY} C${startX} ${dip} ${endX} ${dip} ${endX} ${endY}`
            labelX = (startX + endX) / 2
            labelY = dip - 4
          }
          return (
            <g key={`${transition.from}-${transition.action}`}>
              <path d={path} fill="none" className="stroke-slate-300 dark:stroke-slate-600" strokeWidth={1.5} markerEnd="url(#arrow)" />
              <text
                x={labelX}
                y={labelY}
                textAnchor={forward ? 'start' : 'middle'}
                paintOrder="stroke"
                strokeWidth={4}
                className="fill-slate-600 stroke-white text-[11px] font-medium dark:fill-slate-300 dark:stroke-slate-900"
              >
                {transition.action}
                {transition.requiredRole ? ` · ${transition.requiredRole}` : ''}
              </text>
            </g>
          )
        })}

        {all.map(({ state, x, y }) => {
          const current = state.name === currentState
          return (
            <g key={state.name}>
              <rect
                x={x}
                y={y}
                width={NODE_WIDTH}
                height={NODE_HEIGHT}
                rx={state.type === 'TERMINAL' ? 23 : 10}
                strokeWidth={current ? 3 : 1.5}
                className={current ? 'fill-indigo-600 stroke-indigo-700 dark:fill-indigo-500' : nodeStyles[state.type]}
              />
              <text
                x={x + NODE_WIDTH / 2}
                y={y + NODE_HEIGHT / 2 + 4}
                textAnchor="middle"
                className={`text-xs font-semibold ${current ? 'fill-white' : 'fill-slate-800 dark:fill-slate-100'}`}
              >
                {state.name.length > 22 ? `${state.name.slice(0, 21)}…` : state.name}
              </text>
            </g>
          )
        })}
      </svg>
      <ul className="mt-2 flex flex-wrap gap-4 text-xs text-slate-600 dark:text-slate-400">
        <li className="flex items-center gap-1.5">
          <span className="size-3 rounded-sm border-2 border-sky-500 bg-sky-50" /> Inicial
        </li>
        <li className="flex items-center gap-1.5">
          <span className="size-3 rounded-full border-2 border-emerald-500 bg-emerald-50" /> Final
        </li>
        {currentState && (
          <li className="flex items-center gap-1.5">
            <span className="size-3 rounded-sm bg-indigo-600" /> Estado atual
          </li>
        )}
      </ul>
    </div>
  )
}
