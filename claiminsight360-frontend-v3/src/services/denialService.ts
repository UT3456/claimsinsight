import { apiClient } from './apiClient';
import { Denial, DenialPattern, PaginatedResponse, PaginationParams } from '@/types';

export const denialService = {
  async getDenials(
    params: PaginationParams
  ): Promise<PaginatedResponse<Denial>> {
    return apiClient.get<PaginatedResponse<Denial>>('/denials', params);
  },

  async getDenialById(denialId: string): Promise<Denial> {
    return apiClient.get<Denial>(`/denials/${denialId}`);
  },

  async getClaimDenials(claimId: string): Promise<Denial[]> {
    return apiClient.get<Denial[]>(`/claims/${claimId}/denials`);
  },

  async createDenial(data: Partial<Denial>): Promise<Denial> {
    return apiClient.post<Denial>('/denials', data);
  },

  async updateDenial(denialId: string, data: Partial<Denial>): Promise<Denial> {
    return apiClient.put<Denial>(`/denials/${denialId}`, data);
  },

  async getDenialPatterns(): Promise<DenialPattern[]> {
    return apiClient.get<DenialPattern[]>('/denials/patterns');
  },

  async getDenialAnalytics(startDate: string, endDate: string) {
    return apiClient.get('/denials/analytics', { startDate, endDate });
  },

  async appealDenial(denialId: string, appealReason: string): Promise<Denial> {
    return apiClient.post<Denial>(`/denials/${denialId}/appeal`, { reason: appealReason });
  },
};
