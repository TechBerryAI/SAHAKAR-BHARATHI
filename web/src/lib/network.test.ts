import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  ATTEMPT_TIMEOUT_MS,
  fetchWithColdStart,
  isServerSlow,
  NetworkUnavailableError,
  SLOW_REQUEST_MS,
} from './network.ts'

type Reply = number | 'network-error' | 'hang'

// Stands in for the API host: each attempt takes the next scripted reply.
// 'hang' never answers until the request is aborted, like a host that is still starting.
function scriptServer(...replies: Reply[]) {
  const calls: RequestInit[] = []
  vi.stubGlobal(
    'fetch',
    vi.fn((_url: string, init: RequestInit = {}) => {
      calls.push(init)
      const reply = replies.shift()
      if (reply === undefined) return Promise.reject(new Error('unexpected request'))
      if (reply === 'network-error') return Promise.reject(new TypeError('Failed to fetch'))
      if (reply === 'hang') {
        return new Promise<Response>((_resolve, reject) => {
          init.signal?.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError')))
        })
      }
      return Promise.resolve(new Response('{}', { status: reply }))
    }),
  )
  return calls
}

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
})

describe('fetchWithColdStart', () => {
  it('retries a GET through proxy errors until the server answers', async () => {
    const calls = scriptServer(503, 502, 200)
    const pending = fetchWithColdStart('https://api.test/x')
    await vi.runAllTimersAsync()
    expect((await pending).status).toBe(200)
    expect(calls).toHaveLength(3)
  })

  it('retries a GET that timed out while the host was starting', async () => {
    const calls = scriptServer('hang', 200)
    const pending = fetchWithColdStart('https://api.test/x')
    await vi.advanceTimersByTimeAsync(ATTEMPT_TIMEOUT_MS + 10_000)
    expect((await pending).status).toBe(200)
    expect(calls).toHaveLength(2)
  })

  it('gives up after the retries and reports the network failure', async () => {
    const calls = scriptServer('network-error', 'network-error', 'network-error', 200)
    const pending = fetchWithColdStart('https://api.test/x').catch((err: unknown) => err)
    await vi.runAllTimersAsync()
    expect(await pending).toBeInstanceOf(NetworkUnavailableError)
    expect(calls).toHaveLength(3)
  })

  it('returns the last 503 once retries are exhausted', async () => {
    scriptServer(503, 503, 503)
    const pending = fetchWithColdStart('https://api.test/x')
    await vi.runAllTimersAsync()
    expect((await pending).status).toBe(503)
  })

  it.each(['POST', 'PATCH', 'DELETE'])('sends %s only once, even on 503 or a network error', async (method) => {
    const calls = scriptServer(503)
    const pending = fetchWithColdStart('https://api.test/x', { method })
    await vi.runAllTimersAsync()
    expect((await pending).status).toBe(503)
    expect(calls).toHaveLength(1)

    const failing = scriptServer('network-error', 200)
    const error = fetchWithColdStart('https://api.test/x', { method }).catch((err: unknown) => err)
    await vi.runAllTimersAsync()
    expect(await error).toBeInstanceOf(NetworkUnavailableError)
    expect(failing).toHaveLength(1)
  })

  it('does not retry 401, 404 or 500', async () => {
    for (const status of [401, 404, 500]) {
      const calls = scriptServer(status, 200)
      const pending = fetchWithColdStart('https://api.test/x')
      await vi.runAllTimersAsync()
      expect((await pending).status).toBe(status)
      expect(calls).toHaveLength(1)
    }
  })

  it('aborts a POST that never answers instead of waiting forever', async () => {
    scriptServer('hang')
    const pending = fetchWithColdStart('https://api.test/x', { method: 'POST' }).catch((err: unknown) => err)
    await vi.advanceTimersByTimeAsync(ATTEMPT_TIMEOUT_MS + 1)
    expect(await pending).toBeInstanceOf(NetworkUnavailableError)
  })
})

describe('slow-request flag', () => {
  it('is raised after the threshold while waiting and cleared when the response arrives', async () => {
    let answer: (response: Response) => void = () => {}
    vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>((resolve) => (answer = resolve))))
    const pending = fetchWithColdStart('https://api.test/x', { method: 'POST' })

    await vi.advanceTimersByTimeAsync(SLOW_REQUEST_MS - 1)
    expect(isServerSlow()).toBe(false)
    await vi.advanceTimersByTimeAsync(1)
    expect(isServerSlow()).toBe(true)

    answer(new Response('{}', { status: 200 }))
    await pending
    expect(isServerSlow()).toBe(false)
  })

  it('is never raised for a fast response', async () => {
    scriptServer(200)
    await fetchWithColdStart('https://api.test/x')
    await vi.advanceTimersByTimeAsync(SLOW_REQUEST_MS * 2)
    expect(isServerSlow()).toBe(false)
  })

  it('stays raised during GET retries and clears after a final failure', async () => {
    scriptServer('network-error', 'network-error', 'network-error')
    const pending = fetchWithColdStart('https://api.test/x').catch((err: unknown) => err)
    await vi.advanceTimersByTimeAsync(SLOW_REQUEST_MS)
    expect(isServerSlow()).toBe(true)
    await vi.runAllTimersAsync()
    await pending
    expect(isServerSlow()).toBe(false)
  })
})
