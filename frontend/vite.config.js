import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const proxyTarget = {
  target: 'http://localhost:8080',
  changeOrigin: true,
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/auth': proxyTarget,
      '/users': proxyTarget,
      '/stocks': proxyTarget,
      '/recommendations': proxyTarget,
      '/holdings': proxyTarget,
      '/api': proxyTarget,
    },
  },
})
