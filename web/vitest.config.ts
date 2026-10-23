import { defineConfig } from 'vitest/config'

export default defineConfig({
  define: {
    'import.meta.env.VITE_API_BASE_URL': JSON.stringify('https://api.unit-test.invalid'),
  },
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts', 'vite.config.test.ts'],
    setupFiles: ['src/test/setup.ts'],
    restoreMocks: true,
  },
})
