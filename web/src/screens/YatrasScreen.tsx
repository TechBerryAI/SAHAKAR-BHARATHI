import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { AdminSections } from '../components/AdminSections.tsx'
import { AppBar } from '../components/AppBar.tsx'
import { Button, ErrorBanner, Field, Screen, SelectField } from '../components/ui.tsx'
import { useLanguage } from '../contexts/language.tsx'
import { useSession } from '../hooks/useSession.ts'
import { ApiError, apiErrorText, createYatra, isStaffRole, listDistricts, listYatras } from '../lib/api.ts'
import {
  createActionGate,
  createSearchGate,
  formatWhen,
  isPublicCatalogYatra,
  mergeById,
  toApiInstant,
  yatraStatusLabel,
  type District,
  type YatraSummary,
} from '../lib/yatra.ts'

export function YatrasScreen({ admin = false }: { admin?: boolean }) {
  const { copy } = useLanguage()
  const { profile } = useSession()
  const staff = isStaffRole(profile?.role)
  const [items, setItems] = useState<YatraSummary[]>([])
  const [districts, setDistricts] = useState<District[]>([])
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('')
  const [name, setName] = useState('')
  const [districtId, setDistrictId] = useState('')
  const [startsAt, setStartsAt] = useState('')
  const [endsAt, setEndsAt] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const searchGate = useRef(createSearchGate())
  const createGate = useRef(createActionGate())

  function load(nextQuery = query, nextStatus = status) {
    const requestId = searchGate.current.start()
    setLoading(true)
    listYatras(nextQuery, nextStatus)
      .then((page) => {
        if (!searchGate.current.isCurrent(requestId)) return
        setItems(page.items)
        setError('')
      })
      .catch((err: unknown) => {
        if (!searchGate.current.isCurrent(requestId)) return
        if (err instanceof ApiError && err.status === 401) return
        setError(apiErrorText(err, copy, 'Could not load Yatras.'))
      })
      .finally(() => {
        if (searchGate.current.isCurrent(requestId)) setLoading(false)
      })
  }

  useEffect(() => {
    const requestId = searchGate.current.start()
    let cancelled = false
    listYatras('', '')
      .then((page) => {
        if (cancelled || !searchGate.current.isCurrent(requestId)) return
        setItems(page.items)
        setError('')
      })
      .catch((err: unknown) => {
        if (cancelled || !searchGate.current.isCurrent(requestId)) return
        if (err instanceof ApiError && err.status === 401) return
        setError(apiErrorText(err, copy, 'Could not load Yatras.'))
      })
      .finally(() => {
        if (!cancelled && searchGate.current.isCurrent(requestId)) setLoading(false)
      })
    if (staff) {
      void (async () => {
        const collected: District[] = []
        let offset = 0
        try {
          for (let pageNumber = 0; pageNumber < 100; pageNumber += 1) {
            const page = await listDistricts(offset)
            if (cancelled) return
            collected.splice(0, collected.length, ...mergeById(collected, page.items.filter((district) => district.active)))
            if (!page.has_more) break
            offset += page.items.length
          }
          if (!cancelled) setDistricts(collected)
        } catch {
          if (!cancelled) setDistricts([])
        }
      })()
    }
    return () => {
      cancelled = true
    }
  }, [copy, staff])

  async function onCreate(event: FormEvent) {
    event.preventDefault()
    if (saving || name.trim().length < 2 || !districtId || !startsAt || !endsAt) return
    if (!createGate.current.tryEnter()) return
    setSaving(true)
    setError('')
    try {
      await createYatra({
        name: name.trim(),
        district_id: districtId,
        starts_at: toApiInstant(startsAt),
        ends_at: toApiInstant(endsAt),
      })
      setName('')
      setStartsAt('')
      setEndsAt('')
      load()
    } catch (err: unknown) {
      setError(apiErrorText(err, copy, 'Could not create this Yatra.'))
    } finally {
      createGate.current.leave()
      setSaving(false)
    }
  }

  return (
    <>
      <AppBar title="Rath Yatras" showEmblem />
      <Screen withNav>
        {admin ? <AdminSections /> : null}
        <form
          className="card stack"
          onSubmit={(event) => {
            event.preventDefault()
            load()
          }}
        >
          <Field label="Search" name="yatra-search" value={query} onChange={(event) => setQuery(event.target.value)} />
          <SelectField label="Status" name="yatra-status" value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="">All statuses</option>
            {['draft', 'planned', 'approval_pending', 'approved', 'upcoming', 'in_progress', 'completed', 'archived'].map(
              (value) => (
                <option key={value} value={value}>
                  {yatraStatusLabel(value)}
                </option>
              ),
            )}
          </SelectField>
          <Button type="submit">Search</Button>
        </form>
        {admin && staff ? (
          <form className="card stack" onSubmit={(event) => void onCreate(event)}>
            <h2 className="card__title">New Yatra</h2>
            <Field label="Name" name="yatra-name" value={name} required maxLength={160} onChange={(event) => setName(event.target.value)} />
            <SelectField
              label="District"
              name="yatra-district"
              value={districtId}
              required
              onChange={(event) => setDistrictId(event.target.value)}
            >
              <option value="">Select a district</option>
              {districts.map((district) => (
                <option key={district.id} value={district.id}>
                  {district.name}
                </option>
              ))}
            </SelectField>
            <Field label="Starts" name="yatra-start" type="datetime-local" value={startsAt} required onChange={(event) => setStartsAt(event.target.value)} />
            <Field label="Ends" name="yatra-end" type="datetime-local" value={endsAt} required onChange={(event) => setEndsAt(event.target.value)} />
            <Button type="submit" disabled={saving || name.trim().length < 2 || !districtId}>
              {saving ? copy.pleaseWait : 'Create Yatra'}
            </Button>
          </form>
        ) : null}
        <ErrorBanner message={error} />
        {loading ? (
          <div className="center-state">
            <div className="spinner" aria-label={copy.pleaseWait} />
          </div>
        ) : (admin ? items : items.filter(isPublicCatalogYatra)).length === 0 ? (
          <div className="admin-empty">
            <p>No Yatras to show.</p>
          </div>
        ) : (
          <ul className="admin-list">
            {(admin ? items : items.filter(isPublicCatalogYatra)).map((yatra) => (
              <li key={yatra.id}>
                <Link className="admin-row" to={`${admin ? '/admin/yatras' : '/yatras'}/${yatra.id}`}>
                  <span className="admin-row__body">
                    <span className="admin-row__name">{yatra.name}</span>
                    <span className="admin-row__meta">
                      {yatra.district.name} · {yatra.public_code} · {yatraStatusLabel(yatra.status)}
                    </span>
                    <span className="admin-row__meta">
                      {formatWhen(yatra.starts_at)} – {formatWhen(yatra.ends_at)}
                    </span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Screen>
    </>
  )
}
