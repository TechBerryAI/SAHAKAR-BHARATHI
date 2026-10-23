import { Link } from 'react-router-dom'
import { LanguageSelect } from '../components/LanguageSelect.tsx'
import { Seal } from '../components/Seal.tsx'
import { useLanguage } from '../lib/language.tsx'

export function StartScreen() {
  const { copy } = useLanguage()

  return (
    <section className="start">
      <div className="start__glow" aria-hidden="true" />
      <div className="start__content">
        <header className="start__top">
          <LanguageSelect variant="light" />
        </header>

        <Seal className="start__emblem" />

        <p className="start__honour">{copy.honour}</p>
        <h1 className="start__title">{copy.brandName}</h1>
        <p className="start__subtitle">{copy.subtitle}</p>

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
