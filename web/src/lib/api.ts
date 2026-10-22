import { clearSession, getToken, setProfile, setSession } from './session.ts'
import type {
  Profile,
  ProfileUpdate,
  PhoneVerificationConfirmed,
  PhoneVerificationCreated,
  RegistrationCreate,
  RegistrationResponse,
  SessionCreate,
  SessionResponse,
} from './types.ts'
import { profileFromSession } from './types.ts'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
    this.name = 'ApiError'
  }
}

type DetailItem = {
  msg?: string
}

function parseDetail(data: unknown): string {
  if (!data || typeof data !== 'object' || !('detail' in data)) {
    return 'Something went wrong. Try again.'
  }
  const detail = (data as { detail: unknown }).detail
  if (typeof detail === 'string' && detail.trim()) return detail
  if (Array.isArray(detail)) {
    const parts = detail
      .map((item) => {
        if (typeof item === 'string') return item
        const msg = (item as DetailItem).msg
        return msg ? msg.replace(/^Value error, /, '') : ''
      })
      .filter(Boolean)
    if (parts.length) return parts.join(' ')
  }
  return 'Something went wrong. Try again.'
}

async function request<T>(path: string, init: RequestInit = {}, authed = false): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const token = getToken()
  if (authed && token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(path, { ...init, headers })
  } catch {
    throw new ApiError(0, 'Cannot reach the API. Is the backend running?')
  }

  if (response.status === 204) {
    return undefined as T
  }

  const data: unknown = await response.json().catch(() => null)
  if (!response.ok) {
    if (response.status === 401 && authed && token) {
      clearSession()
    }
    throw new ApiError(response.status, parseDetail(data))
  }
  return data as T
}

export function sendPhoneVerification(mobile: string): Promise<PhoneVerificationCreated> {
  return request<PhoneVerificationCreated>('/api/v1/phone-verifications', {
    method: 'POST',
    body: JSON.stringify({ mobile }),
  })
}

export function confirmPhoneVerification(
  challengeId: string,
  code: string,
): Promise<PhoneVerificationConfirmed> {
  return request<PhoneVerificationConfirmed>(`/api/v1/phone-verifications/${challengeId}/confirm`, {
    method: 'POST',
    body: JSON.stringify({ code }),
  })
}

export function createRegistration(body: RegistrationCreate): Promise<RegistrationResponse> {
  return request<RegistrationResponse>('/api/v1/registrations', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function createSession(body: SessionCreate): Promise<SessionResponse> {
  return request<SessionResponse>('/api/v1/sessions', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function getMe(): Promise<Profile> {
  return request<Profile>('/api/v1/me', { method: 'GET' }, true)
}

export function updateMe(body: ProfileUpdate): Promise<Profile> {
  return request<Profile>(
    '/api/v1/me',
    {
      method: 'PATCH',
      body: JSON.stringify(body),
    },
    true,
  )
}

export function logoutRemote(): Promise<void> {
  return request<void>('/api/v1/sessions/current', { method: 'DELETE' }, true)
}

export async function signIn(identifier: string, pin: string): Promise<Profile> {
  const session = await createSession({ identifier, pin })
  setSession(session.access_token, session.expires_at, profileFromSession(session))
  try {
    const profile = await getMe()
    setProfile(profile)
    return profile
  } catch {
    return profileFromSession(session)
  }
}

export async function registerAndSignIn(body: RegistrationCreate): Promise<Profile> {
  await createRegistration(body)
  return signIn(body.mobile, body.pin)
}

export async function logout(): Promise<void> {
  try {
    if (getToken()) await logoutRemote()
  } catch {
    // Local sign-out still proceeds if the token is already invalid.
  } finally {
    clearSession()
  }
}
