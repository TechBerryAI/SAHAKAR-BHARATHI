import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { LanguageSelect } from '../components/LanguageSelect.tsx'
import { Card, ErrorBanner, Screen, StatusChip } from '../components/ui.tsx'
import { ApiError, apiErrorText, getMe, isStaffRole } from '../lib/api.ts'
import { occupationLabel, statusLabel } from '../lib/copy.ts'
import { firstName, initials, regionLabel } from '../lib/format.ts'
import { useLanguage } from '../lib/language.tsx'
import { getSnapshot, setProfile, useSession } from '../lib/session.ts'

export function HomeScreen() {
  const { copy } = useLanguage()
  const { profile } = useSession()
  // Kept raw so the message follows the chosen language.
  const [loadError, setLoadError] = useState<unknown>(null)
  const [loading, setLoading] = useState(!profile)

  useEffect(() => {
    let cancelled = false
    getMe()
      .then((next) => {
        if (!cancelled) {
          setProfile(next)
          setLoadError(null)
        }
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof ApiError && err.status === 401) return
        if (!getSnapshot().profile) {
          setLoadError(err)
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const error = loadError ? apiErrorText(loadError, copy, copy.loadFailed) : ''

  const name = profile?.full_name ?? ''
  const greeting = name ? `${copy.jaiKisan}, ${firstName(name)}` : copy.jaiKisan
  const region = regionLabel(profile?.district ?? null, profile?.state ?? null)

  return (
    <>
      <AppBar title={copy.home} showEmblem trailing={<LanguageSelect variant="bar" />} />
      <Screen withNav>
        <ErrorBanner message={error} />
        {loading && !profile ? (
          <div className="center-state">
            <div className="spinner" aria-label={copy.saving} />
          </div>
        ) : (
          <>
            <Card>
              <div className="identity">
                <div className="avatar" aria-hidden="true">
                  {initials(name)}
                </div>
                <div>
                  <h2 className="card__title" style={{ marginBottom: 4 }}>
                    {greeting}
                  </h2>
                  <p className="muted">{copy.registeredParticipant}</p>
                </div>
              </div>
              {profile?.occupation || region ? (
                <div className="chips" style={{ marginTop: 12 }}>
                  {profile?.occupation ? (
                    <StatusChip>{occupationLabel(copy, profile.occupation)}</StatusChip>
                  ) : null}
                  {region ? <StatusChip tone="saffron">{region}</StatusChip> : null}
                </div>
              ) : null}
            </Card>
            {isStaffRole(profile?.role) ? (
              <Card kicker="Admin" title="Registered users">
                <p className="card__body">Open the directory to read registration details.</p>
                <Link className="btn btn--primary admin-home-link" to="/admin">
                  View users
                </Link>
              </Card>
            ) : null}
            <Card kicker={copy.account} title={copy.yourProfile}>
              <p className="card__body">
                {copy.profileStatus.replace('{status}', statusLabel(copy, profile?.status || 'active'))}
              </p>
            </Card>
            <Card kicker={copy.mobile} title={profile?.mobile ? `+91 ${profile.mobile}` : copy.mobileOnFile}>
              <p className="card__body">{copy.mobileOnFileBody}</p>
            </Card>
          </>
        )}
      </Screen>
    </>
  )
}
