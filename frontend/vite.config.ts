/**
 * Vite visitor/staff/admin bundler. HTTPS + IPv4 bind so in-page camera works on LAN phones.
 * `/api` is proxied to Spring Boot on 8080 (see specs/TESTING.md).
 */
import os from 'node:os'
import react from '@vitejs/plugin-react'
import basicSsl from '@vitejs/plugin-basic-ssl'
import { defineConfig } from 'vite'

function lanHosts(): string[] {
  const hosts = ['localhost', '127.0.0.1']
  for (const addrs of Object.values(os.networkInterfaces())) {
    for (const addr of addrs ?? []) {
      if (addr.family === 'IPv4' && !addr.internal) {
        hosts.push(addr.address)
      }
    }
  }
  return hosts
}

function viteBase(): string {
  const raw = process.env.VITE_BASE || '/'
  if (raw === '/') return '/'
  return raw.endsWith('/') ? raw : `${raw}/`
}

// HTTPS is required for in-page camera (getUserMedia) on phones over LAN.
// Bind IPv4 explicitly: host:true often listens on :: and Windows phones then fail.
export default defineConfig({
  base: viteBase(),
  plugins: [
    react(),
    basicSsl({
      name: 'sarv-exhibition-portal',
      domains: lanHosts(),
    }),
  ],
  server: {
    host: '0.0.0.0',
    port: 5173,
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    host: '0.0.0.0',
    port: 4173,
    allowedHosts: true,
  },
})
