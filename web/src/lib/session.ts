import { useSyncExternalStore } from 'react'
import type { Profile } from './types.ts'

const STORAGE_KEY = 'sahakar.session'

export type AuthSnapshot = {
  token: string | null
  expiresAt: string | null
  profile: Profile | null
}

const empty: AuthSnapshot = {
  token: null,
  expiresAt: null,
  profile: null,
}

const listeners = new Set<() => void>()

function readStored(): AuthSnapshot {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return empty
    const parsed = JSON.parse(raw) as AuthSnapshot
    if (!parsed.token) return empty
    return {
      token: parsed.token,
      expiresAt: parsed.expiresAt ?? null,
      profile: parsed.profile ?? null,
    }
  } catch {
    return empty
  }
}

let snapshot: AuthSnapshot = readStored()

function persist() {
  if (!snapshot.token) {
    localStorage.removeItem(STORAGE_KEY)
    return
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(snapshot))
}

function emit() {
  persist()
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

export function getToken(): string | null {
  return snapshot.token
}

export function setSession(token: string, expiresAt: string | null, profile: Profile | null) {
  snapshot = { token, expiresAt, profile }
  emit()
}

export function setProfile(profile: Profile) {
  snapshot = { ...snapshot, profile }
  emit()
}

export function clearSession() {
  snapshot = empty
  emit()
}

export function useSession(): AuthSnapshot {
  return useSyncExternalStore(subscribe, getSnapshot, getSnapshot)
}
