#!/usr/bin/env node
// Local API and web app. Run `make start` from the repository root.
//
// 1. Uses the database in backend/.env. A hosted DATABASE_URL does not need Docker.
//    A local database must already be listening, or Docker can start sahakar-postgres.
// 2. Applies database migrations (alembic upgrade head).
// 3. Starts the API on port 8000 (dev_server.py when present, so the test OTP is
//    shown on screen; otherwise plain uvicorn). It listens on every interface so
//    Windows and the Android emulator can reach a server that is running in WSL.
// 4. Starts the web app on http://localhost:5174 and prints the URL.
//
// Ctrl+C stops everything this script started.

import { spawn, spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import net from 'node:net'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { publishLocalApi } from './api-bridge.js'
import { databaseTarget, loadEnvFile } from './dev-database.js'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const BACKEND = path.join(ROOT, 'backend')
const WEB = path.join(ROOT, 'web')
const IS_WINDOWS = process.platform === 'win32'
const ENV_FILE = path.join(BACKEND, '.env')

const API_BIND_HOST = '0.0.0.0'
const API_HOST = '127.0.0.1'
const API_PORT = 8000
const WEB_PORT = 5174
const DB_CONTAINER = 'sahakar-postgres'
const DOCKER_EXE = '/mnt/c/Program Files/Docker/Docker/resources/bin/docker.exe'

const API_URL = `http://${API_HOST}:${API_PORT}`
const WEB_URL = `http://localhost:${WEB_PORT}`

const children = []

function log(message) {
  console.log(`\x1b[36m[start]\x1b[0m ${message}`)
}

function fail(message) {
  console.error(`\x1b[31m[start]\x1b[0m ${message}`)
  stopAll(1)
}

function python() {
  const venv = IS_WINDOWS
    ? path.join(BACKEND, '.venv', 'Scripts', 'python.exe')
    : path.join(BACKEND, '.venv', 'bin', 'python')
  if (!existsSync(venv)) {
    fail(
      `Python virtual environment not found at ${venv}.\n` +
        '  Create it first:  cd backend && python -m venv .venv && .venv/bin/pip install -e ".[dev]"',
    )
  }
  return venv
}

// "localhost" may be IPv4 or IPv6 (Vite often binds ::1 on Windows), so check both.
async function portOpen(port) {
  const results = await Promise.all([portOpenOn(port, '127.0.0.1'), portOpenOn(port, '::1')])
  return results.includes(true)
}

function portOpenOn(port, host, timeoutMs = 1000) {
  return new Promise((resolve) => {
    const socket = net.connect({ port, host })
    const finish = (open) => {
      socket.destroy()
      resolve(open)
    }
    socket.setTimeout(timeoutMs)
    socket.once('connect', () => finish(true))
    socket.once('timeout', () => finish(false))
    socket.once('error', () => finish(false))
  })
}

async function apiHealthy() {
  try {
    const response = await fetch(`${API_URL}/health`, { signal: AbortSignal.timeout(10000) })
    return response.ok
  } catch {
    return false
  }
}

async function waitFor(check, what, seconds) {
  const deadline = Date.now() + seconds * 1000
  while (Date.now() < deadline) {
    if (await check()) return
    await new Promise((resolve) => setTimeout(resolve, 500))
  }
  fail(`${what} did not start within ${seconds} seconds. See the output above.`)
}

// npm is a .cmd script on Windows and needs a shell; give the shell one command string
// (Node warns when args are passed alongside shell: true). Our args contain no spaces.
function shellSafe(command, args) {
  return IS_WINDOWS && !command.endsWith('.exe') ? [[command, ...args].join(' '), [], true] : [command, args, false]
}

function run(name, command, args, options = {}) {
  const [cmd, cmdArgs, shell] = shellSafe(command, args)
  const child = spawn(cmd, cmdArgs, {
    stdio: ['ignore', 'pipe', 'pipe'],
    shell,
    ...options,
  })
  const prefix = `\x1b[35m[${name}]\x1b[0m `
  const pipe = (stream, out) => {
    let buffer = ''
    stream.on('data', (chunk) => {
      buffer += chunk.toString()
      const lines = buffer.split(/\r?\n/)
      buffer = lines.pop()
      for (const line of lines) out.write(prefix + line + '\n')
    })
  }
  pipe(child.stdout, process.stdout)
  pipe(child.stderr, process.stderr)
  child.on('exit', (code) => {
    if (!stopping) fail(`${name} stopped unexpectedly (exit code ${code}).`)
  })
  children.push(child)
  return child
}

let stopping = false

function stopAll(code = 0) {
  if (stopping) return
  stopping = true
  for (const child of children) {
    if (child.exitCode !== null) continue
    if (IS_WINDOWS) {
      // npm and vite run as a tree of processes on Windows; kill the whole tree.
      spawnSync('taskkill', ['/pid', String(child.pid), '/T', '/F'], { stdio: 'ignore' })
    } else {
      child.kill('SIGTERM')
    }
  }
  process.exit(code)
}

process.on('SIGINT', () => {
  log('Stopping…')
  stopAll(0)
})
process.on('SIGTERM', () => stopAll(0))

function dockerReady(bin) {
  return spawnSync(bin, ['info'], { encoding: 'utf8', timeout: 20000 }).status === 0
}

function launchDockerDesktop() {
  const exe = '/mnt/c/Program Files/Docker/Docker/Docker Desktop.exe'
  if (!existsSync(exe)) return false
  log('Starting Docker Desktop…')
  const child = spawn(
    'cmd.exe',
    ['/c', 'start', '', 'C:\\Program Files\\Docker\\Docker\\Docker Desktop.exe'],
    { detached: true, stdio: 'ignore' },
  )
  child.unref()
  return true
}

async function findDocker() {
  const bins = ['docker', ...(existsSync(DOCKER_EXE) ? [DOCKER_EXE] : [])]
  for (const bin of bins) {
    if (dockerReady(bin)) return bin
  }
  if (!launchDockerDesktop()) return undefined
  const deadline = Date.now() + 120_000
  while (Date.now() < deadline) {
    for (const bin of bins) {
      if (dockerReady(bin)) return bin
    }
    await new Promise((resolve) => setTimeout(resolve, 2000))
  }
  return undefined
}

function dockerOutput(result) {
  return `${result.stdout || ''}${result.stderr || ''}`.trim()
}

async function ensureLocalPostgres(env, port) {
  if (await portOpen(port)) {
    log(`PostgreSQL is already running on port ${port}.`)
    return
  }
  const docker = await findDocker()
  if (!docker) {
    fail(
      `PostgreSQL is not running on port ${port}, and Docker is not running.\n` +
        '  Start Docker Desktop, or set DATABASE_URL in backend/.env to a database this machine can reach.',
    )
  }
  const started = spawnSync(docker, ['start', DB_CONTAINER], { encoding: 'utf8' })
  if (started.status !== 0) {
    const user = env.POSTGRES_USER || 'sahakar'
    const password = env.POSTGRES_PASSWORD || ''
    const database = env.POSTGRES_DB || 'sahakar'
    log(`Creating the "${DB_CONTAINER}" container on port ${port}…`)
    const args = [
      'run',
      '-d',
      '--name',
      DB_CONTAINER,
      '-p',
      `${port}:5432`,
      '-e',
      `POSTGRES_USER=${user}`,
      '-e',
      `POSTGRES_DB=${database}`,
    ]
    if (password) args.push('-e', `POSTGRES_PASSWORD=${password}`)
    else args.push('-e', 'POSTGRES_HOST_AUTH_METHOD=trust')
    args.push('postgres:16')
    const created = spawnSync(docker, args, { encoding: 'utf8', timeout: 300000 })
    if (created.status !== 0) {
      fail(
        `PostgreSQL is not running on port ${port} and Docker could not create "${DB_CONTAINER}".\n` +
          dockerOutput(created),
      )
    }
  } else {
    log(`Starting the "${DB_CONTAINER}" Docker container…`)
  }
  await waitFor(() => portOpen(port), 'PostgreSQL', 60)
  // The port opens slightly before Postgres accepts logins.
  await new Promise((resolve) => setTimeout(resolve, 2000))
}

async function startDatabase() {
  const env = loadEnvFile(ENV_FILE)
  const target = databaseTarget(env)
  if (target.error) fail(`${target.error} in backend/.env.`)
  if (target.remote) {
    log(`Using the database from backend/.env (${target.host}:${target.port}). Docker is not required.`)
    if (!(await portOpenOn(target.port, target.host, 8000))) {
      fail(
        `Could not connect to ${target.host}:${target.port}.\n` +
          '  Check DATABASE_URL in backend/.env and that this machine can reach that host.',
      )
    }
    return
  }
  log(`Using local PostgreSQL at ${target.host}:${target.port}.`)
  await ensureLocalPostgres(env, target.port)
}

function migrate() {
  log('Applying database migrations…')
  const result = spawnSync(python(), ['-m', 'alembic', 'upgrade', 'head'], {
    cwd: BACKEND,
    stdio: 'inherit',
  })
  if (result.status !== 0) fail('Database migrations failed. See the output above.')
}

async function startApi() {
  if (await apiHealthy()) {
    log(`API is already running at ${API_URL}.`)
    return
  }
  if (await portOpen(API_PORT)) {
    fail(`Port ${API_PORT} is in use by another program. Close it and try again.`)
  }
  const devServer = path.join(BACKEND, 'dev_server.py')
  const args = existsSync(devServer)
    ? ['dev_server.py']
    : ['-m', 'uvicorn', 'app.main:app', '--host', API_BIND_HOST, '--port', String(API_PORT)]
  log(`Starting the API at ${API_URL}…`)
  run('api', python(), args, { cwd: BACKEND, env: { ...process.env, PYTHONUNBUFFERED: '1' } })
  await waitFor(apiHealthy, 'The API', 90)
}

async function startWeb() {
  if (await portOpen(WEB_PORT)) {
    log(`Web app is already running at ${WEB_URL}.`)
    return
  }
  if (!existsSync(path.join(WEB, 'node_modules'))) {
    log('Installing web dependencies (first run)…')
    const [cmd, cmdArgs, shell] = shellSafe('npm', ['install'])
    const install = spawnSync(cmd, cmdArgs, { cwd: WEB, stdio: 'inherit', shell })
    if (install.status !== 0) fail('npm install failed. See the output above.')
  }
  log(`Starting the web app at ${WEB_URL}…`)
  run('web', 'npm', ['run', 'dev', '--', '--port', String(WEB_PORT), '--strictPort'], {
    cwd: WEB,
    env: { ...process.env, VITE_API_BASE_URL: API_URL },
  })
  await waitFor(() => portOpen(WEB_PORT), 'The web app', 60)
}

await startDatabase()
migrate()
await startApi()
await publishLocalApi({ apiPort: API_PORT, log })
await startWeb()

console.log(`
\x1b[32m  Sahakar Bharati is running\x1b[0m

    App:  \x1b[1m${WEB_URL}\x1b[0m
    API:  ${API_URL}

  Android app: run \x1b[1mmake android\x1b[0m in another terminal.
  Press Ctrl+C to stop.
`)
