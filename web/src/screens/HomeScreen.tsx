import { useEffect, useState } from 'react'
import { AppBar } from '../components/AppBar.tsx'
import { Card, ErrorBanner, Screen, StatusChip } from '../components/ui.tsx'
import { ApiError, getMe } from '../lib/api.ts'
import { firstName, initials, regionLabel } from '../lib/format.ts'
import { getSnapshot, setProfile, useSession } from '../lib/session.ts'

export function HomeScreen() {
  const { profile } = useSession()
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(!profile)

  useEffect(() => {
    let cancelled = false
    getMe()
      .then((next) => {
        if (!cancelled) {
          setProfile(next)
          setError('')
        }
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof ApiError && err.status === 401) return
        if (!getSnapshot().profile) {
          setError(err instanceof ApiError ? err.message : 'Could not load profile.')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const name = profile?.full_name ?? ''
  const greeting = name ? `Jai Kisan, ${firstName(name)}` : 'Jai Kisan'
  const region = regionLabel(profile?.district ?? null, profile?.state ?? null)

  return (
    <>
      <AppBar title="Home" showEmblem />
      <Screen withNav>
        <ErrorBanner message={error} />
        {loading && !profile ? (
          <div className="center-state">
            <div className="spinner" aria-label="Loading" />
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
                  <p className="muted">Registered participant</p>
                </div>
              </div>
              <div className="chips" style={{ marginTop: 12 }}>
                <StatusChip>ID: {profile?.login_id || '—'}</StatusChip>
                {profile?.occupation ? <StatusChip>{profile.occupation}</StatusChip> : null}
                {region ? <StatusChip tone="saffron">{region}</StatusChip> : null}
              </div>
            </Card>
            <Card kicker="Account" title="Your Rath Yatra profile">
              <p className="card__body">
                Status: {profile?.status || 'active'}. Sign in with your login ID or mobile and PIN,
                and update your details from Profile.
              </p>
            </Card>
            <Card kicker="Mobile" title={profile?.mobile ? `+91 ${profile.mobile}` : 'Mobile on file'}>
              <p className="card__body">This number is on file for your Rath Yatra profile.</p>
            </Card>
          </>
        )}
      </Screen>
    </>
  )
}
