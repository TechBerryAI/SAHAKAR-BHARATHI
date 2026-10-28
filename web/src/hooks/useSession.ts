import { useSyncExternalStore } from 'react'
import { getSnapshot, subscribe, type AuthSnapshot } from '../lib/session.ts'

export function useSession(): AuthSnapshot {
  return useSyncExternalStore(subscribe, getSnapshot, getSnapshot)
}
