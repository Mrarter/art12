import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia']
    }),
    Components({
      resolvers: [ElementPlusResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5174,
    proxy: {
      // 管理员接口 -> admin服务(8090)
      '/api/admin': {
        target: 'http://localhost:8090',
        changeOrigin: true
      },
      // 小程序API接口 -> 网关(8080)
      '/api/user': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/product': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/order': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/auction': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/community': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/promotion': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/file': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
