import axiosInstance from './axiosInstance';

export interface AuditLog {
  id: number;
  username: string;
  userId: number | null;
  action: string;
  endpoint: string;
  details: string | null;
  timestamp: string;
}

export interface AuditPage {
  content: AuditLog[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const auditApi = {
  getLogs: (page = 0, size = 20): Promise<AuditPage> =>
    axiosInstance.get('/audit/logs', { params: { page, size } }).then(r => r.data),

  getByUser: (username: string): Promise<AuditLog[]> =>
    axiosInstance.get(`/audit/logs/user/${username}`).then(r => r.data),

  getByAction: (action: string): Promise<AuditLog[]> =>
    axiosInstance.get(`/audit/logs/action/${action}`).then(r => r.data),

  getByDateRange: (from: string, to: string): Promise<AuditLog[]> =>
    axiosInstance.get('/audit/logs/range', { params: { from, to } }).then(r => r.data),

  /** Wipe all audit records — admin only. Returns the number of rows removed. */
  clearAll: (): Promise<{ scope: string; removed: number }> =>
    axiosInstance.delete('/audit/logs').then(r => r.data),

  /** Purge audit entries older than the given ISO timestamp — admin only. */
  clearOlderThan: (olderThan: string): Promise<{ scope: string; olderThan: string; removed: number }> =>
    axiosInstance.delete('/audit/logs', { params: { olderThan } }).then(r => r.data),

  /** Delete a single audit row by ID — admin only. */
  deleteById: (id: number): Promise<{ id: number; removed: boolean }> =>
    axiosInstance.delete(`/audit/logs/${id}`).then(r => r.data),
};
