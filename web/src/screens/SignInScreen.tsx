import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { Button, Card, ErrorBanner, Field, Screen } from '../components/ui.tsx'
import { ApiError, signIn } from '../lib/api.ts'

export function SignInScreen() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    const login = email.trim()
    if (!login || password.length < 6) {
      setError('Enter your email and password.')
      return
    }
    setBusy(true)
    setError('')
    try {
      await signIn(login, password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Sign in failed.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <AppBar title="Sign In" showEmblem />
      <Screen>
        <Card kicker="Rath Yatra" title="Registered participant access">
          <p className="card__body">
            Registration and sign in are required before anyone can use the app.
          </p>
        </Card>
        <form className="stack" onSubmit={onSubmit}>
          <ErrorBanner message={error} />
          <Field
            label="Email"
            name="email"
            type="email"
            autoComplete="email"
            inputMode="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            hint="Use the email from registration."
          />
          <Field
            label="Password"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
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
