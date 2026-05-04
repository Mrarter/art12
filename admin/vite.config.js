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
      // 系统管理接口 -> admin服务(8090)
      // Banner管理: /api/admin/system/banner/xxx -> /admin/banner/xxx
      '/api/admin/system': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/admin\/system/, '/admin')
      },
      // 艺术家评分/资质审核/价格调控 -> product服务(8082)
      '/api/admin/artist': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/admin/, '/admin')
      },
      '/api/admin/artwork': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/admin/, '/admin')
      },
      // 管理员接口 -> admin服务(8090) (处理 /api/admin/xxx -> /admin/xxx)
      '/api/admin': {
        target: 'http://localhost:8090',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/admin/, '/admin')
      },
      // 用户服务 -> 8081
      '/api/user': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/user/, '/user')
      },
      // C端艺术家评分接口 -> product服务(8082)
      '/api/artist': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      // 商品服务 -> 8082
      '/api/product': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/product/, '/product')
      },
      // 订单服务 -> 8083
      '/api/order': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/order/, '/order')
      },
      // 拍卖服务 -> 8084
      '/api/auction': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/auction/, '/auction')
      },
      // 社区服务 -> 8086
      '/api/community': {
        target: 'http://localhost:8086',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/community/, '/community')
      },
      // 分销服务 -> 8085
      '/api/promotion': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/promotion/, '/promoter')
      },
      // 文件上传接口 -> 8087
      '/api/file': {
        target: 'http://localhost:8087',
        changeOrigin: true
      },
      // 静态文件代理 -> 8087 (用于 /upload/xxx 图片访问)
      '/upload': {
        target: 'http://localhost:8087',
        changeOrigin: true
      },
      // 兼容旧路径 /uploads
      '/uploads': {
        target: 'http://localhost:8087',
        changeOrigin: true
      }
    }
  }
})
