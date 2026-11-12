#!/usr/bin/env node
// Builds the Android app and opens it on an emulator or a USB-connected phone:
//
//   node scripts/start-android.js            # uses the local API from `node scripts/start.js`
//   node scripts/start-android.js --prod     # uses the live API instead
//
// 1. Finds the Android SDK and a JDK (JAVA_HOME, or the one bundled with Android Studio).
// 2. Uses a connected device, or starts an emulator (the first one, or --avd <name>).
// 3. Starts the local API if it is not already running. The device reaches it through
//    the Windows bridge. Android 16 blocks the app from 10.0.2.2 until local-network
//    access is granted; this script grants that permission after install.
// 4. Builds the debug app, installs it, and opens it. The script stops if the device
//    still cannot read a health response.
//
// From WSL, a Windows SDK (adb.exe / aapt.exe) is used as-is: adb.exe for the device
// and gradlew.bat via cmd.exe for the build. A Linux SDK still uses ./gradlew.
//
// Options:
//   --prod          Talk to https://sahakar-bharathi-api.onrender.com instead of the local API.
//   --avd <name>    Emulator to start when no device is connected (see: emulator -list-avds).
//   --device <id>   Device serial to use when several are connected (see: adb devices).
//   --logs          After opening the app, stream its log output until Ctrl+C.
//   --reinstall     Uninstall first (clears the app's data on the device). Only needed when an
//                   older build signed with a different debug key is installed.

import { spawn, spawnSync } from 'node:child_process'
import { existsSync, mkdirSync, openSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

import { publishLocalApi } from './api-bridge.js'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const ANDROID = path.join(ROOT, 'android')
const IS_WINDOWS = process.platform === 'win32'
const IS_WSL = !IS_WINDOWS && isWsl()

const PACKAGE = 'com.example.sahakarbharati'
const ACTIVITY = `${PACKAGE}/.MainActivity`
const APK = path.join(ANDROID, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk')
const API_PORT = 8000
const LOCAL_API = `http://127.0.0.1:${API_PORT}`
const PROD_API = 'https://sahakar-bharathi-api.onrender.com'

const args = process.argv.slice(2)
const flag = (name) => args.includes(name)
const option = (name) => {
  const index = args.indexOf(name)
  return index >= 0 ? args[index + 1] : undefined
}
const useProd = flag('--prod')

function log(message) {
  console.log(`\x1b[36m[android]\x1b[0m ${message}`)
}

function warn(message) {
  console.log(`\x1b[33m[android]\x1b[0m ${message}`)
}

function fail(message) {
  console.error(`\x1b[31m[android]\x1b[0m ${message}`)
  process.exit(1)
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

function isWsl() {
  try {
    return /microsoft/i.test(readFileSync('/proc/version', 'utf8'))
  } catch {
    return false
  }
}

function wslpath(flagName, value) {
  const result = spawnSync('wslpath', [flagName, value], { encoding: 'utf8' })
  if (result.status === 0) {
    const mapped = (result.stdout || '').trim()
    if (mapped) return mapped
  }
  return undefined
}

function toWslPath(value) {
  if (!value) return value
  if (value.startsWith('/')) return value
  const mapped = wslpath('-u', value)
  if (mapped) return mapped
  const match = value.replace(/\\/g, '/').match(/^([A-Za-z]):\/(.*)$/)
  if (match) return `/mnt/${match[1].toLowerCase()}/${match[2]}`
  return value
}

function toWindowsPath(value) {
  if (!value) return value
  if (/^[A-Za-z]:[\\/]/.test(value)) return value
  const mapped = wslpath('-w', value)
  if (mapped) return mapped
  const match = value.match(/^\/mnt\/([A-Za-z])\/(.*)$/)
  if (match) return `${match[1].toUpperCase()}:\\${match[2].replace(/\//g, '\\')}`
  return value
}

function hostPath(value) {
  return IS_WSL ? toWslPath(value) : value
}

function sdkLooksValid(dir) {
  if (!dir) return false
  const root = hostPath(dir)
  return existsSync(path.join(root, 'platform-tools', 'adb.exe')) || existsSync(path.join(root, 'platform-tools', 'adb'))
}

function isWindowsSdk(sdk) {
  return existsSync(path.join(sdk, 'platform-tools', 'adb.exe')) && !existsSync(path.join(sdk, 'platform-tools', 'adb'))
}

function toolBin(sdk, folder, name) {
  const win = path.join(sdk, folder, `${name}.exe`)
  if (existsSync(win)) return win
  return path.join(sdk, folder, name)
}

function windowsUserSdkDirs() {
  if (!IS_WSL) return []
  const users = '/mnt/c/Users'
  if (!existsSync(users)) return []
  try {
    return readdirSync(users)
      .filter((name) => !['All Users', 'Default', 'Default User', 'Public', 'desktop.ini'].includes(name))
      .map((name) => path.join(users, name, 'AppData', 'Local', 'Android', 'Sdk'))
  } catch {
    return []
  }
}

// --- Tools -------------------------------------------------------------------------

function sdkFromLocalProperties() {
  for (const file of [path.join(ANDROID, 'local.properties'), path.join(ROOT, 'local.properties')]) {
    if (!existsSync(file)) continue
    const match = readFileSync(file, 'utf8').match(/^sdk\.dir\s*=\s*(.+)$/m)
    // local.properties escapes ':' and '\' (C\:\\Users\\...).
    if (match) return match[1].trim().replace(/\\(.)/g, '$1')
  }
  return undefined
}

function findSdk() {
  const home = os.homedir()
  const candidates = [
    process.env.ANDROID_HOME,
    process.env.ANDROID_SDK_ROOT,
    sdkFromLocalProperties(),
    IS_WINDOWS && process.env.LOCALAPPDATA && path.join(process.env.LOCALAPPDATA, 'Android', 'Sdk'),
    process.platform === 'darwin' && path.join(home, 'Library', 'Android', 'sdk'),
    path.join(home, 'Android', 'Sdk'),
    ...windowsUserSdkDirs(),
  ].filter(Boolean)
  const resolved = candidates.map((dir) => hostPath(dir))
  const sdk = resolved.find(sdkLooksValid)
  if (!sdk) {
    fail(
      'Android SDK not found. Install Android Studio (it installs the SDK), or set ANDROID_HOME\n' +
        `  to the SDK folder. Looked in: ${[...new Set(resolved)].join(', ') || '(nothing configured)'}`,
    )
  }
  return sdk
}

function javaExeName(windowsToolchain) {
  return windowsToolchain ? 'java.exe' : IS_WINDOWS ? 'java.exe' : 'java'
}

function javaWorks(home, windowsToolchain) {
  return Boolean(home) && existsSync(path.join(hostPath(home), 'bin', javaExeName(windowsToolchain)))
}

function windowsStudioJbrHomes() {
  const homes = [
    '/mnt/c/Program Files/Android/Android Studio/jbr',
    '/mnt/c/Program Files/Android/Android Studio1/jbr',
    '/mnt/c/Program Files (x86)/Android/Android Studio/jbr',
  ]
  const users = '/mnt/c/Users'
  if (!existsSync(users)) return homes
  try {
    for (const name of readdirSync(users)) {
      homes.push(path.join(users, name, 'AppData', 'Local', 'Programs', 'Android Studio', 'jbr'))
    }
  } catch {
    // ignore unreadable profile directories
  }
  return homes
}

function findJavaHome(windowsToolchain) {
  if (windowsToolchain && IS_WSL) {
    const fromEnv = process.env.JAVA_HOME && javaWorks(process.env.JAVA_HOME, true) ? process.env.JAVA_HOME : undefined
    const home = [fromEnv, ...windowsStudioJbrHomes()].find((dir) => javaWorks(dir, true))
    if (home) return hostPath(home)
    fail(
      'No Windows JDK (java.exe) found. Android Studio on this machine has an incomplete jbr, or JAVA_HOME\n' +
        '  is a Linux path. Set JAVA_HOME to a Windows JDK 17+ (for example the Android Studio jbr folder\n' +
        '  that contains bin\\java.exe), or run node scripts/start-android.js from Windows PowerShell.',
    )
  }

  const studioJbr = IS_WINDOWS
    ? [
        path.join(process.env.ProgramFiles || 'C:\\Program Files', 'Android', 'Android Studio', 'jbr'),
        path.join(process.env.ProgramFiles || 'C:\\Program Files', 'Android', 'Android Studio1', 'jbr'),
        process.env.LOCALAPPDATA && path.join(process.env.LOCALAPPDATA, 'Programs', 'Android Studio', 'jbr'),
      ]
    : process.platform === 'darwin'
      ? ['/Applications/Android Studio.app/Contents/jbr/Contents/Home']
      : ['/opt/android-studio/jbr', path.join(os.homedir(), 'android-studio', 'jbr')]
  const home = [process.env.JAVA_HOME, ...studioJbr].find((dir) => javaWorks(dir, false))
  if (home) return hostPath(home)
  // Fall back to a `java` on PATH; Gradle finds its own way from there.
  if (spawnSync(IS_WINDOWS ? 'java.exe' : 'java', ['-version'], { stdio: 'ignore' }).status === 0) return undefined
  fail(
    'No Java (JDK 17 or newer) found. Either install a JDK (for example Eclipse Temurin 21)\n' +
      '  and set JAVA_HOME, or repair Android Studio so its bundled JDK (jbr) is complete.',
  )
}

function findCmd() {
  const probe = spawnSync('cmd.exe', ['/c', 'exit 0'])
  if (probe.status === 0) return 'cmd.exe'
  for (const candidate of ['/mnt/c/Windows/System32/cmd.exe', '/mnt/c/Windows/system32/cmd.exe']) {
    if (existsSync(candidate)) return candidate
  }
  fail(
    'cmd.exe is not available from WSL, so the Windows Gradle wrapper cannot run.\n' +
      '  Enable Windows interop, or run node scripts/start-android.js from Windows PowerShell.',
  )
}

// --- Devices -----------------------------------------------------------------------

function adb(sdk, serial, adbArgs, options = {}) {
  const full = serial ? ['-s', serial, ...adbArgs] : adbArgs
  return spawnSync(toolBin(sdk, 'platform-tools', 'adb'), full, { encoding: 'utf8', ...options })
}

function connectedDevices(sdk) {
  const out = adb(sdk, undefined, ['devices']).stdout || ''
  return out
    .split(/\r?\n/)
    .slice(1)
    .map((line) => line.trim().split(/\s+/))
    .filter(([serial, state]) => serial && state === 'device')
    .map(([serial]) => serial)
}

async function waitForBoot(sdk, serial, seconds) {
  for (let i = 0; i < seconds; i++) {
    const booted = adb(sdk, serial, ['shell', 'getprop', 'sys.boot_completed']).stdout?.trim() === '1'
    if (booted) return
    await sleep(1000)
  }
  fail(`The emulator did not finish starting within ${seconds} seconds.`)
}

// The emulator's own message (disk full, bad snapshot, missing hypervisor) is the
// reason it never shows up in `adb devices`. The log is written by launchEmulator.
// Windows Temp is not used: a running emulator keeps that file open, and WSL then
// gets EACCES when the next launch tries to replace it.
const LAUNCH_DIR = path.join(ROOT, '.android-launch')
let emulatorLog = path.join(LAUNCH_DIR, 'emulator.log')

function emulatorFatal(logFile) {
  if (!existsSync(logFile)) return undefined
  const line = readFileSync(logFile, 'utf8')
    .split(/\r?\n/)
    .findLast((entry) => /FATAL\s*\|/.test(entry))
  return line?.replace(/^\s*FATAL\s*\|\s*/, '').trim()
}

function windowsEmulatorRunning() {
  const result = spawnSync(
    'powershell.exe',
    ['-NoProfile', '-Command', '@(Get-Process qemu-system-x86_64,emulator -ErrorAction SilentlyContinue).Count'],
    { encoding: 'utf8' },
  )
  return Number((result.stdout || '').trim()) > 0
}

function writableLaunchFile(name) {
  mkdirSync(LAUNCH_DIR, { recursive: true })
  const file = path.join(LAUNCH_DIR, name)
  try {
    writeFileSync(file, '')
    return file
  } catch (error) {
    if (!['EACCES', 'EPERM', 'EBUSY'].includes(error?.code)) throw error
    const alt = file.replace(/(\.[^.]+)$/, `-${process.pid}$1`)
    writeFileSync(alt, '')
    return alt
  }
}

function launchEmulator(emulator, avd) {
  const args = ['-avd', avd, '-no-snapshot-save', '-no-boot-anim', '-gpu', 'host']
  emulatorLog = writableLaunchFile('emulator.log')
  if ((IS_WSL || IS_WINDOWS) && emulator.endsWith('.exe')) {
    // `start` detaches the Windows process. A direct spawn from WSL is stopped
    // when this script exits, and its console output would otherwise be discarded.
    const bat = writableLaunchFile('emulator.bat')
    const emuWin = toWindowsPath(emulator)
    const logWin = toWindowsPath(emulatorLog)
    writeFileSync(bat, `@echo off\r\n"${emuWin}" ${args.join(' ')} > "${logWin}" 2>&1\r\n`)
    const batWin = toWindowsPath(bat)
    spawnSync(
      'powershell.exe',
      ['-NoProfile', '-Command', `Start-Process -FilePath '${batWin.replace(/'/g, "''")}' -WindowStyle Minimized`],
      { encoding: 'utf8' },
    )
    return
  }
  const fd = openSync(emulatorLog, 'w')
  const child = spawn(emulator, args, { detached: true, stdio: ['ignore', fd, fd] })
  child.unref()
}

async function waitForEmulator(sdk, watchLog = true) {
  const deadline = Date.now() + 180_000
  let restartedAdb = false
  let sawProcess = false
  while (Date.now() < deadline) {
    const devices = connectedDevices(sdk)
    if (devices.length > 0) return devices[0]
    const fatal = watchLog ? emulatorFatal(emulatorLog) : undefined
    const running = IS_WINDOWS || IS_WSL ? windowsEmulatorRunning() : true
    if (running) sawProcess = true
    if (fatal && !running) {
      fail(`The emulator stopped: ${fatal}\n  Log: ${emulatorLog}`)
    }
    // A Windows emulator started from WSL often never appears until adb is restarted.
    if (running && !restartedAdb) {
      adb(sdk, undefined, ['kill-server'])
      adb(sdk, undefined, ['start-server'])
      restartedAdb = true
    }
    await sleep(1000)
  }
  const fatal = watchLog ? emulatorFatal(emulatorLog) : undefined
  if (fatal) fail(`The emulator stopped: ${fatal}\n  Log: ${emulatorLog}`)
  if (!sawProcess) {
    fail(`The emulator did not start. See ${emulatorLog}`)
  }
  fail('The emulator did not appear in `adb devices` within 3 minutes.')
}

// The emulator home screen often freezes on first boot ("Pixel Launcher isn't responding")
// while the app is still building. That dialog is the launcher, not this app. Close it
// and leave our activity in front.
function dismissFrozenDialogs(sdk, serial) {
  for (let attempt = 0; attempt < 3; attempt++) {
    const dumped = adb(
      sdk,
      serial,
      ['shell', 'uiautomator dump /sdcard/window.xml >/dev/null 2>&1; cat /sdcard/window.xml'],
      { timeout: 20000 },
    )
    const xml = `${dumped.stdout || ''}`
    if (!/isn.?t responding/i.test(xml)) return
    const node = xml.split('<node').find((part) => part.includes('Close app'))
    const bounds = node && node.match(/bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/)
    if (!bounds) return
    const x = Math.round((Number(bounds[1]) + Number(bounds[3])) / 2)
    const y = Math.round((Number(bounds[2]) + Number(bounds[4])) / 2)
    log('The emulator home screen stopped responding. Closing that dialog…')
    adb(sdk, serial, ['shell', 'input', 'tap', String(x), String(y)])
  }
}

async function pickDevice(sdk) {
  const wanted = option('--device')
  let devices = connectedDevices(sdk)
  if (wanted) {
    if (!devices.includes(wanted)) fail(`Device "${wanted}" is not connected. Connected: ${devices.join(', ') || 'none'}`)
    return wanted
  }
  if (devices.length > 0) {
    if (devices.length > 1) warn(`Several devices connected; using ${devices[0]} (pick one with --device).`)
    log(`Using connected device ${devices[0]}.`)
    return devices[0]
  }

  const emulator = toolBin(sdk, 'emulator', 'emulator')
  if (!existsSync(emulator)) {
    fail('No device connected and the Android emulator is not installed. Connect a phone with USB debugging on.')
  }
  const avds = (spawnSync(emulator, ['-list-avds'], { encoding: 'utf8' }).stdout || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line && !line.startsWith('INFO'))
  const avd = option('--avd') || avds[0]
  if (!avd) {
    fail('No device connected and no emulator exists. Create one in Android Studio → Device Manager.')
  }
  if (!avds.includes(avd)) fail(`Emulator "${avd}" not found. Available: ${avds.join(', ')}`)

  if ((IS_WINDOWS || IS_WSL) && windowsEmulatorRunning()) {
    log('An emulator is already open. Connecting to it…')
    adb(sdk, undefined, ['kill-server'])
    adb(sdk, undefined, ['start-server'])
    const serial = await waitForEmulator(sdk, false)
    await waitForBoot(sdk, serial, 180)
    await sleep(2000)
    dismissFrozenDialogs(sdk, serial)
    log(`Emulator ${serial} is ready.`)
    return serial
  }

  log(`Starting the "${avd}" emulator (the first start can take a minute)…`)
  // -no-boot-anim avoids the extra launcher work that triggers "isn't responding" on first boot.
  // "auto" sees Intel Vulkan 1.3.215 (needs 1.3.240) and switches to software rendering.
  // That path is missing opengl32sw.dll, so Pixel Launcher freezes. Host GPU uses the
  // installed Intel driver instead.
  launchEmulator(emulator, avd)
  const serial = await waitForEmulator(sdk)
  await waitForBoot(sdk, serial, 180)
  // Boot can report "complete" while Pixel Launcher is still frozen behind a dialog.
  await sleep(2000)
  dismissFrozenDialogs(sdk, serial)
  log(`Emulator ${serial} is ready.`)
  return serial
}

// --- API ---------------------------------------------------------------------------

async function apiHealthy(url) {
  try {
    const response = await fetch(`${url}/health`, { signal: AbortSignal.timeout(10000) })
    return response.ok
  } catch {
    return false
  }
}

function scrubSecrets(text) {
  return text.replace(/postgres(?:ql)?(?:\+\w+)?:\/\/\S+/gi, 'postgresql://***')
}

// start.js stays running after this script exits, so sign-in keeps working.
async function ensureLocalApi() {
  if (await apiHealthy(LOCAL_API)) return
  log('The local API is not running. Starting it…')
  const logFile = path.join(os.tmpdir(), 'sahakar-start.log')
  const output = openSync(logFile, 'a')
  const child = spawn(process.execPath, [path.join(ROOT, 'scripts', 'start.js')], {
    cwd: ROOT,
    detached: true,
    stdio: ['ignore', output, output],
  })
  let exited = null
  child.on('exit', (code) => {
    exited = code ?? 1
  })
  const deadline = Date.now() + 120_000
  while (Date.now() < deadline) {
    if (await apiHealthy(LOCAL_API)) {
      child.unref()
      log(`API is up at ${LOCAL_API}.`)
      return
    }
    if (exited !== null) break
    await sleep(1000)
  }
  child.unref()
  let tail = ''
  try {
    const lines = readFileSync(logFile, 'utf8').trim().split(/\r?\n/)
    tail = scrubSecrets(lines.slice(-25).join('\n'))
  } catch {
    tail = ''
  }
  fail(
    `The local API did not become ready at ${LOCAL_API}.\n` +
      (tail ? `\n${tail}\n` : '') +
      `\n  Full log: ${logFile}`,
  )
}

function deviceReachesApi(sdk, serial, baseUrl) {
  const url = new URL(baseUrl)
  const port = url.port || (url.protocol === 'https:' ? '443' : '80')
  // The request is written on the device. Piping it through adb stdin drops the reply.
  const probe = adb(sdk, serial, [
    'shell',
    `printf "GET /health/live HTTP/1.0\\r\\nHost: ${url.hostname}\\r\\nConnection: close\\r\\n\\r\\n" > /data/local/tmp/sahakar-req.txt; nc -W 5 -w 5 ${url.hostname} ${port} < /data/local/tmp/sahakar-req.txt`,
  ])
  return (probe.stdout || '').includes('"status"')
}

async function chooseApi(sdk, serial) {
  if (useProd) {
    warn(`Using the LIVE API (${PROD_API}). Sign in only with a test account.`)
    return `${PROD_API}/`
  }
  await ensureLocalApi()
  const published = await publishLocalApi({
    apiPort: API_PORT,
    sdk,
    serial,
    log: (message) => log(message),
  })
  const hostPort = published.hostPort || API_PORT
  // 10.0.2.2 is the emulator's name for this Windows machine. adb reverse to
  // Windows port 8000 does not reach the API when that port is already taken,
  // and it does not reach a service that is running inside WSL.
  const emulator = serial.startsWith('emulator-')
  let base = `${LOCAL_API}/`
  if (IS_WSL && emulator) {
    base = `http://10.0.2.2:${hostPort}/`
  } else if (hostPort !== API_PORT) {
    if (adb(sdk, serial, ['reverse', `tcp:${hostPort}`, `tcp:${hostPort}`]).status !== 0) {
      fail(`Could not forward port ${hostPort} to the device (adb reverse failed).`)
    }
    base = `http://127.0.0.1:${hostPort}/`
  } else if (adb(sdk, serial, ['reverse', `tcp:${API_PORT}`, `tcp:${API_PORT}`]).status !== 0) {
    fail('Could not forward port 8000 to the device (adb reverse failed).')
  }
  if (!deviceReachesApi(sdk, serial, base)) {
    fail(
      `The device cannot reach the local API at ${base}health/live.\n` +
        '  The API process is up, but the phone could not read a response.',
    )
  }
  log(`The device can reach the local API at ${base}`)
  return base
}

// --- Build, install, open ------------------------------------------------------------

function gradle(javaHome, sdk, gradleArgs) {
  const windowsToolchain = IS_WINDOWS || (IS_WSL && isWindowsSdk(sdk))

  if (IS_WSL && windowsToolchain) {
    findCmd()
    // WSL's cmd.exe interop translates cwd and WSLENV /p paths. Do not `cd /d`
    // a Windows path (cmd reports invalid volume syntax) or `set "JAVA_HOME=…"`
    // (quotes are stripped and Gradle sees a broken JAVA_HOME).
    const env = { ...process.env, ANDROID_HOME: sdk }
    const wslEnvFlags = ['ANDROID_HOME/p']
    if (javaHome) {
      env.JAVA_HOME = javaHome
      wslEnvFlags.push('JAVA_HOME/p')
    }
    env.WSLENV = [process.env.WSLENV, wslEnvFlags.join(':')].filter(Boolean).join(':')
    log(`Building with Windows Gradle (SDK ${toWindowsPath(sdk)}).`)
    return new Promise((resolve) => {
      const child = spawn('cmd.exe', ['/c', 'gradlew.bat', ...gradleArgs], {
        cwd: ANDROID,
        env,
        stdio: 'inherit',
      })
      child.on('exit', (code) => resolve(code ?? 1))
    })
  }

  const wrapper = path.join(ANDROID, windowsToolchain ? 'gradlew.bat' : 'gradlew')
  return new Promise((resolve) => {
    // gradlew.bat needs a shell on Windows; our arguments contain no spaces.
    const child = IS_WINDOWS
      ? spawn([`"${wrapper}"`, ...gradleArgs].join(' '), { cwd: ANDROID, shell: true, stdio: 'inherit', env: gradleEnv() })
      : spawn(wrapper, gradleArgs, { cwd: ANDROID, stdio: 'inherit', env: gradleEnv() })
    child.on('exit', (code) => resolve(code ?? 1))
  })

  function gradleEnv() {
    return { ...process.env, ANDROID_HOME: sdk, ...(javaHome ? { JAVA_HOME: javaHome } : {}) }
  }
}

function install(sdk, serial) {
  if (flag('--reinstall')) {
    log('Uninstalling the existing app first (--reinstall)…')
    adb(sdk, serial, ['uninstall', PACKAGE])
  }
  log('Installing on the device…')
  const apkForAdb = IS_WSL && isWindowsSdk(sdk) ? toWindowsPath(APK) : APK
  const result = adb(sdk, serial, ['install', '-r', '-t', apkForAdb])
  const output = `${result.stdout || ''}${result.stderr || ''}`
  if (result.status === 0 && output.includes('Success')) {
    // Android 16 blocks the app from the emulator host until this is allowed.
    // Granting here avoids a failed first sign-in while the prompt is still pending.
    const grant = adb(sdk, serial, ['shell', 'pm', 'grant', PACKAGE, 'android.permission.ACCESS_LOCAL_NETWORK'])
    if (grant.status !== 0) {
      warn('Could not pre-grant local network access. Accept the prompt on the device before signing in.')
    }
    return
  }
  if (output.includes('INSTALL_FAILED_UPDATE_INCOMPATIBLE')) {
    fail(
      'The device has an older build signed with a different debug key.\n' +
        '  Rerun with --reinstall to replace it (this clears the app\'s data on that device).',
    )
  }
  fail(`Install failed:\n${output.trim()}`)
}

function streamLogs(sdk, serial) {
  const pid = adb(sdk, serial, ['shell', 'pidof', PACKAGE]).stdout?.trim()
  if (!pid) {
    warn('Could not find the running app process for --logs.')
    return
  }
  log('Streaming app logs. Press Ctrl+C to stop (the app keeps running).')
  const full = ['-s', serial, 'logcat', `--pid=${pid}`]
  spawn(toolBin(sdk, 'platform-tools', 'adb'), full, { stdio: 'inherit' })
}

const sdk = findSdk()
const windowsToolchain = IS_WINDOWS || (IS_WSL && isWindowsSdk(sdk))
if (IS_WSL && windowsToolchain) {
  log(`Using the Windows Android SDK at ${sdk}`)
}
const javaHome = findJavaHome(windowsToolchain)
const serial = await pickDevice(sdk)
const apiBase = await chooseApi(sdk, serial)

log(`Building the debug app (API: ${apiBase})…`)
const code = await gradle(javaHome, sdk, [':app:assembleDebug', `-Psahakar.debugApiBaseUrl=${apiBase}`, '--console=plain'])
if (code !== 0 || !existsSync(APK)) fail('The Android build failed. See the output above.')

install(sdk, serial)
dismissFrozenDialogs(sdk, serial)
adb(sdk, serial, ['shell', 'am', 'force-stop', PACKAGE])
if (adb(sdk, serial, ['shell', 'am', 'start', '-n', ACTIVITY]).status !== 0) fail('Could not open the app.')
await sleep(1500)
dismissFrozenDialogs(sdk, serial)
adb(sdk, serial, ['shell', 'am', 'start', '-n', ACTIVITY])

console.log(`
\x1b[32m  Sahakar Bharati is open on ${serial}\x1b[0m

    API:  ${apiBase}
    Run again after code changes to rebuild and reinstall.
`)

if (flag('--logs')) {
  await sleep(1500)
  streamLogs(sdk, serial)
}
