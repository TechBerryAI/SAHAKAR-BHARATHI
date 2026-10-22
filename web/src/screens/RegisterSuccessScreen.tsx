import { useNavigate } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { Button, Card, Screen, StatusChip } from '../components/ui.tsx'
import { useSession } from '../lib/session.ts'

export function RegisterSuccessScreen() {
  const navigate = useNavigate()
  const { profile } = useSession()
  const loginId = profile?.login_id ?? '—'

  return (
    <>
      <AppBar title="Registered" showEmblem />
      <Screen>
        <Card kicker="Account ready" title="Keep this login ID">
          <p className="card__body">
            Keep this ID for your records. Sign in with your login ID or mobile and PIN.
          </p>
        </Card>
        <Card>
          <div className="profile-row">
            <span className="profile-row__label">Login ID</span>
            <strong className="profile-row__value">{loginId}</strong>
          </div>
          <div className="chips" style={{ marginTop: 12 }}>
            {profile?.occupation ? <StatusChip>{profile.occupation}</StatusChip> : null}
            {profile?.full_name ? <StatusChip tone="saffron">{profile.full_name}</StatusChip> : null}
          </div>
        </Card>
        <Button type="button" onClick={() => navigate('/', { replace: true })}>
          Go to Home
        </Button>
      </Screen>
    </>
  )
}
