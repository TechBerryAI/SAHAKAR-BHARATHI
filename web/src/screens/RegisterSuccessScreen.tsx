import { useNavigate } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { Button, Card, Screen, StatusChip } from '../components/ui.tsx'
import { occupationLabel } from '../lib/copy.ts'
import { useLanguage } from '../lib/language.tsx'
import { useSession } from '../lib/session.ts'

export function RegisterSuccessScreen() {
  const navigate = useNavigate()
  const { copy } = useLanguage()
  const { profile } = useSession()
  const loginId = profile?.login_id ?? '—'

  return (
    <>
      <AppBar title={copy.registered} showEmblem />
      <Screen>
        <Card kicker={copy.accountReady} title={copy.keepLoginId}>
          <p className="card__body">{copy.keepLoginIdBody}</p>
        </Card>
        <Card>
          <div className="profile-row">
            <span className="profile-row__label">{copy.loginId}</span>
            <strong className="profile-row__value">{loginId}</strong>
          </div>
          <div className="chips" style={{ marginTop: 12 }}>
            {profile?.occupation ? (
              <StatusChip>{occupationLabel(copy, profile.occupation)}</StatusChip>
            ) : null}
            {profile?.full_name ? <StatusChip tone="saffron">{profile.full_name}</StatusChip> : null}
          </div>
        </Card>
        <Button type="button" onClick={() => navigate('/', { replace: true })}>
          {copy.goHome}
        </Button>
      </Screen>
    </>
  )
}
