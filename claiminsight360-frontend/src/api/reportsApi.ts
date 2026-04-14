import axiosInstance from './axiosInstance';

export interface AnalyticsReport {
  reportId: number;
  scope: string;
  scopeValue: string;
  metrics: string;
  generatedDate: string;
  generatedBy: string;
  reportData: string | null;
}

export interface CreateReportRequest {
  scope: string;
  scopeValue: string;
  metrics: string;
  generatedDate: string;
  generatedBy: string;
  reportData?: string;
}

export const REPORT_SCOPES = ['PRODUCT', 'REGION', 'CLAIM_TYPE', 'PERIOD'] as const;
export type ReportScope = typeof REPORT_SCOPES[number];

export const reportsApi = {
  getAll: (): Promise<AnalyticsReport[]> =>
    axiosInstance.get('/reports').then(r => r.data),

  getById: (id: number): Promise<AnalyticsReport> =>
    axiosInstance.get(`/reports/${id}`).then(r => r.data),

  getByScope: (scope: string): Promise<AnalyticsReport[]> =>
    axiosInstance.get(`/reports/scope/${scope}`).then(r => r.data),

  getByGeneratedBy: (user: string): Promise<AnalyticsReport[]> =>
    axiosInstance.get(`/reports/generated-by/${user}`).then(r => r.data),

  getByDateRange: (startDate: string, endDate: string): Promise<AnalyticsReport[]> =>
    axiosInstance.get('/reports/date-range', { params: { startDate, endDate } }).then(r => r.data),

  getDashboardSummary: (): Promise<unknown> =>
    axiosInstance.get('/reports/analytics/dashboard-summary').then(r => r.data),

  create: (data: CreateReportRequest): Promise<AnalyticsReport> =>
    axiosInstance.post('/reports', data).then(r => r.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/reports/${id}`).then(r => r.data),
};
