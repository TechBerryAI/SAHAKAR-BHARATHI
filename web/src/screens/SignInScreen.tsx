import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { Button, Card, ErrorBanner, Field, Screen } from '../components/ui.tsx'
import { ApiError, signIn } from '../lib/api.ts'

const PIN_PATTERN = /^\d{4,6}$/

export function SignInScreen() {
  const navigate = useNavigate()
  const [identifier, setIdentifier] = useState('')
  const [pin, setPin] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    const login = identifier.trim()
    if (!login || !PIN_PATTERN.test(pin)) {
      setError('Enter your login ID or mobile and a 4 to 6 digit PIN.')
      return
    }
    setBusy(true)
    setError('')
    try {
      await signIn(login, pin)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Sign in failed.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <AppBar title="Sign In" onBack={() => navigate('/')} />
      <Screen>
        <Card kicker="Rath Yatra" title="Registered participant access">
          <p className="card__body">
            Registration and sign in are required before anyone can use the app.
          </p>
        </Card>
        <form className="stack" onSubmit={onSubmit}>
          <ErrorBanner message={error} />
          <Field
            label="Login ID or mobile"
            name="identifier"
            autoComplete="username"
            value={identifier}
            onChange={(event) => setIdentifier(event.target.value)}
            hint="Use the login ID from registration or your 10-digit mobile number."
          />
          <Field
            label="PIN"
            name="pin"
            type="password"
            inputMode="numeric"
            autoComplete="current-password"
            maxLength={6}
            value={pin}
            onChange={(event) => setPin(event.target.value.replace(/\D/g, '').slice(0, 6))}
          />
          <Button type="submit" disabled={busy}>
            {busy ? 'Signing in…' : 'Sign in'}
          </Button>
        </form>
        <Link className="btn btn--ghost" to="/register">
          Create account
        </Link>
      </Screen>
    </>
  )
}
