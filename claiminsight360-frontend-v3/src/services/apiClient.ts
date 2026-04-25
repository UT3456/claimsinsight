import axios, { AxiosInstance, AxiosError } from 'axios';
import { ApiResponse, ApiError } from '@/types';

const API_GATEWAY_URL = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8888';
const API_TIMEOUT = parseInt(import.meta.env.VITE_API_TIMEOUT || '30000');

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_GATEWAY_URL,
      timeout: API_TIMEOUT,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Add request interceptor for token
    this.client.interceptors.request.use((config) => {
      const token = localStorage.getItem('authToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });

    // Add response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        if (error.response?.status === 401) {
          // Handle unauthorized
          localStorage.removeItem('authToken');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  private extractData<T>(response: unknown): T {
    // Handle different response formats
    if (response && typeof response === 'object') {
      const obj = response as Record<string, unknown>;
      // If response has 'data' property, return that
      if ('data' in obj) {
        return obj.data as T;
      }
      // If response has 'user' and 'token' (LoginResponse), return the whole object
      if ('user' in obj && 'token' in obj) {
        return obj as T;
      }
    }
    return response as T;
  }

  async get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    try {
      const response = await this.client.get(url, { params });
      return this.extractData<T>(response.data);
    } catch (error) {
      throw this.handleError(error);
    }
  }

  async post<T>(url: string, data?: unknown): Promise<T> {
    try {
      const response = await this.client.post(url, data);
      return this.extractData<T>(response.data);
    } catch (error) {
      throw this.handleError(error);
    }
  }

  async put<T>(url: string, data?: unknown): Promise<T> {
    try {
      const response = await this.client.put(url, data);
      return this.extractData<T>(response.data);
    } catch (error) {
      throw this.handleError(error);
    }
  }

  async patch<T>(url: string, data?: unknown): Promise<T> {
    try {
      const response = await this.client.patch(url, data);
      return this.extractData<T>(response.data);
    } catch (error) {
      throw this.handleError(error);
    }
  }

  async delete<T>(url: string): Promise<T> {
    try {
      const response = await this.client.delete(url);
      return this.extractData<T>(response.data);
    } catch (error) {
      throw this.handleError(error);
    }
  }

  private handleError(error: unknown): ApiError {
    if (axios.isAxiosError(error)) {
      const apiError: ApiError = {
        code: error.code || 'UNKNOWN_ERROR',
        message: error.response?.data?.message || error.message || 'An error occurred',
        details: error.response?.data?.details,
      };
      console.error('API Error:', apiError);
      return apiError;
    }
    throw error;
  }
}

export const apiClient = new ApiClient();
