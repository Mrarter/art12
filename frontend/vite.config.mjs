import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import Uni from '@uni-helper/plugin-uni'

const h5ProxyTarget = process.env.VITE_H5_PROXY_TARGET || 'http://127.0.0.1:8080'
const h5UploadProxyTarget = process.env.VITE_H5_UPLOAD_PROXY_TARGET || process.env.VITE_H5_PROXY_TARGET || 'http://127.0.0.1:8087'

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
        target: h5ProxyTarget,
        changeOrigin: true,
        secure: false
      },
      '/product': {
        target: h5ProxyTarget,
        changeOrigin: true,
        secure: false
      },
      '/upload': {
        target: h5UploadProxyTarget,
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    assetsInclude: ['**/*.svg', '**/*.png', '**/*.jpg', '**/*.jpeg', '**/*.gif', '**/*.webp']
  }
})
