import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { Icon } from '../components/icons.tsx'
import {
  Button,
  ChoiceChip,
  ErrorBanner,
  Field,
  InfoList,
  Panel,
  PersonHeader,
  Screen,
  StatusChip,
  type InfoRow,
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
  // Kept raw so the message follows the chosen language.
  const [refreshError, setRefreshError] = useState<unknown>(null)
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
        // Keep the cached profile on screen, but say it could not be refreshed.
        setRefreshError(err)
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

  const message = error || (refreshError ? apiErrorText(refreshError, copy, copy.loadFailed) : '')
  const accountRows: InfoRow[] = [
    { icon: 'badge', label: copy.loginId, value: profile?.login_id || '—' },
    { icon: 'phone', label: copy.mobile, value: profile?.mobile ? formatMobile(profile.mobile) : '—' },
    { icon: 'mail', label: copy.email, value: profile?.email || '—' },
  ]

  return (
    <>
      <AppBar title={copy.profile} showEmblem />
      <Screen withNav>
        <PersonHeader
          initials={initials(profile?.full_name ?? '')}
          name={profile?.full_name || copy.participant}
          role={copy.registeredParticipant}
        >
          {profile?.occupation ? (
            <StatusChip tone="saffron">{occupationLabel(copy, profile.occupation)}</StatusChip>
          ) : null}
        </PersonHeader>

        <Panel title={copy.account} icon="shield">
          <InfoList rows={accountRows} />
        </Panel>

        <form className="stack" onSubmit={onSave}>
          <ErrorBanner message={message} />
          {saved ? <div className="banner banner--ok">{saved}</div> : null}

          <Panel title={copy.formTitle} icon="user">
            <div className="stack">
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
            </div>
          </Panel>

          <Panel title={copy.profileTitle} icon="pin">
            <div className="stack">
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
              <div className="panel__pair">
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
              </div>
              <Field
                label={copy.pincode}
                name="pincode"
                inputMode="numeric"
                maxLength={6}
                value={pincode}
                onChange={(event) => setPincode(event.target.value.replace(/\D/g, '').slice(0, 6))}
              />
            </div>
          </Panel>

          <Button type="submit" className="btn--pill" disabled={saving}>
            {saving ? copy.saving : copy.saveChanges}
          </Button>
        </form>

        <button type="button" className="logout-row" onClick={() => setConfirmLogout(true)}>
          <Icon name="logout" size={20} />
          {copy.logOut}
        </button>
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
