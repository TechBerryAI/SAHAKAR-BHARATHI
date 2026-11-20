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
  revokeTracker,
  setCounterActive,
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

export function CounterPanel({ yatraId }: { yatraId: string }) {
  const { copy } = useLanguage()
  const [counters, setCounters] = useState<YatraCounter[]>([])
  const [name, setName] = useState('')
  const [code, setCode] = useState('')
  const [detail, setDetail] = useState<YatraCounterDetail | null>(null)
  const [qr, setQr] = useState('')
  const [error, setError] = useState('')
  const [listEpoch, setListEpoch] = useState(0)

  useEffect(() => {
    let cancelled = false
    listCounters(yatraId)
      .then((page) => {
        if (cancelled) return
        setCounters(page.items)
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

  async function onCreate(event: FormEvent) {
    event.preventDefault()
    try {
      const created = await createCounter(yatraId, { name: name.trim(), code: code.trim().toUpperCase() })
      setName('')
      setCode('')
      setDetail(created)
      setListEpoch((epoch) => epoch + 1)
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) return
      setError(apiErrorText(err, copy, 'Could not create this counter.'))
    }
  }

  async function open(counterId: string) {
    try {
      setDetail(await getCounter(yatraId, counterId))
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
      setListEpoch((epoch) => epoch + 1)
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) return
      setError(apiErrorText(err, copy, 'Could not update this counter.'))
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
              QR
            </Button>
            <Button type="button" variant="text" onClick={() => void toggle(counter)}>
              {counter.status === 'active' ? 'Deactivate' : 'Activate'}
            </Button>
          </li>
        ))}
      </ul>
      {detail && qr ? <QrImage svg={qr} label={`${detail.name} counter QR`} /> : null}
      <form className="stack" onSubmit={(event) => void onCreate(event)}>
        <Field label="Name" name="counter-name" value={name} onChange={(event) => setName(event.target.value)} />
        <Field label="Code" name="counter-code" value={code} onChange={(event) => setCode(event.target.value)} />
        <Button type="submit">Add counter</Button>
      </form>
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
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) return
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
  const [history, setHistory] = useState<YatraPosition[] | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let stop = false
    async function pull() {
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
    void pull()
    const timer = window.setInterval(() => void pull(), POSITION_POLL_MS)
    return () => {
      stop = true
      window.clearInterval(timer)
    }
  }, [copy, yatraId])

  async function loadHistory() {
    try {
      const page = await listPositions(yatraId)
      setHistory(page.items)
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
      {latest?.received_at ? <p className="muted">Received {formatWhen(latest.received_at)}</p> : null}
      {link ? (
        <a href={link} target="_blank" rel="noreferrer">
          Open in OpenStreetMap
        </a>
      ) : null}
      {showHistory ? (
        <>
          <Button type="button" variant="ghost" onClick={() => void loadHistory()}>
            Position history
          </Button>
          {history ? (
            <ul>
              {history.map((fix) => (
                <li key={fix.id}>
                  {formatWhen(fix.captured_at)} · {fix.latitude}, {fix.longitude}
                </li>
              ))}
            </ul>
          ) : null}
        </>
      ) : null}
    </section>
  )
}
