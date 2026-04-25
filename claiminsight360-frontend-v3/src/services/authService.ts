import { apiClient } from './apiClient';
import { LoginRequest, LoginResponse, User } from '@/types';

export const authService = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    try {
      console.log('Sending login request to /auth/login');
      const response = await apiClient.post<LoginResponse>('/auth/login', credentials);
      
      console.log('Login response received:', { user: response.user?.username });
      
      if (response.token) {
        localStorage.setItem('authToken', response.token);
        console.log('Token stored');
      }
      
      if (response.user) {
        localStorage.setItem('user', JSON.stringify(response.user));
        console.log('User stored');
      }
      
      return response;
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  },

  async logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout', {});
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
    }
  },

  async getCurrentUser(): Promise<User> {
    return apiClient.get<User>('/auth/me');
  },

  async refreshToken(): Promise<LoginResponse> {
    return apiClient.post<LoginResponse>('/auth/refresh', {});
  },

  getStoredUser(): User | null {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  },

  isAuthenticated(): boolean {
    return !!localStorage.getItem('authToken');
  },
};
