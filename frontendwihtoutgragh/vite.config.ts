import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8086',
        changeOrigin: true,
        // text/event-stream needs the proxy NOT to buffer responses
        // (default behavior already streams chunks, but disabling
        // gzip/compression at the proxy level removes any doubt).
        ws: true,
      },
    },
  },
  build: {
    // Split heavy vendor libraries into separate cached chunks so the browser
    // can load and cache them independently. antd + recharts together are ~2 MB
    // parsed JS — splitting them means a code change to one page doesn't bust
    // the antd chunk that all other pages already have cached.
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return;
          if (id.includes('antd') || id.includes('@ant-design')) return 'vendor-antd';
          if (id.includes('recharts') || id.includes('d3-') || id.includes('d3/')
              || id.includes('victory-vendor')) return 'vendor-charts';
          if (id.includes('react-router') || id.includes('react-dom') || id.includes('/react/'))
            return 'vendor-react';
          if (id.includes('@reduxjs') || id.includes('react-redux') || id.includes('redux'))
            return 'vendor-redux';
          return 'vendor-misc';
        },
      },
    },
    // Don't compute brotli size during dev builds — saves ~0.5 s per build.
    reportCompressedSize: false,
    chunkSizeWarningLimit: 1200,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/test/**', 'src/main.tsx', 'src/vite-env.d.ts'],
    },
  },
})
