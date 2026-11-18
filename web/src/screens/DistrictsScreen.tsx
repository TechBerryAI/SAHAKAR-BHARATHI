import { useEffect, useState, type FormEvent } from 'react'
import { AdminSections } from '../components/AdminSections.tsx'
import { AppBar } from '../components/AppBar.tsx'
import { Button, ErrorBanner, Field, Screen } from '../components/ui.tsx'
import { useSession } from '../hooks/useSession.ts'
import { useLanguage } from '../contexts/language.tsx'
import {
  ApiError,
  apiErrorText,
  assignDistrictAdmin,
  createDistrict,
  listAdminUsers,
  listDistricts,
  removeDistrictAdmin,
  updateDistrict,
} from '../lib/api.ts'
import type { District } from '../lib/yatra.ts'

export function DistrictsScreen() {
  const { copy } = useLanguage()
  const { profile } = useSession()
  const superAdmin = profile?.role === 'super_admin'
  const [districts, setDistricts] = useState<District[]>([])
  const [name, setName] = useState('')
  const [code, setCode] = useState('')
  const [stateName, setStateName] = useState('')
  const [assignTo, setAssignTo] = useState<string | null>(null)
  const [loginQuery, setLoginQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  function load() {
    setLoading(true)
    listDistricts()
      .then((page) => {
        setDistricts(page.items)
        setError('')
      })
      .catch((err: unknown) => {
        if (err instanceof ApiError && err.status === 401) return
        setError(apiErrorText(err, copy, 'Could not load districts.'))
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    let cancelled = false
    listDistricts()
      .then((page) => {
        if (cancelled) return
        setDistricts(page.items)
        setError('')
      })
      .catch((err: unknown) => {
        if (cancelled || (err instanceof ApiError && err.status === 401)) return
        setError(apiErrorText(err, copy, 'Could not load districts.'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [copy])

  async function onCreate(event: FormEvent) {
    event.preventDefault()
    if (!superAdmin || saving || name.trim().length < 2 || code.trim().length < 2) return
    setSaving(true)
    setError('')
    try {
      await createDistrict({
        name: name.trim(),
        code: code.trim().toUpperCase(),
        state_name: stateName.trim() || undefined,
      })
      setName('')
      setCode('')
      setStateName('')
      load()
    } catch (err: unknown) {
      setError(apiErrorText(err, copy, 'Could not create this district.'))
    } finally {
      setSaving(false)
    }
  }

  async function onAssign(event: FormEvent) {
    event.preventDefault()
    if (!assignTo || saving) return
    setSaving(true)
    setError('')
    try {
      const found = await listAdminUsers(loginQuery.trim())
      const match = found.items.find(
        (person) => person.login_id.toLowerCase() === loginQuery.trim().toLowerCase(),
      )
      if (!match) {
        setError('No admin account matched that login ID.')
        return
      }
      await assignDistrictAdmin(assignTo, match.id)
      setLoginQuery('')
      setAssignTo(null)
      load()
    } catch (err: unknown) {
      setError(apiErrorText(err, copy, 'Could not assign this district administrator.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <AppBar title="Districts" showEmblem />
      <Screen withNav>
        <AdminSections />
        {superAdmin ? (
          <form className="card stack" onSubmit={(event) => void onCreate(event)}>
            <h2 className="card__title">New district</h2>
            <Field label="Name" name="district-name" value={name} required maxLength={120} onChange={(event) => setName(event.target.value)} />
            <Field label="Code" name="district-code" value={code} required maxLength={16} onChange={(event) => setCode(event.target.value)} />
            <Field label="State" name="district-state" value={stateName} maxLength={120} onChange={(event) => setStateName(event.target.value)} />
            <Button type="submit" disabled={saving || name.trim().length < 2 || code.trim().length < 2}>
              {saving ? copy.pleaseWait : 'Create district'}
            </Button>
          </form>
        ) : null}
        <ErrorBanner message={error} />
        {loading ? (
          <div className="center-state">
            <div className="spinner" aria-label={copy.pleaseWait} />
          </div>
        ) : districts.length === 0 ? (
          <div className="admin-empty">
            <p>No districts in your scope.</p>
          </div>
        ) : (
          <ul className="admin-list">
            {districts.map((district) => (
              <li key={district.id} className="card stack">
                <div>
                  <p className="admin-row__name">{district.name}</p>
                  <p className="admin-row__meta">
                    {district.code}
                    {district.state_name ? ` · ${district.state_name}` : ''}
                    {district.active ? '' : ' · Inactive'}
                  </p>
                </div>
                <ul className="stack">
                  {district.administrators.map((person) => (
                    <li key={person.user_id}>
                      {person.full_name} ({person.login_id})
                      {superAdmin ? (
                        <Button
                          type="button"
                          variant="text"
                          onClick={() => {
                            void removeDistrictAdmin(district.id, person.user_id).then(load).catch((err: unknown) => {
                              setError(apiErrorText(err, copy, 'Could not remove this district administrator.'))
                            })
                          }}
                        >
                          Remove
                        </Button>
                      ) : null}
                    </li>
                  ))}
                </ul>
                {superAdmin ? (
                  <div className="stack">
                    <Button
                      type="button"
                      variant="ghost"
                      onClick={() => {
                        void updateDistrict(district.id, { active: !district.active }).then(load)
                      }}
                    >
                      {district.active ? 'Deactivate' : 'Activate'}
                    </Button>
                    {assignTo === district.id ? (
                      <form className="stack" onSubmit={(event) => void onAssign(event)}>
                        <Field
                          label="Admin login ID"
                          name={`assign-${district.id}`}
                          value={loginQuery}
                          onChange={(event) => setLoginQuery(event.target.value)}
                        />
                        <Button type="submit" disabled={saving || loginQuery.trim().length < 2}>
                          Assign
                        </Button>
                      </form>
                    ) : (
                      <Button type="button" variant="ghost" onClick={() => setAssignTo(district.id)}>
                        Assign administrator
                      </Button>
                    )}
                  </div>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </Screen>
    </>
  )
}
