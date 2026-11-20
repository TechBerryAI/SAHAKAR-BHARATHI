import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { Button, ErrorBanner, Field, Screen } from '../components/ui.tsx'
import { useLanguage } from '../contexts/language.tsx'
import { ApiError, apiErrorText, scanYatraAttendance } from '../lib/api.ts'
import { counterQrFromPayload } from '../lib/counterQr.ts'
import type { YatraFieldAttendance } from '../lib/yatra.ts'

export function YatraAttendanceScreen() {
  const { yatraId = '' } = useParams()
  const { copy } = useLanguage()
  const [raw, setRaw] = useState('')
  const [record, setRecord] = useState<YatraFieldAttendance | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

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
      const saved = await scanYatraAttendance(yatraId, raw.trim())
      setRecord(saved)
      setError('')
    } catch (err: unknown) {
      setRecord(null)
      if (err instanceof ApiError && err.status === 401) return
      setError(apiErrorText(err, copy, 'Attendance could not be recorded.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <AppBar title="Counter attendance" showEmblem />
      <Screen withNav>
        <p>
          <Link to={`/yatras/${yatraId}`}>Back to Yatra</Link>
        </p>
        <ErrorBanner message={error} />
        <form className="card stack" onSubmit={(event) => void onSubmit(event)}>
          <h2 className="card__title">Scan a counter</h2>
          <p>Paste the counter QR text. A booth QR is not accepted.</p>
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
      </Screen>
    </>
  )
}
