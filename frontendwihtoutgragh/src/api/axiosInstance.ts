import axios, { AxiosError } from 'axios';
import { store }                        from '../store';
import { clearCredentials, setCredentials } from '../store/slices/authSlice';

// Use env var if set (production), otherwise fall back to '/api' which
// the Vite dev proxy forwards to http://localhost:8086 — avoids CORS in dev.
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

const axiosInstance = axios.create({
  baseURL: BASE_URL,
  // 8 s gives lazy-initialising Spring services enough runway to wake up
  // their beans on first hit, without blocking the UI for 15 s when a
  // service is genuinely down.
  timeout: 8_000,
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

// ── Token refresh state — prevent concurrent refresh storms ────────────────
let isRefreshing = false;
let pendingQueue: Array<{ resolve: (token: string) => void; reject: (err: unknown) => void }> = [];

function flushQueue(token: string | null, err: unknown = null) {
  pendingQueue.forEach(p => token ? p.resolve(token) : p.reject(err));
  pendingQueue = [];
}

// ── Response interceptor — normalize errors, handle auth ───────────────────
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const status = error.response?.status;
    const originalRequest = error.config as any;

    if (status === 401 && !originalRequest?._retry) {
      // Don't attempt refresh on the auth endpoints themselves
      const url = originalRequest?.url ?? '';
      if (url.includes('/auth/login') || url.includes('/auth/refresh')) {
        store.dispatch(clearCredentials());
        redirectToLogin();
        (error as any).userMessage = getApiErrorMessage(error);
        return Promise.reject(error);
      }

      // Try to use the refresh token to get a new access token silently
      const refreshToken = localStorage.getItem('ci360_refresh');
      if (!refreshToken) {
        store.dispatch(clearCredentials());
        redirectToLogin();
        (error as any).userMessage = getApiErrorMessage(error);
        return Promise.reject(error);
      }

      if (isRefreshing) {
        // Another request already triggered a refresh — queue this one
        return new Promise((resolve, reject) => {
          pendingQueue.push({
            resolve: (token) => {
              originalRequest.headers['Authorization'] = `Bearer ${token}`;
              resolve(axiosInstance(originalRequest));
            },
            reject,
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const { data } = await axiosInstance.post('/auth/refresh', { refreshToken });
        store.dispatch(setCredentials(data));
        const newToken = data.accessToken as string;
        axiosInstance.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
        originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
        flushQueue(newToken);
        return axiosInstance(originalRequest);
      } catch (refreshErr) {
        flushQueue(null, refreshErr);
        store.dispatch(clearCredentials());
        redirectToLogin();
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }

    // Attach a normalized message so call sites can do `err.userMessage`
    // without re-implementing the shape lookup every time.
    (error as any).userMessage = getApiErrorMessage(error);
    return Promise.reject(error);
  },
);

function redirectToLogin() {
  const path = window.location.pathname;
  if (!path.startsWith('/login')) {
    try {
      const intended = path + window.location.search + window.location.hash;
      if (intended && intended !== '/') {
        sessionStorage.setItem('ci360.redirectAfterLogin', intended);
      }
    } catch { /* sessionStorage may be unavailable */ }
    window.location.href = '/login';
  }
}

export default axiosInstance;
