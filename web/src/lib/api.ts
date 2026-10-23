import {
  applyAuthenticated,
  clearSession,
  getAccessToken,
  getSnapshot,
  markReady,
  setAccessToken,
} from './session.ts'
import type { AppCopy } from './copy.ts'
import type {
  PhoneVerificationConfirmed,
  PhoneVerificationCreated,
  Profile,
  ProfileUpdate,
  RegistrationCreate,
  SessionResponse,
} from './types.ts'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
    this.name = 'ApiError'
  }
}

// The API sends English error details; show the known ones in the chosen language.
const SERVER_MESSAGES: Record<string, keyof AppCopy> = {
  'An account with these details already exists.': 'duplicateAccount',
  'Login ID or PIN is incorrect.': 'invalidCredentials',
  'Too many attempts. Try again later.': 'tooManyAttempts',
  'The code is incorrect or has expired.': 'otpInvalid',
  'Verify this mobile number before registering.': 'otpVerifyRequired',
}

export function apiErrorText(err: unknown, copy: AppCopy, fallback: string): string {
  if (!(err instanceof ApiError)) return fallback
  if (err.status === 0) return copy.networkError
  const key = SERVER_MESSAGES[err.message]
  if (key) return copy[key]
  if (err.status === 429) return copy.tooManyAttempts
  return fallback
}

const rawBase = import.meta.env.VITE_API_BASE_URL
if (!rawBase?.trim()) {
  throw new Error(
    'Missing VITE_API_BASE_URL. Copy web/.env.example to web/.env.local for local development.',
  )
}

const API_BASE_URL = rawBase.replace(/\/+$/, '')

function asString(value: unknown): string | null {
  return typeof value === 'string' && value.length ? value : null
}

export function profileFromPayload(data: Record<string, unknown>): Profile {
  return {
    login_id: String(data.login_id ?? ''),
    full_name: String(data.full_name ?? ''),
    mobile: String(data.mobile ?? ''),
    email: asString(data.email),
    gender: asString(data.gender),
    date_of_birth: asString(data.date_of_birth),
    occupation: asString(data.occupation),
    address_line: asString(data.address_line),
    locality: asString(data.locality),
    district: asString(data.district),
    state: asString(data.state),
    pincode: asString(data.pincode),
    status: String(data.status ?? 'active'),
  }
}

function messageFromDetail(payload: unknown, fallback: string): string {
  if (!payload || typeof payload !== 'object' || !('detail' in payload)) {
    return fallback
  }
  const detail = (payload as { detail: unknown }).detail
  if (typeof detail === 'string' && detail.trim()) {
    return detail
  }
  if (Array.isArray(detail) && detail.length > 0) {
    const first = detail[0]
    if (first && typeof first === 'object' && 'msg' in first) {
      const msg = (first as { msg?: unknown }).msg
      if (typeof msg === 'string' && msg.trim()) {
        return msg
      }
    }
  }
  return fallback
}

async function parseJson(response: Response): Promise<unknown> {
  const raw = await response.text()
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE'
  body?: unknown
  auth?: boolean
  fallback: string
}

async function request<T>(path: string, options: RequestOptions): Promise<T> {
  const headers: Record<string, string> = {}
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  const requiresAuth = options.auth !== false
  if (requiresAuth) {
    const token = getAccessToken()
    if (!token) {
      clearSession()
      throw new ApiError(401, 'Authentication required.')
    }
    headers.Authorization = `Bearer ${token}`
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })
  } catch {
    throw new ApiError(0, 'The server could not be reached. Check your connection.')
  }

  if (response.status === 204) {
    return undefined as T
  }

  const payload = await parseJson(response)
  if (!response.ok) {
    if (response.status === 401 && requiresAuth) {
      clearSession()
    }
    throw new ApiError(response.status, messageFromDetail(payload, options.fallback))
  }
  return payload as T
}

export function sendPhoneVerification(mobile: string): Promise<PhoneVerificationCreated> {
  return request<PhoneVerificationCreated>('/api/v1/phone-verifications', {
    method: 'POST',
    body: { mobile },
    auth: false,
    fallback: 'Could not send the verification code.',
  })
}

export function confirmPhoneVerification(
  challengeId: string,
  code: string,
): Promise<PhoneVerificationConfirmed> {
  return request<PhoneVerificationConfirmed>(`/api/v1/phone-verifications/${challengeId}/confirm`, {
    method: 'POST',
    body: { code },
    auth: false,
    fallback: 'Could not verify the code.',
  })
}

// Sign-in and app start already load the profile; screens mounting right after reuse it
// instead of repeating GET /me, which costs several database round-trips on the server.
const PROFILE_FRESH_MS = 30_000
let profileLoadedAt = 0
let profileRequest: Promise<Profile> | null = null

function rememberProfile(payload: Record<string, unknown>): Profile {
  const profile = profileFromPayload(payload)
  profileLoadedAt = Date.now()
  applyAuthenticated(profile)
  return profile
}

export function getMe(options: { fresh?: boolean } = {}): Promise<Profile> {
  if (!options.fresh) {
    const cached = getSnapshot().profile
    if (cached && Date.now() - profileLoadedAt < PROFILE_FRESH_MS) {
      return Promise.resolve(cached)
    }
    if (profileRequest) return profileRequest
  }
  const pending = request<Record<string, unknown>>('/api/v1/me', {
    fallback: 'Could not load profile.',
  }).then(rememberProfile)
  profileRequest = pending
  const settle = () => {
    if (profileRequest === pending) profileRequest = null
  }
  pending.then(settle, settle)
  return pending
}

export async function updateMe(body: ProfileUpdate): Promise<Profile> {
  const payload = await request<Record<string, unknown>>('/api/v1/me', {
    method: 'PATCH',
    body,
    fallback: 'Could not update profile.',
  })
  return rememberProfile(payload)
}

export async function signIn(identifier: string, pin: string): Promise<Profile> {
  const session = await request<SessionResponse>('/api/v1/sessions', {
    method: 'POST',
    body: { identifier: identifier.trim(), pin },
    auth: false,
    fallback: 'Sign in failed.',
  })
  setAccessToken(session.access_token)
  // The session response already carries the full profile, so there is no /me round trip;
  // it also primes the cache so the home screen does not fetch /me either.
  return rememberProfile(session)
}

export async function registerAndSignIn(body: RegistrationCreate): Promise<Profile> {
  await request('/api/v1/registrations', {
    method: 'POST',
    body,
    auth: false,
    fallback: 'Registration failed.',
  })
  return signIn(body.mobile, body.pin)
}

export async function logout(): Promise<void> {
  const token = getAccessToken()
  clearSession()
  if (!token) return
  try {
    await fetch(`${API_BASE_URL}/api/v1/sessions/current`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${token}` },
    })
  } catch {
    // Local session is already cleared.
  }
}

export async function restoreSession(): Promise<void> {
  if (!getAccessToken()) {
    markReady()
    return
  }
  try {
    await getMe()
  } catch {
    if (!getSnapshot().ready) {
      markReady()
    }
  }
}
