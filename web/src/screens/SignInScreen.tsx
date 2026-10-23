import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Seal } from '../components/Seal.tsx'
import { Button, ErrorBanner, Field } from '../components/ui.tsx'
import { apiErrorText, signIn } from '../lib/api.ts'
import { useLanguage } from '../lib/language.tsx'

const PIN_PATTERN = /^\d{4,6}$/

export function SignInScreen() {
  const navigate = useNavigate()
  const { copy } = useLanguage()
  const [identifier, setIdentifier] = useState('')
  const [pin, setPin] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    const login = identifier.trim()
    if (!login || !PIN_PATTERN.test(pin)) {
      setError(copy.enterLoginPin)
      return
    }
    setBusy(true)
    setError('')
    try {
      await signIn(login, pin)
      navigate('/', { replace: true })
    } catch (err) {
      setError(apiErrorText(err, copy, copy.signInFailed))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="signin">
      <header className="signin__bar">
        <button type="button" className="signin__back" aria-label={copy.back} onClick={() => navigate('/')}>
          <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M19 11H7.83l4.58-4.59L11 5l-7 7 7 7 1.41-1.41L7.83 13H19z" />
          </svg>
        </button>
        <h1>{copy.signIn}</h1>
      </header>

      <form className="signin__form" onSubmit={onSubmit}>
        <div className="signin__body">
          <Seal className="signin__emblem" />
          <h2 className="signin__title">{copy.accessTitle}</h2>
          <p className="signin__lead">{copy.accessBody}</p>
          <ErrorBanner message={error} />
          <Field
            label={copy.loginIdLabel}
            name="identifier"
            autoComplete="username"
            inputMode="text"
            value={identifier}
            onChange={(event) => setIdentifier(event.target.value)}
            hint={copy.loginIdHint}
          />
          <Field
            label={copy.pinLabel}
            name="pin"
            type="password"
            inputMode="numeric"
            autoComplete="current-password"
            maxLength={6}
            value={pin}
            onChange={(event) => setPin(event.target.value.replace(/\D/g, '').slice(0, 6))}
          />
        </div>
        <div className="signin__footer">
          <Button type="submit" className="signin__submit" disabled={busy}>
            {busy ? copy.signingIn : copy.signIn}
            {busy ? null : <span aria-hidden="true">→</span>}
          </Button>
          <Link className="signin__create" to="/register">
            {copy.createAccount}
          </Link>
        </div>
      </form>
    </section>
  )
}
