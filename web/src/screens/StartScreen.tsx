import { Link } from 'react-router-dom'
import { LanguageSelect } from '../components/LanguageSelect.tsx'
import { useLanguage } from '../lib/language.tsx'

export function StartScreen() {
  const { copy } = useLanguage()

  return (
    <section className="start">
      <div className="start__glow" aria-hidden="true" />
      <div className="start__content">
        <header className="start__top">
          <span className="start__gov">
            <span className="start__dot" aria-hidden="true" />
            {copy.govChip}
          </span>
          <LanguageSelect />
        </header>

        <img className="start__emblem" src="/emblem.png" alt="" />

        <p className="start__honour">{copy.honour}</p>
        <h1 className="start__title">Sahakar Bharati</h1>
        <p className="start__subtitle">{copy.subtitle}</p>
        <p className="start__tagline">{copy.tagline}</p>

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
