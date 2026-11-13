// Runs every automated suite. A failing suite stops the run.
// Backend still needs a PostgreSQL database whose name contains "test".
//
//   node scripts/test-all.js
//   node scripts/test-all.js backend
//   node scripts/test-all.js android --offline

import { spawnSync } from 'node:child_process'
import { existsSync, readdirSync, readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

import { gradleInvocation } from './gradle-command.js'

const root = dirname(dirname(fileURLToPath(import.meta.url)))
const windows = process.platform === 'win32'
const suites = ['backend', 'web', 'android', 'scripts']
const scriptTests = ['scripts/dev-database.test.js', 'scripts/gradle-command.test.js']

const requested = process.argv.slice(2)
const selected = requested.filter((arg) => suites.includes(arg))
const extra = requested.filter((arg) => !suites.includes(arg))
const toRun = selected.length ? selected : suites

if (extra.length && selected.length !== 1) {
  console.error('Pass extra arguments only when running one suite. Example: node scripts/test-all.js backend -- -k attendance')
  process.exit(1)
}

function run(name, command, args, cwd, options = {}) {
  console.log(`\n== ${name} ==`)
  const result = spawnSync(command, args, {
    cwd,
    stdio: 'inherit',
    shell: options.shell ?? false,
    env: options.env ? { ...process.env, ...options.env } : process.env,
  })
  if (result.error) {
    console.error(`\n${name} failed to start: ${result.error.message}`)
    process.exit(1)
  }
  if (result.status !== 0) {
    console.error(`\n${name} failed`)
    process.exit(result.status ?? 1)
  }
}

function isWsl() {
  try {
    return /microsoft/i.test(readFileSync('/proc/version', 'utf8'))
  } catch {
    return false
  }
}

function toWslPath(value) {
  if (!value || value.startsWith('/')) return value
  const mapped = spawnSync('wslpath', ['-u', value], { encoding: 'utf8' })
  if (mapped.status === 0 && mapped.stdout.trim()) return mapped.stdout.trim()
  const match = value.replace(/\\/g, '/').match(/^([A-Za-z]):\/(.*)$/)
  if (match) return `/mnt/${match[1].toLowerCase()}/${match[2]}`
  return value
}

function windowsSdkCandidates() {
  const users = '/mnt/c/Users'
  const dirs = [process.env.ANDROID_HOME, process.env.ANDROID_SDK_ROOT].filter(Boolean)
  if (!existsSync(users)) return dirs
  try {
    for (const name of readdirSync(users)) {
      if (['All Users', 'Default', 'Default User', 'Public', 'desktop.ini'].includes(name)) continue
      dirs.push(join(users, name, 'AppData', 'Local', 'Android', 'Sdk'))
    }
  } catch {
    // Ignore profile directories that cannot be read.
  }
  return dirs
}

function findWindowsSdk() {
  return windowsSdkCandidates()
    .map(toWslPath)
    .find(
      (dir) =>
        existsSync(join(dir, 'platform-tools', 'adb.exe')) &&
        !existsSync(join(dir, 'platform-tools', 'adb')),
    )
}

function javaExe(home) {
  return Boolean(home) && existsSync(join(toWslPath(home), 'bin', 'java.exe'))
}

function findWindowsJava() {
  const homes = [
    process.env.JAVA_HOME,
    '/mnt/c/Program Files/Android/Android Studio/jbr',
    '/mnt/c/Program Files/Android/Android Studio1/jbr',
    '/mnt/c/Program Files (x86)/Android/Android Studio/jbr',
  ]
  const users = '/mnt/c/Users'
  if (existsSync(users)) {
    try {
      for (const name of readdirSync(users)) {
        homes.push(join(users, name, 'AppData', 'Local', 'Programs', 'Android Studio', 'jbr'))
      }
    } catch {
      // Ignore profile directories that cannot be read.
    }
  }
  return homes.find(javaExe)
}

function runBackend() {
  const venvPytest = join(root, 'backend', '.venv', windows ? 'Scripts/pytest.exe' : 'bin/pytest')
  const pytest = existsSync(venvPytest) ? venvPytest : 'pytest'
  run('backend', pytest, extra, join(root, 'backend'), { shell: windows })
}

function runAndroid() {
  const wsl = !windows && isWsl()
  const sdk = wsl ? findWindowsSdk() : process.env.ANDROID_HOME
  const javaHome = wsl && sdk ? findWindowsJava() : process.env.JAVA_HOME
  const command = gradleInvocation({
    platform: process.platform,
    wsl,
    windowsSdk: Boolean(sdk),
    javaHome,
    sdk,
    tasks: [':app:testDebugUnitTest', ...extra],
    wslenv: process.env.WSLENV,
  })
  if (command.error) {
    console.error(`\nandroid failed to start: ${command.error}`)
    process.exit(1)
  }
  run('android', command.command, command.args, join(root, 'android'), command)
}

const runners = {
  backend: runBackend,
  web: () => run('web', 'npm', ['test', ...extra], join(root, 'web'), { shell: windows }),
  android: runAndroid,
  scripts: () => run('scripts', 'node', ['--test', ...scriptTests, ...extra], root),
}

for (const name of toRun) runners[name]()

console.log(toRun.length === suites.length ? '\nAll test suites passed.' : `\n${toRun.join(', ')} passed.`)
