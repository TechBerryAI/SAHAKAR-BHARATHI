import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { boothQrSvg } from '../lib/boothCode.ts'
import { AdminSections } from '../components/AdminSections.tsx'
import { AppBar } from '../components/AppBar.tsx'
import { Button, ErrorBanner, Screen } from '../components/ui.tsx'
import { useLanguage } from '../contexts/language.tsx'
import { ApiError, apiErrorText, getPlace, listPlaceAttendance, updatePlace } from '../lib/api.ts'
import { formatMobile, regionLabel } from '../lib/format.ts'
import type { AttendanceRecord, Place } from '../lib/types.ts'

function BoothQr({ svg, label }: { svg: string; label: string }) {
  const doc = new DOMParser().parseFromString(svg, 'image/svg+xml')
  const root = doc.documentElement
  if (root.nodeName.toLowerCase() !== 'svg' || doc.querySelector('parsererror')) {
    return <p className="muted">QR could not be drawn.</p>
  }
  return (
    <div
      className="print-qr"
      role="img"
      aria-label={label}
      dangerouslySetInnerHTML={{ __html: root.outerHTML }}
    />
  )
}

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
  const [qrSvg, setQrSvg] = useState('')
  const [qrFor, setQrFor] = useState('')
  const [qrError, setQrError] = useState('')
  const [qrErrorFor, setQrErrorFor] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [errorFor, setErrorFor] = useState('')
  const current = place?.id === placeId ? place : null
  const banner = errorFor === placeId ? error : ''
  const pending = !current && errorFor !== placeId

  useEffect(() => {
    let cancelled = false
    getPlace(placeId)
      .then(async (found) => {
        if (cancelled) return
        setPlace(found)
        setVisits([])
        setTotal(0)
        setError('')
        setErrorFor('')
        try {
          const attendance = await listPlaceAttendance(found.id)
          if (cancelled) return
          setVisits(attendance.items)
          setTotal(attendance.total)
        } catch (err: unknown) {
          if (cancelled) return
          if (err instanceof ApiError && err.status === 401) return
          setError(apiErrorText(err, copy, 'Could not load attendance.'))
          setErrorFor(placeId)
        }
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof ApiError && err.status === 401) return
        setError(apiErrorText(err, copy, 'Could not load this booth.'))
        setErrorFor(placeId)
      })
    return () => {
      cancelled = true
    }
  }, [placeId, copy])

  useEffect(() => {
    const code = current?.code
    if (!code) return
    let cancelled = false
    void boothQrSvg(code)
      .then((svg) => {
        if (cancelled) return
        setQrSvg(svg)
        setQrFor(code)
      })
      .catch(() => {
        if (cancelled) return
        setQrError('QR could not be drawn.')
        setQrErrorFor(code)
      })
    return () => {
      cancelled = true
    }
  }, [current?.code])

  async function onToggle() {
    if (!current || busy) return
    setBusy(true)
    setError('')
    setErrorFor('')
    try {
      const updated = await updatePlace(current.id, { active: !current.active })
      setPlace(updated)
    } catch (err: unknown) {
      setError(apiErrorText(err, copy, 'Could not update this booth.'))
      setErrorFor(placeId)
    } finally {
      setBusy(false)
    }
  }

  async function onLoadMore() {
    if (!current) return
    setBusy(true)
    try {
      const page = await listPlaceAttendance(current.id, visits.length)
      setVisits((items) => [...items, ...page.items])
      setTotal(page.total)
    } catch (err: unknown) {
      setError(apiErrorText(err, copy, 'Could not load attendance.'))
      setErrorFor(placeId)
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <AppBar title={current?.name ?? 'Booth'} onBack={() => navigate('/admin/places')} />
      <Screen withNav>
        <AdminSections />
        <ErrorBanner message={banner} />
        {pending ? (
          <div className="center-state">
            <div className="spinner" aria-label={copy.pleaseWait} />
          </div>
        ) : current ? (
          <>
            <section className="print-sheet">
              <h2 className="print-sheet__name">{current.name}</h2>
              {qrFor === current.code && qrSvg ? (
                <BoothQr svg={qrSvg} label={`Booth QR for code ${current.code}`} />
              ) : qrErrorFor === current.code && qrError ? (
                <p className="muted">{qrError}</p>
              ) : null}
              <p className="print-sheet__code">{current.code}</p>
              <p className="muted print-sheet__hint">Scan this QR, or enter the code under it.</p>
            </section>
            <div className="stack no-print">
              <Button type="button" onClick={() => window.print()}>
                Print QR
              </Button>
              <Button type="button" variant="ghost" disabled={busy} onClick={() => void onToggle()}>
                {current.active ? 'Stop accepting visits' : 'Accept visits again'}
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
