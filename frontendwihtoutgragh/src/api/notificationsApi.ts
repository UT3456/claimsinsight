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
// NotificationService wraps every response in an ApiResponse envelope:
//   { success: boolean, message: string, data: <payload> }
// Every other service in the platform uses a flat shape, so this helper exists
// only for the calls in this file.
//
// Resilience: if the BE accidentally drops the envelope (or a proxy/middleware
// reshapes it), we fall through and return the raw response — but we WARN
// so the developer knows the contract is drifting. Silent fallthrough was the
// original behaviour and it hid bugs.
function unwrap<T>(response: unknown): T {
  if (
    response !== null &&
    typeof response === 'object' &&
    'data' in (response as object)
  ) {
    const env = response as { success?: boolean; message?: string; data: T };
    // If the envelope explicitly reports failure, log it. The HTTP status
    // would normally be non-2xx in this case (axios would have thrown),
    // but defensive code in case BE returns 200 with success:false.
    if (env.success === false) {
      console.warn('[notificationsApi] envelope reported success=false:', env.message);
    }
    return env.data;
  }
  console.warn('[notificationsApi] response did not match expected ApiResponse envelope; ' +
               'returning raw payload. Backend contract may have changed.');
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
