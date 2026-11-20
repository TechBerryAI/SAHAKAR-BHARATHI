import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createTracker, listYatras, publishYatra, scanYatraAttendance } from './api.ts'
import { clearSession } from './session.ts'
import {
  POSITION_POLL_MS,
  documentOpen,
  freshnessLabel,
  osmPositionLink,
  protocolCategoryLabel,
  protocolStatusLabel,
  yatraStatusLabel,
} from './yatra.ts'
import { counterQrFromPayload, counterQrText } from './counterQr.ts'

const API = 'https://api.unit-test.invalid'

beforeEach(() => {
  clearSession()
  localStorage.clear()
  localStorage.setItem('sahakar_access_token', 'tok-yatra')
})

describe('yatra labels', () => {
  it('names lifecycle and protocol states', () => {
    expect(yatraStatusLabel('approval_pending')).toBe('Approval pending')
    expect(yatraStatusLabel('in_progress')).toBe('In progress')
    expect(protocolStatusLabel('under_review')).toBe('Under review')
    expect(protocolStatusLabel('resubmitted')).toBe('Resubmitted')
    expect(protocolCategoryLabel('pre')).toBe('Pre-Yatra')
    expect(protocolCategoryLabel('during')).toBe('During Yatra')
    expect(protocolCategoryLabel('post')).toBe('Post-Yatra')
  })

  it('allows documents only before review locks the item', () => {
    expect(documentOpen('pending')).toBe(true)
    expect(documentOpen('rejected')).toBe(true)
    expect(documentOpen('under_review')).toBe(false)
    expect(documentOpen('approved')).toBe(false)
  })
})

describe('counter QR and tracking', () => {
  it('accepts only a counter placard and polls every 15 seconds', () => {
    const token = 'abcdefghijklmnopqrstuvwxyz012345'
    expect(counterQrFromPayload(counterQrText(token))).toBe(token)
    expect(counterQrFromPayload('SB-PLACE:004218')).toBeNull()
    expect(POSITION_POLL_MS).toBe(15_000)
    expect(freshnessLabel('live')).toBe('Live')
    expect(freshnessLabel('stale')).toBe('Stale')
    expect(freshnessLabel('offline')).toBe('Offline')
    expect(osmPositionLink(19.9, 73.7)).toBe('https://www.openstreetmap.org/?mlat=19.9&mlon=73.7#map=16/19.9/73.7')
  })
})

describe('yatra API calls', () => {
  it('lists Yatras with the search and status the server will scope', async () => {
    const calls: { url: string; init: RequestInit }[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init: RequestInit = {}) => {
        calls.push({ url, init })
        return new Response(JSON.stringify({ items: [], total: 0, limit: 20, offset: 0 }), { status: 200 })
      }),
    )
    await listYatras('Nashik', 'upcoming')
    expect(calls[0].url).toBe(`${API}/api/v1/yatras?limit=20&offset=0&q=Nashik&status=upcoming`)
    expect(calls[0].init.headers).toMatchObject({ Authorization: 'Bearer tok-yatra' })
  })

  it('publishes with POST and does not send a status field', async () => {
    const calls: { url: string; init: RequestInit }[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init: RequestInit = {}) => {
        calls.push({ url, init })
        return new Response(JSON.stringify({ id: 'yatra-1', published: true }), { status: 200 })
      }),
    )
    await publishYatra('yatra-1')
    expect(calls[0].url).toBe(`${API}/api/v1/yatras/yatra-1/publish`)
    expect(calls[0].init.method).toBe('POST')
    expect(calls[0].init.body).toBeUndefined()
  })

  it('posts a counter scan and does not use the booth attendance path', async () => {
    const calls: { url: string; init: RequestInit }[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init: RequestInit = {}) => {
        calls.push({ url, init })
        return new Response(JSON.stringify({ id: 'attendance-1', already_recorded: false }), { status: 200 })
      }),
    )
    await scanYatraAttendance('yatra-1', 'SB-COUNTER:abcdefghijklmnopqrstuvwxyz012345')
    expect(calls[0].url).toBe(`${API}/api/v1/yatras/yatra-1/attendance`)
    expect(calls[0].url).not.toContain('/api/v1/attendance')
    expect(calls[0].init.method).toBe('POST')
    expect(calls[0].init.body).toBe(JSON.stringify({ qr_token: 'SB-COUNTER:abcdefghijklmnopqrstuvwxyz012345' }))
  })

  it('shows a tracker code once and does not store it in the browser', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        return new Response(
          JSON.stringify({
            id: 'tracker-1',
            label: 'Lead van',
            status: 'active',
            assigned_at: '2026-11-20T05:00:00Z',
            revoked_at: null,
            tracker_token: 'tracker-secret',
          }),
          { status: 201 },
        )
      }),
    )
    const created = await createTracker('yatra-1', 'Lead van')
    expect(created.tracker_token).toBe('tracker-secret')
    expect(localStorage.getItem('sahakar_access_token')).toBe('tok-yatra')
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index) ?? ''
      expect(`${key}=${localStorage.getItem(key)}`).not.toContain('tracker-secret')
    }
  })
})
