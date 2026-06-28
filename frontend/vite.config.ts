import { defineConfig, type ProxyOptions } from 'vite'
import react from '@vitejs/plugin-react'

function apiProxy(): ProxyOptions {
  return {
    target: 'http://localhost:8080',
    changeOrigin: true,
    bypass: (req) => {
      const acceptHeader = req.headers.accept;
      const acceptsHtml = Array.isArray(acceptHeader)
        ? acceptHeader.some((value) => value.includes('text/html'))
        : acceptHeader?.includes('text/html') ?? false;

      if (req.method === 'GET' && acceptsHtml) {
        return '/index.html';
      }
    },
    configure: (proxy) => {
      proxy.on('proxyReq', (proxyReq) => {
        proxyReq.removeHeader('origin');
      });
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/login': apiProxy(),
      '/register': apiProxy(),
      '/sends': apiProxy(),
      '/send': apiProxy(),
    },
  },
})
