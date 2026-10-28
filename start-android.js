#!/usr/bin/env node
// Builds the Android app and opens it on an emulator or a USB-connected phone:
//
//   node start-android.js            # uses the local API from `node start.js`
//   node start-android.js --prod     # uses the live API instead
//
// 1. Finds the Android SDK and a JDK (JAVA_HOME, or the one bundled with Android Studio).
// 2. Uses a connected device, or starts an emulator (the first one, or --avd <name>).
// 3. Forwards the device's port 8000 to this computer, so the app reaches the local API.
// 4. Builds the debug app, installs it, and opens it.
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
import { existsSync, readdirSync, readFileSync } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = path.dirname(fileURLToPath(import.meta.url))
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
        '  that contains bin\\java.exe), or run node start-android.js from Windows PowerShell.',
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
      '  Enable Windows interop, or run node start-android.js from Windows PowerShell.',
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

  log(`Starting the "${avd}" emulator (the first start can take a minute)…`)
  // Detached, so the emulator keeps running after this script finishes.
  spawn(emulator, ['-avd', avd, '-no-snapshot-save'], { detached: true, stdio: 'ignore' }).unref()
  for (let i = 0; i < 120 && devices.length === 0; i++) {
    await sleep(1000)
    devices = connectedDevices(sdk)
  }
  if (devices.length === 0) fail('The emulator did not appear in `adb devices` within 2 minutes.')
  await waitForBoot(sdk, devices[0], 180)
  log(`Emulator ${devices[0]} is ready.`)
  return devices[0]
}

// --- API ---------------------------------------------------------------------------

async function apiHealthy(url) {
  try {
    const response = await fetch(`${url}/health/live`, { signal: AbortSignal.timeout(3000) })
    return response.ok
  } catch {
    return false
  }
}

async function chooseApi(sdk, serial) {
  if (useProd) {
    warn(`Using the LIVE API (${PROD_API}). Sign in only with a test account.`)
    return `${PROD_API}/`
  }
  if (!(await apiHealthy(LOCAL_API))) {
    warn(
      `The local API is not running at ${LOCAL_API}. The app will open but cannot sign in until you\n` +
        '  run `node start.js` in another terminal (or rerun this with --prod).',
    )
  }
  // The device's own 127.0.0.1:8000 is forwarded to this computer, for emulators and USB phones alike.
  if (adb(sdk, serial, ['reverse', `tcp:${API_PORT}`, `tcp:${API_PORT}`]).status !== 0) {
    fail('Could not forward port 8000 to the device (adb reverse failed).')
  }
  return `${LOCAL_API}/`
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
  if (result.status === 0 && output.includes('Success')) return
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
adb(sdk, serial, ['shell', 'am', 'force-stop', PACKAGE])
if (adb(sdk, serial, ['shell', 'am', 'start', '-n', ACTIVITY]).status !== 0) fail('Could not open the app.')

console.log(`
\x1b[32m  Sahakar Bharati is open on ${serial}\x1b[0m

    API:  ${apiBase}
    Run again after code changes to rebuild and reinstall.
`)

if (flag('--logs')) {
  await sleep(1500)
  streamLogs(sdk, serial)
}
