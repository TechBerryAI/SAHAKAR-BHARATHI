import { NavLink, useLocation } from 'react-router-dom'
import { useSession } from '../hooks/useSession.ts'

export function AdminSections() {
  const { pathname } = useLocation()
  const { profile } = useSession()
  const places = pathname.startsWith('/admin/places')
  const yatras = pathname.startsWith('/admin/yatras')
  const districts = pathname.startsWith('/admin/districts')
  const people = !places && !yatras && !districts
  return (
    <nav className="admin-sections" aria-label="Admin sections">
      <NavLink to="/admin" end className={() => `admin-section${people ? ' is-active' : ''}`}>
        People
      </NavLink>
      <NavLink to="/admin/places" className={() => `admin-section${places ? ' is-active' : ''}`}>
        Booths
      </NavLink>
      <NavLink to="/admin/yatras" className={() => `admin-section${yatras ? ' is-active' : ''}`}>
        Yatras
      </NavLink>
      {profile?.role === 'super_admin' ? (
        <NavLink to="/admin/districts" className={() => `admin-section${districts ? ' is-active' : ''}`}>
          Districts
        </NavLink>
      ) : null}
    </nav>
  )
}
