import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // M2: default to gateway entry; can still override by VITE_API_PROXY_TARGET
  const apiProxyTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8090'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      proxy: {
        '/api': {
          target: apiProxyTarget,
          changeOrigin: true,
          // rewrite: (path) => path.replace(/^\/api/, '')
        }
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('node_modules/zrender')) {
              return 'zrender-vendor'
            }
            if (id.includes('node_modules/echarts')) {
              return 'echarts-vendor'
            }
            if (id.includes('node_modules/vue') || id.includes('node_modules/vue-router')) {
              return 'vue-vendor'
            }
          }
        }
      }
    }
  }
})
