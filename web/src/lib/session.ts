import { onAuthStateChanged, type User } from 'firebase/auth'
import { useSyncExternalStore } from 'react'
import { auth } from './firebase.ts'
import type { Profile } from './types.ts'

export type AuthSnapshot = {
  ready: boolean
  uid: string | null
  user: User | null
  profile: Profile | null
}

const empty: AuthSnapshot = {
  ready: false,
  uid: null,
  user: null,
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

export function setUser(user: User | null) {
  snapshot = {
    ready: true,
    uid: user?.uid ?? null,
    user,
    profile: user ? snapshot.profile : null,
  }
  emit()
}

export function setProfile(profile: Profile | null) {
  snapshot = { ...snapshot, profile }
  emit()
}

export function clearSession() {
  snapshot = { ready: true, uid: null, user: null, profile: null }
  emit()
}

onAuthStateChanged(auth, (user) => {
  snapshot = {
    ready: true,
    uid: user?.uid ?? null,
    user,
    profile: user ? snapshot.profile : null,
  }
  emit()
})

export function useSession(): AuthSnapshot {
  return useSyncExternalStore(subscribe, getSnapshot, getSnapshot)
}
