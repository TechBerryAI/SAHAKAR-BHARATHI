import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listYatras, publishYatra } from './api.ts'
import { clearSession } from './session.ts'
import {
  documentOpen,
  protocolCategoryLabel,
  protocolStatusLabel,
  yatraStatusLabel,
} from './yatra.ts'

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
})
