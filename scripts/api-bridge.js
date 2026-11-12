// The debug app calls http://127.0.0.1:8000/ on the phone. `adb reverse` delivers that
// to Windows localhost, which is not the API process inside WSL. When Windows
// localhost:8000 is a dead listener, this opens a localhost proxy aimed at the
// WSL address and points the phone at the proxy.

import { spawn, spawnSync } from 'node:child_process'
import { closeSync, existsSync, openSync, readdirSync, readFileSync, unlinkSync } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const PROXY_SCRIPT = path.join(ROOT, 'scripts', 'win-tcp-proxy.ps1')
// Fixed port. The debug app is built with this address, so it must not move
// between launches or an already installed app would miss the API.
const PROXY_PORT = 18000

export function isWsl() {
  try {
    return /microsoft/i.test(readFileSync('/proc/version', 'utf8'))
  } catch {
    return false
  }
}

export function wslIpv4() {
  if (!isWsl()) return undefined
  const result = spawnSync('hostname', ['-I'], { encoding: 'utf8' })
  return (result.stdout || '')
    .trim()
    .split(/\s+/)
    .find((ip) => /^\d+\.\d+\.\d+\.\d+$/.test(ip))
}

function toWindowsPath(value) {
  if (!value) return value
  if (/^[A-Za-z]:[\\/]/.test(value)) return value
  const mapped = spawnSync('wslpath', ['-w', value], { encoding: 'utf8' })
  if (mapped.status === 0 && mapped.stdout.trim()) return mapped.stdout.trim()
  const match = value.match(/^\/mnt\/([A-Za-z])\/(.*)$/)
  if (match) return `${match[1].toUpperCase()}:\\${match[2].replace(/\//g, '\\')}`
  return value
}

// PowerShell runs on Windows, so this is the Windows localhost adb reverse dials.
// curl.exe started from WSL can be answered by the WSL listener instead.
function windowsHttpOk(port) {
  return windowsRequestOk(`http://127.0.0.1:${port}/health/live`)
}

function hostHttpOk(host, port) {
  return windowsRequestOk(`http://${host}:${port}/health/live`)
}

function windowsRequestOk(url) {
  const result = spawnSync(
    'powershell.exe',
    [
      '-NoProfile',
      '-Command',
      `try { $r = Invoke-WebRequest -Uri '${url}' -UseBasicParsing -TimeoutSec 3; if ($r.StatusCode -eq 200 -and $r.Content -match '"status"') { exit 0 } else { exit 1 } } catch { exit 1 }`,
    ],
    { encoding: 'utf8', timeout: 8000 },
  )
  return result.status === 0
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function waitUntil(check, attempts) {
  for (let i = 0; i < attempts; i++) {
    if (await check()) return true
    await sleep(250)
  }
  return false
}

// Two starters (start.js and start-android.js) can publish at the same moment.
async function withBridgeLock(work) {
  const lock = path.join(os.tmpdir(), 'sahakar-api-bridge.lock')
  const deadline = Date.now() + 30_000
  let fd
  while (Date.now() < deadline) {
    try {
      fd = openSync(lock, 'wx')
      break
    } catch {
      await sleep(200)
    }
  }
  if (fd === undefined) return work()
  try {
    return await work()
  } finally {
    closeSync(fd)
    try {
      unlinkSync(lock)
    } catch {
      // another publish already removed it
    }
  }
}

function listeningPid(port) {
  const result = spawnSync('cmd.exe', ['/c', `netstat -ano | findstr :${port}`], { encoding: 'utf8' })
  const line = (result.stdout || '')
    .split(/\r?\n/)
    .find((entry) => entry.includes('LISTENING') && entry.includes(`:${port}`))
  const pid = line?.trim().split(/\s+/).pop()
  return pid && /^\d+$/.test(pid) ? pid : undefined
}

function commandLine(pid) {
  const result = spawnSync(
    'powershell.exe',
    ['-NoProfile', '-Command', `(Get-CimInstance Win32_Process -Filter "ProcessId=${pid}").CommandLine`],
    { encoding: 'utf8', timeout: 8000 },
  )
  return result.stdout || ''
}

// A previous proxy can hold the port and answer with an empty body. Replace only
// that process; never kill an unrelated program that happened to use the port.
function replaceStaleProxy(port) {
  const pid = listeningPid(port)
  if (!pid) return
  if (!commandLine(pid).includes('win-tcp-proxy.ps1')) return
  spawnSync('cmd.exe', ['/c', `taskkill /PID ${pid} /F`], { encoding: 'utf8' })
}

async function ensureProxy(targetHost, targetPort) {
  if (await windowsHttpOk(PROXY_PORT)) return PROXY_PORT
  replaceStaleProxy(PROXY_PORT)
  const script = toWindowsPath(PROXY_SCRIPT)
  const child = spawn(
    'powershell.exe',
    [
      '-NoProfile',
      '-ExecutionPolicy',
      'Bypass',
      '-File',
      script,
      '-ListenPort',
      String(PROXY_PORT),
      '-TargetHost',
      targetHost,
      '-TargetPort',
      String(targetPort),
    ],
    { detached: true, stdio: 'ignore' },
  )
  child.unref()
  if (await waitUntil(() => windowsHttpOk(PROXY_PORT), 20)) return PROXY_PORT
  return undefined
}

function findAdb() {
  const roots = [process.env.ANDROID_HOME, process.env.ANDROID_SDK_ROOT].filter(Boolean)
  const users = '/mnt/c/Users'
  if (existsSync(users)) {
    try {
      for (const name of readdirSync(users)) {
        roots.push(path.join(users, name, 'AppData', 'Local', 'Android', 'Sdk'))
      }
    } catch {
      // unreadable profile directory
    }
  }
  for (const root of roots) {
    for (const name of ['adb.exe', 'adb']) {
      const candidate = path.join(root, 'platform-tools', name)
      if (existsSync(candidate)) return candidate
    }
  }
  return undefined
}

function connectedSerials(adbBin) {
  const result = spawnSync(adbBin, ['devices'], { encoding: 'utf8' })
  return (result.stdout || '')
    .split(/\r?\n/)
    .slice(1)
    .map((line) => line.trim().split(/\s+/))
    .filter(([serial, state]) => serial && state === 'device')
    .map(([serial]) => serial)
}

function reversePort(adbBin, serial, devicePort, hostPort) {
  const args = serial ? ['-s', serial, 'reverse', `tcp:${devicePort}`, `tcp:${hostPort}`] : ['reverse', `tcp:${devicePort}`, `tcp:${hostPort}`]
  return spawnSync(adbBin, args, { encoding: 'utf8' }).status === 0
}

/**
 * Make http://127.0.0.1:8000/ on a phone reach the API. Returns the Windows
 * port adb reverse should target, or the API port when no bridge was needed.
 */
export async function publishLocalApi({ apiPort = 8000, sdk, serial, log = () => {} } = {}) {
  return withBridgeLock(async () => {
    let hostPort = apiPort
    if (isWsl()) {
      const ip = wslIpv4()
      // Port 8000 on Windows is often held by an editor forward. Some Windows
      // programs still reach the API through it, and adb reverse does not.
      // Always dial the WSL address from a proxy adb can use.
      const apiOnWslIp = ip ? await waitUntil(() => hostHttpOk(ip, apiPort), 12) : false
      if (apiOnWslIp) {
        log(`Bridging the phone to the API at ${ip}:${apiPort}.`)
        const proxyPort = await ensureProxy(ip, apiPort)
        if (!proxyPort) {
          log(
            `Could not open a Windows proxy to ${ip}:${apiPort}. ` +
              `Open http://${ip}:${apiPort}/health/live in the Windows browser instead of 127.0.0.1.`,
          )
        } else {
          hostPort = proxyPort
          log(`Phone port ${apiPort} will forward to Windows localhost:${proxyPort}.`)
        }
      }
    }
    return finishPublish({ apiPort, hostPort, sdk, serial, log })
  })
}

async function finishPublish({ apiPort, hostPort, sdk, serial, log }) {

  const adbBin = sdk
    ? existsSync(path.join(sdk, 'platform-tools', 'adb.exe'))
      ? path.join(sdk, 'platform-tools', 'adb.exe')
      : path.join(sdk, 'platform-tools', 'adb')
    : findAdb()
  if (!adbBin || !existsSync(adbBin)) return { hostPort }

  const serials = serial ? [serial] : connectedSerials(adbBin)
  for (const device of serials) {
    if (!reversePort(adbBin, device, apiPort, hostPort)) {
      log(`Could not forward port ${apiPort} on ${device}.`)
    }
  }
  return { hostPort, adbBin }
}
