import { vi, describe, it, expect, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./axiosInstance', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getApiErrorMessage: vi.fn(),
}));

import axiosInstance from './axiosInstance';
import { usersApi, type User } from './usersApi';

const get   = axiosInstance.get    as Mock;
const patch = axiosInstance.patch  as Mock;
const del   = axiosInstance.delete as Mock;

beforeEach(() => { vi.clearAllMocks(); });

const user: User = {
  id: 1,
  username: 'admin_alice',
  name: 'Alice Admin',
  email: 'alice.admin@acme.com',
  phone: '+1-202-555-0101',
  role: 'ROLE_ADMIN',
  enabled: true,
  createdAt: '2025-01-15T09:00:00',
  updatedAt: '2025-01-15T09:00:00',
};

// ── getAll ───────────────────────────────────────────────────────────────────
describe('usersApi.getAll()', () => {
  it('GETs /users and returns the array', async () => {
    get.mockResolvedValueOnce({ data: [user] });
    const result = await usersApi.getAll();
    expect(get).toHaveBeenCalledWith('/users');
    expect(result).toEqual([user]);
  });

  it('returns an empty array when the server has no users', async () => {
    get.mockResolvedValueOnce({ data: [] });
    const result = await usersApi.getAll();
    expect(result).toHaveLength(0);
  });
});

// ── getById ──────────────────────────────────────────────────────────────────
describe('usersApi.getById()', () => {
  it('GETs /users/:id and returns one user', async () => {
    get.mockResolvedValueOnce({ data: user });
    const result = await usersApi.getById(1);
    expect(get).toHaveBeenCalledWith('/users/1');
    expect(result).toEqual(user);
  });
});

// ── update ───────────────────────────────────────────────────────────────────
describe('usersApi.update()', () => {
  it('PATCHes /users/:id with a role change', async () => {
    const updated = { ...user, role: 'ROLE_CLAIMS_MANAGER' as const };
    patch.mockResolvedValueOnce({ data: updated });
    const result = await usersApi.update(1, { role: 'ROLE_CLAIMS_MANAGER' });
    expect(patch).toHaveBeenCalledWith('/users/1', { role: 'ROLE_CLAIMS_MANAGER' });
    expect(result.role).toBe('ROLE_CLAIMS_MANAGER');
  });

  it('PATCHes with an enabled toggle', async () => {
    const updated = { ...user, enabled: false };
    patch.mockResolvedValueOnce({ data: updated });
    const result = await usersApi.update(1, { enabled: false });
    expect(patch).toHaveBeenCalledWith('/users/1', { enabled: false });
    expect(result.enabled).toBe(false);
  });

  it('supports partial updates with both role and enabled at once', async () => {
    const updated = { ...user, role: 'ROLE_FRAUD_ANALYST' as const, enabled: false };
    patch.mockResolvedValueOnce({ data: updated });
    await usersApi.update(1, { role: 'ROLE_FRAUD_ANALYST', enabled: false });
    expect(patch).toHaveBeenCalledWith('/users/1', { role: 'ROLE_FRAUD_ANALYST', enabled: false });
  });
});

// ── delete ───────────────────────────────────────────────────────────────────
describe('usersApi.delete()', () => {
  it('DELETEs /users/:id and resolves to undefined', async () => {
    del.mockResolvedValueOnce({ data: { message: 'User deleted' } });
    const result = await usersApi.delete(2);
    expect(del).toHaveBeenCalledWith('/users/2');
    expect(result).toBeUndefined();
  });

  it('propagates the rejection so the page shows the error toast (e.g. 400 self-delete)', async () => {
    const err = Object.assign(new Error('cannot delete self'), {
      userMessage: 'You cannot delete your own account',
      response: { status: 400 },
    });
    del.mockRejectedValueOnce(err);
    await expect(usersApi.delete(1)).rejects.toMatchObject({ userMessage: expect.stringContaining('own account') });
  });
});
