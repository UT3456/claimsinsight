import { apiClient } from './apiClient';
import { FraudRisk, FraudMetrics, PaginatedResponse, PaginationParams } from '@/types';

export const fraudService = {
  async getFraudRisks(
    params: PaginationParams
  ): Promise<PaginatedResponse<FraudRisk>> {
    return apiClient.get<PaginatedResponse<FraudRisk>>('/fraud-risks', params);
  },

  async getFraudRiskById(fraudRiskId: string): Promise<FraudRisk> {
    return apiClient.get<FraudRisk>(`/fraud-risks/${fraudRiskId}`);
  },

  async getClaimFraudRisk(claimId: string): Promise<FraudRisk> {
    return apiClient.get<FraudRisk>(`/claims/${claimId}/fraud-risk`);
  },

  async updateFraudReviewStatus(fraudRiskId: string, status: string): Promise<FraudRisk> {
    return apiClient.patch<FraudRisk>(`/fraud-risks/${fraudRiskId}/status`, { status });
  },

  async addInvestigationNotes(fraudRiskId: string, notes: string): Promise<FraudRisk> {
    return apiClient.patch<FraudRisk>(`/fraud-risks/${fraudRiskId}/notes`, { notes });
  },

  async getFraudMetrics(): Promise<FraudMetrics> {
    return apiClient.get<FraudMetrics>('/fraud-risks/metrics');
  },

  async calculateClaimFraudScore(claimId: string): Promise<{ riskScore: number; factors: string[] }> {
    return apiClient.post('/fraud-risks/calculate-score', { claimId });
  },

  async getFraudTrends(startDate: string, endDate: string) {
    return apiClient.get('/fraud-risks/trends', { startDate, endDate });
  },
};
