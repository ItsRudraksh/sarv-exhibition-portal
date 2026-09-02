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

// HTTPS is required for in-page camera (getUserMedia) on phones over LAN.
// Bind IPv4 explicitly: host:true often listens on :: and Windows phones then fail.
export default defineConfig({
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
