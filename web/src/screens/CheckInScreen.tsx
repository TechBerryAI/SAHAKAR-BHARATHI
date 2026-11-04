import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { Button, Card, ErrorBanner, Field, Screen } from '../components/ui.tsx'
import { useLanguage } from '../contexts/language.tsx'
import { ApiError, apiErrorText, checkIn, myAttendance } from '../lib/api.ts'
import { boothCodeFromManual, boothCodeFromQr } from '../lib/boothCode.ts'
import { formatMobile, regionLabel } from '../lib/format.ts'
import type { AttendanceRecord } from '../lib/types.ts'

type BarcodeHit = { rawValue?: string }
type BarcodeDetectorLike = { detect: (source: HTMLVideoElement) => Promise<BarcodeHit[]> }

function openDetector(): BarcodeDetectorLike | null {
  const scope = window as unknown as {
    BarcodeDetector?: new (options: { formats: string[] }) => BarcodeDetectorLike
  }
  if (!scope.BarcodeDetector) return null
  try {
    return new scope.BarcodeDetector({ formats: ['qr_code'] })
  } catch {
    return null
  }
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

function VisitCard({ visit, already }: { visit: AttendanceRecord; already: boolean }) {
  const place = regionLabel(visit.district, visit.state)
  return (
    <Card
      className={already ? 'card--note' : 'card--success'}
      title={already ? 'Already marked present' : 'You are marked present'}
    >
      <p className="card__body checkin-place">{visit.place_name}</p>
      <p className="muted">{visitTime(visit.checked_in_at)}</p>
      <p className="muted">Recorded from your profile</p>
      <p className="checkin-name">{visit.full_name}</p>
      <p className="muted">
        {formatMobile(visit.mobile)}
        {place ? ` · ${place}` : ''}
      </p>
      <p className="muted">{visit.login_id}</p>
    </Card>
  )
}

export function CheckInScreen() {
  const navigate = useNavigate()
  const { copy } = useLanguage()
  const videoRef = useRef<HTMLVideoElement>(null)
  const detectorRef = useRef<BarcodeDetectorLike | null>(null)
  const lock = useRef({ code: '', until: 0 })
  const submitRef = useRef<(raw: string, source: 'qr' | 'code') => void>(() => {})
  const [code, setCode] = useState('')
  const [cameraOn, setCameraOn] = useState(false)
  const [cameraNote, setCameraNote] = useState('')
  const [latest, setLatest] = useState<AttendanceRecord | null>(null)
  const [history, setHistory] = useState<AttendanceRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const canScan = openDetector() !== null

  useEffect(() => {
    let cancelled = false
    myAttendance()
      .then((page) => {
        if (cancelled) return
        setHistory(page.items)
        setError('')
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof ApiError && err.status === 401) return
        setError(apiErrorText(err, copy, 'Check-in history could not be loaded.'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [copy])

  useEffect(() => {
    if (!cameraOn) return
    const video = videoRef.current
    const detector = openDetector()
    detectorRef.current = detector
    if (!video || !detector) return
    let stop = false
    let stream: MediaStream | null = null
    const timer = window.setInterval(() => {
      if (stop || video.readyState < 2) return
      void detector
        .detect(video)
        .then((codes) => {
          const raw = codes.find((item) => item.rawValue)?.rawValue
          if (raw) submitRef.current(raw, 'qr')
        })
        .catch(() => undefined)
    }, 400)
    void navigator.mediaDevices
      .getUserMedia({ video: { facingMode: 'environment' } })
      .then(async (next) => {
        if (stop) {
          next.getTracks().forEach((track) => track.stop())
          return
        }
        stream = next
        video.srcObject = next
        await video.play()
      })
      .catch(() => {
        if (!stop) setCameraNote('Camera is unavailable. Enter the 6-digit code printed under the QR.')
      })
    return () => {
      stop = true
      window.clearInterval(timer)
      stream?.getTracks().forEach((track) => track.stop())
      video.srcObject = null
      detectorRef.current = null
    }
  }, [cameraOn])

  async function submit(raw: string, source: 'qr' | 'code') {
    const parsed = source === 'qr' ? boothCodeFromQr(raw) : boothCodeFromManual(raw)
    if (!parsed || busy) return
    const now = Date.now()
    if (parsed === lock.current.code && now < lock.current.until) return
    lock.current = { code: parsed, until: now + 4000 }
    setBusy(true)
    setError('')
    try {
      const visit = await checkIn(parsed, source)
      setLatest(visit)
      setHistory((current) => [visit, ...current.filter((item) => item.id !== visit.id)])
      if (source === 'code') setCode('')
    } catch (err: unknown) {
      setError(apiErrorText(err, copy, 'Attendance could not be recorded.'))
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    submitRef.current = (raw, source) => {
      void submit(raw, source)
    }
  })

  return (
    <>
      <AppBar title="Event Check-in" onBack={() => navigate('/')} />
      <Screen withNav>
        <ErrorBanner message={error} />
        {latest ? <VisitCard visit={latest} already={latest.already_recorded} /> : null}
        <Card title="Scan booth QR">
          <p className="card__body">Point the camera at the booth QR. Only a Sahakar Bharati booth code is accepted.</p>
          {canScan ? (
            cameraOn ? (
              <>
                <video ref={videoRef} className="checkin-camera" muted playsInline />
                <Button
                  type="button"
                  disabled={busy}
                  onClick={() => {
                    const video = videoRef.current
                    const detector = detectorRef.current
                    if (!video || !detector || video.readyState < 2) {
                      setCameraNote('Point the camera at the booth QR, then tap Scan QR.')
                      return
                    }
                    void detector
                      .detect(video)
                      .then((codes) => {
                        const raw = codes.find((item) => item.rawValue)?.rawValue ?? ''
                        if (!boothCodeFromQr(raw)) {
                          setCameraNote('No booth QR in the frame. Move closer and tap Scan QR.')
                          return
                        }
                        setCameraNote('')
                        submitRef.current(raw, 'qr')
                      })
                      .catch(() => setCameraNote('No booth QR in the frame. Move closer and tap Scan QR.'))
                  }}
                >
                  Scan QR
                </Button>
              </>
            ) : (
              <Button type="button" onClick={() => setCameraOn(true)}>
                Use camera
              </Button>
            )
          ) : (
            <p className="muted">This browser cannot read QR codes. Enter the 6-digit code printed under the QR.</p>
          )}
          {cameraNote ? <p className="muted">{cameraNote}</p> : null}
        </Card>
        <form
          className="card stack"
          onSubmit={(event) => {
            event.preventDefault()
            void submit(code, 'code')
          }}
        >
          <Field
            label="Booth code"
            name="booth-code"
            inputMode="numeric"
            autoComplete="off"
            maxLength={6}
            value={code}
            placeholder="6 digits"
            onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
          />
          <Button type="submit" disabled={busy || code.length !== 6}>
            {busy ? copy.pleaseWait : 'Mark attendance'}
          </Button>
          <p className="muted">The same code is printed under the QR.</p>
        </form>
        <Card title="Your visits">
          {loading && history.length === 0 ? (
            <div className="center-state">
              <div className="spinner" aria-label={copy.pleaseWait} />
            </div>
          ) : history.length === 0 ? (
            <p className="card__body">No visits yet.</p>
          ) : (
            <ul className="admin-list">
              {history.map((visit) => (
                <li key={visit.id} className="admin-row attendance-row">
                  <span className="admin-row__body">
                    <span className="admin-row__name">{visit.place_name}</span>
                    <span className="admin-row__meta">{visitTime(visit.checked_in_at)}</span>
                  </span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </Screen>
    </>
  )
}
