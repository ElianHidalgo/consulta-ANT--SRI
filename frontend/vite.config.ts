import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// En desarrollo, el proxy hace el papel que en producción cumple Nginx.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/v1/licencias': 'http://localhost:8082',
      '/api/v1/contribuyentes': 'http://localhost:8081',
      '/api/v1/vehiculos': 'http://localhost:8081',
    },
  },
});
