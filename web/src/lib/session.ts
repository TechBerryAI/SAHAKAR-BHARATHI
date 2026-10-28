import type { Profile } from './types.ts'

export const ACCESS_TOKEN_KEY = 'sahakar_access_token'

export type AuthSnapshot = {
  ready: boolean
  uid: string | null
  profile: Profile | null
  // A stored token exists but the server could not be reached to confirm it.
  // The app must not treat this as signed out (the token may still be valid).
  restoreFailed: boolean
}

const empty: AuthSnapshot = {
  ready: false,
  uid: null,
  profile: null,
  restoreFailed: false,
}

const listeners = new Set<() => void>()

let snapshot: AuthSnapshot = empty

function emit() {
  for (const listener of listeners) listener()
}

export function subscribe(listener: () => void): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function getSnapshot(): AuthSnapshot {
  return snapshot
}

export function getAccessToken(): string | null {
  try {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY)
    return token && token.length ? token : null
  } catch {
    return null
  }
}

export function setAccessToken(token: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function applyAuthenticated(profile: Profile) {
  snapshot = {
    ready: true,
    uid: profile.login_id,
    profile,
    restoreFailed: false,
  }
  emit()
}

export function setProfile(profile: Profile | null) {
  snapshot = {
    ...snapshot,
    uid: profile?.login_id ?? snapshot.uid,
    profile,
  }
  emit()
}

export function markReady() {
  snapshot = { ...snapshot, ready: true }
  emit()
}

export function markRestoring() {
  snapshot = { ...snapshot, ready: false, restoreFailed: false }
  emit()
}

export function markRestoreFailed() {
  snapshot = { ready: true, uid: null, profile: null, restoreFailed: true }
  emit()
}

export function clearSession() {
  try {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
  } catch {
    // Ignore quota / private-mode failures; in-memory session still clears.
  }
  snapshot = { ready: true, uid: null, profile: null, restoreFailed: false }
  emit()
}
