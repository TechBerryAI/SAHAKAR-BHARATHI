import { NavLink, useLocation } from 'react-router-dom'

export function AdminSections() {
  const { pathname } = useLocation()
  const places = pathname.startsWith('/admin/places')
  return (
    <nav className="admin-sections" aria-label="Admin sections">
      <NavLink to="/admin" end className={() => `admin-section${places ? '' : ' is-active'}`}>
        People
      </NavLink>
      <NavLink to="/admin/places" className={() => `admin-section${places ? ' is-active' : ''}`}>
        Booths
      </NavLink>
    </nav>
  )
}
