import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ApiError,
  apiErrorText,
  getMe,
  logout,
  registerAndSignIn,
  RegisteredButSignInFailedError,
  restoreSession,
  signIn,
  updateMe,
} from './api.ts'
import { copyFor } from './copy.ts'
import { ACCESS_TOKEN_KEY, clearSession, getSnapshot } from './session.ts'
import type { RegistrationCreate } from './types.ts'

const API = 'https://api.unit-test.invalid'

const profileBody = {
  login_id: 'SB-1234ABCD',
  full_name: 'Anita Desai',
  mobile: '9876543210',
  email: null,
  gender: 'Female',
  date_of_birth: null,
  occupation: 'Farmer',
  address_line: null,
  locality: null,
  district: 'Nashik',
  state: 'Maharashtra',
  pincode: null,
  status: 'active',
}

type Reply = { status: number; body?: unknown } | 'network-error'

// Stands in for the server: each call takes the next scripted reply and records the request.
function scriptServer(...replies: Reply[]) {
  const calls: { url: string; init: RequestInit }[] = []
  const fetchMock = vi.fn(async (url: string, init: RequestInit = {}) => {
    calls.push({ url, init })
    const reply = replies.shift()
    if (!reply) throw new Error(`Unexpected request to ${url}`)
    if (reply === 'network-error') throw new TypeError('Failed to fetch')
    const text = reply.body === undefined ? '' : JSON.stringify(reply.body)
    return new Response(reply.status === 204 ? null : text, { status: reply.status })
  })
  vi.stubGlobal('fetch', fetchMock)
  return calls
}

const registration: RegistrationCreate = {
  full_name: 'Anita Desai',
  mobile: '9876543210',
  pin: '2468',
  verification_token: 'verification-token-value',
}

beforeEach(() => {
  clearSession()
  localStorage.clear()
})

describe('session lifecycle', () => {
  it('signs in, stores the token and the profile from the session response', async () => {
    const calls = scriptServer({ status: 200, body: { ...profileBody, access_token: 'tok-1', token_type: 'bearer' } })
    const profile = await signIn(' SB-1234ABCD ', '2468')
    expect(profile.login_id).toBe('SB-1234ABCD')
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBe('tok-1')
    expect(getSnapshot().uid).toBe('SB-1234ABCD')
    expect(calls[0].url).toBe(`${API}/api/v1/sessions`)
    expect(JSON.parse(String(calls[0].init.body))).toEqual({ identifier: 'SB-1234ABCD', pin: '2468' })
  })

  it('a wrong PIN is an error, not a session', async () => {
    scriptServer({ status: 401, body: { detail: 'Login ID or PIN is incorrect.' } })
    const error = await signIn('SB-1234ABCD', '0000').catch((err: unknown) => err)
    expect(error).toBeInstanceOf(ApiError)
    expect(apiErrorText(error, copyFor('en'), 'fallback')).toBe(copyFor('en').invalidCredentials)
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull()
  })

  it('an expired or revoked session clears local state on the next API call', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'expired')
    scriptServer({ status: 401, body: { detail: 'Session is invalid or expired.' } })
    await expect(getMe({ fresh: true })).rejects.toMatchObject({ status: 401 })
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull()
    expect(getSnapshot()).toMatchObject({ ready: true, uid: null, restoreFailed: false })
  })

  it('logout clears local state even when the server cannot be reached, and asks it to revoke', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    const calls = scriptServer('network-error')
    await logout()
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull()
    expect(calls[0].url).toBe(`${API}/api/v1/sessions/current`)
    expect(calls[0].init.method).toBe('DELETE')
    expect(calls[0].init.keepalive).toBe(true)
    expect(new Headers(calls[0].init.headers).get('Authorization')).toBe('Bearer tok-1')
  })
})

describe('restoring a saved session at start-up', () => {
  it('restores a valid session from the server', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    scriptServer({ status: 200, body: profileBody })
    await restoreSession()
    expect(getSnapshot()).toMatchObject({ ready: true, uid: 'SB-1234ABCD', restoreFailed: false })
  })

  it('keeps the token and reports failure when offline instead of looking signed out', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    scriptServer('network-error')
    await restoreSession()
    expect(getSnapshot()).toMatchObject({ ready: true, uid: null, restoreFailed: true })
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBe('tok-1')
  })

  it('treats a server error like being offline', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    scriptServer({ status: 503, body: { detail: 'unavailable' } })
    await restoreSession()
    expect(getSnapshot().restoreFailed).toBe(true)
  })

  it('drops a rejected token', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    scriptServer({ status: 401, body: { detail: 'Session is invalid or expired.' } })
    await restoreSession()
    expect(getSnapshot()).toMatchObject({ ready: true, uid: null, restoreFailed: false })
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull()
  })

  it('without a token, is ready immediately and makes no request', async () => {
    const calls = scriptServer()
    await restoreSession()
    expect(getSnapshot()).toMatchObject({ ready: true, uid: null, restoreFailed: false })
    expect(calls).toHaveLength(0)
  })
})

describe('registration and profile', () => {
  it('reports a created account whose sign-in failed, with its login ID', async () => {
    scriptServer({ status: 201, body: { ...profileBody, registration_id: 'SB-1234ABCD' } }, { status: 503 })
    const error = await registerAndSignIn(registration).catch((err: unknown) => err)
    expect(error).toBeInstanceOf(RegisteredButSignInFailedError)
    expect((error as RegisteredButSignInFailedError).loginId).toBe('SB-1234ABCD')
  })

  it('a failed registration is not reported as success', async () => {
    scriptServer({ status: 409, body: { detail: 'An account with these details already exists.' } })
    const error = await registerAndSignIn(registration).catch((err: unknown) => err)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).not.toBeInstanceOf(RegisteredButSignInFailedError)
    expect(apiErrorText(error, copyFor('hi'), 'fallback')).toBe(copyFor('hi').duplicateAccount)
  })

  it('a rejected profile update leaves the profile unchanged', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    scriptServer({ status: 200, body: profileBody }, { status: 422, body: { detail: [{ msg: 'PIN code must be 6 digits.' }] } })
    await getMe({ fresh: true })
    await expect(updateMe({ pincode: '12' })).rejects.toMatchObject({ status: 422 })
    expect(getSnapshot().profile?.full_name).toBe('Anita Desai')
  })

  it('a network failure is reported as such', () => {
    expect(apiErrorText(new ApiError(0, 'x'), copyFor('en'), 'fallback')).toBe(copyFor('en').networkError)
    expect(apiErrorText(new Error('boom'), copyFor('en'), 'fallback')).toBe('fallback')
  })
})
