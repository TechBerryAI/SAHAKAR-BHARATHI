import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { AdminSections } from '../components/AdminSections.tsx'
import { AppBar } from '../components/AppBar.tsx'
import { Button, ErrorBanner, Field, Screen } from '../components/ui.tsx'
import { useLanguage } from '../contexts/language.tsx'
import { ApiError, apiErrorText, createPlace, listPlaces } from '../lib/api.ts'
import type { Place } from '../lib/types.ts'

export function AdminPlacesScreen() {
  const { copy } = useLanguage()
  const [name, setName] = useState('')
  const [places, setPlaces] = useState<Place[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    listPlaces()
      .then((page) => {
        if (cancelled) return
        setPlaces(page.items)
        setError('')
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof ApiError && err.status === 401) return
        setError(apiErrorText(err, copy, 'Could not load booths.'))
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
    const trimmed = name.trim()
    if (trimmed.length < 2 || saving) return
    setSaving(true)
    setError('')
    try {
      const created = await createPlace(trimmed)
      setPlaces((current) => [created, ...current.filter((place) => place.id !== created.id)])
      setName('')
    } catch (err: unknown) {
      setError(apiErrorText(err, copy, 'Could not create this booth.'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <AppBar title="Booths" showEmblem />
      <Screen withNav>
        <AdminSections />
        <form className="card stack" onSubmit={(event) => void onCreate(event)}>
          <h2 className="card__title">New booth</h2>
          <Field
            label="Booth name"
            name="place-name"
            value={name}
            required
            maxLength={120}
            placeholder="Pune stall"
            onChange={(event) => setName(event.target.value)}
          />
          <Button type="submit" disabled={saving || name.trim().length < 2}>
            {saving ? copy.pleaseWait : 'Create booth'}
          </Button>
        </form>
        <ErrorBanner message={error} />
        {loading ? (
          <div className="center-state">
            <div className="spinner" aria-label={copy.pleaseWait} />
          </div>
        ) : places.length === 0 ? (
          <div className="admin-empty">
            <p>No booths yet.</p>
            <p className="muted">Create one, then print its QR for the placard.</p>
          </div>
        ) : (
          <ul className="admin-list">
            {places.map((place) => (
              <li key={place.id}>
                <Link className="admin-row" to={`/admin/places/${place.id}`}>
                  <span className="avatar admin-row__avatar" aria-hidden="true">
                    {place.name
                      .trim()
                      .split(/\s+/)
                      .slice(0, 2)
                      .map((part) => part.charAt(0).toUpperCase())
                      .join('')}
                  </span>
                  <span className="admin-row__body">
                    <span className="admin-row__name">{place.name}</span>
                    <span className="admin-row__meta admin-row__meta--codes">
                      <span className="place-code">{place.code}</span>
                      <span className={`place-status${place.active ? ' is-open' : ''}`}>
                        {place.active ? 'Accepting visits' : 'Not accepting visits'}
                      </span>
                    </span>
                  </span>
                  <svg className="admin-row__chevron" width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
                    <path fill="currentColor" d="M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z" />
                  </svg>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Screen>
    </>
  )
}
