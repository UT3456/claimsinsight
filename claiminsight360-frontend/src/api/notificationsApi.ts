import axiosInstance from './axiosInstance';

export interface Notification {
  notificationId: number;
  userId: number;
  title: string;
  message: string;
  category: string;
  referenceId: string | null;
  status: string;
  createdDate: string;
  readDate: string | null;
}

export interface CreateNotificationRequest {
  userId: number;
  title: string;
  message: string;
  category: string;
  referenceId?: string;
}

export const NOTIFICATION_CATEGORIES = ['RISK', 'DENIAL', 'COST', 'PERFORMANCE', 'AGING', 'SYSTEM'] as const;
export const NOTIFICATION_STATUSES   = ['UNREAD', 'READ', 'DISMISSED', 'ACTIONED'] as const;

// ── Response wrapper helper ───────────────────────────────────────────────────
// NotificationService wraps every response: { success, message, data: <payload> }
function unwrap<T>(response: unknown): T {
  if (
    response !== null &&
    typeof response === 'object' &&
    'data' in (response as object)
  ) {
    return (response as { data: T }).data;
  }
  return response as T;
}

export const notificationsApi = {
  getForUser: (userId: number): Promise<Notification[]> =>
    axiosInstance.get(`/notifications/user/${userId}`)
      .then(r => unwrap<Notification[]>(r.data) ?? []),

  getByStatus: (userId: number, status: string): Promise<Notification[]> =>
    axiosInstance.get(`/notifications/user/${userId}/status/${status}`)
      .then(r => unwrap<Notification[]>(r.data) ?? []),

  getByCategory: (userId: number, category: string): Promise<Notification[]> =>
    axiosInstance.get(`/notifications/user/${userId}/category/${category}`)
      .then(r => unwrap<Notification[]>(r.data) ?? []),

  getById: (id: number): Promise<Notification> =>
    axiosInstance.get(`/notifications/${id}`)
      .then(r => unwrap<Notification>(r.data)),

  getUnreadCount: (userId: number): Promise<number> =>
    axiosInstance.get(`/notifications/unread-count/${userId}`)
      .then(r => {
        const val = unwrap<number>(r.data);
        return typeof val === 'number' ? val : Number(val) || 0;
      }),

  updateStatus: (id: number, status: string): Promise<Notification> =>
    axiosInstance.patch(`/notifications/${id}/status`, { status })
      .then(r => unwrap<Notification>(r.data)),

  markAllRead: (userId: number): Promise<string> =>
    axiosInstance.patch(`/notifications/user/${userId}/mark-all-read`)
      .then(r => unwrap<string>(r.data)),

  create: (data: CreateNotificationRequest): Promise<Notification> =>
    axiosInstance.post('/notifications', data)
      .then(r => unwrap<Notification>(r.data)),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/notifications/${id}`)
      .then(r => unwrap<void>(r.data)),
};
