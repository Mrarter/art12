import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import Uni from '@uni-helper/plugin-uni'

export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  plugins: [
    Uni()
  ],
  server: {
    // In this environment binding 0.0.0.0 may be blocked; use loopback for local dev.
    port: 5176,
    host: '127.0.0.1',
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/product': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/upload': {
        target: 'http://127.0.0.1:8087',
        changeOrigin: true
      }
    }
  },
  build: {
    assetsInclude: ['**/*.svg', '**/*.png', '**/*.jpg', '**/*.jpeg', '**/*.gif', '**/*.webp']
  }
})
