import { apiClient } from './apiClient';
import { Notification, PaginatedResponse, PaginationParams } from '@/types';

export const notificationService = {
  async getNotifications(
    params: PaginationParams
  ): Promise<PaginatedResponse<Notification>> {
    return apiClient.get<PaginatedResponse<Notification>>('/notifications', params);
  },

  async markAsRead(notificationId: string): Promise<Notification> {
    return apiClient.patch<Notification>(`/notifications/${notificationId}/read`, {});
  },

  async markAllAsRead(): Promise<void> {
    await apiClient.patch('/notifications/read-all', {});
  },

  async deleteNotification(notificationId: string): Promise<void> {
    await apiClient.delete(`/notifications/${notificationId}`);
  },

  async getUnreadCount(): Promise<number> {
    const response = await apiClient.get<{ count: number }>('/notifications/unread-count');
    return response.count;
  },

  async subscribeToNotifications(userId: string): Promise<void> {
    await apiClient.post('/notifications/subscribe', { userId });
  },
};
