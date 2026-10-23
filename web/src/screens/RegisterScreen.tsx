import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Card, ErrorBanner, Field, RequiredMark } from '../components/ui.tsx'
import {
  ApiError,
  apiErrorText,
  confirmPhoneVerification,
  registerAndSignIn,
  sendPhoneVerification,
} from '../lib/api.ts'
import { blankToNull } from '../lib/format.ts'
import type { AppCopy } from '../lib/copy.ts'
import { useLanguage } from '../lib/language.tsx'
import { useSession } from '../lib/session.ts'
import type { Gender, Occupation, RegistrationCreate } from '../lib/types.ts'
import { GENDERS, OCCUPATIONS } from '../lib/types.ts'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PIN_PATTERN = /^\d{4,6}$/
const PINCODE_PATTERN = /^\d{6}$/
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

function titlesFor(copy: AppCopy) {
  return [copy.formTitle, copy.contactTitle, copy.profileTitle, copy.securityTitle]
}

function tracksFor(copy: AppCopy) {
  return [copy.stepPersonalTrack, copy.stepContactTrack, copy.stepProfileTrack, copy.stepSecurityTrack]
}

function sectionId(index: number) {
  return `reg-section-${index}`
}

function genderLabel(copy: AppCopy, gender: Gender) {
  if (gender === 'Male') return copy.male
  if (gender === 'Female') return copy.female
  return copy.other
}

function occupationLabel(copy: AppCopy, occupation: Occupation) {
  if (occupation === 'Farmer') return copy.farmer
  if (occupation === 'Fisherman') return copy.fisherman
  if (occupation === 'Both') return copy.both
  return copy.allied
}

function isValidIsoDate(iso: string): boolean {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso)
  if (!match) return false
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const date = new Date(year, month - 1, day)
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day
}

function joinDob(year: string, month: string, day: string) {
  if (!year || !month || !day) return ''
  return `${year}-${month}-${day.padStart(2, '0')}`
}

function validateSection(section: number, form: FormState, copy: AppCopy): string {
  if (section === 0) {
    if (form.full_name.trim().length < 2) return copy.fullNameRequired
    if (!form.gender) return copy.genderRequired
    if (!form.date_of_birth || !isValidIsoDate(form.date_of_birth)) return copy.dobRequired
    if (ageYears(form.date_of_birth) < 18) return copy.minAge
    if (!form.occupation) return copy.occupationRequired
    return ''
  }
  if (section === 1) {
    if (!INDIAN_MOBILE.test(form.mobile)) return copy.mobileRequired
    if (form.email.trim() && !EMAIL_PATTERN.test(form.email.trim())) return copy.validEmail
    return ''
  }
  if (section === 2) {
    if (!form.district.trim()) return copy.districtRequired
    if (!form.state.trim()) return copy.stateRequired
    if (!PINCODE_PATTERN.test(form.pincode)) return copy.pinDigits
    return ''
  }
  if (!PIN_PATTERN.test(form.pin)) return copy.passwordLength
  if (form.pin !== form.confirmPin) return copy.passwordMatch
  return ''
}

// Required items per section, mirroring validateSection: a section is complete
// exactly when all of its items pass. Optional fields only count when they are invalid.
function sectionChecks(section: number, form: FormState): boolean[] {
  if (section === 0) {
    const dobOk =
      isValidIsoDate(form.date_of_birth) && ageYears(form.date_of_birth) >= 18
    return [form.full_name.trim().length >= 2, Boolean(form.gender), dobOk, Boolean(form.occupation)]
  }
  if (section === 1) {
    const emailOk = !form.email.trim() || EMAIL_PATTERN.test(form.email.trim())
    return [INDIAN_MOBILE.test(form.mobile), emailOk]
  }
  if (section === 2) {
    return [Boolean(form.district.trim()), Boolean(form.state.trim()), PINCODE_PATTERN.test(form.pincode)]
  }
  return [PIN_PATTERN.test(form.pin), form.confirmPin !== '' && form.confirmPin === form.pin]
}

export function RegisterScreen() {
  const navigate = useNavigate()
  const { copy } = useLanguage()
  const { ready, uid } = useSession()
  const createdOnThisPage = useRef(false)
  const bodyRef = useRef<HTMLDivElement>(null)
  const verifyErrorRef = useRef<HTMLDivElement>(null)
  const [gate, setGate] = useState<Gate>('mobile')
  const [form, setForm] = useState<FormState>(emptyForm)
  const [otp, setOtp] = useState('')
  const [challengeId, setChallengeId] = useState('')
  const [verificationToken, setVerificationToken] = useState('')
  const [otpNote, setOtpNote] = useState('')
  const [error, setError] = useState('')
  // Section whose validation failed; null shows the error at the top of the form.
  const [errorSection, setErrorSection] = useState<number | null>(null)
  const [activeSection, setActiveSection] = useState(0)
  const [busy, setBusy] = useState(false)
  const [otpBusy, setOtpBusy] = useState(false)
  const [showSos, setShowSos] = useState(false)
  const [dobDay, setDobDay] = useState('')
  const [dobMonth, setDobMonth] = useState('')
  const [dobYear, setDobYear] = useState('')
  const [locating, setLocating] = useState(false)
  const [locationNote, setLocationNote] = useState('')
  const titles = titlesFor(copy)
  const tracks = tracksFor(copy)
  const fractions = titles.map((_, index) => {
    const checks = sectionChecks(index, form)
    return checks.filter(Boolean).length / checks.length
  })
  const completed = fractions.map((fraction) => fraction === 1)
  const doneCount = completed.filter(Boolean).length
  const progressPercent = Math.round(
    (fractions.reduce((sum, fraction) => sum + fraction, 0) / fractions.length) * 100,
  )
  const progressBadge = copy.formProgress
    .replace('{done}', String(doneCount))
    .replace('{percent}', String(progressPercent))
  const currentYear = new Date().getFullYear()
  const years = Array.from({ length: 90 }, (_, index) => String(currentYear - 10 - index))
  const months = Array.from({ length: 12 }, (_, index) => String(index + 1).padStart(2, '0'))
  const days = Array.from({ length: 31 }, (_, index) => String(index + 1).padStart(2, '0'))

  useEffect(() => {
    if (ready && uid && !createdOnThisPage.current) navigate('/', { replace: true })
  }, [ready, uid, navigate])

  // The verify panel scrolls; without this a wrong-OTP error can sit out of view
  // and people keep pressing Verify until the code locks.
  useEffect(() => {
    if (error && gate !== 'details') {
      verifyErrorRef.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    }
  }, [error, gate])

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }))
    if (key === 'mobile') {
      setChallengeId('')
      setVerificationToken('')
      setOtp('')
      setOtpNote('')
    }
  }

  async function fillFromLocation() {
    if (locating || !navigator.geolocation) {
      setLocationNote(copy.locationUnavailable)
      return
    }
    setLocating(true)
    setLocationNote(copy.locatingAddress)
    try {
      const position = await new Promise<GeolocationPosition>((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(resolve, reject, {
          enableHighAccuracy: true,
          timeout: 12000,
        })
      })
      const { latitude, longitude } = position.coords
      const response = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${latitude}&lon=${longitude}`,
        { headers: { Accept: 'application/json' } },
      )
      if (!response.ok) throw new Error('geocode')
      const data = (await response.json()) as {
        address?: Record<string, string>
        display_name?: string
      }
      const place = data.address ?? {}
      const line = [place.house_number, place.road || place.pedestrian || place.residential]
        .filter(Boolean)
        .join(' ')
      setForm((current) => ({
        ...current,
        address_line: line || data.display_name?.split(',')[0] || current.address_line,
        locality:
          place.village ||
          place.town ||
          place.suburb ||
          place.neighbourhood ||
          place.city_district ||
          current.locality,
        district: place.county || place.state_district || place.city || current.district,
        state: place.state || current.state,
        pincode: (place.postcode || '').replace(/\D/g, '').slice(0, 6) || current.pincode,
      }))
      setLocationNote(copy.locationFilled)
    } catch (err) {
      const code = typeof err === 'object' && err && 'code' in err ? Number(err.code) : 0
      setLocationNote(code === 1 ? copy.locationDenied : copy.locationUnavailable)
    } finally {
      setLocating(false)
    }
  }

  function setDob(next: { day?: string; month?: string; year?: string }) {
    const day = next.day ?? dobDay
    const month = next.month ?? dobMonth
    const year = next.year ?? dobYear
    if (next.day !== undefined) setDobDay(next.day)
    if (next.month !== undefined) setDobMonth(next.month)
    if (next.year !== undefined) setDobYear(next.year)
    update('date_of_birth', joinDob(year, month, day))
  }

  function goBack() {
    setError('')
    setErrorSection(null)
    if (gate === 'mobile') {
      navigate('/')
      return
    }
    setGate('mobile')
  }

  function scrollToSection(section: number) {
    document.getElementById(sectionId(section))?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  // The section in view is the last one whose heading has passed 30% of the scroll area;
  // once scrolled to the very bottom it is the last section.
  function trackActiveSection() {
    const body = bodyRef.current
    if (!body) return
    if (body.scrollTop > 0 && body.scrollTop + body.clientHeight >= body.scrollHeight - 4) {
      setActiveSection(titles.length - 1)
      return
    }
    const marker = body.getBoundingClientRect().top + body.clientHeight * 0.3
    let current = 0
    titles.forEach((_, index) => {
      const top = document.getElementById(sectionId(index))?.getBoundingClientRect().top
      if (top !== undefined && top <= marker) current = index
    })
    setActiveSection(current)
  }

  function showError(message: string, section: number | null) {
    setError(message)
    setErrorSection(section)
    if (section === null) bodyRef.current?.scrollTo({ top: 0, behavior: 'smooth' })
    else scrollToSection(section)
  }

  function sectionProps(index: number) {
    return {
      index,
      title: titles[index],
      done: completed[index],
      error: errorSection === index ? error : '',
    }
  }

  async function sendOtp() {
    if (!INDIAN_MOBILE.test(form.mobile)) {
      setError(copy.mobileRequired)
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
      setOtpNote(copy.otpSent)
    } catch (err) {
      setError(apiErrorText(err, copy, copy.otpSendFailed))
    } finally {
      setOtpBusy(false)
    }
  }

  async function verifyOtp(event: FormEvent) {
    event.preventDefault()
    if (!challengeId) {
      setError(copy.otpSendFirst)
      return
    }
    if (otp.length !== 6) {
      setError(copy.otpRequired)
      return
    }
    setOtpBusy(true)
    setError('')
    try {
      const confirmed = await confirmPhoneVerification(challengeId, otp)
      setVerificationToken(confirmed.verification_token)
      setOtpNote(copy.otpVerified)
      setGate('details')
    } catch (err) {
      setError(apiErrorText(err, copy, copy.otpVerifyFailed))
      if (err instanceof ApiError && err.status === 429) {
        // This code can no longer be verified; go back to "Send OTP" for a new one.
        setChallengeId('')
        setOtp('')
        setOtpNote('')
      }
    } finally {
      setOtpBusy(false)
    }
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    for (let section = 0; section < titles.length; section += 1) {
      const message = validateSection(section, form, copy)
      if (message) {
        showError(message, section)
        return
      }
    }
    if (!verificationToken) {
      showError(copy.otpVerifyRequired, 1)
      return
    }
    setError('')
    setErrorSection(null)

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
      showError(apiErrorText(err, copy, copy.registrationFailed), null)
    } finally {
      setBusy(false)
    }
  }

  if (gate !== 'details') {
    return (
      <section className="verify">
        <header className="verify__bar">
          <button type="button" className="verify__back" aria-label={copy.back} onClick={goBack}>
            <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M19 11H7.83l4.58-4.59L11 5l-7 7 7 7 1.41-1.41L7.83 13H19z" />
            </svg>
          </button>
          <h1>{copy.verifyMobile}</h1>
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
            <Card title={copy.mobileTitle}>
              <p className="card__body">{copy.mobileBody}</p>
            </Card>
            <Card>
              <label className="field" htmlFor="mobile">
                <span className="field__label">{copy.mobileLabel}</span>
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
                <p className="field__hint">{copy.mobileHint}</p>
              </label>
              {challengeId ? (
                <div className="verify__otp">
                  <Field
                    label={copy.otpLabel}
                    name="otp"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    maxLength={6}
                    value={otp}
                    onChange={(event) => setOtp(event.target.value.replace(/\D/g, '').slice(0, 6))}
                    hint={copy.otpSmsHint}
                  />
                  <button
                    type="button"
                    className="btn btn--text"
                    disabled={otpBusy}
                    onClick={() => {
                      void sendOtp()
                    }}
                  >
                    {copy.resendOtp}
                  </button>
                </div>
              ) : null}
              {otpNote ? <p className="field__hint">{otpNote}</p> : null}
            </Card>
            {error ? (
              <div ref={verifyErrorRef}>
                <ErrorBanner message={error} />
              </div>
            ) : null}
          </div>
          <div className="verify__footer">
            <Button type="submit" className="verify__submit" disabled={otpBusy}>
              {otpBusy ? copy.pleaseWait : challengeId ? copy.verifyOtp : copy.sendOtp}
              <span aria-hidden="true">→</span>
            </Button>
          </div>
        </form>
      </section>
    )
  }

  return (
    <section className="reg">
      <header className="reg__bar">
        <button type="button" className="reg__back" aria-label={copy.back} onClick={goBack}>
          <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M19 11H7.83l4.58-4.59L11 5l-7 7 7 7 1.41-1.41L7.83 13H19z" />
          </svg>
        </button>
        <h1>{copy.registrationTitle}</h1>
        <button type="button" className="reg__sos" onClick={() => setShowSos(true)}>
          {copy.sos}
        </button>
      </header>
      <div className="reg__accent" />
      <form className="reg__form" onSubmit={onSubmit} noValidate>
        <div className="reg__body" ref={bodyRef} onScroll={trackActiveSection}>
          {errorSection === null ? <ErrorBanner message={error} /> : null}
          <section className="reg-card">
            <div className="reg-card__row">
              <span className="reg-chip">
                <span className="reg-chip__dot" />
                {progressBadge}
              </span>
              <span className="reg-portal">
                <svg width="14" height="14" viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    fill="currentColor"
                    d="M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11z"
                  />
                </svg>
                {copy.govtPortal}
              </span>
            </div>
            <ol className="reg-steps">
              {tracks.map((label, index) => (
                <li key={label}>
                  <button
                    type="button"
                    className={
                      [completed[index] && 'is-done', index === activeSection && 'is-active']
                        .filter(Boolean)
                        .join(' ') || undefined
                    }
                    aria-current={index === activeSection ? 'step' : undefined}
                    onClick={() => scrollToSection(index)}
                  >
                    <span className="reg-steps__bar" aria-hidden="true">
                      <span
                        className="reg-steps__fill"
                        style={{ width: `${Math.round(fractions[index] * 100)}%` }}
                      />
                    </span>
                    <span className="reg-steps__label">
                      {completed[index] ? (
                        <svg
                          className="reg-steps__check"
                          width="12"
                          height="12"
                          viewBox="0 0 24 24"
                          aria-hidden="true"
                        >
                          <path fill="currentColor" d="M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
                        </svg>
                      ) : null}
                      {label}
                    </span>
                  </button>
                </li>
              ))}
            </ol>
          </section>

          <RegSection {...sectionProps(0)}>
            <section className="reg-card">
              <label className="field" htmlFor="full_name">
                <span className="field__label">
                  {copy.legalName}
                  <RequiredMark />
                </span>
                <input
                  id="full_name"
                  name="full_name"
                  className="field__control"
                  autoComplete="name"
                  required
                  value={form.full_name}
                  onChange={(event) => update('full_name', event.target.value)}
                />
                <p className="field__hint">{copy.nameAadhaarHint}</p>
              </label>
            </section>
            <section className="reg-card">
              <span className="field__label" id="gender_label">
                {copy.gender}
                <RequiredMark />
              </span>
              <div className="reg-genders" role="group" aria-labelledby="gender_label">
                {GENDERS.map((gender) => (
                  <button
                    key={gender}
                    type="button"
                    className={`reg-gender${form.gender === gender ? ' is-selected' : ''}`}
                    aria-pressed={form.gender === gender}
                    onClick={() => update('gender', form.gender === gender ? '' : gender)}
                  >
                    <GenderIcon gender={gender} />
                    {genderLabel(copy, gender)}
                  </button>
                ))}
              </div>
            </section>
            <section className="reg-card">
              <span className="field__label">
                {copy.dobFull}
                <RequiredMark />
              </span>
              <div className="reg-dob">
                <select
                  className="field__control"
                  aria-label={copy.dobDay}
                  required
                  value={dobDay}
                  onChange={(event) => setDob({ day: event.target.value })}
                >
                  <option value="">{copy.dobDay}</option>
                  {days.map((day) => (
                    <option key={day} value={day}>
                      {day}
                    </option>
                  ))}
                </select>
                <select
                  className="field__control"
                  aria-label={copy.dobMonth}
                  required
                  value={dobMonth}
                  onChange={(event) => setDob({ month: event.target.value })}
                >
                  <option value="">{copy.dobMonth}</option>
                  {months.map((month) => (
                    <option key={month} value={month}>
                      {month}
                    </option>
                  ))}
                </select>
                <select
                  className="field__control"
                  aria-label={copy.dobYear}
                  required
                  value={dobYear}
                  onChange={(event) => setDob({ year: event.target.value })}
                >
                  <option value="">{copy.dobYear}</option>
                  {years.map((year) => (
                    <option key={year} value={year}>
                      {year}
                    </option>
                  ))}
                </select>
              </div>
              <p className="field__hint">{copy.ageMin}</p>
            </section>
            <section className="reg-card">
              <span className="field__label">
                {copy.occupation}
                <RequiredMark />
              </span>
              <p className="field__hint">{copy.occupationHint}</p>
              <select
                className="field__control"
                aria-label={copy.occupation}
                required
                value={form.occupation}
                onChange={(event) => update('occupation', event.target.value as Occupation | '')}
              >
                <option value="">{copy.occupationSelect}</option>
                {OCCUPATIONS.map((occupation) => (
                  <option key={occupation} value={occupation}>
                    {occupationLabel(copy, occupation)}
                  </option>
                ))}
              </select>
            </section>
          </RegSection>

          <RegSection {...sectionProps(1)}>
            <section className="reg-card">
              <p className="reg-lead__title">{copy.contactCardTitle}</p>
              <p className="reg-lead__body">{copy.contactCardBody}</p>
            </section>
            <section className="reg-card">
              <label className="field" htmlFor="contact_mobile">
                <span className="field__label">{copy.mobileLabel}</span>
                <span className="reg-phone">
                  <span className="reg-phone__prefix">+91</span>
                  <input
                    id="contact_mobile"
                    name="mobile"
                    className="reg-phone__input"
                    readOnly
                    value={form.mobile}
                  />
                </span>
                <p className="field__hint">{copy.contactVerifiedHint}</p>
              </label>
            </section>
            <section className="reg-card">
              <Field
                label={copy.emailOptional}
                name="email"
                type="email"
                autoComplete="email"
                value={form.email}
                onChange={(event) => update('email', event.target.value)}
                hint={copy.contactEmailHint}
              />
            </section>
          </RegSection>

          <RegSection {...sectionProps(2)}>
            <section className="reg-card">
              <p className="reg-lead__body">{copy.addressCardBody}</p>
              <button
                type="button"
                className="reg-locate"
                onClick={() => void fillFromLocation()}
                disabled={locating}
              >
                <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
                  <path
                    fill="currentColor"
                    d="M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8m8.94 3A8.99 8.99 0 0 0 13 3.06V1h-2v2.06A8.99 8.99 0 0 0 3.06 11H1v2h2.06A8.99 8.99 0 0 0 11 20.94V23h2v-2.06A8.99 8.99 0 0 0 20.94 13H23v-2zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7"
                  />
                </svg>
                <span>
                  <span className="reg-locate__title">
                    {locating ? copy.locatingAddress : copy.useMyLocation}
                  </span>
                  <span className="reg-locate__hint">{copy.useMyLocationHint}</span>
                </span>
              </button>
              {locationNote ? <p className="field__hint">{locationNote}</p> : null}
              <Field
                label={copy.houseLabel}
                name="address_line"
                autoComplete="street-address"
                value={form.address_line}
                onChange={(event) => update('address_line', event.target.value)}
                hint={copy.houseHint}
              />
              <Field
                label={copy.localityLabel}
                name="locality"
                value={form.locality}
                onChange={(event) => update('locality', event.target.value)}
              />
              <div className="reg-split">
                <Field
                  label={copy.districtLabel}
                  name="district"
                  required
                  value={form.district}
                  onChange={(event) => update('district', event.target.value)}
                />
                <Field
                  label={copy.stateLabel}
                  name="state"
                  required
                  value={form.state}
                  onChange={(event) => update('state', event.target.value)}
                />
              </div>
              <Field
                label={copy.pincodeLabel}
                name="pincode"
                inputMode="numeric"
                maxLength={6}
                required
                value={form.pincode}
                onChange={(event) => update('pincode', event.target.value.replace(/\D/g, '').slice(0, 6))}
              />
            </section>
          </RegSection>

          <RegSection {...sectionProps(3)}>
            <section className="reg-card">
              <p className="reg-lead__title">{copy.securityCardTitle}</p>
              <p className="reg-lead__body">{copy.securityCardBody}</p>
            </section>
            <section className="reg-card">
              <Field
                label={copy.securityPinLabel}
                name="pin"
                type="password"
                inputMode="numeric"
                autoComplete="new-password"
                maxLength={6}
                required
                value={form.pin}
                onChange={(event) => update('pin', event.target.value.replace(/\D/g, '').slice(0, 6))}
                hint={copy.securityPinHint}
                revealable
                revealShowLabel={copy.showPin}
                revealHideLabel={copy.hidePin}
              />
            </section>
            <section className="reg-card">
              <Field
                label={copy.securityConfirmPin}
                name="confirmPin"
                type="password"
                inputMode="numeric"
                autoComplete="new-password"
                maxLength={6}
                required
                value={form.confirmPin}
                onChange={(event) =>
                  update('confirmPin', event.target.value.replace(/\D/g, '').slice(0, 6))
                }
              />
            </section>
          </RegSection>

          <section className="reg-card reg-privacy">
            <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill="currentColor"
                d="M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5zm-1 6h2v2h-2zm0 4h2v6h-2z"
              />
            </svg>
            <div>
              <p className="reg-privacy__title">{copy.privacyTitle}</p>
              <p className="reg-privacy__body">{copy.privacyBody}</p>
            </div>
          </section>
        </div>
        <div className="reg__footer">
          <Button type="submit" className="reg__submit" disabled={busy}>
            {busy ? copy.registering : copy.createAccountBtn}
            {busy ? null : <span aria-hidden="true">→</span>}
          </Button>
        </div>
      </form>
      {showSos ? (
        <div className="dialog-backdrop" role="presentation">
          <div className="dialog" role="dialog" aria-labelledby="sos-title">
            <h2 className="card__title" id="sos-title">
              {copy.sosTitle}
            </h2>
            <p className="card__body">{copy.sosBody}</p>
            <div className="stack" style={{ marginTop: 16 }}>
              <Button type="button" onClick={() => setShowSos(false)}>
                {copy.cancel}
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  )
}

function RegSection({
  index,
  title,
  done,
  error,
  children,
}: {
  index: number
  title: string
  done: boolean
  error: string
  children: ReactNode
}) {
  const id = sectionId(index)
  const headingId = `${id}-title`
  return (
    <section className="reg-section" id={id} aria-labelledby={headingId}>
      <h2 className="reg-section__title" id={headingId}>
        <span className={`reg-section__num${done ? ' is-done' : ''}`} aria-hidden="true">
          {index + 1}
        </span>
        {title}
      </h2>
      <ErrorBanner message={error} />
      {children}
    </section>
  )
}

function GenderIcon({ gender }: { gender: Gender }) {
  if (gender === 'Male') {
    return (
      <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden="true">
        <path
          fill="currentColor"
          d="M20 4h-6v2h2.6l-3.28 3.28A5.98 5.98 0 0 0 9 8a6 6 0 1 0 6 6c0-1.3-.42-2.5-1.12-3.48L17.4 8H20zm-11 14a4 4 0 1 1 0-8 4 4 0 0 1 0 8"
        />
      </svg>
    )
  }
  if (gender === 'Female') {
    return (
      <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden="true">
        <path
          fill="currentColor"
          d="M12 4a6 6 0 0 1 1 11.92V18h3v2h-3v2h-2v-2H8v-2h3v-2.08A6 6 0 0 1 12 4m0 2a4 4 0 1 0 0 8 4 4 0 0 0 0-8"
        />
      </svg>
    )
  }
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="currentColor"
        d="M12 2a6 6 0 0 1 1 11.92V16h3v2h-3v3h-2v-3H8v-2h3v-2.08A6 6 0 0 1 12 2m7 1v2h-2.6l1.3 1.3-1.4 1.4L15 6.4V9h-2V4zM12 4a4 4 0 1 0 0 8 4 4 0 0 0 0-8"
      />
    </svg>
  )
}
