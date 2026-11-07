import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(() => ({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        // 开发环境通过 Vite 代理请求后端，避免浏览器直接跨域。
        target: process.env.VITE_BACKEND_URL || 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
}))
