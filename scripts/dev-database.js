// Reads the database the API will actually use (backend/.env) so start.js does not
// insist on a local Docker container when a hosted database is already configured.

import { readFileSync } from 'node:fs'

const LOCAL_HOSTS = new Set(['localhost', '127.0.0.1', '::1', '0.0.0.0'])

export function parseEnv(text) {
  const env = {}
  for (const line of String(text).split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) continue
    const match = trimmed.match(/^(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$/)
    if (!match) continue
    let value = match[2].trim()
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1)
    }
    env[match[1]] = value
  }
  return env
}

export function loadEnvFile(file) {
  try {
    return parseEnv(readFileSync(file, 'utf8'))
  } catch {
    return {}
  }
}

// Host and port only. The userinfo (which holds the password) is never returned.
export function databaseTarget(env) {
  const url = (env.DATABASE_URL || '').trim()
  if (url) {
    const normalized = url
      .replace(/^postgresql\+\w+:\/\//i, 'postgresql://')
      .replace(/^postgres:\/\//i, 'postgresql://')
    const at = normalized.lastIndexOf('@')
    if (at === -1) return { error: 'DATABASE_URL has no host' }
    const authority = normalized.slice(at + 1).split(/[/?]/)[0]
    let host = authority
    let port = 5432
    if (authority.startsWith('[')) {
      const end = authority.indexOf(']')
      if (end === -1) return { error: 'DATABASE_URL has a bad IPv6 host' }
      host = authority.slice(1, end)
      const rest = authority.slice(end + 1)
      if (rest.startsWith(':')) port = Number(rest.slice(1))
    } else if (authority.includes(':')) {
      const split = authority.lastIndexOf(':')
      host = authority.slice(0, split)
      port = Number(authority.slice(split + 1))
    }
    if (!host || !Number.isInteger(port) || port <= 0) return { error: 'DATABASE_URL has no host' }
    return { host, port, remote: !LOCAL_HOSTS.has(host.toLowerCase()), source: 'DATABASE_URL' }
  }

  const host = (env.POSTGRES_HOST || 'localhost').trim() || 'localhost'
  const port = Number(env.POSTGRES_PORT || 5432)
  if (!Number.isInteger(port) || port <= 0) return { error: 'POSTGRES_PORT is not a port number' }
  return { host, port, remote: !LOCAL_HOSTS.has(host.toLowerCase()), source: 'POSTGRES_*' }
}
