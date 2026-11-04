import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import QRCode from 'qrcode'
import { boothQrText } from '../lib/boothCode.ts'
import { AdminSections } from '../components/AdminSections.tsx'
import { AppBar } from '../components/AppBar.tsx'
import { Button, ErrorBanner, Screen } from '../components/ui.tsx'
import { useLanguage } from '../contexts/language.tsx'
import { ApiError, apiErrorText, listPlaceAttendance, listPlaces, updatePlace } from '../lib/api.ts'
import { formatMobile, regionLabel } from '../lib/format.ts'
import type { AttendanceRecord, Place } from '../lib/types.ts'

function visitTime(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return new Intl.DateTimeFormat('en-IN', {
    timeZone: 'Asia/Kolkata',
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

export function AdminPlaceScreen() {
  const { placeId = '' } = useParams()
  const navigate = useNavigate()
  const { copy } = useLanguage()
  const [place, setPlace] = useState<Place | null>(null)
  const [visits, setVisits] = useState<AttendanceRecord[]>([])
  const [total, setTotal] = useState(0)
  const [qrUrl, setQrUrl] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    listPlaces()
      .then(async (page) => {
        const found = page.items.find((item) => item.id === placeId) ?? null
        if (cancelled) return
        setPlace(found)
        if (!found) {
          setError('That booth was not found.')
          return
        }
        const attendance = await listPlaceAttendance(found.id)
        if (cancelled) return
        setVisits(attendance.items)
        setTotal(attendance.total)
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof ApiError && err.status === 401) return
        setError(apiErrorText(err, copy, 'Could not load this booth.'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [placeId, copy])

  useEffect(() => {
    if (!place) return
    let cancelled = false
    void QRCode.toDataURL(boothQrText(place.code), { margin: 1, width: 320, errorCorrectionLevel: 'M' }).then(
      (url) => {
        if (!cancelled) setQrUrl(url)
      },
    )
    return () => {
      cancelled = true
    }
  }, [place])

  async function onToggle() {
    if (!place || busy) return
    setBusy(true)
    setError('')
    try {
      const updated = await updatePlace(place.id, { active: !place.active })
      setPlace(updated)
    } catch (err: unknown) {
      setError(apiErrorText(err, copy, 'Could not update this booth.'))
    } finally {
      setBusy(false)
    }
  }

  async function onLoadMore() {
    if (!place) return
    setBusy(true)
    try {
      const page = await listPlaceAttendance(place.id, visits.length)
      setVisits((current) => [...current, ...page.items])
      setTotal(page.total)
    } catch (err: unknown) {
      setError(apiErrorText(err, copy, 'Could not load attendance.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <AppBar title={place?.name ?? 'Booth'} onBack={() => navigate('/admin/places')} />
      <Screen withNav>
        <AdminSections />
        <ErrorBanner message={error} />
        {loading ? (
          <div className="center-state">
            <div className="spinner" aria-label={copy.pleaseWait} />
          </div>
        ) : place ? (
          <>
            <section className="print-sheet">
              <h2 className="print-sheet__name">{place.name}</h2>
              {qrUrl ? (
                <img className="print-qr" src={qrUrl} width={280} height={280} alt="" />
              ) : null}
              <p className="print-sheet__code">{place.code}</p>
              <p className="muted print-sheet__hint">Scan this QR, or enter the code under it.</p>
            </section>
            <div className="stack no-print">
              <Button type="button" onClick={() => window.print()}>
                Print QR
              </Button>
              <Button type="button" variant="ghost" disabled={busy} onClick={() => void onToggle()}>
                {place.active ? 'Stop accepting visits' : 'Accept visits again'}
              </Button>
            </div>
            <h2 className="attendance-heading no-print">Who was here</h2>
            <p className="admin-count no-print" aria-live="polite">
              {total} {total === 1 ? 'visit' : 'visits'}
            </p>
            {visits.length === 0 ? (
              <div className="admin-empty no-print">
                <p>No one has checked in here yet.</p>
              </div>
            ) : (
              <ul className="admin-list no-print">
                {visits.map((visit) => {
                  const place = regionLabel(visit.district, visit.state)
                  return (
                    <li key={visit.id} className="admin-row attendance-row">
                      <span className="admin-row__body">
                        <span className="admin-row__name">{visit.full_name}</span>
                        <span className="admin-row__meta">
                          {formatMobile(visit.mobile)}
                          {place ? ` · ${place}` : ''}
                        </span>
                        <span className="admin-row__id">
                          {visit.login_id}
                          <span>{visitTime(visit.checked_in_at)}</span>
                        </span>
                      </span>
                    </li>
                  )
                })}
              </ul>
            )}
            {visits.length < total ? (
              <Button type="button" variant="ghost" className="no-print" disabled={busy} onClick={() => void onLoadMore()}>
                {busy ? copy.pleaseWait : 'Load more'}
              </Button>
            ) : null}
          </>
        ) : null}
      </Screen>
    </>
  )
}
