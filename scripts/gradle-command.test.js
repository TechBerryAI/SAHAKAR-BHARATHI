import assert from 'node:assert/strict'
import test from 'node:test'

import { gradleInvocation } from './gradle-command.js'

test('Windows uses gradlew.bat', () => {
  const command = gradleInvocation({ platform: 'win32' })
  assert.equal(command.command, 'gradlew.bat')
  assert.deepEqual(command.args, [':app:testDebugUnitTest'])
  assert.equal(command.shell, true)
})

test('Linux uses the shell wrapper', () => {
  const command = gradleInvocation({ platform: 'linux', sdk: '/opt/android-sdk' })
  assert.equal(command.command, './gradlew')
  assert.equal(command.env.ANDROID_HOME, '/opt/android-sdk')
  assert.equal(command.shell, false)
})

test('WSL with a Windows SDK runs gradlew.bat through cmd.exe', () => {
  const command = gradleInvocation({
    platform: 'linux',
    wsl: true,
    windowsSdk: true,
    javaHome: '/mnt/c/Program Files/Android/Android Studio1/jbr',
    sdk: '/mnt/c/Users/Admin/AppData/Local/Android/Sdk',
    wslenv: 'WT_SESSION',
    tasks: [':app:testDebugUnitTest', '--offline'],
  })
  assert.equal(command.command, 'cmd.exe')
  assert.deepEqual(command.args, ['/c', 'gradlew.bat', ':app:testDebugUnitTest', '--offline'])
  assert.equal(command.env.JAVA_HOME, '/mnt/c/Program Files/Android/Android Studio1/jbr')
  assert.equal(command.env.ANDROID_HOME, '/mnt/c/Users/Admin/AppData/Local/Android/Sdk')
  assert.equal(command.env.WSLENV, 'WT_SESSION:ANDROID_HOME/p:JAVA_HOME/p')
})

test('WSL with a Windows SDK and no JDK reports a clear error', () => {
  const command = gradleInvocation({
    platform: 'linux',
    wsl: true,
    windowsSdk: true,
    sdk: '/mnt/c/Users/Admin/AppData/Local/Android/Sdk',
  })
  assert.match(command.error, /java\.exe/)
})
