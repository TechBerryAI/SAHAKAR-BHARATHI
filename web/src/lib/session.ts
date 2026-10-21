import { useSyncExternalStore } from 'react'
import type { Profile } from './types.ts'

export const ACCESS_TOKEN_KEY = 'sahakar_access_token'

export type AuthSnapshot = {
  ready: boolean
  uid: string | null
  profile: Profile | null
}

const empty: AuthSnapshot = {
  ready: false,
  uid: null,
  profile: null,
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

export function clearSession() {
  try {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
  } catch {
    // Ignore quota / private-mode failures; in-memory session still clears.
  }
  snapshot = { ready: true, uid: null, profile: null }
  emit()
}

export function useSession(): AuthSnapshot {
  return useSyncExternalStore(subscribe, getSnapshot, getSnapshot)
}
