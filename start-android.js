import { spawn } from 'node:child_process'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.dirname(fileURLToPath(import.meta.url))
const child = spawn(
  process.execPath,
  [path.join(root, 'scripts', 'start-android.js'), ...process.argv.slice(2)],
  { stdio: 'inherit', cwd: root },
)

child.on('exit', (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal)
    return
  }
  process.exit(code ?? 1)
})
