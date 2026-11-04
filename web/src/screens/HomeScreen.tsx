import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { LanguageSelect } from '../components/LanguageSelect.tsx'
import { ErrorBanner, Screen, StatusChip } from '../components/ui.tsx'
import { useLanguage } from '../contexts/language.tsx'
import { useSession } from '../hooks/useSession.ts'
import { ApiError, apiErrorText, getMe, isStaffRole } from '../lib/api.ts'
import { occupationLabel, statusLabel } from '../lib/copy.ts'
import { firstName, initials, regionLabel } from '../lib/format.ts'
import { getSnapshot, setProfile } from '../lib/session.ts'

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

  const staff = isStaffRole(profile?.role)

  return (
    <>
      <AppBar title={copy.brandName} showEmblem trailing={<LanguageSelect variant="bar" />} />
      <Screen withNav>
        <ErrorBanner message={error} />
        {loading && !profile ? (
          <div className="center-state">
            <div className="spinner" aria-label={copy.saving} />
          </div>
        ) : (
          <>
            <section className="home-hero">
              <div className="identity">
                <div className="avatar" aria-hidden="true">
                  {initials(name)}
                </div>
                <div>
                  <p className="home-hero__hello">{copy.registeredParticipant}</p>
                  <h2 className="card__title">{greeting}</h2>
                </div>
              </div>
              {profile?.occupation || region ? (
                <div className="chips home-hero__chips">
                  {profile?.occupation ? (
                    <StatusChip>{occupationLabel(copy, profile.occupation)}</StatusChip>
                  ) : null}
                  {region ? <StatusChip tone="saffron">{region}</StatusChip> : null}
                </div>
              ) : null}
            </section>

            <Link className="home-action" to="/check-in">
              <span className="home-action__icon" aria-hidden="true">
                <svg width="26" height="26" viewBox="0 0 24 24">
                  <path
                    fill="currentColor"
                    d="M3 11h8V3H3zm2-6h4v4H5zM3 21h8v-8H3zm2-6h4v4H5zM13 3v8h8V3zm6 6h-4V5h4zM13 13h2v2h-2zm2 2h2v2h-2zm-2 2h2v2h-2zm4-4h2v2h-2zm2 2h2v2h-2zm-2 2h2v2h-2zm-2 2h2v2h-2zm4 0h2v2h-2z"
                  />
                </svg>
              </span>
              <span className="home-action__copy">
                <span className="home-action__title">Scan booth QR</span>
                <span className="home-action__body">
                  Scan the booth QR, or enter the 6-digit code printed under it.
                </span>
              </span>
              <svg className="home-action__chevron" width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
                <path fill="currentColor" d="M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z" />
              </svg>
            </Link>

            {staff ? (
              <div className="home-shortcuts">
                <Link className="home-shortcut" to="/admin">
                  <span className="home-shortcut__kicker">Admin</span>
                  <span className="home-shortcut__title">Registered users</span>
                  <span className="home-shortcut__body">Open the directory to read registration details.</span>
                </Link>
                <Link className="home-shortcut" to="/admin/places">
                  <span className="home-shortcut__kicker">Admin</span>
                  <span className="home-shortcut__title">Booths</span>
                  <span className="home-shortcut__body">Create a booth, print its QR, and see who checked in.</span>
                </Link>
              </div>
            ) : null}

            <Link className="home-fact home-fact--link" to="/profile">
              <span className="home-fact__copy">
                <span className="card__kicker">{copy.account}</span>
                <span className="home-fact__title">{copy.yourProfile}</span>
                <span className="card__body">
                  {copy.profileStatus.replace('{status}', statusLabel(copy, profile?.status || 'active'))}
                </span>
              </span>
              <svg className="home-action__chevron" width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
                <path fill="currentColor" d="M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z" />
              </svg>
            </Link>

            <section className="home-fact">
              <p className="card__kicker">{copy.mobile}</p>
              <p className="home-fact__value">
                {profile?.mobile ? `+91 ${profile.mobile}` : copy.mobileOnFile}
              </p>
              <p className="card__body">{copy.mobileOnFileBody}</p>
            </section>
          </>
        )}
      </Screen>
    </>
  )
}
