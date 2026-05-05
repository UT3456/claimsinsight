import { vi, describe, it, expect, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./axiosInstance', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getApiErrorMessage: vi.fn(),
}));

import axiosInstance from './axiosInstance';
import { authApi } from './authApi';

const post = axiosInstance.post as Mock;

beforeEach(() => { vi.clearAllMocks(); });

const loginResp = { accessToken: 'tok', tokenType: 'Bearer', expiresIn: 3600, username: 'alice', role: 'ROLE_ADMIN' };

describe('authApi.login()', () => {
  it('POSTs to /auth/login with the credentials', async () => {
    post.mockResolvedValueOnce({ data: loginResp });
    await authApi.login({ username: 'alice', password: 'pw' });
    expect(post).toHaveBeenCalledWith('/auth/login', { username: 'alice', password: 'pw' });
  });

  it('returns the AuthResponse from the response body', async () => {
    post.mockResolvedValueOnce({ data: loginResp });
    const result = await authApi.login({ username: 'alice', password: 'pw' });
    expect(result).toEqual(loginResp);
  });

  it('propagates errors from axios', async () => {
    post.mockRejectedValueOnce(new Error('401 Unauthorized'));
    await expect(authApi.login({ username: 'bad', password: 'bad' })).rejects.toThrow('401 Unauthorized');
  });
});

describe('authApi.register()', () => {
  it('POSTs to /auth/register', async () => {
    post.mockResolvedValueOnce({ data: loginResp });
    await authApi.register({ username: 'newuser', email: 'new@example.com', password: 'pw' });
    expect(post).toHaveBeenCalledWith('/auth/register', {
      username: 'newuser', email: 'new@example.com', password: 'pw',
    });
  });

  it('returns the AuthResponse', async () => {
    post.mockResolvedValueOnce({ data: loginResp });
    const result = await authApi.register({ username: 'x', email: 'x@x.com', password: 'y' });
    expect(result.username).toBe('alice');
    expect(result.accessToken).toBe('tok');
  });
});
