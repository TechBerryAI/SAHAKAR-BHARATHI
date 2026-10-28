import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { Button, ErrorBanner, InfoList, Panel, PersonHeader, Screen, StatusChip } from '../components/ui.tsx'
import { useLanguage } from '../contexts/language.tsx'
import { useSession } from '../hooks/useSession.ts'
import { ApiError, apiErrorText, getAdminUser, setAdminUserRole } from '../lib/api.ts'
import { formatMobile, initials } from '../lib/format.ts'
import type { AdminUserDetail } from '../lib/types.ts'

function roleLabel(role: string): string {
  if (role === 'super_admin') return 'Super admin'
  if (role === 'admin') return 'Admin'
  return 'Participant'
}

function titleCase(value: string): string {
  return value
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(' ')
}

function shown(value: string | null | undefined): string {
  return value?.trim() ? value : '—'
}

export function AdminUserScreen() {
  const { userId = '' } = useParams()
  const navigate = useNavigate()
  const { copy } = useLanguage()
  const { profile: me } = useSession()
  const [person, setPerson] = useState<AdminUserDetail | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let cancelled = false
    getAdminUser(userId)
      .then((next) => {
        if (!cancelled) {
          setPerson(next)
          setError('')
        }
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof ApiError && err.status === 401) return
        setError(apiErrorText(err, copy, 'Could not load this person.'))
      })
    return () => {
      cancelled = true
    }
  }, [userId, copy])

  async function onRole(role: 'admin' | 'participant') {
    setBusy(true)
    setError('')
    try {
      setPerson(await setAdminUserRole(userId, role))
    } catch (err: unknown) {
      setError(apiErrorText(err, copy, 'Could not update access.'))
    } finally {
      setBusy(false)
    }
  }

  const canGrant = me?.role === 'super_admin' && person && person.role !== 'super_admin' && person.login_id !== me.login_id

  return (
    <>
      <AppBar title="User details" onBack={() => navigate('/admin')} />
      <Screen withNav>
        <ErrorBanner message={error} />
        {!person && !error ? (
          <div className="center-state">
            <div className="spinner" aria-label={copy.pleaseWait} />
          </div>
        ) : person ? (
          <div className="stack">
            <PersonHeader
              initials={initials(person.full_name)}
              name={titleCase(person.full_name)}
              role={roleLabel(person.role)}
            >
              <StatusChip tone={person.role === 'participant' ? 'green' : 'saffron'}>
                {person.login_id}
              </StatusChip>
            </PersonHeader>

            <Panel title={copy.account} icon="shield">
              <InfoList
                rows={[
                  { icon: 'badge', label: copy.loginId, value: person.login_id },
                  { icon: 'phone', label: copy.mobile, value: formatMobile(person.mobile) },
                  { icon: 'mail', label: copy.email, value: shown(person.email) },
                  { icon: 'user', label: copy.gender, value: person.gender ? titleCase(person.gender) : '—' },
                  { icon: 'user', label: copy.occupation, value: person.occupation ? titleCase(person.occupation) : '—' },
                ]}
              />
            </Panel>

            <Panel title={copy.profileTitle} icon="pin">
              <InfoList
                rows={[
                  { label: copy.address, value: shown(person.address_line) },
                  { label: copy.locality, value: person.locality ? titleCase(person.locality) : '—' },
                  { label: copy.district, value: person.district ? titleCase(person.district) : '—' },
                  { label: copy.state, value: person.state ? titleCase(person.state) : '—' },
                  { label: copy.pincode, value: shown(person.pincode) },
                ]}
              />
            </Panel>

            {canGrant ? (
              <section className="admin-access">
                <h2>{person.role === 'admin' ? 'Admin access is on' : 'Participant only'}</h2>
                <p>
                  {person.role === 'admin'
                    ? 'This person can open the directory. They cannot grant access to anyone else.'
                    : 'Give admin access so this person can open the directory and read registrations.'}
                </p>
                {person.role === 'admin' ? (
                  <Button type="button" variant="ghost" disabled={busy} onClick={() => void onRole('participant')}>
                    {busy ? copy.pleaseWait : 'Remove admin access'}
                  </Button>
                ) : (
                  <Button type="button" disabled={busy} onClick={() => void onRole('admin')}>
                    {busy ? copy.pleaseWait : 'Give admin access'}
                  </Button>
                )}
              </section>
            ) : null}
          </div>
        ) : null}
      </Screen>
    </>
  )
}
