import { useEffect, useState, type FormEvent } from 'react'
import { counterQrSvg } from '../lib/counterQr.ts'
import {
  ApiError,
  apiErrorText,
  createCounter,
  createTracker,
  getCounter,
  getLatestPosition,
  listCounters,
  listPositions,
  listTrackers,
  listYatraAttendance,
  rotateCounterQr,
  revokeTracker,
  setCounterActive,
  updateCounter,
} from '../lib/api.ts'
import { useLanguage } from '../contexts/language.tsx'
import { Button, ErrorBanner, Field } from '../components/ui.tsx'
import {
  POSITION_POLL_MS,
  formatWhen,
  freshnessLabel,
  osmPositionLink,
  type LatestPosition,
  type YatraCounter,
  type YatraCounterDetail,
  type YatraPosition,
  type YatraStop,
  type YatraTracker,
} from '../lib/yatra.ts'

function QrImage({ svg, label }: { svg: string; label: string }) {
  const doc = new DOMParser().parseFromString(svg, 'image/svg+xml')
  const root = doc.documentElement
  if (root.nodeName.toLowerCase() !== 'svg' || doc.querySelector('parsererror')) {
    return <p className="muted">QR could not be drawn.</p>
  }
  return (
    <div className="print-qr" role="img" aria-label={label} dangerouslySetInnerHTML={{ __html: root.outerHTML }} />
  )
}

type CounterFormState = {
  name: string
  code: string
  description: string
  stopId: string
  latitude: string
  longitude: string
  accuracy: string
  geofence: string
  clearStop: boolean
  clearCoordinates: boolean
  clearGeofence: boolean
}

const emptyCounterForm = (): CounterFormState => ({
  name: '',
  code: '',
  description: '',
  stopId: '',
  latitude: '',
  longitude: '',
  accuracy: '',
  geofence: '',
  clearStop: false,
  clearCoordinates: false,
  clearGeofence: false,
})

function optionalNumber(raw: string): number | undefined {
  const trimmed = raw.trim()
  if (!trimmed) return undefined
  const value = Number(trimmed)
  return Number.isFinite(value) ? value : undefined
}

export function CounterPanel({ yatraId, stops }: { yatraId: string; stops: YatraStop[] }) {
  const { copy } = useLanguage()
  const [counters, setCounters] = useState<YatraCounter[]>([])
  const [form, setForm] = useState<CounterFormState>(emptyCounterForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [detail, setDetail] = useState<YatraCounterDetail | null>(null)
  const [qr, setQr] = useState('')
  const [error, setError] = useState('')
  const [listEpoch, setListEpoch] = useState(0)
  const [offset, setOffset] = useState(0)
  const [hasMore, setHasMore] = useState(false)

  useEffect(() => {
    let cancelled = false
    listCounters(yatraId)
      .then((page) => {
        if (cancelled) return
        setCounters(page.items)
        setOffset(page.items.length)
        setHasMore(page.has_more)
        setError('')
      })
      .catch((err: unknown) => {
        if (cancelled || (err instanceof ApiError && err.status === 401)) return
        setError(apiErrorText(err, copy, 'Could not load counters.'))
      })
    return () => {
      cancelled = true
    }
  }, [copy, yatraId, listEpoch])

  useEffect(() => {
    const text = detail?.qr_text
    if (!text) return
    let cancelled = false
    void counterQrSvg(text)
      .then((svg) => {
        if (!cancelled) setQr(svg)
      })
      .catch(() => {
        if (!cancelled) setQr('')
      })
    return () => {
      cancelled = true
    }
  }, [detail?.qr_text])

  function fillForm(counter: YatraCounterDetail) {
    setEditingId(counter.id)
    setForm({
      name: counter.name,
      code: counter.code,
      description: counter.description ?? '',
      stopId: counter.stop_id ?? '',
      latitude: counter.latitude != null ? String(counter.latitude) : '',
      longitude: counter.longitude != null ? String(counter.longitude) : '',
      accuracy: counter.location_accuracy_m != null ? String(counter.location_accuracy_m) : '',
      geofence: counter.geofence_radius_m != null ? String(counter.geofence_radius_m) : '',
      clearStop: false,
      clearCoordinates: false,
      clearGeofence: false,
    })
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    const body = {
      name: form.name.trim(),
      code: form.code.trim().toUpperCase(),
      description: form.description.trim() || undefined,
      stop_id: form.clearStop ? undefined : form.stopId || undefined,
      latitude: form.clearCoordinates ? undefined : optionalNumber(form.latitude),
      longitude: form.clearCoordinates ? undefined : optionalNumber(form.longitude),
      location_accuracy_m: form.clearCoordinates ? undefined : optionalNumber(form.accuracy),
      geofence_radius_m: form.clearGeofence ? undefined : optionalNumber(form.geofence),
    }
    try {
      if (editingId) {
        const updated = await updateCounter(yatraId, editingId, {
          ...body,
          clear_stop: form.clearStop,
          clear_coordinates: form.clearCoordinates,
          clear_geofence: form.clearGeofence,
          clear_description: !form.description.trim(),
        })
        setDetail(updated)
        fillForm(updated)
      } else {
        const created = await createCounter(yatraId, body)
        setDetail(created)
        fillForm(created)
      }
      setListEpoch((epoch) => epoch + 1)
      setError('')
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) return
      setError(apiErrorText(err, copy, editingId ? 'Could not update this counter.' : 'Could not create this counter.'))
    }
  }

  async function open(counterId: string) {
    try {
      const next = await getCounter(yatraId, counterId)
      setDetail(next)
      fillForm(next)
      setError('')
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) return
      setError(apiErrorText(err, copy, 'Could not load this counter.'))
    }
  }

  async function toggle(counter: YatraCounter) {
    try {
      const next = await setCounterActive(yatraId, counter.id, counter.status !== 'active')
      setDetail(next)
      fillForm(next)
      setListEpoch((epoch) => epoch + 1)
      setError('')
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) return
      setError(apiErrorText(err, copy, 'Could not update this counter.'))
    }
  }

  async function onRotate() {
    if (!detail) return
    if (!window.confirm('Rotate this QR? The old printed code stops working immediately.')) return
    try {
      const next = await rotateCounterQr(yatraId, detail.id)
      setDetail(next)
      fillForm(next)
      setListEpoch((epoch) => epoch + 1)
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) return
      setError(apiErrorText(err, copy, 'Could not rotate this QR code.'))
    }
  }

  async function loadMore() {
    try {
      const page = await listCounters(yatraId, offset)
      setCounters((prev) => {
        const seen = new Set(prev.map((item) => item.id))
        return [...prev, ...page.items.filter((item) => !seen.has(item.id))]
      })
      setOffset((value) => value + page.items.length)
      setHasMore(page.has_more)
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) return
      setError(apiErrorText(err, copy, 'Could not load counters.'))
    }
  }

  return (
    <section className="card stack">
      <h2 className="card__title">Counters</h2>
      <ErrorBanner message={error} />
      {counters.length === 0 ? <p>No counters yet. A new counter starts inactive.</p> : null}
      <ul>
        {counters.map((counter) => (
          <li key={counter.id}>
            {counter.code} · {counter.name} · {counter.status}
            <Button type="button" variant="text" onClick={() => void open(counter.id)}>
              Edit / QR
            </Button>
            <Button type="button" variant="text" onClick={() => void toggle(counter)}>
              {counter.status === 'active' ? 'Deactivate' : 'Activate'}
            </Button>
          </li>
        ))}
      </ul>
      {hasMore ? (
        <Button type="button" variant="ghost" onClick={() => void loadMore()}>
          Load more
        </Button>
      ) : null}
      {detail && qr ? <QrImage svg={qr} label={`${detail.name} counter QR`} /> : null}
      {detail ? (
        <Button type="button" variant="ghost" onClick={() => void onRotate()}>
          Rotate QR
        </Button>
      ) : null}
      <form className="stack" onSubmit={(event) => void onSubmit(event)}>
        <Field
          label="Name"
          name="counter-name"
          value={form.name}
          onChange={(event) => setForm((prev) => ({ ...prev, name: event.target.value }))}
        />
        <Field
          label="Code"
          name="counter-code"
          value={form.code}
          onChange={(event) => setForm((prev) => ({ ...prev, code: event.target.value }))}
        />
        <Field
          label="Description"
          name="counter-description"
          value={form.description}
          onChange={(event) => setForm((prev) => ({ ...prev, description: event.target.value }))}
        />
        <label className="stack">
          Stop
          <select
            value={form.stopId}
            onChange={(event) => setForm((prev) => ({ ...prev, stopId: event.target.value, clearStop: false }))}
          >
            <option value="">No stop</option>
            {stops.map((stop) => (
              <option key={stop.id} value={stop.id}>
                {stop.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          <input
            type="checkbox"
            checked={form.clearStop}
            onChange={(event) => setForm((prev) => ({ ...prev, clearStop: event.target.checked }))}
          />{' '}
          Clear stop
        </label>
        <Field
          label="Latitude"
          name="counter-lat"
          value={form.latitude}
          onChange={(event) => setForm((prev) => ({ ...prev, latitude: event.target.value, clearCoordinates: false }))}
        />
        <Field
          label="Longitude"
          name="counter-lng"
          value={form.longitude}
          onChange={(event) => setForm((prev) => ({ ...prev, longitude: event.target.value, clearCoordinates: false }))}
        />
        <Field
          label="Location accuracy (m)"
          name="counter-accuracy"
          value={form.accuracy}
          onChange={(event) => setForm((prev) => ({ ...prev, accuracy: event.target.value, clearCoordinates: false }))}
        />
        <label>
          <input
            type="checkbox"
            checked={form.clearCoordinates}
            onChange={(event) => setForm((prev) => ({ ...prev, clearCoordinates: event.target.checked }))}
          />{' '}
          Clear location
        </label>
        <Field
          label="Geofence radius (m)"
          name="counter-geofence"
          value={form.geofence}
          onChange={(event) => setForm((prev) => ({ ...prev, geofence: event.target.value, clearGeofence: false }))}
        />
        <label>
          <input
            type="checkbox"
            checked={form.clearGeofence}
            onChange={(event) => setForm((prev) => ({ ...prev, clearGeofence: event.target.checked }))}
          />{' '}
          Clear geofence (use global default)
        </label>
        <Button type="submit">{editingId ? 'Save counter' : 'Add counter'}</Button>
        {editingId ? (
          <Button
            type="button"
            variant="ghost"
            onClick={() => {
              setEditingId(null)
              setDetail(null)
              setQr('')
              setForm(emptyCounterForm())
            }}
          >
            New counter
          </Button>
        ) : null}
      </form>
      {/* offset kept for load-more stability */}
      <span className="sr-only">{offset}</span>
    </section>
  )
}

export function TrackerPanel({ yatraId }: { yatraId: string }) {
  const { copy } = useLanguage()
  const [trackers, setTrackers] = useState<YatraTracker[]>([])
  const [label, setLabel] = useState('')
  const [issued, setIssued] = useState('')
  const [error, setError] = useState('')
  const [listEpoch, setListEpoch] = useState(0)

  useEffect(() => {
    let cancelled = false
    listTrackers(yatraId)
      .then((page) => {
        if (cancelled) return
        setTrackers(page.items)
        setError('')
      })
      .catch((err: unknown) => {
        if (cancelled || (err instanceof ApiError && err.status === 401)) return
        setError(apiErrorText(err, copy, 'Could not load trackers.'))
      })
    return () => {
      cancelled = true
    }
  }, [copy, yatraId, listEpoch])

  async function onCreate(event: FormEvent) {
    event.preventDefault()
    try {
      const created = await createTracker(yatraId, label.trim())
      setIssued(created.tracker_token)
      setLabel('')
      setListEpoch((epoch) => epoch + 1)
      setError('')
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) return
      if (err instanceof ApiError && err.status === 409) {
        setError(apiErrorText(err, copy, copy.activeTrackerExists))
        return
      }
      setError(apiErrorText(err, copy, 'Could not assign a tracker.'))
    }
  }

  async function onRevoke(trackerId: string) {
    try {
      await revokeTracker(yatraId, trackerId)
      setListEpoch((epoch) => epoch + 1)
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) return
      setError(apiErrorText(err, copy, 'Could not revoke this tracker.'))
    }
  }

  return (
    <section className="card stack">
      <h2 className="card__title">Trackers</h2>
      <ErrorBanner message={error} />
      {issued ? (
        <p>
          Copy this tracker code now. It is shown once and is not saved in this browser.
          <br />
          <strong>{issued}</strong>
        </p>
      ) : null}
      <ul>
        {trackers.map((tracker) => (
          <li key={tracker.id}>
            {tracker.label} · {tracker.status}
            {tracker.status === 'active' ? (
              <Button type="button" variant="text" onClick={() => void onRevoke(tracker.id)}>
                Revoke
              </Button>
            ) : null}
          </li>
        ))}
      </ul>
      <form className="stack" onSubmit={(event) => void onCreate(event)}>
        <Field label="Label" name="tracker-label" value={label} onChange={(event) => setLabel(event.target.value)} />
        <Button type="submit">Assign tracker</Button>
      </form>
    </section>
  )
}

export function TrackingPanel({ yatraId, showHistory }: { yatraId: string; showHistory: boolean }) {
  const { copy } = useLanguage()
  const [latest, setLatest] = useState<LatestPosition | null>(null)
  const [history, setHistory] = useState<YatraPosition[]>([])
  const [cursor, setCursor] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let stop = false
    let timer = 0
    async function pull() {
      if (document.hidden) return
      try {
        const next = await getLatestPosition(yatraId)
        if (!stop) {
          setLatest(next)
          setError('')
        }
      } catch (err: unknown) {
        if (stop || (err instanceof ApiError && err.status === 401)) return
        setError(apiErrorText(err, copy, 'Could not load the latest position.'))
      }
    }
    function onVisibility() {
      if (!document.hidden) void pull()
    }
    void pull()
    timer = window.setInterval(() => void pull(), POSITION_POLL_MS)
    document.addEventListener('visibilitychange', onVisibility)
    return () => {
      stop = true
      window.clearInterval(timer)
      document.removeEventListener('visibilitychange', onVisibility)
    }
  }, [copy, yatraId])

  async function loadHistory(reset = true) {
    try {
      const page = await listPositions(yatraId, { before: reset ? null : cursor })
      setHistory((prev) => (reset ? page.items : [...prev, ...page.items]))
      setCursor(page.next_cursor)
      setHasMore(page.has_more)
      setError('')
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) return
      setError(apiErrorText(err, copy, 'Could not load position history.'))
    }
  }

  const link =
    latest && latest.latitude != null && latest.longitude != null
      ? osmPositionLink(latest.latitude, latest.longitude)
      : ''

  return (
    <section className="card stack">
      <h2 className="card__title">Position</h2>
      <ErrorBanner message={error} />
      <p>{latest ? freshnessLabel(latest.freshness) : 'Waiting for a position.'}</p>
      {latest?.captured_at ? <p>Position time {formatWhen(latest.captured_at)}</p> : null}
      {latest?.received_at ? <p className="muted">Received {formatWhen(latest.received_at)}</p> : null}
      {latest?.accuracy_m != null ? <p className="muted">Accuracy {latest.accuracy_m} m</p> : null}
      {latest?.latitude != null && latest.longitude != null ? (
        <p>
          {latest.latitude.toFixed(6)}, {latest.longitude.toFixed(6)}
        </p>
      ) : (
        <p className="muted">Coordinates are hidden while the pin is offline.</p>
      )}
      {link ? (
        <a href={link} target="_blank" rel="noreferrer">
          Open in OpenStreetMap
        </a>
      ) : null}
      {showHistory ? (
        <>
          <Button type="button" variant="ghost" onClick={() => void loadHistory(true)}>
            Position history
          </Button>
          {history.length ? (
            <ul>
              {history.map((fix) => (
                <li key={fix.id}>
                  {formatWhen(fix.captured_at)} · {fix.latitude.toFixed(6)}, {fix.longitude.toFixed(6)}
                </li>
              ))}
            </ul>
          ) : null}
          {hasMore ? (
            <Button type="button" variant="ghost" onClick={() => void loadHistory(false)}>
              Load more
            </Button>
          ) : null}
        </>
      ) : null}
    </section>
  )
}

export function AttendanceRosterPanel({ yatraId }: { yatraId: string }) {
  const { copy } = useLanguage()
  const [items, setItems] = useState<import('../lib/yatra.ts').YatraFieldAttendance[]>([])
  const [offset, setOffset] = useState(0)
  const [hasMore, setHasMore] = useState(false)
  const [date, setDate] = useState('')
  const [error, setError] = useState('')
  const [listEpoch, setListEpoch] = useState(0)

  useEffect(() => {
    let cancelled = false
    listYatraAttendance(yatraId, 0, { attendance_date: date || undefined })
      .then((page) => {
        if (cancelled) return
        setItems(page.items)
        setOffset(page.items.length)
        setHasMore(page.has_more)
        setError('')
      })
      .catch((err: unknown) => {
        if (cancelled || (err instanceof ApiError && err.status === 401)) return
        setError(apiErrorText(err, copy, 'Could not load attendance.'))
      })
    return () => {
      cancelled = true
    }
  }, [copy, yatraId, date, listEpoch])

  async function loadMore() {
    try {
      const page = await listYatraAttendance(yatraId, offset, {
        attendance_date: date || undefined,
      })
      setItems((prev) => {
        const seen = new Set(prev.map((item) => item.id))
        return [...prev, ...page.items.filter((item) => !seen.has(item.id))]
      })
      setOffset((value) => value + page.items.length)
      setHasMore(page.has_more)
      setError('')
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) return
      setError(apiErrorText(err, copy, 'Could not load attendance.'))
    }
  }

  return (
    <section className="card stack">
      <h2 className="card__title">Attendance roster</h2>
      <ErrorBanner message={error} />
      <Field
        label="Date"
        name="roster-date"
        value={date}
        onChange={(event) => {
          setDate(event.target.value)
          setListEpoch((epoch) => epoch + 1)
        }}
      />
      <ul>
        {items.map((item) => (
          <li key={item.id}>
            {item.full_name} · {item.login_id} · {item.counter_name} · {formatWhen(item.checked_in_at)}
            {item.scan_distance_m != null ? ` · ${item.scan_distance_m} m` : ''}
          </li>
        ))}
      </ul>
      {hasMore ? (
        <Button type="button" variant="ghost" onClick={() => void loadMore()}>
          Load more
        </Button>
      ) : null}
    </section>
  )
}
