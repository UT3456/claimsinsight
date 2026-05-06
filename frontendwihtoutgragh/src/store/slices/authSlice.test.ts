import { describe, it, expect, beforeEach } from 'vitest';
import { configureStore } from '@reduxjs/toolkit';
import authReducer, {
  setCredentials,
  clearCredentials,
  selectIsAuthenticated,
  selectCurrentUser,
  selectRole,
  selectToken,
  selectUserId,
  selectIsAdmin,
} from './authSlice';
import type { AuthResponse } from '../../types/auth.types';

// ── Helper: build an unsigned JWT with a given userId ─────────────────────────
function buildJwt(userId: number, role = 'ROLE_ADMIN'): string {
  const exp = Math.floor(Date.now() / 1000) + 3600;
  const b64 = (o: object) =>
    btoa(JSON.stringify(o)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  return `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64({ sub: 'testuser', role, userId, iat: 0, exp })}.sig`;
}

// ── Helper: create a fresh isolated store per test ────────────────────────────
function makeStore() {
  return configureStore({ reducer: { auth: authReducer } });
}

// Clear localStorage before every test so the persisted state doesn't bleed
beforeEach(() => {
  localStorage.clear();
});

// ── Initial state ─────────────────────────────────────────────────────────────
describe('initial state', () => {
  it('starts unauthenticated when localStorage is empty', () => {
    const store = makeStore();
    const state = store.getState().auth;
    expect(state.isAuthenticated).toBe(false);
    expect(state.token).toBeNull();
    expect(state.username).toBeNull();
    expect(state.role).toBeNull();
    expect(state.userId).toBeNull();
  });
});

// ── setCredentials ────────────────────────────────────────────────────────────
describe('setCredentials', () => {
  const makePayload = (userId = 42, role = 'ROLE_ADMIN'): AuthResponse => ({
    accessToken: buildJwt(userId, role),
    tokenType: 'Bearer',
    expiresIn: 3600,
    username: 'alice',
    role,
  });

  it('sets isAuthenticated to true', () => {
    const store = makeStore();
    store.dispatch(setCredentials(makePayload()));
    expect(store.getState().auth.isAuthenticated).toBe(true);
  });

  it('stores username and role in state', () => {
    const store = makeStore();
    store.dispatch(setCredentials(makePayload(42, 'ROLE_CLAIMS_ANALYST')));
    const state = store.getState().auth;
    expect(state.username).toBe('alice');
    expect(state.role).toBe('ROLE_CLAIMS_ANALYST');
  });

  it('extracts userId from the JWT payload', () => {
    const store = makeStore();
    store.dispatch(setCredentials(makePayload(99)));
    expect(store.getState().auth.userId).toBe(99);
  });

  it('stores the raw token', () => {
    const store = makeStore();
    const payload = makePayload();
    store.dispatch(setCredentials(payload));
    expect(store.getState().auth.token).toBe(payload.accessToken);
  });

  it('persists token and user to localStorage', () => {
    const store = makeStore();
    const payload = makePayload();
    store.dispatch(setCredentials(payload));
    expect(localStorage.getItem('ci360_token')).toBe(payload.accessToken);
    expect(localStorage.getItem('ci360_user')).not.toBeNull();
    const stored = JSON.parse(localStorage.getItem('ci360_user')!);
    expect(stored.username).toBe('alice');
    expect(stored.role).toBe('ROLE_ADMIN');
  });

  it('persists refreshToken to localStorage when provided in the response', () => {
    const store = makeStore();
    const payload: AuthResponse = {
      ...makePayload(),
      refreshToken: 'ref-token-uuid-abc',
    };
    store.dispatch(setCredentials(payload));
    expect(localStorage.getItem('ci360_refresh')).toBe('ref-token-uuid-abc');
  });

  it('omits refreshToken when the backend response does not include one', () => {
    const store = makeStore();
    store.dispatch(setCredentials(makePayload()));
    // Backend may not yet emit refreshToken — we should silently skip the write
    expect(localStorage.getItem('ci360_refresh')).toBeNull();
  });
});

// ── clearCredentials ──────────────────────────────────────────────────────────
describe('clearCredentials', () => {
  it('resets all fields to null and isAuthenticated to false', () => {
    const store = makeStore();
    store.dispatch(setCredentials({
      accessToken: buildJwt(1),
      tokenType: 'Bearer',
      expiresIn: 3600,
      username: 'bob',
      role: 'ROLE_ADMIN',
    }));
    store.dispatch(clearCredentials());
    const state = store.getState().auth;
    expect(state.isAuthenticated).toBe(false);
    expect(state.token).toBeNull();
    expect(state.username).toBeNull();
    expect(state.role).toBeNull();
    expect(state.userId).toBeNull();
  });

  it('removes both keys from localStorage', () => {
    const store = makeStore();
    store.dispatch(setCredentials({
      accessToken: buildJwt(1),
      tokenType: 'Bearer',
      expiresIn: 3600,
      username: 'bob',
      role: 'ROLE_ADMIN',
    }));
    store.dispatch(clearCredentials());
    expect(localStorage.getItem('ci360_token')).toBeNull();
    expect(localStorage.getItem('ci360_user')).toBeNull();
  });

  it('also wipes the refreshToken on logout (security-critical — must not leak)', () => {
    const store = makeStore();
    store.dispatch(setCredentials({
      accessToken: buildJwt(1),
      refreshToken: 'ref-token-to-be-killed',
      tokenType: 'Bearer',
      expiresIn: 3600,
      username: 'bob',
      role: 'ROLE_ADMIN',
    }));
    expect(localStorage.getItem('ci360_refresh')).toBe('ref-token-to-be-killed');

    store.dispatch(clearCredentials());
    expect(localStorage.getItem('ci360_refresh')).toBeNull();
  });
});

// ── Selectors ─────────────────────────────────────────────────────────────────
describe('selectors', () => {
  it('selectIsAuthenticated reflects current auth state', () => {
    const store = makeStore();
    expect(selectIsAuthenticated(store.getState())).toBe(false);
    store.dispatch(setCredentials({
      accessToken: buildJwt(5),
      tokenType: 'Bearer',
      expiresIn: 3600,
      username: 'carol',
      role: 'ROLE_ACTUARY',
    }));
    expect(selectIsAuthenticated(store.getState())).toBe(true);
  });

  it('selectCurrentUser returns the username', () => {
    const store = makeStore();
    store.dispatch(setCredentials({
      accessToken: buildJwt(5),
      tokenType: 'Bearer',
      expiresIn: 3600,
      username: 'carol',
      role: 'ROLE_ACTUARY',
    }));
    expect(selectCurrentUser(store.getState())).toBe('carol');
  });

  it('selectRole returns the role string', () => {
    const store = makeStore();
    store.dispatch(setCredentials({
      accessToken: buildJwt(5),
      tokenType: 'Bearer',
      expiresIn: 3600,
      username: 'carol',
      role: 'ROLE_FRAUD_ANALYST',
    }));
    expect(selectRole(store.getState())).toBe('ROLE_FRAUD_ANALYST');
  });

  it('selectIsAdmin returns true only for ROLE_ADMIN', () => {
    const store = makeStore();
    store.dispatch(setCredentials({
      accessToken: buildJwt(1, 'ROLE_ACTUARY'),
      tokenType: 'Bearer',
      expiresIn: 3600,
      username: 'dave',
      role: 'ROLE_ACTUARY',
    }));
    expect(selectIsAdmin(store.getState())).toBe(false);

    store.dispatch(setCredentials({
      accessToken: buildJwt(1, 'ROLE_ADMIN'),
      tokenType: 'Bearer',
      expiresIn: 3600,
      username: 'superadmin',
      role: 'ROLE_ADMIN',
    }));
    expect(selectIsAdmin(store.getState())).toBe(true);
  });

  it('selectToken returns the raw access token', () => {
    const store = makeStore();
    const token = buildJwt(10);
    store.dispatch(setCredentials({
      accessToken: token,
      tokenType: 'Bearer',
      expiresIn: 3600,
      username: 'eve',
      role: 'ROLE_ADMIN',
    }));
    expect(selectToken(store.getState())).toBe(token);
  });

  it('selectUserId returns the userId extracted from JWT', () => {
    const store = makeStore();
    store.dispatch(setCredentials({
      accessToken: buildJwt(77),
      tokenType: 'Bearer',
      expiresIn: 3600,
      username: 'frank',
      role: 'ROLE_ADMIN',
    }));
    expect(selectUserId(store.getState())).toBe(77);
  });
});
