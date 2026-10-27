import type { ReactNode } from 'react'
import { useLanguage } from '../lib/language.tsx'
import { useServerSlow } from '../lib/network.ts'
import { BottomNav } from './BottomNav.tsx'

type AppShellProps = {
  children: ReactNode
  showNav?: boolean
}

// Shown while a request has been waiting a few seconds, typically while the API host
// starts after being idle, so a slow first request does not look like a hang.
function ServerWakingBanner() {
  const slow = useServerSlow()
  const { copy } = useLanguage()
  return (
    <div className="waking-banner" role="status" aria-live="polite" hidden={!slow}>
      {slow ? (
        <>
          <span className="waking-banner__spinner" aria-hidden="true" />
          <span>{copy.serverWaking}</span>
        </>
      ) : null}
    </div>
  )
}

export function AppShell({ children, showNav = false }: AppShellProps) {
  return (
    <div className="app-canvas">
      <div className="app-frame" data-app-frame>
        <ServerWakingBanner />
        <div className="app-body">{children}</div>
        {showNav ? <BottomNav /> : null}
      </div>
    </div>
  )
}
