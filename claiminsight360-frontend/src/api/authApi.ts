import axiosInstance from './axiosInstance';
import type { LoginRequest, RegisterRequest, AuthResponse } from '../types/auth.types';

export const authApi = {
  login(data: LoginRequest): Promise<AuthResponse> {
    return axiosInstance
      .post<AuthResponse>('/auth/login', data)
      .then((res) => res.data);
  },

  register(data: RegisterRequest): Promise<AuthResponse> {
    return axiosInstance
      .post<AuthResponse>('/auth/register', data)
      .then((res) => res.data);
  },
};
