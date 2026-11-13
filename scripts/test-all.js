// Runs every automated suite. A failing suite stops the run.
// Backend still needs a PostgreSQL database whose name contains "test".

import { spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = dirname(dirname(fileURLToPath(import.meta.url)))
const windows = process.platform === 'win32'

function run(name, command, args, cwd) {
  console.log(`\n== ${name} ==`)
  const result = spawnSync(command, args, {
    cwd,
    stdio: 'inherit',
    shell: windows,
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

const venvPytest = join(root, 'backend', '.venv', windows ? 'Scripts/pytest.exe' : 'bin/pytest')
const pytest = existsSync(venvPytest) ? venvPytest : 'pytest'

run('backend', pytest, [], join(root, 'backend'))
run('web', 'npm', ['test'], join(root, 'web'))
run('android', windows ? 'gradlew.bat' : './gradlew', [':app:testDebugUnitTest'], join(root, 'android'))
run('scripts', 'node', ['--test', 'scripts/dev-database.test.js'], root)

console.log('\nAll test suites passed.')
