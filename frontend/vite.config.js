import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// El backend permite CORS desde http://localhost:5173, por eso fijamos ese puerto.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true },
});
