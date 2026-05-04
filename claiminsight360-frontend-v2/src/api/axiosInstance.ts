import axios, { AxiosError } from 'axios';
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

/**
 * Pull a human-readable message out of any error shape the platform emits.
 * Backend services use one of two envelopes:
 *   { timestamp, status, error, message }   (most services)
 *   { success, message, data }              (NotificationService)
 * Network failures and unknown shapes fall back gracefully.
 */
export function getApiErrorMessage(err: unknown): string {
  const ax = err as AxiosError<any>;
  if (ax?.response?.data) {
    const d = ax.response.data;
    if (typeof d === 'string') return d;
    if (typeof d.message === 'string' && d.message) return d.message;
    if (typeof d.error === 'string' && d.error) return d.error;
  }
  if (ax?.code === 'ECONNABORTED') return 'Request timed out — the server is taking too long to respond.';
  if (ax?.message === 'Network Error') return 'Cannot reach the server. Check your connection or the gateway.';
  return ax?.message || 'Something went wrong.';
}

// ── Response interceptor — normalize errors, handle auth ───────────────────
axiosInstance.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const status = error.response?.status;

    if (status === 401) {
      store.dispatch(clearCredentials());
      // Avoid redirect loop if we're already on /login
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }

    // Attach a normalized message so call sites can do `err.userMessage`
    // without re-implementing the shape lookup every time.
    (error as any).userMessage = getApiErrorMessage(error);
    return Promise.reject(error);
  },
);

export default axiosInstance;
