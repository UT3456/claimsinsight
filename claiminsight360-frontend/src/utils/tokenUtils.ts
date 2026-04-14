/**
 * Lightweight JWT utilities — no extra library needed.
 * The backend signs with HMAC-SHA256; we only need to decode the payload.
 */

interface JwtPayload {
  sub:    string;        // username
  role:   string;        // e.g. ROLE_ADMIN
  userId: number;        // from JwtService claims
  iat:    number;
  exp:    number;
}

/** Decode the JWT payload (base64url) without verifying signature. */
export function decodeToken(token: string): JwtPayload | null {
  try {
    const parts   = token.split('.');
    if (parts.length !== 3) return null;
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const json    = decodeURIComponent(
      atob(payload)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join(''),
    );
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

/** Returns true if the token is expired (or null). */
export function isTokenExpired(token: string | null): boolean {
  if (!token) return true;
  const payload = decodeToken(token);
  if (!payload) return true;
  return Date.now() >= payload.exp * 1000;
}

/** Extract userId from JWT payload. */
export function getUserIdFromToken(token: string | null): number | null {
  if (!token) return null;
  return decodeToken(token)?.userId ?? null;
}
