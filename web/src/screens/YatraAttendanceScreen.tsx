import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { Button, ErrorBanner, Field, Screen } from '../components/ui.tsx'
import { useLanguage } from '../contexts/language.tsx'
import { ApiError, apiErrorText, listMyYatraAttendance, scanYatraAttendance } from '../lib/api.ts'
import { counterQrFromPayload } from '../lib/counterQr.ts'
import type { YatraFieldAttendance } from '../lib/yatra.ts'

function readFix(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('geolocation-unavailable'))
      return
    }
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 15000,
      maximumAge: 30000,
    })
  })
}

export function YatraAttendanceScreen() {
  const { yatraId = '' } = useParams()
  const { copy } = useLanguage()
  const [raw, setRaw] = useState('')
  const [record, setRecord] = useState<YatraFieldAttendance | null>(null)
  const [mine, setMine] = useState<YatraFieldAttendance[]>([])
  const [mineOffset, setMineOffset] = useState(0)
  const [mineMore, setMineMore] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let cancelled = false
    listMyYatraAttendance(yatraId, 0)
      .then((page) => {
        if (cancelled) return
        setMine(page.items)
        setMineOffset(page.items.length)
        setMineMore(page.has_more)
      })
      .catch((err: unknown) => {
        if (cancelled || (err instanceof ApiError && err.status === 401)) return
      })
    return () => {
      cancelled = true
    }
  }, [yatraId, record])

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    const token = counterQrFromPayload(raw)
    if (!token) {
      setRecord(null)
      setError(copy.counterQrInvalid)
      return
    }
    setBusy(true)
    try {
      const fix = await readFix()
      const saved = await scanYatraAttendance(yatraId, {
        qr_token: raw.trim(),
        latitude: fix.coords.latitude,
        longitude: fix.coords.longitude,
        accuracy_m: fix.coords.accuracy,
        captured_at: new Date(fix.timestamp).toISOString(),
      })
      setRecord(saved)
      setError('')
    } catch (err: unknown) {
      setRecord(null)
      if (err instanceof ApiError && err.status === 401) return
      if (err && typeof err === 'object' && 'code' in err) {
        const code = Number((err as GeolocationPositionError).code)
        if (code === 1) {
          setError(copy.locationInaccurate)
          return
        }
      }
      setError(apiErrorText(err, copy, 'Attendance could not be recorded.'))
    } finally {
      setBusy(false)
    }
  }

  async function loadMoreMine() {
    try {
      const page = await listMyYatraAttendance(yatraId, mineOffset)
      setMine((prev) => {
        const seen = new Set(prev.map((item) => item.id))
        return [...prev, ...page.items.filter((item) => !seen.has(item.id))]
      })
      setMineOffset((offset) => offset + page.items.length)
      setMineMore(page.has_more)
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) return
      setError(apiErrorText(err, copy, 'Could not load your attendance.'))
    }
  }

  return (
    <>
      <AppBar title="Counter attendance" showEmblem />
      <Screen withNav>
        <p>
          <Link to={`/yatras/${yatraId}`}>{copy.back}</Link>
        </p>
        <ErrorBanner message={error} />
        <form className="card stack" onSubmit={(event) => void onSubmit(event)}>
          <h2 className="card__title">Scan a counter</h2>
          <p>Paste the counter QR text. Your phone location is checked at the counter.</p>
          <Field label="Counter QR" name="counter-qr" value={raw} onChange={(event) => setRaw(event.target.value)} />
          <Button type="submit" disabled={busy}>
            {busy ? 'Sending…' : 'Mark attendance'}
          </Button>
        </form>
        {record ? (
          <section className={record.already_recorded ? 'card card--note' : 'card card--success'}>
            <h2 className="card__title">
              {record.already_recorded
                ? 'Already marked present at this counter today.'
                : 'You are marked present at this counter.'}
            </h2>
            <p>{record.counter_name}</p>
            <p className="muted">{record.attendance_date}</p>
          </section>
        ) : null}
        <section className="card stack">
          <h2 className="card__title">My attendance</h2>
          <ul className="stack">
            {mine.map((item) => (
              <li key={item.id}>
                {item.counter_name} · {item.attendance_date}
              </li>
            ))}
          </ul>
          {mineMore ? (
            <Button type="button" onClick={() => void loadMoreMine()}>
              Load more
            </Button>
          ) : null}
        </section>
      </Screen>
    </>
  )
}
