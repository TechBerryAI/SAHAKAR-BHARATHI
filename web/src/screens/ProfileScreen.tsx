import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import {
  Button,
  Card,
  ChoiceChip,
  ErrorBanner,
  Field,
  Screen,
  StatusChip,
} from '../components/ui.tsx'
import { ApiError, apiErrorText, getMe, logout, updateMe } from '../lib/api.ts'
import { occupationLabel } from '../lib/copy.ts'
import { blankToNull, formatMobile, initials } from '../lib/format.ts'
import { useLanguage } from '../lib/language.tsx'
import { setProfile, useSession } from '../lib/session.ts'
import type { Occupation } from '../lib/types.ts'
import { OCCUPATIONS } from '../lib/types.ts'

export function ProfileScreen() {
  const navigate = useNavigate()
  const { copy } = useLanguage()
  const { profile } = useSession()
  const [fullName, setFullName] = useState(profile?.full_name ?? '')
  const [occupation, setOccupation] = useState<Occupation | ''>(
    (profile?.occupation as Occupation | null) ?? '',
  )
  const [addressLine, setAddressLine] = useState(profile?.address_line ?? '')
  const [locality, setLocality] = useState(profile?.locality ?? '')
  const [district, setDistrict] = useState(profile?.district ?? '')
  const [stateName, setStateName] = useState(profile?.state ?? '')
  const [pincode, setPincode] = useState(profile?.pincode ?? '')
  const [error, setError] = useState('')
  const [saved, setSaved] = useState('')
  const [saving, setSaving] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const [confirmLogout, setConfirmLogout] = useState(false)

  useEffect(() => {
    let cancelled = false
    getMe()
      .then((next) => {
        if (cancelled) return
        setProfile(next)
        setFullName(next.full_name)
        setOccupation((next.occupation as Occupation | null) ?? '')
        setAddressLine(next.address_line ?? '')
        setLocality(next.locality ?? '')
        setDistrict(next.district ?? '')
        setStateName(next.state ?? '')
        setPincode(next.pincode ?? '')
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof ApiError && err.status === 401) return
        // Keep the cached session profile if refresh fails.
      })
    return () => {
      cancelled = true
    }
  }, [])

  async function onSave(event: FormEvent) {
    event.preventDefault()
    if (fullName.trim().length < 2) {
      setError(copy.fullNameRequired)
      return
    }
    if (pincode && !/^\d{6}$/.test(pincode)) {
      setError(copy.pinDigits)
      return
    }
    setSaving(true)
    setError('')
    setSaved('')
    try {
      const next = await updateMe({
        full_name: fullName.trim(),
        occupation: occupation || null,
        address_line: blankToNull(addressLine),
        locality: blankToNull(locality),
        district: blankToNull(district),
        state: blankToNull(stateName),
        pincode: blankToNull(pincode),
      })
      setProfile(next)
      setSaved(copy.saved)
    } catch (err) {
      setError(apiErrorText(err, copy, copy.updateFailed))
    } finally {
      setSaving(false)
    }
  }

  async function onLogout() {
    setLoggingOut(true)
    await logout()
    navigate('/sign-in', { replace: true })
  }

  return (
    <>
      <AppBar title={copy.profile} showEmblem />
      <Screen withNav>
        <Card>
          <div className="identity">
            <div className="avatar avatar--lg" aria-hidden="true">
              {initials(profile?.full_name ?? '')}
            </div>
            <div>
              <h2 className="card__title">{profile?.full_name || copy.participant}</h2>
              <p className="muted">{copy.registeredParticipant}</p>
              <div className="chips" style={{ marginTop: 8 }}>
                <StatusChip tone="saffron">{profile?.login_id || '—'}</StatusChip>
              </div>
            </div>
          </div>
        </Card>

        <Card kicker={copy.account}>
          <div className="stack">
            <div className="profile-row">
              <span className="profile-row__label">{copy.loginId}</span>
              <span className="profile-row__value">{profile?.login_id || '—'}</span>
            </div>
            <div className="profile-row">
              <span className="profile-row__label">{copy.email}</span>
              <span className="profile-row__value">{profile?.email || '—'}</span>
            </div>
            <div className="profile-row">
              <span className="profile-row__label">{copy.mobile}</span>
              <span className="profile-row__value">
                {profile?.mobile ? formatMobile(profile.mobile) : '—'}
              </span>
            </div>
          </div>
        </Card>

        <form className="stack" onSubmit={onSave}>
          <Card kicker={copy.editDetails} title={copy.updateProfile}>
            <div className="stack">
              <ErrorBanner message={error} />
              {saved ? <div className="banner banner--ok">{saved}</div> : null}
              <Field
                label={copy.fullName}
                name="full_name"
                value={fullName}
                onChange={(event) => setFullName(event.target.value)}
              />
              <div className="field">
                <span className="field__label">{copy.occupation}</span>
                <div className="chips">
                  {OCCUPATIONS.map((item) => (
                    <ChoiceChip
                      key={item}
                      label={occupationLabel(copy, item)}
                      selected={occupation === item}
                      onClick={() => setOccupation(occupation === item ? '' : item)}
                    />
                  ))}
                </div>
              </div>
              <Field
                label={copy.address}
                name="address_line"
                value={addressLine}
                onChange={(event) => setAddressLine(event.target.value)}
              />
              <Field
                label={copy.locality}
                name="locality"
                value={locality}
                onChange={(event) => setLocality(event.target.value)}
              />
              <Field
                label={copy.district}
                name="district"
                value={district}
                onChange={(event) => setDistrict(event.target.value)}
              />
              <Field
                label={copy.state}
                name="state"
                value={stateName}
                onChange={(event) => setStateName(event.target.value)}
              />
              <Field
                label={copy.pincode}
                name="pincode"
                inputMode="numeric"
                maxLength={6}
                value={pincode}
                onChange={(event) => setPincode(event.target.value.replace(/\D/g, '').slice(0, 6))}
              />
            </div>
          </Card>
          <Button type="submit" disabled={saving}>
            {saving ? copy.saving : copy.saveChanges}
          </Button>
        </form>

        <Button type="button" variant="ghost" onClick={() => setConfirmLogout(true)}>
          {copy.logOut}
        </Button>
      </Screen>

      {confirmLogout ? (
        <div className="dialog-backdrop" role="presentation">
          <div className="dialog" role="dialog" aria-labelledby="logout-title">
            <h2 className="card__title" id="logout-title">
              {copy.logOutConfirm}
            </h2>
            <p className="card__body">{copy.logOutBody}</p>
            <div className="stack" style={{ marginTop: 16 }}>
              <Button type="button" variant="danger" onClick={onLogout} disabled={loggingOut}>
                {copy.logOut}
              </Button>
              <Button type="button" variant="ghost" onClick={() => setConfirmLogout(false)}>
                {copy.cancel}
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}
