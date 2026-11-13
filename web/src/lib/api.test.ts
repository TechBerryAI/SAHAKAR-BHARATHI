import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ApiError,
  apiErrorKey,
  apiErrorText,
  checkIn,
  confirmPhoneVerification,
  createPlace,
  getAdminUser,
  getMe,
  getPlace,
  isStaffRole,
  listAdminUsers,
  listPlaceAttendance,
  listPlaces,
  logout,
  myAttendance,
  profileFromPayload,
  registerAndSignIn,
  RegisteredButSignInFailedError,
  resolveApiBase,
  restoreSession,
  sendPhoneVerification,
  setAdminUserRole,
  signIn,
  updateMe,
  updatePlace,
} from './api.ts'
import { copyFor } from './copy.ts'
import { ACCESS_TOKEN_KEY, clearSession, getSnapshot } from './session.ts'
import type { RegistrationCreate } from './types.ts'

const API = 'https://api.unit-test.invalid'

describe('resolveApiBase', () => {
  it('keeps a remote API URL in dev and production', () => {
    expect(resolveApiBase(API, true)).toBe(API)
    expect(resolveApiBase(`${API}/`, false)).toBe(API)
  })

  it('uses the Vite proxy for a loopback API during dev', () => {
    expect(resolveApiBase('http://127.0.0.1:8000/', true)).toBe('')
    expect(resolveApiBase('http://localhost:8000', true)).toBe('')
  })

  it('keeps a loopback URL outside dev', () => {
    expect(resolveApiBase('http://127.0.0.1:8000', false)).toBe('http://127.0.0.1:8000')
  })

  it('uses the proxy for every loopback host, and refuses a missing production URL', () => {
    expect(resolveApiBase('http://[::1]:8000', true)).toBe('')
    expect(resolveApiBase('http://0.0.0.0:8000/', true)).toBe('')
    expect(resolveApiBase('', true)).toBe('')
    expect(resolveApiBase('not a url', true)).toBe('not a url')
    expect(() => resolveApiBase('  ', false)).toThrow(/VITE_API_BASE_URL/)
  })
})

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
    const profile = await signIn(' 9876543210 ', '2468')
    expect(profile.login_id).toBe('SB-1234ABCD')
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBe('tok-1')
    expect(getSnapshot().uid).toBe('SB-1234ABCD')
    expect(calls[0].url).toBe(`${API}/api/v1/sessions`)
    expect(JSON.parse(String(calls[0].init.body))).toEqual({ identifier: '9876543210', pin: '2468' })
  })

  it('a wrong PIN is an error, not a session', async () => {
    scriptServer({ status: 401, body: { detail: 'Mobile number or PIN is incorrect.' } })
    const error = await signIn('9876543210', '0000').catch((err: unknown) => err)
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
  it('restores a session once the server finishes starting (502 then 200)', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    const calls = scriptServer({ status: 502 }, { status: 200, body: profileBody })
    vi.useFakeTimers()
    const pending = restoreSession()
    await vi.runAllTimersAsync()
    await pending
    vi.useRealTimers()
    expect(calls).toHaveLength(2)
    expect(getSnapshot()).toMatchObject({ ready: true, uid: 'SB-1234ABCD', restoreFailed: false })
  })

  it('does not retry sign-in, so a PIN attempt is never sent twice', async () => {
    const calls = scriptServer({ status: 503 }, { status: 200, body: { ...profileBody, access_token: 'tok-1' } })
    await expect(signIn('SB-1234ABCD', '2468')).rejects.toMatchObject({ status: 503 })
    expect(calls).toHaveLength(1)
  })

  it('restores a valid session from the server', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    scriptServer({ status: 200, body: profileBody })
    await restoreSession()
    expect(getSnapshot()).toMatchObject({ ready: true, uid: 'SB-1234ABCD', restoreFailed: false })
  })

  it('keeps the token and reports failure when offline instead of looking signed out', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    // The profile load is a GET, so it is retried before giving up.
    const calls = scriptServer('network-error', 'network-error', 'network-error')
    vi.useFakeTimers()
    const pending = restoreSession()
    await vi.runAllTimersAsync()
    await pending
    vi.useRealTimers()
    expect(calls).toHaveLength(3)
    expect(getSnapshot()).toMatchObject({ ready: true, uid: null, restoreFailed: true })
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBe('tok-1')
  })

  it('treats a server error like being offline', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    const unavailable = { status: 503, body: { detail: 'unavailable' } }
    scriptServer(unavailable, unavailable, unavailable)
    vi.useFakeTimers()
    const pending = restoreSession()
    await vi.runAllTimersAsync()
    await pending
    vi.useRealTimers()
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

  it('sends the booth code without a profile, and reads this person’s visits', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    const visit = {
      id: 'visit-1',
      place_id: 'place-1',
      place_name: 'Pune stall',
      checked_in_at: '2026-11-03T10:13:00Z',
      attendance_date: '2026-11-03',
      source: 'code',
      already_recorded: false,
      login_id: 'SB-1234ABCD',
      full_name: 'Anita Desai',
      mobile: '9876543210',
      email: null,
      gender: null,
      occupation: null,
      address_line: null,
      locality: null,
      district: 'Nashik',
      state: 'Maharashtra',
      pincode: null,
    }
    const calls = scriptServer({ status: 200, body: visit }, { status: 200, body: { items: [visit], total: 1 } })
    const recorded = await checkIn('004218', 'code')
    const history = await myAttendance()
    expect(recorded.place_name).toBe('Pune stall')
    expect(history.total).toBe(1)
    expect(calls[0].url).toBe(`${API}/api/v1/attendance`)
    expect(calls[0].init.method).toBe('POST')
    expect(JSON.parse(String(calls[0].init.body))).toEqual({ code: '004218', source: 'code' })
    expect(JSON.parse(String(calls[0].init.body))).not.toHaveProperty('full_name')
    expect(calls[1].url).toBe(`${API}/api/v1/attendance`)
    expect(calls[1].init.method).toBe('GET')
  })

  it('keeps booth check-in errors and hides unknown ones', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    const fallback = 'Attendance could not be recorded.'
    scriptServer(
      { status: 404, body: { detail: 'That booth was not found.' } },
      { status: 400, body: { detail: 'This booth is not accepting check-ins.' } },
      { status: 422, body: { detail: [{ msg: 'Value error, Enter the 6-digit booth code.' }] } },
      { status: 429, body: { detail: 'Too many attempts. Try again later.' } },
      { status: 500, body: { detail: 'database exploded' } },
    )
    const missing = await checkIn('000000', 'qr').catch((err: unknown) => err)
    const inactive = await checkIn('000000', 'qr').catch((err: unknown) => err)
    const invalid = await checkIn('000000', 'qr').catch((err: unknown) => err)
    const limited = await checkIn('000000', 'qr').catch((err: unknown) => err)
    const unknown = await checkIn('000000', 'qr').catch((err: unknown) => err)
    const english = copyFor('en')
    expect(apiErrorText(missing, english, fallback)).toBe(english.boothNotFound)
    expect(apiErrorText(inactive, english, fallback)).toBe(english.boothInactive)
    expect(apiErrorText(invalid, english, fallback)).toBe(english.boothCodeInvalid)
    expect(apiErrorText(limited, english, fallback)).toBe(english.tooManyAttempts)
    expect(apiErrorText(unknown, english, fallback)).toBe(fallback)
  })

  it('a network failure is reported as such', () => {
    expect(apiErrorText(new ApiError(0, 'x'), copyFor('en'), 'fallback')).toBe(copyFor('en').networkError)
    expect(apiErrorText(new Error('boom'), copyFor('en'), 'fallback')).toBe('fallback')
  })

  it('maps the remaining known server messages', () => {
    const english = copyFor('en')
    const cases: Array<[string, keyof typeof english]> = [
      ['An account already exists for this mobile number.', 'mobileRegistered'],
      ['An account already exists with this email address.', 'emailRegistered'],
      ['The code is incorrect or has expired.', 'otpInvalid'],
      ['Verify this mobile number before registering.', 'verificationExpired'],
    ]
    for (const [message, key] of cases) {
      const error = new ApiError(400, message)
      expect(apiErrorKey(error)).toBe(key)
      expect(apiErrorText(error, english, 'fallback')).toBe(english[key])
    }
    expect(apiErrorKey(new ApiError(429, 'slow down'))).toBe('tooManyAttempts')
  })

  it('builds a profile, defaulting the role and dropping blank strings', () => {
    expect(profileFromPayload({ login_id: 'SB-1', email: '', role: 'owner' })).toMatchObject({
      login_id: 'SB-1',
      email: null,
      role: 'participant',
      status: 'active',
    })
    expect(isStaffRole('admin')).toBe(true)
    expect(isStaffRole('super_admin')).toBe(true)
    expect(isStaffRole('participant')).toBe(false)
    expect(isStaffRole(undefined)).toBe(false)
  })

  it('signs in after a successful registration and saves a profile update', async () => {
    const calls = scriptServer(
      { status: 201, body: { login_id: 'SB-1234ABCD' } },
      { status: 200, body: { ...profileBody, access_token: 'tok-1', token_type: 'bearer' } },
    )
    const created = await registerAndSignIn(registration)
    expect(created.login_id).toBe('SB-1234ABCD')
    expect(calls[1].url).toBe(`${API}/api/v1/sessions`)
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBe('tok-1')

    const updated = scriptServer({ status: 200, body: { ...profileBody, full_name: 'Anita Updated' } })
    const profile = await updateMe({ full_name: 'Anita Updated' })
    expect(profile.full_name).toBe('Anita Updated')
    expect(getSnapshot().profile?.full_name).toBe('Anita Updated')
    expect(updated[0].init.method).toBe('PATCH')
  })

  it('reuses a fresh profile and shares one in-flight load', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    const calls = scriptServer({ status: 200, body: profileBody })
    await getMe({ fresh: true })
    await getMe()
    expect(calls).toHaveLength(1)

    clearSession()
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    let release: (response: Response) => void = () => {}
    const pending: string[] = []
    vi.stubGlobal('fetch', vi.fn((url: string) => {
      pending.push(url)
      return new Promise<Response>((resolve) => {
        release = resolve
      })
    }))
    const first = getMe()
    const second = getMe()
    expect(pending).toHaveLength(1)
    release(new Response(JSON.stringify(profileBody), { status: 200 }))
    const [left, right] = await Promise.all([first, second])
    expect(left.login_id).toBe(right.login_id)
  })

  it('sends phone verification without a session', async () => {
    const calls = scriptServer(
      { status: 202, body: { id: 'challenge-1', expires_in_seconds: 300 } },
      { status: 200, body: { verification_token: 'verify-1', expires_in_seconds: 900 } },
    )
    const sent = await sendPhoneVerification('9876543210')
    const confirmed = await confirmPhoneVerification(sent.id, '123456')
    expect(confirmed.verification_token).toBe('verify-1')
    expect(calls[0].url).toBe(`${API}/api/v1/phone-verifications`)
    expect(calls[0].init.headers).not.toHaveProperty('Authorization')
    expect(JSON.parse(String(calls[0].init.body))).toEqual({ mobile: '9876543210' })
    expect(calls[1].url).toBe(`${API}/api/v1/phone-verifications/challenge-1/confirm`)
    expect(JSON.parse(String(calls[1].init.body))).toEqual({ code: '123456' })
  })

  it('calls the admin user and booth endpoints with the signed-in token', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    const calls = scriptServer(
      { status: 200, body: { items: [], total: 0, limit: 20, offset: 0 } },
      { status: 200, body: { ...profileBody, id: 'user-1', created_at: '2026-11-01T00:00:00Z' } },
      { status: 200, body: { ...profileBody, id: 'user-1', created_at: '2026-11-01T00:00:00Z', role: 'admin' } },
      { status: 200, body: { items: [] } },
      { status: 201, body: { id: 'place-1', name: 'Gate booth', code: '004218', active: true } },
      { status: 200, body: { id: 'place-1', name: 'Gate booth', code: '004218', active: true } },
      { status: 200, body: { id: 'place-1', name: 'Gate booth', code: '004218', active: false } },
      { status: 200, body: { items: [], total: 0, limit: 50, offset: 50 } },
    )
    await listAdminUsers('  Anita  ', 20)
    await getAdminUser('user-1')
    await setAdminUserRole('user-1', 'admin')
    await listPlaces()
    await createPlace('Gate booth')
    await getPlace('place-1')
    await updatePlace('place-1', { active: false })
    await listPlaceAttendance('place-1', 50)

    expect(calls[0].url).toBe(`${API}/api/v1/admin/users?limit=20&offset=20&q=Anita`)
    expect(calls[1].url).toBe(`${API}/api/v1/admin/users/user-1`)
    expect(calls[2].init.method).toBe('POST')
    expect(JSON.parse(String(calls[2].init.body))).toEqual({ role: 'admin' })
    expect(calls[3].url).toBe(`${API}/api/v1/admin/places`)
    expect(JSON.parse(String(calls[4].init.body))).toEqual({ name: 'Gate booth' })
    expect(calls[5].url).toBe(`${API}/api/v1/admin/places/place-1`)
    expect(calls[6].init.method).toBe('PATCH')
    expect(calls[7].url).toBe(`${API}/api/v1/admin/places/place-1/attendance?limit=50&offset=50`)
    for (const call of calls) {
      expect(new Headers(call.init.headers).get('Authorization')).toBe('Bearer tok-1')
    }
  })

  it('does not call the server when logging out with no token', async () => {
    const calls = scriptServer()
    await logout()
    expect(calls).toHaveLength(0)
  })

  it('revokes the token when logout reaches the server', async () => {
    localStorage.setItem(ACCESS_TOKEN_KEY, 'tok-1')
    const calls = scriptServer({ status: 204 })
    await logout()
    expect(localStorage.getItem(ACCESS_TOKEN_KEY)).toBeNull()
    expect(calls[0].init.method).toBe('DELETE')
    expect(calls[0].url).toBe(`${API}/api/v1/sessions/current`)
  })
})
