import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { Button, Card, ChoiceChip, ErrorBanner, Field, Screen } from '../components/ui.tsx'
import { ApiError, registerAndSignIn } from '../lib/api.ts'
import { blankToNull } from '../lib/format.ts'
import { useSession } from '../lib/session.ts'
import type { Gender, Occupation, RegistrationCreate } from '../lib/types.ts'
import { GENDERS, OCCUPATIONS } from '../lib/types.ts'

const STEPS = ['Personal', 'Contact', 'Address', 'Password']
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

type FormState = {
  full_name: string
  gender: Gender | ''
  date_of_birth: string
  occupation: Occupation | ''
  mobile: string
  email: string
  address_line: string
  locality: string
  district: string
  state: string
  pincode: string
  password: string
  confirmPassword: string
}

const emptyForm: FormState = {
  full_name: '',
  gender: '',
  date_of_birth: '',
  occupation: '',
  mobile: '',
  email: '',
  address_line: '',
  locality: '',
  district: '',
  state: '',
  pincode: '',
  password: '',
  confirmPassword: '',
}

function ageYears(iso: string): number {
  const dob = new Date(`${iso}T00:00:00`)
  if (Number.isNaN(dob.getTime())) return -1
  const today = new Date()
  let years = today.getFullYear() - dob.getFullYear()
  const beforeBirthday =
    today.getMonth() < dob.getMonth() ||
    (today.getMonth() === dob.getMonth() && today.getDate() < dob.getDate())
  if (beforeBirthday) years -= 1
  return years
}

function validateStep(step: number, form: FormState): string {
  if (step === 0) {
    if (form.full_name.trim().length < 2) return 'Full name is required.'
    if (form.date_of_birth && ageYears(form.date_of_birth) < 18) return 'Minimum age is 18.'
    return ''
  }
  if (step === 1) {
    if (!/^[6-9]\d{9}$/.test(form.mobile)) return 'Enter a valid 10-digit Indian mobile number.'
    if (!EMAIL_PATTERN.test(form.email.trim())) return 'Enter a valid email address.'
    return ''
  }
  if (step === 2) {
    if (form.pincode && !/^\d{6}$/.test(form.pincode)) return 'PIN code must be 6 digits.'
    return ''
  }
  if (form.password.length < 6) return 'Password must be at least 6 characters.'
  if (form.password !== form.confirmPassword) return 'Password and confirm password must match.'
  return ''
}

export function RegisterScreen() {
  const navigate = useNavigate()
  const { ready, uid } = useSession()
  const createdOnThisPage = useRef(false)
  const [step, setStep] = useState(0)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (ready && uid && !createdOnThisPage.current) navigate('/', { replace: true })
  }, [ready, uid, navigate])

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  function goBack() {
    if (step === 0) {
      navigate('/sign-in')
      return
    }
    setError('')
    setStep((current) => current - 1)
  }

  async function onContinue(event: FormEvent) {
    event.preventDefault()
    const message = validateStep(step, form)
    if (message) {
      setError(message)
      return
    }
    setError('')
    if (step < 3) {
      setStep((current) => current + 1)
      return
    }

    const body: RegistrationCreate = {
      full_name: form.full_name.trim(),
      gender: form.gender || null,
      date_of_birth: blankToNull(form.date_of_birth),
      occupation: form.occupation || null,
      mobile: form.mobile,
      email: form.email.trim(),
      address_line: blankToNull(form.address_line),
      locality: blankToNull(form.locality),
      district: blankToNull(form.district),
      state: blankToNull(form.state),
      pincode: blankToNull(form.pincode),
      password: form.password,
    }

    setBusy(true)
    try {
      createdOnThisPage.current = true
      await registerAndSignIn(body)
      navigate('/register/success', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Registration failed.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <AppBar title="Register" onBack={goBack} />
      <Screen>
        <div className="progress" aria-label={`Step ${step + 1} of 4`}>
          {STEPS.map((label, index) => (
            <span
              key={label}
              className={`progress__step${index < step ? ' is-done' : ''}${index === step ? ' is-current' : ''}`}
            />
          ))}
        </div>
        <Card kicker={`Step ${step + 1} of 4`} title={STEPS[step]} />
        <form className="stack" onSubmit={onContinue}>
          <ErrorBanner message={error} />
          {step === 0 ? (
            <>
              <Field
                label="Full name"
                name="full_name"
                autoComplete="name"
                value={form.full_name}
                onChange={(event) => update('full_name', event.target.value)}
              />
              <div className="field">
                <span className="field__label">Gender</span>
                <div className="chips">
                  {GENDERS.map((gender) => (
                    <ChoiceChip
                      key={gender}
                      label={gender}
                      selected={form.gender === gender}
                      onClick={() => update('gender', form.gender === gender ? '' : gender)}
                    />
                  ))}
                </div>
              </div>
              <Field
                label="Date of birth"
                name="date_of_birth"
                type="date"
                value={form.date_of_birth}
                onChange={(event) => update('date_of_birth', event.target.value)}
                hint="Optional. Participants must be 18 or older."
              />
              <div className="field">
                <span className="field__label">Occupation</span>
                <div className="chips">
                  {OCCUPATIONS.map((occupation) => (
                    <ChoiceChip
                      key={occupation}
                      label={occupation}
                      selected={form.occupation === occupation}
                      onClick={() =>
                        update('occupation', form.occupation === occupation ? '' : occupation)
                      }
                    />
                  ))}
                </div>
              </div>
            </>
          ) : null}
          {step === 1 ? (
            <>
              <Field
                label="Mobile"
                name="mobile"
                inputMode="numeric"
                autoComplete="tel"
                maxLength={10}
                value={form.mobile}
                onChange={(event) => update('mobile', event.target.value.replace(/\D/g, '').slice(0, 10))}
                hint="10-digit Indian mobile number."
              />
              <Field
                label="Email"
                name="email"
                type="email"
                autoComplete="email"
                value={form.email}
                onChange={(event) => update('email', event.target.value)}
                hint="Required. You will use this email to sign in."
              />
            </>
          ) : null}
          {step === 2 ? (
            <>
              <Field
                label="Address"
                name="address_line"
                autoComplete="street-address"
                value={form.address_line}
                onChange={(event) => update('address_line', event.target.value)}
              />
              <Field
                label="Locality"
                name="locality"
                value={form.locality}
                onChange={(event) => update('locality', event.target.value)}
              />
              <Field
                label="District"
                name="district"
                value={form.district}
                onChange={(event) => update('district', event.target.value)}
              />
              <Field
                label="State"
                name="state"
                value={form.state}
                onChange={(event) => update('state', event.target.value)}
              />
              <Field
                label="PIN code"
                name="pincode"
                inputMode="numeric"
                maxLength={6}
                value={form.pincode}
                onChange={(event) => update('pincode', event.target.value.replace(/\D/g, '').slice(0, 6))}
              />
            </>
          ) : null}
          {step === 3 ? (
            <>
              <Field
                label="Create password"
                name="password"
                type="password"
                autoComplete="new-password"
                value={form.password}
                onChange={(event) => update('password', event.target.value)}
                hint="At least 6 characters. Needed every time you sign in."
              />
              <Field
                label="Confirm password"
                name="confirmPassword"
                type="password"
                autoComplete="new-password"
                value={form.confirmPassword}
                onChange={(event) => update('confirmPassword', event.target.value)}
              />
            </>
          ) : null}
          <Button type="submit" disabled={busy}>
            {busy ? 'Registering…' : step === 3 ? 'Create account' : 'Continue'}
          </Button>
        </form>
      </Screen>
    </>
  )
}
