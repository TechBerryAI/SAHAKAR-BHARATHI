import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { AdminSections } from '../components/AdminSections.tsx'
import { AppBar } from '../components/AppBar.tsx'
import { Button, ErrorBanner, Field, Screen, SelectField } from '../components/ui.tsx'
import { useLanguage } from '../contexts/language.tsx'
import { useSession } from '../hooks/useSession.ts'
import { ApiError, apiErrorText, createYatra, isStaffRole, listDistricts, listYatras } from '../lib/api.ts'
import { formatWhen, toApiInstant, yatraStatusLabel, type District, type YatraSummary } from '../lib/yatra.ts'

export function YatrasScreen({ admin = false }: { admin?: boolean }) {
  const { copy } = useLanguage()
  const { profile } = useSession()
  const staff = isStaffRole(profile?.role)
  const [items, setItems] = useState<YatraSummary[]>([])
  const [total, setTotal] = useState(0)
  const [districts, setDistricts] = useState<District[]>([])
  const [districtTotal, setDistrictTotal] = useState(0)
  const [loadingMore, setLoadingMore] = useState(false)
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('')
  const [name, setName] = useState('')
  const [districtId, setDistrictId] = useState('')
  const [startsAt, setStartsAt] = useState('')
  const [endsAt, setEndsAt] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  function load(nextQuery = query, nextStatus = status) {
    setLoading(true)
    listYatras(nextQuery, nextStatus)
      .then((page) => {
        setItems(page.items)
        setTotal(page.total)
        setError('')
      })
      .catch((err: unknown) => {
        if (err instanceof ApiError && err.status === 401) return
        setError(apiErrorText(err, copy, 'Could not load Yatras.'))
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    let cancelled = false
    listYatras('', '')
      .then((page) => {
        if (cancelled) return
        setItems(page.items)
        setTotal(page.total)
        setError('')
      })
      .catch((err: unknown) => {
        if (cancelled || (err instanceof ApiError && err.status === 401)) return
        setError(apiErrorText(err, copy, 'Could not load Yatras.'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    if (staff) {
      listDistricts()
        .then((page) => {
          if (!cancelled) {
            setDistricts(page.items)
            setDistrictTotal(page.total)
          }
        })
        .catch(() => {
          if (!cancelled) setDistricts([])
        })
    }
    return () => {
      cancelled = true
    }
  }, [copy, staff])

  async function onCreate(event: FormEvent) {
    event.preventDefault()
    if (saving || name.trim().length < 2 || !districtId || !startsAt || !endsAt) return
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
              {districts.filter((district) => district.active).map((district) => (
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
        ) : items.length === 0 ? (
          <div className="admin-empty">
            <p>No Yatras to show.</p>
          </div>
        ) : (
          <ul className="admin-list">
            {items.map((yatra) => (
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
        {!loading && items.length < total ? (
          <Button
            type="button"
            variant="ghost"
            disabled={loadingMore}
            onClick={() => {
              setLoadingMore(true)
              listYatras(query, status, items.length)
                .then((page) => {
                  setItems((current) => {
                    const seen = new Set(current.map((yatra) => yatra.id))
                    return [...current, ...page.items.filter((yatra) => !seen.has(yatra.id))]
                  })
                  setTotal(page.total)
                })
                .catch((err: unknown) => {
                  if (!(err instanceof ApiError && err.status === 401)) {
                    setError(apiErrorText(err, copy, 'Could not load Yatras.'))
                  }
                })
                .finally(() => setLoadingMore(false))
            }}
          >
            {loadingMore ? copy.pleaseWait : 'Load more'}
          </Button>
        ) : null}
        {staff && districts.length < districtTotal ? (
          <Button
            type="button"
            variant="ghost"
            disabled={loadingMore}
            onClick={() => {
              setLoadingMore(true)
              listDistricts(districts.length)
                .then((page) => {
                  setDistricts((current) => {
                    const seen = new Set(current.map((district) => district.id))
                    return [...current, ...page.items.filter((district) => !seen.has(district.id))]
                  })
                  setDistrictTotal(page.total)
                })
                .catch(() => setDistrictTotal(districts.length))
                .finally(() => setLoadingMore(false))
            }}
          >
            {loadingMore ? copy.pleaseWait : 'Load more districts'}
          </Button>
        ) : null}
      </Screen>
    </>
  )
}
