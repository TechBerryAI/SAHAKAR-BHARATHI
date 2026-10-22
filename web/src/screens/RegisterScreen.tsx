import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { Button, Card, ChoiceChip, ErrorBanner, Field, Screen } from '../components/ui.tsx'
import { ApiError, confirmPhoneVerification, registerAndSignIn, sendPhoneVerification } from '../lib/api.ts'
import { blankToNull } from '../lib/format.ts'
import { getToken } from '../lib/session.ts'
import type { Gender, Occupation, RegistrationCreate } from '../lib/types.ts'
import { GENDERS, OCCUPATIONS } from '../lib/types.ts'

const STEPS = ['Personal', 'Contact', 'Address', 'PIN']

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

function validateStep(step: number, form: FormState, mobileVerified: boolean): string {
  if (step === 0) {
    if (form.full_name.trim().length < 2) return 'Full name is required.'
    if (form.date_of_birth && ageYears(form.date_of_birth) < 18) return 'Minimum age is 18.'
    return ''
  }
  if (step === 1) {
    if (!/^[6-9]\d{9}$/.test(form.mobile)) return 'Enter a valid 10-digit Indian mobile number.'
    if (!mobileVerified) return 'Verify the OTP sent to this mobile number.'
    return ''
  }
  if (step === 2) {
    if (form.pincode && !/^\d{6}$/.test(form.pincode)) return 'PIN code must be 6 digits.'
    return ''
  }
  if (form.pin.length < 4 || form.pin.length > 6 || !/^\d+$/.test(form.pin)) {
    return 'PIN must be 4 to 6 digits.'
  }
  if (form.pin !== form.confirmPin) return 'PIN and confirm PIN must match.'
  return ''
}

export function RegisterScreen() {
  const navigate = useNavigate()
  const [alreadySignedIn] = useState(() => Boolean(getToken()))
  const [step, setStep] = useState(0)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [otpBusy, setOtpBusy] = useState(false)
  const [otp, setOtp] = useState('')
  const [challengeId, setChallengeId] = useState('')
  const [verificationToken, setVerificationToken] = useState('')
  const [otpNote, setOtpNote] = useState('')

  useEffect(() => {
    if (alreadySignedIn) navigate('/', { replace: true })
  }, [alreadySignedIn, navigate])

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
    if (key === 'mobile') {
      setVerificationToken('')
      setChallengeId('')
      setOtp('')
      setOtpNote('')
    }
  }

  async function sendOtp() {
    if (!/^[6-9]\d{9}$/.test(form.mobile)) {
      setError('Enter a valid 10-digit Indian mobile number.')
      return
    }
    setError('')
    setOtpBusy(true)
    try {
      const sent = await sendPhoneVerification(form.mobile)
      setChallengeId(sent.id)
      setVerificationToken('')
      setOtp('')
      setOtpNote('OTP sent. It expires in 5 minutes.')
    } catch (err) {
      setOtpNote('')
      setError(err instanceof ApiError ? err.message : 'Could not send the OTP.')
    } finally {
      setOtpBusy(false)
    }
  }

  async function confirmOtp() {
    if (!challengeId) {
      setError('Send an OTP before verifying.')
      return
    }
    if (!/^\d{6}$/.test(otp)) {
      setError('Enter the 6-digit OTP.')
      return
    }
    setError('')
    setOtpBusy(true)
    try {
      const confirmed = await confirmPhoneVerification(challengeId, otp)
      setVerificationToken(confirmed.verification_token)
      setOtpNote('Mobile number verified.')
    } catch (err) {
      setVerificationToken('')
      setError(err instanceof ApiError ? err.message : 'Could not verify the OTP.')
    } finally {
      setOtpBusy(false)
    }
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
    const message = validateStep(step, form, Boolean(verificationToken))
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
      email: blankToNull(form.email),
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
              <Button type="button" variant="ghost" disabled={otpBusy || Boolean(verificationToken)} onClick={sendOtp}>
                {verificationToken ? 'Mobile verified' : 'Send OTP'}
              </Button>
              {challengeId && !verificationToken ? (
                <>
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
                  <Button type="button" disabled={otpBusy} onClick={confirmOtp}>
                    Verify OTP
                  </Button>
                </>
              ) : null}
              {otpNote ? <p className="field__hint">{otpNote}</p> : null}
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
