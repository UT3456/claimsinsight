import { vi, describe, it, expect, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./axiosInstance', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getApiErrorMessage: vi.fn(),
}));

import axiosInstance from './axiosInstance';
import { auditApi } from './auditApi';

const get = axiosInstance.get    as Mock;
const del = axiosInstance.delete as Mock;

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

// ── clearAll() — destructive admin action; verify exact endpoint ──────────
describe('auditApi.clearAll()', () => {
  it('DELETEs /audit/logs (no params) and returns the row count', async () => {
    del.mockResolvedValueOnce({ data: { scope: 'all', removed: 8233 } });
    const result = await auditApi.clearAll();
    expect(del).toHaveBeenCalledWith('/audit/logs');
    expect(result.removed).toBe(8233);
    expect(result.scope).toBe('all');
  });
});

// ── clearOlderThan() — bulk purge with cutoff timestamp ───────────────────
describe('auditApi.clearOlderThan()', () => {
  it('DELETEs /audit/logs with the olderThan ISO param', async () => {
    del.mockResolvedValueOnce({ data: { scope: 'olderThan', olderThan: '2026-04-01T00:00:00', removed: 412 } });
    const result = await auditApi.clearOlderThan('2026-04-01T00:00:00');
    expect(del).toHaveBeenCalledWith('/audit/logs', { params: { olderThan: '2026-04-01T00:00:00' } });
    expect(result.removed).toBe(412);
  });
});

// ── deleteById() — surgical delete from per-row trash icon ────────────────
describe('auditApi.deleteById()', () => {
  it('DELETEs /audit/logs/:id', async () => {
    del.mockResolvedValueOnce({ data: { id: 99, removed: true } });
    const result = await auditApi.deleteById(99);
    expect(del).toHaveBeenCalledWith('/audit/logs/99');
    expect(result.removed).toBe(true);
  });
});
