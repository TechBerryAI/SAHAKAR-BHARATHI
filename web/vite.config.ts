import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv, type Plugin } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

const LOCAL_HOSTS = new Set(['localhost', '127.0.0.1', '0.0.0.0', '[::1]', '10.0.2.2'])

// Returns a problem description, or null when the URL is safe to ship in a production build.
export function productionApiUrlProblem(value: string | undefined): string | null {
  if (!value?.trim()) return 'is not set'
  let url: URL
  try {
    url = new URL(value.trim())
  } catch {
    return 'is not a valid URL'
  }
  const host = url.hostname.toLowerCase()
  if (url.protocol !== 'https:') return 'must use https://'
  if (LOCAL_HOSTS.has(host) || host.startsWith('127.')) return 'points at a local development server'
  if (/(^|\.)(invalid|example|test|localhost)$/.test(host) || host === 'example.com' || host.endsWith('.example.com')) {
    return 'is a placeholder host'
  }
  if (url.search || url.hash || (url.pathname !== '/' && url.pathname !== '')) {
    return 'must be an origin only (no path, query or fragment)'
  }
  return null
}

// The bearer token lives in localStorage, so the page must not run or load
// scripts from anywhere else, and may only send data to itself and the API.
function contentSecurityPolicy(apiOrigin: string): string {
  return [
    "default-src 'self'",
    "script-src 'self'",
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
    "font-src 'self' https://fonts.gstatic.com",
    "img-src 'self' data: blob:",
    `connect-src 'self' ${apiOrigin}`,
    "manifest-src 'self'",
    "worker-src 'self'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
  ].join('; ')
}

function productionGuards(apiBaseUrl: string | undefined): Plugin {
  return {
    name: 'sahakar-production-guards',
    apply: 'build',
    configResolved(config) {
      if (config.mode !== 'production') return
      const problem = productionApiUrlProblem(apiBaseUrl)
      if (problem) {
        throw new Error(
          `VITE_API_BASE_URL ${problem} (value: '${apiBaseUrl ?? ''}'). Production builds must use the ` +
            'HTTPS API origin, e.g. VITE_API_BASE_URL=https://<api-host>. See web/README.md.',
        )
      }
    },
    transformIndexHtml() {
      if (!apiBaseUrl) return []
      return [
        {
          tag: 'meta',
          attrs: {
            'http-equiv': 'Content-Security-Policy',
            content: contentSecurityPolicy(new URL(apiBaseUrl).origin),
          },
          injectTo: 'head-prepend',
        },
      ]
    },
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')
  const apiBaseUrl = process.env.VITE_API_BASE_URL ?? env.VITE_API_BASE_URL

  return {
    plugins: [
      productionGuards(apiBaseUrl),
      react(),
      VitePWA({
        registerType: 'autoUpdate',
        injectRegister: 'auto',
        includeAssets: ['favicon.svg', 'icons/apple-touch-icon.png'],
        manifest: {
          name: 'Sahakar Bharati Rath Yatra',
          short_name: 'Rath Yatra',
          description:
            'Sahakar Bharati Rath Yatra participant app for registration, sign-in, and profile.',
          theme_color: '#E9872A',
          background_color: '#0F3D2E',
          display: 'standalone',
          start_url: '/',
          scope: '/',
          lang: 'en',
          icons: [
            {
              src: 'icons/icon-192.png',
              sizes: '192x192',
              type: 'image/png',
            },
            {
              src: 'icons/icon-512.png',
              sizes: '512x512',
              type: 'image/png',
            },
            {
              src: 'icons/icon-maskable-512.png',
              sizes: '512x512',
              type: 'image/png',
              purpose: 'maskable',
            },
          ],
        },
        workbox: {
          // Only the static app shell is precached. There is deliberately no
          // runtimeCaching: API calls (cross-origin, authenticated) always go to
          // the network and are never stored by the service worker.
          globPatterns: ['**/*.{js,css,html,svg,png,ico,webmanifest}'],
          navigateFallback: 'index.html',
          cleanupOutdatedCaches: true,
        },
      }),
    ],
  }
})
