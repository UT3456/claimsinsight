import { vi, describe, it, expect, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./axiosInstance', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getApiErrorMessage: vi.fn(),
}));

import axiosInstance from './axiosInstance';
import { auditApi } from './auditApi';

const get = axiosInstance.get as Mock;

beforeEach(() => { vi.clearAllMocks(); });

const log: import('./auditApi').AuditLog = {
  id: 1, username: 'alice', userId: 42, action: 'LOGIN',
  endpoint: '/api/auth/login', details: null, timestamp: '2026-01-01T10:00:00',
};

const page: import('./auditApi').AuditPage = {
  content: [log], totalElements: 1, totalPages: 1, number: 0, size: 20,
};

// ── getLogs ───────────────────────────────────────────────────────────────────
describe('auditApi.getLogs()', () => {
  it('GETs /audit/logs with default pagination (page=0, size=20)', async () => {
    get.mockResolvedValueOnce({ data: page });
    const result = await auditApi.getLogs();
    expect(get).toHaveBeenCalledWith('/audit/logs', { params: { page: 0, size: 20 } });
    expect(result).toEqual(page);
  });

  it('GETs /audit/logs with custom pagination', async () => {
    get.mockResolvedValueOnce({ data: page });
    await auditApi.getLogs(2, 50);
    expect(get).toHaveBeenCalledWith('/audit/logs', { params: { page: 2, size: 50 } });
  });

  it('returns the AuditPage with content, totalElements and pagination metadata', async () => {
    get.mockResolvedValueOnce({ data: page });
    const result = await auditApi.getLogs();
    expect(result.content).toHaveLength(1);
    expect(result.totalElements).toBe(1);
    expect(result.totalPages).toBe(1);
  });
});

// ── getByUser ─────────────────────────────────────────────────────────────────
describe('auditApi.getByUser()', () => {
  it('GETs /audit/logs/user/:username', async () => {
    get.mockResolvedValueOnce({ data: [log] });
    const result = await auditApi.getByUser('alice');
    expect(get).toHaveBeenCalledWith('/audit/logs/user/alice');
    expect(result).toEqual([log]);
  });
});

// ── getByAction ───────────────────────────────────────────────────────────────
describe('auditApi.getByAction()', () => {
  it('GETs /audit/logs/action/:action', async () => {
    get.mockResolvedValueOnce({ data: [log] });
    await auditApi.getByAction('LOGIN');
    expect(get).toHaveBeenCalledWith('/audit/logs/action/LOGIN');
  });
});

// ── getByDateRange ────────────────────────────────────────────────────────────
describe('auditApi.getByDateRange()', () => {
  it('GETs /audit/logs/range with from and to params', async () => {
    get.mockResolvedValueOnce({ data: [log] });
    await auditApi.getByDateRange('2026-01-01', '2026-01-31');
    expect(get).toHaveBeenCalledWith('/audit/logs/range', {
      params: { from: '2026-01-01', to: '2026-01-31' },
    });
  });

  it('returns the list of matching logs', async () => {
    get.mockResolvedValueOnce({ data: [log] });
    const result = await auditApi.getByDateRange('2026-01-01', '2026-01-31');
    expect(result[0].action).toBe('LOGIN');
    expect(result[0].username).toBe('alice');
  });
});
