import { Link } from 'react-router-dom'

export function StartScreen() {
  return (
    <section className="start">
      <div className="start__glow" aria-hidden="true" />
      <div className="start__content">
        <header className="start__top">
          <span className="start__gov">
            <span className="start__dot" aria-hidden="true" />
            Government of India
          </span>
          <span className="start__lang">English</span>
        </header>

        <img className="start__emblem" src="/emblem.png" alt="" />

        <p className="start__honour">Honouring India's food heroes</p>
        <h1 className="start__title">Sahakar Bharati</h1>
        <p className="start__subtitle">Farmers &amp; Fishermen Celebration</p>
        <p className="start__tagline">
          A nationwide tribute to the extraordinary work of India's farmers and fishermen.
        </p>

        <div className="start__actions">
          <Link className="btn btn--primary start__register" to="/register">
            Register Now
            <span aria-hidden="true">→</span>
          </Link>
          <Link className="start__signin" to="/sign-in">
            <span aria-hidden="true">→</span>
            Already registered? Sign In
          </Link>
        </div>
      </div>
    </section>
  )
}
