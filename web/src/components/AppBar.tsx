import type { ReactNode } from 'react'
import { useLanguage } from '../lib/language.tsx'
import { Emblem } from './Emblem.tsx'

type AppBarProps = {
  title: string
  onBack?: () => void
  showEmblem?: boolean
  trailing?: ReactNode
}

export function AppBar({ title, onBack, showEmblem = false, trailing }: AppBarProps) {
  const { copy } = useLanguage()
  return (
    <header className="app-bar">
      <div className="app-bar__side">
        {onBack ? (
          <button type="button" className="icon-button" aria-label={copy.back} onClick={onBack}>
            <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill="currentColor"
                d="M19 11H7.83l4.58-4.59L11 5l-7 7 7 7 1.41-1.41L7.83 13H19z"
              />
            </svg>
          </button>
        ) : showEmblem ? (
          <Emblem size={26} />
        ) : null}
      </div>
      <h1 className="app-bar__title">{title}</h1>
      <div className={`app-bar__side${trailing ? ' app-bar__side--wide' : ''}`}>{trailing}</div>
    </header>
  )
}
