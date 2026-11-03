import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { AppBar } from '../components/AppBar.tsx'
import { Button, ErrorBanner, Screen } from '../components/ui.tsx'
import { useLanguage } from '../contexts/language.tsx'
import { ApiError, apiErrorText, listAdminUsers } from '../lib/api.ts'
import { formatMobile, initials, regionLabel } from '../lib/format.ts'
import type { AdminUserSummary } from '../lib/types.ts'

function roleLabel(role: string): string {
  if (role === 'super_admin') return 'Super admin'
  if (role === 'admin') return 'Admin'
  return 'Participant'
}

function titleCase(value: string): string {
  return value
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(' ')
}

export function AdminUsersScreen() {
  const { copy } = useLanguage()
  const [query, setQuery] = useState('')
  const [submitted, setSubmitted] = useState('')
  const [users, setUsers] = useState<AdminUserSummary[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    listAdminUsers(submitted)
      .then((page) => {
        if (cancelled) return
        setUsers(page.items)
        setTotal(page.total)
        setError('')
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof ApiError && err.status === 401) return
        setError(apiErrorText(err, copy, 'Could not load registered users.'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [submitted, copy])

  function onSearch(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setSubmitted(query)
  }

  async function onLoadMore() {
    setLoadingMore(true)
    try {
      const page = await listAdminUsers(submitted, users.length)
      setUsers((current) => [...current, ...page.items])
      setTotal(page.total)
      setError('')
    } catch (err: unknown) {
      setError(apiErrorText(err, copy, 'Could not load registered users.'))
    } finally {
      setLoadingMore(false)
    }
  }

  return (
    <>
      <AppBar title="Registered users" showEmblem />
      <Screen withNav>
        <form className="admin-search" onSubmit={onSearch}>
          <label className="admin-search__field" htmlFor="admin-search">
            <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill="currentColor"
                d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 5 1.5-1.5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14"
              />
            </svg>
            <input
              id="admin-search"
              name="q"
              value={query}
              placeholder="Search name, mobile, or login ID"
              onChange={(event) => setQuery(event.target.value)}
            />
          </label>
        </form>
        <p className="admin-count" aria-live="polite">
          {loading ? 'Loading…' : `${total} ${total === 1 ? 'person' : 'people'}`}
        </p>
        <ErrorBanner message={error} />
        {loading ? (
          <div className="center-state">
            <div className="spinner" aria-label={copy.pleaseWait} />
          </div>
        ) : users.length === 0 ? (
          <div className="admin-empty">
            <p>No one matches this search.</p>
            <p className="muted">Try a login ID, a mobile number, or part of a name.</p>
          </div>
        ) : (
          <ul className="admin-list">
            {users.map((user) => {
              const place = regionLabel(
                user.district ? titleCase(user.district) : null,
                user.state ? titleCase(user.state) : null,
              )
              return (
                <li key={user.id}>
                  <Link className="admin-row" to={`/admin/users/${user.id}`}>
                    <span className="avatar admin-row__avatar" aria-hidden="true">
                      {initials(user.full_name)}
                    </span>
                    <span className="admin-row__body">
                      <span className="admin-row__name">{titleCase(user.full_name)}</span>
                      <span className="admin-row__meta">
                        {formatMobile(user.mobile)}
                        {place ? ` · ${place}` : ''}
                      </span>
                      <span className="admin-row__id">
                        {user.login_id}
                        <span className={`role-chip role-chip--${user.role}`}>{roleLabel(user.role)}</span>
                      </span>
                    </span>
                    <svg className="admin-row__chevron" width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
                      <path fill="currentColor" d="M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z" />
                    </svg>
                  </Link>
                </li>
              )
            })}
          </ul>
        )}
        {!loading && users.length < total ? (
          <Button type="button" variant="ghost" disabled={loadingMore} onClick={() => void onLoadMore()}>
            {loadingMore ? copy.pleaseWait : 'Load more'}
          </Button>
        ) : null}
      </Screen>
    </>
  )
}
