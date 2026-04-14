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
};
