import { afterEach, describe, expect, it } from 'vitest'
import {
  applyAuthenticated,
  clearSession,
  getAccessToken,
  getSnapshot,
  setAccessToken,
  setProfile,
  subscribe,
} from './session.ts'
import type { Profile } from './types.ts'

const profile: Profile = {
  login_id: 'SB-1234ABCD',
  full_name: 'Anita Desai',
  mobile: '9876543210',
  email: null,
  gender: null,
  date_of_birth: null,
  occupation: null,
  address_line: null,
  locality: null,
  district: 'Nashik',
  state: 'Maharashtra',
  pincode: null,
  status: 'active',
  role: 'participant',
}

const originalStorage = globalThis.localStorage

afterEach(() => {
  Object.defineProperty(globalThis, 'localStorage', { value: originalStorage, configurable: true })
  clearSession()
})

describe('session', () => {
  it('notifies subscribers when the profile changes, and stops after unsubscribe', () => {
    const seen: Array<string | null> = []
    const stop = subscribe(() => seen.push(getSnapshot().profile?.full_name ?? null))
    applyAuthenticated(profile)
    setProfile({ ...profile, full_name: 'Anita Updated', login_id: 'SB-NEW' })
    expect(getSnapshot()).toMatchObject({ uid: 'SB-NEW', profile: { full_name: 'Anita Updated' } })
    setProfile(null)
    expect(getSnapshot()).toMatchObject({ uid: 'SB-NEW', profile: null })
    stop()
    clearSession()
    expect(seen).toEqual(['Anita Desai', 'Anita Updated', null])
  })

  it('treats a blank token as signed out and survives storage failures', () => {
    setAccessToken('')
    expect(getAccessToken()).toBeNull()

    Object.defineProperty(globalThis, 'localStorage', {
      configurable: true,
      value: {
        getItem() {
          throw new Error('denied')
        },
        removeItem() {
          throw new Error('denied')
        },
        setItem() {
          throw new Error('denied')
        },
      },
    })
    expect(getAccessToken()).toBeNull()
    expect(() => clearSession()).not.toThrow()
    expect(getSnapshot()).toMatchObject({ ready: true, uid: null, profile: null })
  })
})
