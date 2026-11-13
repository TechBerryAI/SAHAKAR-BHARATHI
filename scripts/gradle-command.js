// Chooses the Gradle wrapper invocation for Android unit tests.
// WSL with a Windows SDK must call gradlew.bat through cmd.exe. Linux Gradle
// looks for aapt, and the Windows SDK only has aapt.exe.

export function gradleInvocation({
  platform,
  wsl = false,
  windowsSdk = false,
  javaHome,
  sdk,
  tasks,
  wslenv,
}) {
  const args = tasks?.length ? tasks : [':app:testDebugUnitTest']

  if (platform === 'win32') {
    return { command: 'gradlew.bat', args, shell: true, env: {} }
  }

  if (wsl && windowsSdk) {
    if (!javaHome || !sdk) {
      return {
        error:
          'The Windows Android SDK needs a JDK whose bin folder contains java.exe. ' +
          'Set JAVA_HOME to the Android Studio jbr folder, or run the tests from Windows.',
      }
    }
    const flags = ['ANDROID_HOME/p', 'JAVA_HOME/p']
    return {
      command: 'cmd.exe',
      args: ['/c', 'gradlew.bat', ...args],
      shell: false,
      env: {
        ANDROID_HOME: sdk,
        JAVA_HOME: javaHome,
        WSLENV: [wslenv, flags.join(':')].filter(Boolean).join(':'),
      },
    }
  }

  return {
    command: './gradlew',
    args,
    shell: false,
    env: sdk ? { ANDROID_HOME: sdk } : {},
  }
}
