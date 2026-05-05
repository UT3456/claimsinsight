/**
 * Tests for the axiosInstance module.
 * We only test getApiErrorMessage() here — it is pure logic with no HTTP calls.
 * The interceptors (JWT inject, 401 auto-logout) are tested in integration through
 * the individual API-service test files.
 */
import { describe, it, expect } from 'vitest';
import { getApiErrorMessage } from './axiosInstance';

// Helper to build a fake AxiosError-shaped object
function makeAxiosError(overrides: Record<string, unknown> = {}) {
  return {
    isAxiosError: true,
    message: 'Axios error',
    ...overrides,
  };
}

describe('getApiErrorMessage()', () => {
  // ── response.data is a plain string ────────────────────────────────────────
  it('returns the data string when response.data is a plain string', () => {
    const err = makeAxiosError({ response: { data: 'Bad Request' } });
    expect(getApiErrorMessage(err)).toBe('Bad Request');
  });

  // ── standard envelope: { timestamp, status, error, message } ───────────────
  it('returns data.message when envelope has a message field', () => {
    const err = makeAxiosError({
      response: { data: { timestamp: '2026-01-01', status: 400, error: 'Bad Request', message: 'Feed not found' } },
    });
    expect(getApiErrorMessage(err)).toBe('Feed not found');
  });

  it('falls back to data.error when message is absent', () => {
    const err = makeAxiosError({
      response: { data: { status: 500, error: 'Internal Server Error' } },
    });
    expect(getApiErrorMessage(err)).toBe('Internal Server Error');
  });

  // ── NotificationService envelope: { success, message, data } ───────────────
  it('returns data.message from the NotificationService envelope', () => {
    const err = makeAxiosError({
      response: { data: { success: false, message: 'Notification not found', data: null } },
    });
    expect(getApiErrorMessage(err)).toBe('Notification not found');
  });

  // ── Network / timeout errors ────────────────────────────────────────────────
  it('returns timeout message when code is ECONNABORTED', () => {
    const err = makeAxiosError({ code: 'ECONNABORTED' });
    expect(getApiErrorMessage(err)).toContain('timed out');
  });

  it('returns network message when message is "Network Error"', () => {
    const err = makeAxiosError({ message: 'Network Error' });
    expect(getApiErrorMessage(err)).toContain('Cannot reach the server');
  });

  // ── Unknown / fallback ──────────────────────────────────────────────────────
  it('returns the axios message as a last resort', () => {
    const err = makeAxiosError({ message: 'Something weird happened' });
    expect(getApiErrorMessage(err)).toBe('Something weird happened');
  });

  it('returns fallback string for null/undefined input', () => {
    const result = getApiErrorMessage(null);
    expect(typeof result).toBe('string');
    expect(result.length).toBeGreaterThan(0);
  });

  it('skips empty message string and falls back to error field', () => {
    const err = makeAxiosError({
      response: { data: { message: '', error: 'Conflict' } },
    });
    expect(getApiErrorMessage(err)).toBe('Conflict');
  });
});
