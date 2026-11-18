import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AdminSections } from '../components/AdminSections.tsx'
import { AppBar } from '../components/AppBar.tsx'
import { Button, ErrorBanner, Field, Screen, SelectField } from '../components/ui.tsx'
import { useLanguage } from '../contexts/language.tsx'
import {
  ApiError,
  apiErrorText,
  approveProtocolItem,
  assignYatraAdmin,
  changeYatraStatus,
  createProtocolItem,
  createStop,
  deleteStop,
  downloadProtocolDocument,
  getYatra,
  getYatraRoute,
  listAdminUsers,
  listProtocol,
  publishYatra,
  rejectProtocolItem,
  removeYatraAdmin,
  reviewProtocolItem,
  submitProtocolItem,
  updateYatra,
  updateYatraRoute,
  uploadProtocolDocument,
} from '../lib/api.ts'
import {
  documentOpen,
  formatWhen,
  protocolCategoryLabel,
  protocolStatusLabel,
  yatraStatusLabel,
  type ProtocolItem,
  type YatraDetail,
  type YatraRoute,
} from '../lib/yatra.ts'

export function YatraScreen({ admin = false }: { admin?: boolean }) {
  const { yatraId = '' } = useParams()
  const { copy } = useLanguage()
  const [yatra, setYatra] = useState<YatraDetail | null>(null)
  const [items, setItems] = useState<ProtocolItem[] | null>(null)
  const [route, setRoute] = useState<YatraRoute | null>(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [stopName, setStopName] = useState('')
  const [stopAddress, setStopAddress] = useState('')
  const [routeLabel, setRouteLabel] = useState('')
  const [loginQuery, setLoginQuery] = useState('')
  const [itemTitle, setItemTitle] = useState('')
  const [itemCategory, setItemCategory] = useState('pre')
  const [rejecting, setRejecting] = useState<string | null>(null)
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  function load() {
    setLoading(true)
    getYatra(yatraId)
      .then((detail) => {
        setYatra(detail)
        setName(detail.name)
        setDescription(detail.description ?? '')
        setError('')
        if (detail.capabilities.submit_protocol || detail.capabilities.review_protocol || detail.capabilities.manage_items) {
          return listProtocol(yatraId).then((page) => setItems(page.items))
        }
        setItems(null)
        return undefined
      })
      .catch((err: unknown) => {
        if (err instanceof ApiError && err.status === 401) return
        setError(apiErrorText(err, copy, 'Could not load this Yatra.'))
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    let cancelled = false
    getYatra(yatraId)
      .then((detail) => {
        if (cancelled) return
        setYatra(detail)
        setName(detail.name)
        setDescription(detail.description ?? '')
        setError('')
        if (
          detail.capabilities.submit_protocol ||
          detail.capabilities.review_protocol ||
          detail.capabilities.manage_items
        ) {
          return listProtocol(yatraId).then((page) => {
            if (!cancelled) setItems(page.items)
          })
        }
        setItems(null)
        return undefined
      })
      .catch((err: unknown) => {
        if (cancelled || (err instanceof ApiError && err.status === 401)) return
        setError(apiErrorText(err, copy, 'Could not load this Yatra.'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [copy, yatraId])

  useEffect(() => {
    if (!yatra?.capabilities.manage_stops) return
    getYatraRoute(yatraId)
      .then((value) => {
        setRoute(value)
        setRouteLabel(value.label ?? '')
      })
      .catch(() => setRoute(null))
  }, [yatra?.capabilities.manage_stops, yatraId])

  async function run(action: () => Promise<unknown>) {
    setError('')
    try {
      await action()
      load()
    } catch (err: unknown) {
      setError(apiErrorText(err, copy, 'That change could not be saved.'))
    }
  }

  async function onAssign(event: FormEvent) {
    event.preventDefault()
    const found = await listAdminUsers(loginQuery.trim())
    const match = found.items.find((person) => person.login_id.toLowerCase() === loginQuery.trim().toLowerCase())
    if (!match) {
      setError('No account matched that login ID.')
      return
    }
    await run(async () => {
      await assignYatraAdmin(yatraId, match.id)
      setLoginQuery('')
    })
  }

  if (loading && !yatra) {
    return (
      <Screen withNav>
        <div className="center-state">
          <div className="spinner" aria-label={copy.pleaseWait} />
        </div>
      </Screen>
    )
  }

  if (!yatra) {
    return (
      <Screen withNav>
        <ErrorBanner message={error || 'That Yatra was not found.'} />
      </Screen>
    )
  }

  const back = admin ? '/admin/yatras' : '/yatras'

  return (
    <>
      <AppBar title={yatra.name} showEmblem />
      <Screen withNav>
        {admin ? <AdminSections /> : null}
        <p>
          <Link to={back}>Back to Yatras</Link>
        </p>
        <ErrorBanner message={error} />
        <section className="card stack">
          <p className="card__kicker">{yatra.public_code}</p>
          <h2 className="card__title">{yatra.name}</h2>
          <p>{yatra.description}</p>
          <p>
            {yatra.district.name}
            {yatra.district.code ? ` (${yatra.district.code})` : ''}
          </p>
          <p>
            {formatWhen(yatra.starts_at)} – {formatWhen(yatra.ends_at)}
          </p>
          <p>
            {yatraStatusLabel(yatra.status)}
            {yatra.published ? ' · Published' : ' · Not published'}
          </p>
          {yatra.capabilities.transition && yatra.next_status ? (
            <Button type="button" onClick={() => void run(() => changeYatraStatus(yatraId, yatra.next_status!))}>
              Mark as {yatraStatusLabel(yatra.next_status)}
            </Button>
          ) : null}
          {yatra.capabilities.publish ? (
            <Button type="button" variant="ghost" onClick={() => void run(() => publishYatra(yatraId))}>
              Publish
            </Button>
          ) : null}
        </section>

        {yatra.capabilities.edit ? (
          <form
            className="card stack"
            onSubmit={(event) => {
              event.preventDefault()
              void run(() => updateYatra(yatraId, { name: name.trim(), description: description.trim() }))
            }}
          >
            <h2 className="card__title">Edit</h2>
            <Field label="Name" name="edit-name" value={name} onChange={(event) => setName(event.target.value)} />
            <Field
              label="Description"
              name="edit-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
            />
            <Button type="submit">Save</Button>
          </form>
        ) : null}

        <section className="card stack">
          <h2 className="card__title">Stops</h2>
          {yatra.stops.length === 0 ? <p>No stops yet.</p> : null}
          <ol>
            {yatra.stops.map((stop) => (
              <li key={stop.id}>
                {stop.sequence}. {stop.name}
                {stop.address ? ` — ${stop.address}` : ''}
                {stop.latitude != null && stop.longitude != null ? ` (${stop.latitude}, ${stop.longitude})` : ''}
                {yatra.capabilities.manage_stops ? (
                  <Button type="button" variant="text" onClick={() => void run(() => deleteStop(yatraId, stop.id))}>
                    Remove
                  </Button>
                ) : null}
              </li>
            ))}
          </ol>
          {yatra.capabilities.manage_stops ? (
            <form
              className="stack"
              onSubmit={(event) => {
                event.preventDefault()
                void run(async () => {
                  await createStop(yatraId, { name: stopName.trim(), address: stopAddress.trim() || undefined })
                  setStopName('')
                  setStopAddress('')
                })
              }}
            >
              <Field label="Stop name" name="stop-name" value={stopName} onChange={(event) => setStopName(event.target.value)} />
              <Field
                label="Address"
                name="stop-address"
                value={stopAddress}
                onChange={(event) => setStopAddress(event.target.value)}
              />
              <Button type="submit" disabled={stopName.trim().length < 2}>
                Add stop
              </Button>
            </form>
          ) : null}
        </section>

        {yatra.capabilities.manage_stops && route ? (
          <form
            className="card stack"
            onSubmit={(event) => {
              event.preventDefault()
              void run(() => updateYatraRoute(yatraId, { label: routeLabel.trim(), geometry: route.geometry ?? undefined }))
            }}
          >
            <h2 className="card__title">Route</h2>
            <Field label="Label" name="route-label" value={routeLabel} onChange={(event) => setRouteLabel(event.target.value)} />
            <Button type="submit">Save route label</Button>
          </form>
        ) : null}

        {yatra.capabilities.assign_admin ? (
          <section className="card stack">
            <h2 className="card__title">Yatra administrators</h2>
            <ul>
              {yatra.administrators.map((person) => (
                <li key={person.user_id}>
                  {person.full_name} ({person.login_id})
                  <Button type="button" variant="text" onClick={() => void run(() => removeYatraAdmin(yatraId, person.user_id))}>
                    Remove
                  </Button>
                </li>
              ))}
            </ul>
            <form className="stack" onSubmit={(event) => void onAssign(event)}>
              <Field
                label="Login ID"
                name="yatra-admin-login"
                value={loginQuery}
                onChange={(event) => setLoginQuery(event.target.value)}
              />
              <Button type="submit" disabled={loginQuery.trim().length < 2}>
                Assign
              </Button>
            </form>
          </section>
        ) : null}

        {items ? (
          <section className="card stack">
            <h2 className="card__title">Protocol</h2>
            {items.map((item) => (
              <article key={item.id} className="stack">
                <h3>
                  {item.title} {item.required ? '' : '(optional)'}
                </h3>
                <p>
                  {protocolCategoryLabel(item.category)} · {protocolStatusLabel(item.status)}
                  {item.overdue ? ' · Overdue' : ''}
                </p>
                {item.rejection_reason ? <p>Rejected: {item.rejection_reason}</p> : null}
                {item.submitted_at ? <p>Submitted {formatWhen(item.submitted_at)}</p> : null}
                {item.reviewed_at ? <p>Reviewed {formatWhen(item.reviewed_at)}</p> : null}
                <ul>
                  {item.documents.map((document) => (
                    <li key={document.id}>
                      <button
                        type="button"
                        className="btn btn--text"
                        onClick={() =>
                          void downloadProtocolDocument(yatraId, item.id, document.id, document.original_filename).catch(
                            (err: unknown) => setError(apiErrorText(err, copy, 'Could not download this document.')),
                          )
                        }
                      >
                        {document.original_filename}
                      </button>
                    </li>
                  ))}
                </ul>
                {yatra.capabilities.submit_protocol && (item.status === 'pending' || item.status === 'rejected') ? (
                  <Button type="button" onClick={() => void run(() => submitProtocolItem(yatraId, item.id))}>
                    {item.status === 'rejected' ? 'Resubmit' : 'Submit'}
                  </Button>
                ) : null}
                {yatra.capabilities.review_protocol && (item.status === 'submitted' || item.status === 'resubmitted') ? (
                  <Button type="button" variant="ghost" onClick={() => void run(() => reviewProtocolItem(yatraId, item.id))}>
                    Start review
                  </Button>
                ) : null}
                {yatra.capabilities.review_protocol && item.status === 'under_review' ? (
                  <>
                    <Button type="button" onClick={() => void run(() => approveProtocolItem(yatraId, item.id))}>
                      Approve
                    </Button>
                    {rejecting === item.id ? (
                      <form
                        className="stack"
                        onSubmit={(event) => {
                          event.preventDefault()
                          void run(async () => {
                            await rejectProtocolItem(yatraId, item.id, reason.trim())
                            setReason('')
                            setRejecting(null)
                          })
                        }}
                      >
                        <Field label="Rejection reason" name={`reason-${item.id}`} value={reason} onChange={(event) => setReason(event.target.value)} />
                        <Button type="submit" variant="danger" disabled={reason.trim().length < 3}>
                          Reject
                        </Button>
                      </form>
                    ) : (
                      <Button type="button" variant="danger" onClick={() => setRejecting(item.id)}>
                        Reject
                      </Button>
                    )}
                  </>
                ) : null}
                {yatra.capabilities.submit_protocol && documentOpen(item.status) ? (
                  <label className="field">
                    <span className="field__label">Upload PDF, JPEG, or PNG</span>
                    <input
                      type="file"
                      accept="application/pdf,image/jpeg,image/png"
                      onChange={(event) => {
                        const file = event.target.files?.[0]
                        event.target.value = ''
                        if (!file) return
                        void run(() => uploadProtocolDocument(yatraId, item.id, file))
                      }}
                    />
                  </label>
                ) : null}
              </article>
            ))}
            {yatra.capabilities.manage_items ? (
              <form
                className="stack"
                onSubmit={(event) => {
                  event.preventDefault()
                  void run(async () => {
                    await createProtocolItem(yatraId, {
                      category: itemCategory,
                      title: itemTitle.trim(),
                      required: false,
                    })
                    setItemTitle('')
                  })
                }}
              >
                <h3>Add checklist item</h3>
                <SelectField label="Phase" name="item-category" value={itemCategory} onChange={(event) => setItemCategory(event.target.value)}>
                  <option value="pre">Pre-Yatra</option>
                  <option value="during">During Yatra</option>
                  <option value="post">Post-Yatra</option>
                </SelectField>
                <Field label="Title" name="item-title" value={itemTitle} onChange={(event) => setItemTitle(event.target.value)} />
                <Button type="submit" disabled={itemTitle.trim().length < 2}>
                  Add item
                </Button>
              </form>
            ) : null}
          </section>
        ) : null}
      </Screen>
    </>
  )
}
