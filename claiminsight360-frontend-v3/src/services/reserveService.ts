import { apiClient } from './apiClient';
import { CostReserve, ReserveMetrics, PaginatedResponse, PaginationParams } from '@/types';

/**
 * Reserve Service - Connects via API Gateway to Cost Reserve Service
 * Routes: /api/reserves/**, /api/costs/**
 */
export const reserveService = {
  async getReserves(
    params: PaginationParams
  ): Promise<PaginatedResponse<CostReserve>> {
    return apiClient.get<PaginatedResponse<CostReserve>>('/api/reserves', params);
  },

  async getReserveById(reserveId: string): Promise<CostReserve> {
    return apiClient.get<CostReserve>(`/api/reserves/${reserveId}`);
  },

  async getClaimReserves(claimId: string): Promise<CostReserve[]> {
    return apiClient.get<CostReserve[]>(`/api/reserves?claimId=${claimId}`);
  },

  async createReserve(data: Partial<CostReserve>): Promise<CostReserve> {
    return apiClient.post<CostReserve>('/api/reserves', data);
  },

  async updateReserve(reserveId: string, data: Partial<CostReserve>): Promise<CostReserve> {
    return apiClient.put<CostReserve>(`/api/reserves/${reserveId}`, data);
  },

  async releaseReserve(reserveId: string): Promise<CostReserve> {
    return apiClient.patch<CostReserve>(`/api/reserves/${reserveId}/release`, {});
  },

  async getReserveMetrics(): Promise<ReserveMetrics> {
    return apiClient.get<ReserveMetrics>('/api/costs/metrics');
  },

  async getReserveAnalytics(startDate: string, endDate: string) {
    return apiClient.get('/api/costs/analytics', { startDate, endDate });
  },

  async bulkUpdateReserves(updates: Array<{ reserveId: string; amount: number }>): Promise<CostReserve[]> {
    return apiClient.post<CostReserve[]>('/api/reserves/bulk-update', { updates });
  },
};
