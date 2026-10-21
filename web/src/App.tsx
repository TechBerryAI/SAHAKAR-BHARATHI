import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { AppShell } from './components/AppShell.tsx'
import { useSession } from './lib/session.ts'
import { HomeScreen } from './screens/HomeScreen.tsx'
import { ProfileScreen } from './screens/ProfileScreen.tsx'
import { RegisterScreen } from './screens/RegisterScreen.tsx'
import { RegisterSuccessScreen } from './screens/RegisterSuccessScreen.tsx'
import { SignInScreen } from './screens/SignInScreen.tsx'

function GuestRoute({ children }: { children: ReactNode }) {
  const { uid } = useSession()
  if (uid) return <Navigate to="/" replace />
  return children
}

function ProtectedRoute({ children }: { children: ReactNode }) {
  const { uid } = useSession()
  if (!uid) return <Navigate to="/sign-in" replace />
  return children
}

function AppRoutes() {
  const location = useLocation()
  const { ready } = useSession()

  if (!ready) {
    return (
      <div className="center-state">
        <div className="spinner" aria-label="Loading" />
      </div>
    )
  }

  return (
    <div className="screen-slot" key={location.pathname}>
      <Routes location={location}>
        <Route
          path="/sign-in"
          element={
            <GuestRoute>
              <SignInScreen />
            </GuestRoute>
          }
        />
        <Route path="/register" element={<RegisterScreen />} />
        <Route
          path="/register/success"
          element={
            <ProtectedRoute>
              <RegisterSuccessScreen />
            </ProtectedRoute>
          }
        />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <HomeScreen />
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <ProfileScreen />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}

export default function App() {
  const location = useLocation()
  const showNav = location.pathname === '/' || location.pathname === '/profile'

  return (
    <AppShell showNav={showNav}>
      <AppRoutes />
    </AppShell>
  )
}
