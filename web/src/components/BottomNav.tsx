import { NavLink } from 'react-router-dom'
import { useLanguage } from '../contexts/language.tsx'
import { useSession } from '../hooks/useSession.ts'
import { isStaffRole } from '../lib/api.ts'

export function BottomNav() {
  const { copy } = useLanguage()
  const { profile } = useSession()
  const staff = isStaffRole(profile?.role)
  return (
    <nav className="bottom-nav" aria-label={copy.mainNav}>
      <NavLink to="/" end className={({ isActive }) => `nav-item${isActive ? ' is-active' : ''}`}>
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path
            fill="currentColor"
            d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"
          />
        </svg>
        {copy.home}
      </NavLink>
      <NavLink
        to="/profile"
        className={({ isActive }) => `nav-item${isActive ? ' is-active' : ''}`}
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path
            fill="currentColor"
            d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8V22h19.2v-2.8c0-3.2-6.4-4.8-9.6-4.8z"
          />
        </svg>
        {copy.profile}
      </NavLink>
      {staff ? (
        <NavLink to="/admin" className={({ isActive }) => `nav-item${isActive ? ' is-active' : ''}`}>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              fill="currentColor"
              d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3m-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3m0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5C15 14.17 10.33 13 8 13m8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5"
            />
          </svg>
          Admin
        </NavLink>
      ) : null}
    </nav>
  )
}
