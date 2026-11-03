import { useState, type FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Seal } from '../components/Seal.tsx'
import { Button, ErrorBanner, Field } from '../components/ui.tsx'
import { useLanguage } from '../contexts/language.tsx'
import { apiErrorText, signIn } from '../lib/api.ts'

const PIN_PATTERN = /^\d{4,6}$/
const MOBILE_PATTERN = /^[6-9]\d{9}$/

export function SignInScreen() {
  const navigate = useNavigate()
  const location = useLocation()
  const { copy } = useLanguage()
  const routeState = location.state as { identifier?: string; accountReady?: boolean } | null
  // Registration sends the number when sign-in after create failed, or when the number already has an account.
  const presetMobile = (routeState?.identifier ?? '').replace(/\D/g, '').slice(-10)
  const [mobile, setMobile] = useState(() => (MOBILE_PATTERN.test(presetMobile) ? presetMobile : ''))
  const [pin, setPin] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!MOBILE_PATTERN.test(mobile) || !PIN_PATTERN.test(pin)) {
      setError(copy.enterLoginPin)
      return
    }
    setBusy(true)
    setError('')
    try {
      await signIn(mobile, pin)
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
          {routeState?.accountReady && mobile && !error ? (
            <div className="banner banner--ok" role="status">
              {copy.accountReady} — {copy.mobileLabel}: <strong>{mobile}</strong>
            </div>
          ) : null}
          <ErrorBanner message={error} />
          <Field
            label={copy.mobileLabel}
            name="mobile"
            autoComplete="tel"
            inputMode="numeric"
            maxLength={10}
            value={mobile}
            onChange={(event) => setMobile(event.target.value.replace(/\D/g, '').slice(0, 10))}
            hint={copy.mobileHint}
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
        </div>
      </form>
    </section>
  )
}
