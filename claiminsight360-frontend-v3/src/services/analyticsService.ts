import { apiClient } from './apiClient';
import { AnalyticsMetrics, Report } from '@/types';

export const analyticsService = {
  async getDashboardMetrics(
    startDate: string,
    endDate: string
  ): Promise<AnalyticsMetrics> {
    return apiClient.get<AnalyticsMetrics>('/analytics/dashboard', { startDate, endDate });
  },

  async getClaimsMetrics(startDate: string, endDate: string) {
    return apiClient.get('/analytics/claims', { startDate, endDate });
  },

  async getDenialMetrics(startDate: string, endDate: string) {
    return apiClient.get('/analytics/denials', { startDate, endDate });
  },

  async getFraudMetrics(startDate: string, endDate: string) {
    return apiClient.get('/analytics/fraud', { startDate, endDate });
  },

  async getReserveMetrics(startDate: string, endDate: string) {
    return apiClient.get('/analytics/reserves', { startDate, endDate });
  },

  async getAdjusterMetrics(startDate: string, endDate: string) {
    return apiClient.get('/analytics/adjusters', { startDate, endDate });
  },

  async getReport(reportId: string): Promise<Report> {
    return apiClient.get<Report>(`/reports/${reportId}`);
  },

  async createReport(data: Partial<Report>): Promise<Report> {
    return apiClient.post<Report>('/reports', data);
  },

  async generateReport(reportType: string, filters: Record<string, unknown>) {
    return apiClient.post(`/reports/generate/${reportType}`, { filters });
  },

  async exportReport(reportId: string, format: 'pdf' | 'excel' | 'csv') {
    return apiClient.get(`/reports/${reportId}/export`, { format });
  },
};
