import { apiClient } from './apiClient';
import { Adjuster, AdjusterWorkload, PaginatedResponse, PaginationParams } from '@/types';

/**
 * Adjuster Service - Connects via API Gateway to Adjuster & Operations Service
 * Routes: /api/adjusters/**
 */
export const adjusterService = {
  async getAdjusters(
    params: PaginationParams
  ): Promise<PaginatedResponse<Adjuster>> {
    return apiClient.get<PaginatedResponse<Adjuster>>('/api/adjusters', params);
  },

  async getAdjusterById(adjusterId: string): Promise<Adjuster> {
    return apiClient.get<Adjuster>(`/api/adjusters/${adjusterId}`);
  },

  async createAdjuster(data: Partial<Adjuster>): Promise<Adjuster> {
    return apiClient.post<Adjuster>('/api/adjusters', data);
  },

  async updateAdjuster(adjusterId: string, data: Partial<Adjuster>): Promise<Adjuster> {
    return apiClient.put<Adjuster>(`/api/adjusters/${adjusterId}`, data);
  },

  async deactivateAdjuster(adjusterId: string): Promise<Adjuster> {
    return apiClient.patch<Adjuster>(`/api/adjusters/${adjusterId}/deactivate`, {});
  },

  async getAdjusterWorkload(adjusterId: string): Promise<AdjusterWorkload> {
    return apiClient.get<AdjusterWorkload>(`/api/adjusters/${adjusterId}/workload`);
  },

  async getAllWorkloads(): Promise<AdjusterWorkload[]> {
    return apiClient.get<AdjusterWorkload[]>('/api/adjusters/workloads');
  },

  async getAdjusterClaims(adjusterId: string, params: PaginationParams) {
    return apiClient.get(`/api/adjusters/${adjusterId}/claims`, params);
  },

  async getAdjusterPerformance(adjusterId: string, startDate: string, endDate: string) {
    return apiClient.get(`/api/adjusters/${adjusterId}/performance`, { startDate, endDate });
  },
};
