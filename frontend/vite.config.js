import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// La SPA se sirve desde el backend Spring Boot en el mismo origen.
// En desarrollo, el proxy reenvía /api, /storage y /diagramas al backend local.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/storage': 'http://localhost:8080',
      '/diagramas': 'http://localhost:8080',
      '/logout': 'http://localhost:8080',
      '/login': 'http://localhost:8080',
    },
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
  },
});
