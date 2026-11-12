import assert from 'node:assert/strict'
import test from 'node:test'

import { databaseTarget, parseEnv } from './dev-database.js'

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
