import { vi, describe, it, expect, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./axiosInstance', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getApiErrorMessage: vi.fn(),
}));

import axiosInstance from './axiosInstance';
import { notificationsApi } from './notificationsApi';

const get   = axiosInstance.get    as Mock;
const post  = axiosInstance.post   as Mock;
const patch = axiosInstance.patch  as Mock;
const del   = axiosInstance.delete as Mock;

beforeEach(() => { vi.clearAllMocks(); });

const notif: import('./notificationsApi').Notification = {
  notificationId: 1, userId: 42, title: 'Alert', message: 'Something happened',
  category: 'RISK', referenceId: 'CLM-001', status: 'UNREAD',
  createdDate: '2026-01-01T00:00:00', readDate: null,
};

// NotificationService wraps all responses in { success, message, data }
const envelope = (data: unknown) => ({ success: true, message: 'OK', data });

// ── unwrap envelope ───────────────────────────────────────────────────────────
describe('notificationsApi envelope unwrapping', () => {
  it('extracts data from a well-formed envelope', async () => {
    get.mockResolvedValueOnce({ data: envelope([notif]) });
    const result = await notificationsApi.getForUser(42);
    expect(result).toEqual([notif]);
  });

  it('returns empty array (not null) when envelope.data is undefined', async () => {
    get.mockResolvedValueOnce({ data: envelope(undefined) });
    const result = await notificationsApi.getForUser(42);
    expect(result).toEqual([]);
  });

  it('falls back gracefully when the envelope is missing (flat response)', async () => {
    // Backend contract drift — responds with a flat array instead of envelope
    get.mockResolvedValueOnce({ data: [notif] });
    const result = await notificationsApi.getForUser(42);
    // unwrap() returns the raw payload as-is
    expect(result).toEqual([notif]);
  });
});

// ── getForUser ────────────────────────────────────────────────────────────────
describe('notificationsApi.getForUser()', () => {
  it('GETs /notifications/user/:userId', async () => {
    get.mockResolvedValueOnce({ data: envelope([notif]) });
    await notificationsApi.getForUser(42);
    expect(get).toHaveBeenCalledWith('/notifications/user/42');
  });
});

// ── getByStatus ───────────────────────────────────────────────────────────────
describe('notificationsApi.getByStatus()', () => {
  it('GETs /notifications/user/:userId/status/:status', async () => {
    get.mockResolvedValueOnce({ data: envelope([notif]) });
    await notificationsApi.getByStatus(42, 'UNREAD');
    expect(get).toHaveBeenCalledWith('/notifications/user/42/status/UNREAD');
  });
});

// ── getByCategory ─────────────────────────────────────────────────────────────
describe('notificationsApi.getByCategory()', () => {
  it('GETs /notifications/user/:userId/category/:category', async () => {
    get.mockResolvedValueOnce({ data: envelope([notif]) });
    await notificationsApi.getByCategory(42, 'RISK');
    expect(get).toHaveBeenCalledWith('/notifications/user/42/category/RISK');
  });
});

// ── getUnreadCount ────────────────────────────────────────────────────────────
describe('notificationsApi.getUnreadCount()', () => {
  it('GETs /notifications/unread-count/:userId', async () => {
    get.mockResolvedValueOnce({ data: envelope(5) });
    const count = await notificationsApi.getUnreadCount(42);
    expect(get).toHaveBeenCalledWith('/notifications/unread-count/42');
    expect(count).toBe(5);
  });

  it('returns 0 when the unwrapped value is null', async () => {
    get.mockResolvedValueOnce({ data: envelope(null) });
    const count = await notificationsApi.getUnreadCount(42);
    expect(count).toBe(0);
  });

  it('coerces a string number to a number', async () => {
    get.mockResolvedValueOnce({ data: envelope('7') });
    const count = await notificationsApi.getUnreadCount(42);
    expect(count).toBe(7);
  });
});

// ── updateStatus ──────────────────────────────────────────────────────────────
describe('notificationsApi.updateStatus()', () => {
  it('PATCHes /notifications/:id/status', async () => {
    patch.mockResolvedValueOnce({ data: envelope({ ...notif, status: 'READ' }) });
    const result = await notificationsApi.updateStatus(1, 'READ');
    expect(patch).toHaveBeenCalledWith('/notifications/1/status', { status: 'READ' });
    expect(result.status).toBe('READ');
  });
});

// ── markAllRead ───────────────────────────────────────────────────────────────
describe('notificationsApi.markAllRead()', () => {
  it('PATCHes /notifications/user/:userId/mark-all-read', async () => {
    patch.mockResolvedValueOnce({ data: envelope('5 notifications marked as read') });
    const result = await notificationsApi.markAllRead(42);
    expect(patch).toHaveBeenCalledWith('/notifications/user/42/mark-all-read');
    expect(result).toBe('5 notifications marked as read');
  });
});

// ── create ────────────────────────────────────────────────────────────────────
describe('notificationsApi.create()', () => {
  it('POSTs to /notifications', async () => {
    const req = { userId: 42, title: 'Alert', message: 'Msg', category: 'RISK' };
    post.mockResolvedValueOnce({ data: envelope(notif) });
    const result = await notificationsApi.create(req);
    expect(post).toHaveBeenCalledWith('/notifications', req);
    expect(result.title).toBe('Alert');
  });
});

// ── delete ────────────────────────────────────────────────────────────────────
describe('notificationsApi.delete()', () => {
  it('DELETEs /notifications/:id', async () => {
    del.mockResolvedValueOnce({ data: envelope(undefined) });
    await notificationsApi.delete(1);
    expect(del).toHaveBeenCalledWith('/notifications/1');
  });
});
