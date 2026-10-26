import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { AppShell } from './components/AppShell.tsx'
import { Button, Card, Screen } from './components/ui.tsx'
import { logout, restoreSession } from './lib/api.ts'
import { useLanguage } from './lib/language.tsx'
import { useSession } from './lib/session.ts'
import { isStaffRole } from './lib/api.ts'
import { AdminUserScreen } from './screens/AdminUserScreen.tsx'
import { AdminUsersScreen } from './screens/AdminUsersScreen.tsx'
import { HomeScreen } from './screens/HomeScreen.tsx'
import { ProfileScreen } from './screens/ProfileScreen.tsx'
import { RegisterScreen } from './screens/RegisterScreen.tsx'
import { RegisterSuccessScreen } from './screens/RegisterSuccessScreen.tsx'
import { SignInScreen } from './screens/SignInScreen.tsx'
import { StartScreen } from './screens/StartScreen.tsx'

function GuestRoute({ children }: { children: ReactNode }) {
  const { uid } = useSession()
  if (uid) return <Navigate to="/" replace />
  return children
}

function ProtectedRoute({ children }: { children: ReactNode }) {
  const { uid } = useSession()
  if (!uid) return <Navigate to="/" replace />
  return children
}

function AdminRoute({ children }: { children: ReactNode }) {
  const { uid, profile } = useSession()
  if (!uid) return <Navigate to="/" replace />
  if (!isStaffRole(profile?.role)) return <Navigate to="/" replace />
  return children
}

function HomeOrStart() {
  const { uid } = useSession()
  if (!uid) return <StartScreen />
  return <HomeScreen />
}

function RestoreFailed() {
  const { copy } = useLanguage()
  return (
    <Screen>
      <Card title={copy.networkError}>
        <div className="stack">
          <Button type="button" onClick={() => void restoreSession()}>
            {copy.goHome}
          </Button>
          <Button type="button" variant="ghost" onClick={() => void logout()}>
            {copy.logOut}
          </Button>
        </div>
      </Card>
    </Screen>
  )
}

function AppRoutes() {
  const location = useLocation()
  const { ready, restoreFailed } = useSession()
  const { copy } = useLanguage()

  if (!ready) {
    return (
      <div className="center-state">
        <div className="spinner" aria-label={copy.pleaseWait} />
      </div>
    )
  }

  // A saved session could not be checked (offline or server down). Showing the
  // signed-out start screen here would look like an unexplained logout.
  if (restoreFailed) {
    return <RestoreFailed />
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
        <Route path="/" element={<HomeOrStart />} />
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <ProfileScreen />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={
            <AdminRoute>
              <AdminUsersScreen />
            </AdminRoute>
          }
        />
        <Route
          path="/admin/users/:userId"
          element={
            <AdminRoute>
              <AdminUserScreen />
            </AdminRoute>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}

export default function App() {
  const location = useLocation()
  const { uid } = useSession()
  const showNav =
    Boolean(uid) &&
    (location.pathname === '/' || location.pathname === '/profile' || location.pathname.startsWith('/admin'))

  return (
    <AppShell showNav={showNav}>
      <AppRoutes />
    </AppShell>
  )
}
