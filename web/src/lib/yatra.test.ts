import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listDistricts, listYatras, publishYatra } from './api.ts'
import { clearSession } from './session.ts'
import {
  createActionGate,
  createSearchGate,
  documentOpen,
  isPublicCatalogYatra,
  mergeById,
  protocolCategoryLabel,
  protocolStatusLabel,
  publicStops,
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

  it('requests the next district page instead of a larger first page', async () => {
    const calls: string[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string) => {
        calls.push(url)
        return new Response(JSON.stringify({ items: [], total: 120, limit: 50, offset: 50, has_more: true }), {
          status: 200,
        })
      }),
    )
    await listDistricts(50)
    expect(calls[0]).toBe(`${API}/api/v1/admin/districts?limit=50&offset=50`)
  })
})

describe('phase 2A action and search guards', () => {
  it('lets the first mutation through and ignores a second click until it finishes', () => {
    const gate = createActionGate()
    expect(gate.tryEnter()).toBe(true)
    expect(gate.tryEnter()).toBe(false)
    gate.leave()
    expect(gate.tryEnter()).toBe(true)
  })

  it('keeps only the latest search response', async () => {
    const gate = createSearchGate()
    const first = gate.start()
    const second = gate.start()
    let shown = ''
    const finish = async (id: number, value: string) => {
      await Promise.resolve()
      if (gate.isCurrent(id)) shown = value
    }
    await finish(second, 'B')
    await finish(first, 'A')
    expect(gate.isCurrent(first)).toBe(false)
    expect(shown).toBe('B')
  })

  it('hides archived Yatras and inactive stops from the public catalog', () => {
    expect(isPublicCatalogYatra({ published: true, status: 'upcoming' })).toBe(true)
    expect(isPublicCatalogYatra({ published: true, status: 'archived' })).toBe(false)
    const stops = publicStops([
      { id: 'open', active: true },
      { id: 'closed', active: false },
    ])
    expect(stops.map((stop) => stop.id)).toEqual(['open'])
  })

  it('appends a later district page without duplicating ids', () => {
    const page = mergeById(
      [{ id: 'd1' }, { id: 'd2' }],
      [{ id: 'd2' }, { id: 'd3' }],
    )
    expect(page.map((district) => district.id)).toEqual(['d1', 'd2', 'd3'])
  })
})
