import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { AuthResponse }          from '../../types/auth.types';
import { getUserIdFromToken }         from '../../utils/tokenUtils';

const TOKEN_KEY   = 'ci360_token';
const REFRESH_KEY = 'ci360_refresh';
const USER_KEY    = 'ci360_user';

interface AuthState {
  token:           string | null;
  username:        string | null;
  role:            string | null;
  userId:          number | null;
  expiresIn:       number | null;
  isAuthenticated: boolean;
}

function loadFromStorage(): AuthState {
  try {
    const token = localStorage.getItem(TOKEN_KEY);
    const raw   = localStorage.getItem(USER_KEY);
    if (token && raw) {
      const user   = JSON.parse(raw) as Omit<AuthState, 'token' | 'isAuthenticated'>;
      const userId = user.userId ?? getUserIdFromToken(token);
      return { token, isAuthenticated: true, ...user, userId };
    }
  } catch { /* ignore */ }
  return { token: null, username: null, role: null, userId: null, expiresIn: null, isAuthenticated: false };
}

const initialState: AuthState = loadFromStorage();

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials(state, action: PayloadAction<AuthResponse>) {
      const { accessToken, refreshToken, username, role, expiresIn } = action.payload;
      const userId = getUserIdFromToken(accessToken);

      state.token           = accessToken;
      state.username        = username;
      state.role            = role;
      state.userId          = userId;
      state.expiresIn       = expiresIn;
      state.isAuthenticated = true;

      localStorage.setItem(TOKEN_KEY, accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify({ username, role, userId, expiresIn }));
      if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken);
    },
    clearCredentials(state) {
      state.token           = null;
      state.username        = null;
      state.role            = null;
      state.userId          = null;
      state.expiresIn       = null;
      state.isAuthenticated = false;

      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(REFRESH_KEY);
      localStorage.removeItem(USER_KEY);
    },
  },
});

export const { setCredentials, clearCredentials } = authSlice.actions;
export default authSlice.reducer;

// ── Selectors ─────────────────────────────────────────────────────────────────
export const selectIsAuthenticated = (s: { auth: AuthState }) => s.auth.isAuthenticated;
export const selectCurrentUser     = (s: { auth: AuthState }) => s.auth.username;
export const selectRole            = (s: { auth: AuthState }) => s.auth.role;
export const selectToken           = (s: { auth: AuthState }) => s.auth.token;
export const selectUserId          = (s: { auth: AuthState }) => s.auth.userId;
export const selectIsAdmin         = (s: { auth: AuthState }) => s.auth.role === 'ROLE_ADMIN';
