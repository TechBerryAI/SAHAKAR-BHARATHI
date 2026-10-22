import type { ReactNode } from 'react'
import { BottomNav } from './BottomNav.tsx'

type AppShellProps = {
  children: ReactNode
  showNav?: boolean
}

export function AppShell({ children, showNav = false }: AppShellProps) {
  return (
    <div className="app-canvas">
      <div className="app-frame" data-app-frame>
        <div className="app-body">{children}</div>
        {showNav ? <BottomNav /> : null}
      </div>
    </div>
  )
}
