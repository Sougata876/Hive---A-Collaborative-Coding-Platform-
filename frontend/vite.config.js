import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  define: {
    // sockjs-client is authored against Node globals; without this alias it throws
    // "global is not defined" on load and takes the whole app down with it.
    global: 'globalThis',
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
  build: {
    // Monaco and the collaboration stack are large by nature; splitting them keeps the app chunk
    // readable. Vite 8 runs on Rolldown, which requires manualChunks to be a function.
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('monaco-editor')) return 'monaco'
          if (/node_modules[\\/](yjs|y-monaco|y-protocols|lib0|@stomp|sockjs-client)/.test(id)) {
            return 'collab'
          }
          return undefined
        },
      },
    },
  },
})
