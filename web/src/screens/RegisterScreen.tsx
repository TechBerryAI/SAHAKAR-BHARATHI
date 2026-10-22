import { useEffect, useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { Button, Card, ChoiceChip, ErrorBanner, Field, Screen } from '../components/ui.tsx'
import {
  ApiError,
  confirmPhoneVerification,
  registerAndSignIn,
  sendPhoneVerification,
} from '../lib/api.ts'
import { blankToNull } from '../lib/format.ts'
import { useSession } from '../lib/session.ts'
import type { Gender, Occupation, RegistrationCreate } from '../lib/types.ts'
import { GENDERS, OCCUPATIONS } from '../lib/types.ts'

const STEPS = ['Personal', 'Contact', 'Address', 'PIN']
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PIN_PATTERN = /^\d{4,6}$/
const INDIAN_MOBILE = /^[6-9]\d{9}$/

type Gate = 'mobile' | 'details'

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
  pin: string
  confirmPin: string
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
  pin: '',
  confirmPin: '',
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
    if (!INDIAN_MOBILE.test(form.mobile)) return 'Enter a valid 10-digit Indian mobile number.'
    if (form.email.trim() && !EMAIL_PATTERN.test(form.email.trim())) {
      return 'Enter a valid email address.'
    }
    return ''
  }
  if (step === 2) {
    if (form.pincode && !/^\d{6}$/.test(form.pincode)) return 'PIN code must be 6 digits.'
    return ''
  }
  if (!PIN_PATTERN.test(form.pin)) return 'PIN must be 4 to 6 digits.'
  if (form.pin !== form.confirmPin) return 'PIN and confirm PIN must match.'
  return ''
}

export function RegisterScreen() {
  const navigate = useNavigate()
  const { ready, uid } = useSession()
  const createdOnThisPage = useRef(false)
  const [gate, setGate] = useState<Gate>('mobile')
  const [step, setStep] = useState(0)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [otp, setOtp] = useState('')
  const [challengeId, setChallengeId] = useState('')
  const [verificationToken, setVerificationToken] = useState('')
  const [otpNote, setOtpNote] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [otpBusy, setOtpBusy] = useState(false)

  useEffect(() => {
    if (ready && uid && !createdOnThisPage.current) navigate('/', { replace: true })
  }, [ready, uid, navigate])

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
    if (key === 'mobile') {
      setChallengeId('')
      setVerificationToken('')
      setOtp('')
      setOtpNote('')
    }
  }

  function goBack() {
    setError('')
    if (gate === 'mobile') {
      navigate('/')
      return
    }
    if (step === 0) {
      setGate('mobile')
      return
    }
    setStep((current) => current - 1)
  }

  async function sendOtp() {
    if (!INDIAN_MOBILE.test(form.mobile)) {
      setError('Enter a valid 10-digit Indian mobile number.')
      return
    }
    setOtpBusy(true)
    setError('')
    setOtpNote('')
    try {
      const created = await sendPhoneVerification(form.mobile)
      setChallengeId(created.id)
      setVerificationToken('')
      setOtp('')
      setOtpNote('OTP sent. It expires in 5 minutes.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not send the OTP.')
    } finally {
      setOtpBusy(false)
    }
  }

  async function verifyOtp(event: FormEvent) {
    event.preventDefault()
    if (!challengeId) {
      setError('Send an OTP first.')
      return
    }
    if (otp.length !== 6) {
      setError('Enter the 6-digit OTP.')
      return
    }
    setOtpBusy(true)
    setError('')
    try {
      const confirmed = await confirmPhoneVerification(challengeId, otp)
      setVerificationToken(confirmed.verification_token)
      setOtpNote('Mobile number verified.')
      setStep(0)
      setGate('details')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not verify the OTP.')
    } finally {
      setOtpBusy(false)
    }
  }

  async function onContinue(event: FormEvent) {
    event.preventDefault()
    const message = validateStep(step, form)
    if (message) {
      setError(message)
      return
    }
    if (!verificationToken) {
      setError('Verify the OTP sent to this mobile number.')
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
      email: blankToNull(form.email.trim()),
      address_line: blankToNull(form.address_line),
      locality: blankToNull(form.locality),
      district: blankToNull(form.district),
      state: blankToNull(form.state),
      pincode: blankToNull(form.pincode),
      pin: form.pin,
      verification_token: verificationToken,
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

  if (gate !== 'details') {
    return (
      <section className="verify">
        <header className="verify__bar">
          <button type="button" className="verify__back" aria-label="Back" onClick={goBack}>
            <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M19 11H7.83l4.58-4.59L11 5l-7 7 7 7 1.41-1.41L7.83 13H19z" />
            </svg>
          </button>
          <h1>Verify mobile</h1>
        </header>
        <form
          className="verify__form"
          onSubmit={(event) => {
            event.preventDefault()
            if (!challengeId) void sendOtp()
            else void verifyOtp(event)
          }}
        >
          <div className="verify__body">
            <ErrorBanner message={error} />
            <Card title="Mobile number">
              <p className="card__body">
                Enter your mobile number before registration. Send the OTP, verify it, and the
                personal details page will open.
              </p>
            </Card>
            <Card>
              <label className="field" htmlFor="mobile">
                <span className="field__label">Mobile number</span>
                <span className="verify__phone">
                  <span className="verify__prefix">+91</span>
                  <input
                    id="mobile"
                    name="mobile"
                    className="verify__input"
                    inputMode="numeric"
                    autoComplete="tel"
                    maxLength={10}
                    value={form.mobile}
                    onChange={(event) =>
                      update('mobile', event.target.value.replace(/\D/g, '').slice(0, 10))
                    }
                  />
                </span>
                <p className="field__hint">10-digit Indian mobile number.</p>
              </label>
              {challengeId ? (
                <div className="verify__otp">
                  <Field
                    label="OTP"
                    name="otp"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    maxLength={6}
                    value={otp}
                    onChange={(event) => setOtp(event.target.value.replace(/\D/g, '').slice(0, 6))}
                    hint="6-digit code from the SMS."
                  />
                  <button
                    type="button"
                    className="btn btn--text"
                    disabled={otpBusy}
                    onClick={() => {
                      void sendOtp()
                    }}
                  >
                    Resend OTP
                  </button>
                </div>
              ) : null}
              {otpNote ? <p className="field__hint">{otpNote}</p> : null}
            </Card>
          </div>
          <div className="verify__footer">
            <Button type="submit" className="verify__submit" disabled={otpBusy}>
              {otpBusy ? 'Please wait…' : challengeId ? 'Verify OTP' : 'Send OTP'}
              <span aria-hidden="true">→</span>
            </Button>
          </div>
        </form>
      </section>
    )
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
                readOnly
                value={form.mobile}
                hint="Verified with OTP."
              />
              <Field
                label="Email"
                name="email"
                type="email"
                autoComplete="email"
                value={form.email}
                onChange={(event) => update('email', event.target.value)}
                hint="Optional."
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
                label="Create PIN"
                name="pin"
                type="password"
                inputMode="numeric"
                autoComplete="new-password"
                maxLength={6}
                value={form.pin}
                onChange={(event) => update('pin', event.target.value.replace(/\D/g, '').slice(0, 6))}
                hint="4 to 6 digits. Needed every time you sign in."
              />
              <Field
                label="Confirm PIN"
                name="confirmPin"
                type="password"
                inputMode="numeric"
                autoComplete="new-password"
                maxLength={6}
                value={form.confirmPin}
                onChange={(event) =>
                  update('confirmPin', event.target.value.replace(/\D/g, '').slice(0, 6))
                }
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
