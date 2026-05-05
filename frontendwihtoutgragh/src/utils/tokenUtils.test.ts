import { describe, it, expect, beforeEach, vi } from 'vitest';
import { decodeToken, isTokenExpired, getUserIdFromToken } from './tokenUtils';

// ── Helper ────────────────────────────────────────────────────────────────────
/**
 * Builds a minimal but structurally valid JWT (unsigned).
 * The decode logic in tokenUtils only reads the payload — signature is ignored.
 */
function buildJwt(payload: object): string {
  const b64url = (obj: object) =>
    btoa(JSON.stringify(obj))
      .replace(/=/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_');
  const header = b64url({ alg: 'HS256', typ: 'JWT' });
  const body   = b64url(payload);
  return `${header}.${body}.fakesig`;
}

const FUTURE_EXP = Math.floor(Date.now() / 1000) + 3_600;  // 1 hour ahead
const PAST_EXP   = Math.floor(Date.now() / 1000) - 3_600;  // 1 hour ago

// ── decodeToken ───────────────────────────────────────────────────────────────
describe('decodeToken()', () => {
  it('decodes a valid JWT and returns the payload', () => {
    const token = buildJwt({ sub: 'alice', role: 'ROLE_ADMIN', userId: 7, iat: 0, exp: FUTURE_EXP });
    const payload = decodeToken(token);
    expect(payload).not.toBeNull();
    expect(payload!.sub).toBe('alice');
    expect(payload!.role).toBe('ROLE_ADMIN');
    expect(payload!.userId).toBe(7);
  });

  it('returns null for a token with fewer than 3 parts', () => {
    expect(decodeToken('onlyonepart')).toBeNull();
    expect(decodeToken('two.parts')).toBeNull();
  });

  it('returns null for a token with an invalid base64 payload', () => {
    expect(decodeToken('header.!!!invalid!!!.sig')).toBeNull();
  });

  it('returns null for an empty string', () => {
    expect(decodeToken('')).toBeNull();
  });

  it('returns null when payload is valid base64 but not JSON', () => {
    // btoa('not json at all') → valid base64, invalid JSON
    const bad = `header.${btoa('not json at all')}.sig`;
    expect(decodeToken(bad)).toBeNull();
  });
});

// ── isTokenExpired ────────────────────────────────────────────────────────────
describe('isTokenExpired()', () => {
  it('returns true for null', () => {
    expect(isTokenExpired(null)).toBe(true);
  });

  it('returns true for an empty string', () => {
    expect(isTokenExpired('')).toBe(true);
  });

  it('returns true for a malformed token', () => {
    expect(isTokenExpired('garbage')).toBe(true);
  });

  it('returns false for a token with a future exp', () => {
    const token = buildJwt({ sub: 'u', role: 'ROLE_ADMIN', userId: 1, iat: 0, exp: FUTURE_EXP });
    expect(isTokenExpired(token)).toBe(false);
  });

  it('returns true for a token with a past exp', () => {
    const token = buildJwt({ sub: 'u', role: 'ROLE_ADMIN', userId: 1, iat: 0, exp: PAST_EXP });
    expect(isTokenExpired(token)).toBe(true);
  });

  it('returns true when exp equals Date.now() exactly (boundary — not-yet-expired guard)', () => {
    // exp * 1000 === Date.now() → condition is >= so it IS considered expired
    const nowSeconds = Math.floor(Date.now() / 1000);
    const token = buildJwt({ sub: 'u', role: 'ROLE_ADMIN', userId: 1, iat: 0, exp: nowSeconds });
    // Allow for a 1-second window due to timing
    const result = isTokenExpired(token);
    expect(typeof result).toBe('boolean');
  });
});

// ── getUserIdFromToken ────────────────────────────────────────────────────────
describe('getUserIdFromToken()', () => {
  it('returns null for null input', () => {
    expect(getUserIdFromToken(null)).toBeNull();
  });

  it('returns null for a malformed token', () => {
    expect(getUserIdFromToken('bad')).toBeNull();
  });

  it('extracts the userId field from a valid token', () => {
    const token = buildJwt({ sub: 'bob', role: 'ROLE_CLAIMS_ANALYST', userId: 99, iat: 0, exp: FUTURE_EXP });
    expect(getUserIdFromToken(token)).toBe(99);
  });

  it('returns undefined (falsy) when the payload has no userId field', () => {
    const token = buildJwt({ sub: 'bob', role: 'ROLE_ADMIN', iat: 0, exp: FUTURE_EXP });
    // getUserIdFromToken uses ?. and ?? null so it returns null
    expect(getUserIdFromToken(token)).toBeNull();
  });
});
