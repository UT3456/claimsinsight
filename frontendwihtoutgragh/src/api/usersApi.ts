import axiosInstance from './axiosInstance';
import type { AppRole } from '../utils/roles';

/**
 * User management — admin-only endpoints exposed by the api-gateway's
 * embedded identity module. All routes require ROLE_ADMIN.
 */

export interface User {
  id: number;
  username: string;
  name: string | null;
  email: string;
  phone: string | null;
  role: AppRole;
  enabled: boolean;
  createdAt: string;
  updatedAt: string | null;
}

export interface UpdateUserRequest {
  /** New role; omit to leave unchanged. */
  role?: AppRole;
  /** Enable/disable the account; omit to leave unchanged. */
  enabled?: boolean;
}

export const usersApi = {
  getAll: (): Promise<User[]> =>
    axiosInstance.get('/users').then(r => r.data),

  getById: (id: number): Promise<User> =>
    axiosInstance.get(`/users/${id}`).then(r => r.data),

  update: (id: number, request: UpdateUserRequest): Promise<User> =>
    axiosInstance.patch(`/users/${id}`, request).then(r => r.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/users/${id}`).then(() => undefined),
};
