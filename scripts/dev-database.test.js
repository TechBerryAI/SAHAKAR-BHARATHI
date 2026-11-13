import assert from 'node:assert/strict'
import { mkdtempSync, rmSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import test from 'node:test'

import { databaseTarget, loadEnvFile, parseEnv } from './dev-database.js'

test('parseEnv keeps quoted values and skips comments', () => {
  const env = parseEnv(`
# comment
export POSTGRES_HOST=localhost
DATABASE_URL="postgresql://user:secret@db.example.com:5432/app"
EMPTY=
`)
  assert.equal(env.POSTGRES_HOST, 'localhost')
  assert.equal(env.DATABASE_URL, 'postgresql://user:secret@db.example.com:5432/app')
  assert.equal(env.EMPTY, '')
})

test('a hosted DATABASE_URL does not require local Docker', () => {
  const target = databaseTarget({
    DATABASE_URL:
      'postgresql+psycopg://postgres.project:s3cret@aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require',
    POSTGRES_PORT: '5433',
  })
  assert.equal(target.remote, true)
  assert.equal(target.host, 'aws-0-ap-south-1.pooler.supabase.com')
  assert.equal(target.port, 5432)
  assert.equal(target.source, 'DATABASE_URL')
  assert.equal(JSON.stringify(target).includes('s3cret'), false)
})

test('a password that contains an encoded @ still yields the host', () => {
  const target = databaseTarget({
    DATABASE_URL: 'postgresql://user:p%40ss@127.0.0.1:5433/sahakar',
  })
  assert.equal(target.remote, false)
  assert.equal(target.host, '127.0.0.1')
  assert.equal(target.port, 5433)
})

test('without DATABASE_URL, POSTGRES_* is the local default', () => {
  const target = databaseTarget({})
  assert.equal(target.remote, false)
  assert.equal(target.host, 'localhost')
  assert.equal(target.port, 5432)
})

test('loadEnvFile reads a file and ignores a missing one', () => {
  const dir = mkdtempSync(join(tmpdir(), 'sahakar-env-'))
  const file = join(dir, '.env')
  try {
    writeFileSync(file, "POSTGRES_HOST='db.internal'\n# ignored\n")
    assert.equal(loadEnvFile(file).POSTGRES_HOST, 'db.internal')
    assert.deepEqual(loadEnvFile(join(dir, 'missing.env')), {})
  } finally {
    rmSync(dir, { recursive: true, force: true })
  }
})

test('databaseTarget reports a missing host, a bad IPv6 host, and a bad port', () => {
  assert.equal(
    databaseTarget({ DATABASE_URL: 'postgresql://user:secret' }).error,
    'DATABASE_URL has no host',
  )
  assert.equal(
    databaseTarget({ DATABASE_URL: 'postgresql://user:secret@[::1/db' }).error,
    'DATABASE_URL has a bad IPv6 host',
  )
  assert.equal(databaseTarget({ POSTGRES_PORT: 'nope' }).error, 'POSTGRES_PORT is not a port number')
  assert.equal(databaseTarget({ POSTGRES_PORT: '0' }).error, 'POSTGRES_PORT is not a port number')

  const ipv6 = databaseTarget({ DATABASE_URL: 'postgresql://user:secret@[::1]:5432/sahakar' })
  assert.equal(ipv6.host, '::1')
  assert.equal(ipv6.port, 5432)
  assert.equal(ipv6.remote, false)
  assert.equal(ipv6.source, 'DATABASE_URL')
})
