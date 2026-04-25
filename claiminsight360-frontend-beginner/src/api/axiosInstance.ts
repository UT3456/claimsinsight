import axios from 'axios';
import { store }            from '../store';
import { clearCredentials } from '../store/slices/authSlice';

// Use env var if set (production), otherwise fall back to '/api' which
// the Vite dev proxy forwards to http://localhost:8086 — avoids CORS in dev.
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const axiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
});

// ── Request interceptor — attach JWT ────────────────────────────────────────
axiosInstance.interceptors.request.use((config) => {
  const isAuthEndpoint = config.url?.startsWith('/auth');
  if (!isAuthEndpoint) {
    const token = store.getState().auth.token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

// ── Response interceptor — handle 401 / 403 / 5xx ──────────────────────────
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    if (status === 401) {
      store.dispatch(clearCredentials());
      window.location.href = '/login';
    }
    return Promise.reject(error);
  },
);

export default axiosInstance;
