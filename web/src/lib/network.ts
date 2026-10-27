import { useSyncExternalStore } from 'react'

// The API host suspends the service when idle; the first request afterwards can take
// most of a minute while it starts. Wait long enough for that, retry only requests
// that are safe to repeat, and tell the UI when a request is slow.

export const ATTEMPT_TIMEOUT_MS = 75_000
export const RETRY_BACKOFF_MS = [2_000, 5_000]
export const SLOW_REQUEST_MS = 4_000

const RETRYABLE_STATUS = new Set([502, 503, 504])
const SAFE_METHODS = new Set(['GET', 'HEAD'])

/** Thrown when every attempt failed without an HTTP response (offline, DNS, timeout). */
export class NetworkUnavailableError extends Error {
  constructor() {
    super('The server could not be reached.')
    this.name = 'NetworkUnavailableError'
  }
}

let slowRequests = 0
const listeners = new Set<() => void>()

function setSlow(delta: number) {
  slowRequests += delta
  for (const listener of listeners) listener()
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function isServerSlow(): boolean {
  return slowRequests > 0
}

/** True while any request has been waiting longer than SLOW_REQUEST_MS. */
export function useServerSlow(): boolean {
  return useSyncExternalStore(subscribe, isServerSlow, isServerSlow)
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function attempt(url: string, init: RequestInit): Promise<Response> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), ATTEMPT_TIMEOUT_MS)
  try {
    return await fetch(url, { ...init, signal: controller.signal })
  } finally {
    clearTimeout(timer)
  }
}

/**
 * fetch() that survives an API cold start. GET/HEAD are retried after a network
 * failure, a timeout, or 502/503/504; anything that changes data is sent once.
 */
export async function fetchWithColdStart(url: string, init: RequestInit = {}): Promise<Response> {
  const retryable = SAFE_METHODS.has((init.method ?? 'GET').toUpperCase())
  let marked = false
  const slowTimer = setTimeout(() => {
    marked = true
    setSlow(1)
  }, SLOW_REQUEST_MS)
  try {
    for (let tries = 0; ; tries += 1) {
      const canRetry = retryable && tries < RETRY_BACKOFF_MS.length
      let response: Response
      try {
        response = await attempt(url, init)
      } catch {
        if (!canRetry) throw new NetworkUnavailableError()
        await delay(RETRY_BACKOFF_MS[tries])
        continue
      }
      if (canRetry && RETRYABLE_STATUS.has(response.status)) {
        await delay(RETRY_BACKOFF_MS[tries])
        continue
      }
      return response
    }
  } finally {
    clearTimeout(slowTimer)
    if (marked) setSlow(-1)
  }
}
