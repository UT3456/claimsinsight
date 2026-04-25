import { apiClient } from './apiClient';
import { Claim, ClaimFilter, PaginatedResponse, PaginationParams } from '@/types';

/**
 * Claims Service - Connects via API Gateway to Data Ingestion Service
 * Routes: /api/feeds/**
 */
export const claimsService = {
  async getClaims(
    params: PaginationParams & ClaimFilter
  ): Promise<PaginatedResponse<Claim>> {
    return apiClient.get<PaginatedResponse<Claim>>('/api/feeds/claims', params);
  },

  async getClaimById(claimId: string): Promise<Claim> {
    return apiClient.get<Claim>(`/api/feeds/claims/${claimId}`);
  },

  async createClaim(data: Partial<Claim>): Promise<Claim> {
    return apiClient.post<Claim>('/api/feeds/claims', data);
  },

  async updateClaim(claimId: string, data: Partial<Claim>): Promise<Claim> {
    return apiClient.put<Claim>(`/api/feeds/claims/${claimId}`, data);
  },

  async deleteClaim(claimId: string): Promise<void> {
    await apiClient.delete(`/api/feeds/claims/${claimId}`);
  },

  async assignClaimToAdjuster(claimId: string, adjusterId: string): Promise<Claim> {
    return apiClient.patch<Claim>(`/api/feeds/claims/${claimId}/assign`, { adjusterId });
  },

  async updateClaimStatus(claimId: string, status: string): Promise<Claim> {
    return apiClient.patch<Claim>(`/api/feeds/claims/${claimId}/status`, { status });
  },

  async searchClaims(searchTerm: string): Promise<Claim[]> {
    return apiClient.get<Claim[]>('/api/feeds/claims/search', { q: searchTerm });
  },
};
