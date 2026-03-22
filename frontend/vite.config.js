import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),  // Tailwind v4 uses a Vite plugin, not postcss
  ],
  server: {
    host: '0.0.0.0',
    proxy: {
      // Any request to /api/* is forwarded to Spring Boot
      // This avoids CORS entirely during development
      '/api': 'http://localhost:8080'
    }
  }
})
