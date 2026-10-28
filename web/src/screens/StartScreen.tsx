import { useLayoutEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { LanguageSelect } from '../components/LanguageSelect.tsx'
import { Seal } from '../components/Seal.tsx'
import { useLanguage } from '../contexts/language.tsx'

export function StartScreen() {
  const { copy } = useLanguage()
  const titleRef = useRef<HTMLHeadingElement>(null)
  const subtitleRef = useRef<HTMLParagraphElement>(null)

  // The welcome lines stay on one row each, so the block stays a tidy centred stack on any
  // phone: shrink a line's font until it fits the screen width.
  useLayoutEffect(() => {
    const lines = [titleRef.current, subtitleRef.current].filter((el) => el !== null)
    const fit = () => {
      for (const line of lines) {
        line.style.fontSize = ''
        const room = line.clientWidth
        const needed = line.scrollWidth
        if (needed > room) {
          const base = parseFloat(getComputedStyle(line).fontSize)
          line.style.fontSize = `${Math.floor(base * (room / needed) * 10) / 10}px`
        }
      }
    }
    fit()
    document.fonts?.ready.then(fit)
    window.addEventListener('resize', fit)
    return () => window.removeEventListener('resize', fit)
  }, [copy.welcomeTitle, copy.subtitle])

  return (
    <section className="start">
      <div className="start__glow" aria-hidden="true" />
      <div className="start__content">
        <header className="start__top">
          <LanguageSelect variant="light" />
        </header>

        <div className="start__hero">
          <div className="start__seal">
            <Seal className="start__emblem" />
          </div>
          <p className="start__honour">{copy.honour}</p>
          <h1 ref={titleRef} className="start__title">
            {copy.welcomeTitle}
          </h1>
          <p ref={subtitleRef} className="start__subtitle">
            {copy.subtitle}
          </p>
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
