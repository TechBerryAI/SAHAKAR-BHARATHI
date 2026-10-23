import { useLayoutEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { LanguageSelect } from '../components/LanguageSelect.tsx'
import { Seal } from '../components/Seal.tsx'
import { useLanguage } from '../lib/language.tsx'

export function StartScreen() {
  const { copy } = useLanguage()
  const titleRef = useRef<HTMLHeadingElement>(null)

  // The welcome line stays on one row: shrink its font until it fits the screen width.
  useLayoutEffect(() => {
    const title = titleRef.current
    if (!title) return
    const fit = () => {
      title.style.fontSize = ''
      const room = title.clientWidth
      const needed = title.scrollWidth
      if (needed > room) {
        const base = parseFloat(getComputedStyle(title).fontSize)
        title.style.fontSize = `${Math.floor(base * (room / needed) * 10) / 10}px`
      }
    }
    fit()
    document.fonts?.ready.then(fit)
    window.addEventListener('resize', fit)
    return () => window.removeEventListener('resize', fit)
  }, [copy.welcomeTitle])

  return (
    <section className="start">
      <div className="start__glow" aria-hidden="true" />
      <div className="start__content">
        <header className="start__top">
          <LanguageSelect variant="light" />
        </header>

        <div className="start__hero">
          <Seal className="start__emblem" />
          <p className="start__honour">{copy.honour}</p>
          <h1 ref={titleRef} className="start__title">
            {copy.welcomeTitle}
          </h1>
          <p className="start__subtitle">{copy.subtitle}</p>
        </div>

        <div className="start__actions">
          <Link className="btn btn--primary start__register" to="/register">
            {copy.registerNow}
            <span aria-hidden="true">→</span>
          </Link>
          <Link className="start__signin" to="/sign-in">
            <span aria-hidden="true">→</span>
            {copy.alreadyRegistered}
          </Link>
        </div>
      </div>
    </section>
  )
}
