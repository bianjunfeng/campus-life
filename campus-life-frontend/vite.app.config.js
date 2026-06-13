import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import fs from 'node:fs'
import path from 'node:path'

export function createAppConfig(appName, devPort) {
  return defineConfig(({ mode }) => {
    const projectRoot = fileURLToPath(new URL('.', import.meta.url))
    const env = loadEnv(mode, projectRoot, '')
    const apiProxyTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8090'
    const wsProxyTarget = apiProxyTarget.replace(/^http/i, 'ws')
    const appIndexPath = path.join(projectRoot, 'apps', appName, 'index.html')
    const publicBase = {
      'consumer-web': '/',
      'merchant-web': '/merchant/',
      'admin-web': '/admin/'
    }[appName] || '/'

    return {
      root: projectRoot,
      base: publicBase,
      plugins: [
        vue(),
        {
          name: 'campus-life-app-html-fallback',
          configureServer(server) {
            server.middlewares.use(async (req, res, next) => {
              const url = req.url || '/'
              const acceptsHtml = req.headers.accept?.includes('text/html')
              const isAssetRequest = url.startsWith('/api')
                || url.startsWith('/ws')
                || url.startsWith('/@')
                || url.startsWith('/src/')
                || url.startsWith('/apps/')
                || url.startsWith('/node_modules/')
                || url.includes('.')

              if (!acceptsHtml || isAssetRequest) {
                next()
                return
              }

              try {
                const html = fs.readFileSync(appIndexPath, 'utf-8')
                const transformed = await server.transformIndexHtml(url, html)
                res.statusCode = 200
                res.setHeader('Content-Type', 'text/html')
                res.end(transformed)
              } catch (error) {
                next(error)
              }
            })
          }
        }
      ],
      resolve: {
        alias: {
          '@': fileURLToPath(new URL('./src', import.meta.url)),
          '@shared': fileURLToPath(new URL('./src/shared', import.meta.url))
        }
      },
      server: {
        port: devPort,
        strictPort: true,
        fs: {
          allow: [projectRoot]
        },
        proxy: {
          '/api': {
            target: apiProxyTarget,
            changeOrigin: true
          },
          '/ws': {
            target: wsProxyTarget,
            changeOrigin: true,
            ws: true
          }
        }
      },
      build: {
        outDir: `dist/${appName}/apps/${appName}`,
        emptyOutDir: true,
        chunkSizeWarningLimit: 700,
        rollupOptions: {
          input: appIndexPath,
          output: {
            manualChunks(id) {
              const normalizedId = id.replace(/\\/g, '/')
              if (normalizedId.includes('node_modules/echarts') || normalizedId.includes('node_modules/zrender')) {
                return 'charts-vendor'
              }
              if (normalizedId.includes('node_modules/vue') || normalizedId.includes('node_modules/vue-router')) {
                return 'vue-vendor'
              }
            }
          }
        }
      }
    }
  })
}
