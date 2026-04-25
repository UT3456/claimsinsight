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

  /**
   * Download a generated report as PDF or CSV. Returns a Blob carrying the
   * exact bytes produced by analytics-report-service.
   */
  export: (id: number, format: 'pdf' | 'csv'): Promise<{ blob: Blob; filename: string }> =>
    axiosInstance
      .get(`/reports/${id}/export`, {
        params: { format },
        responseType: 'blob',
      })
      .then((response) => {
        const contentType =
          (response.headers['content-type'] as string | undefined) ??
          (format === 'pdf' ? 'application/pdf' : 'text/csv');
        const blob = new Blob([response.data as BlobPart], { type: contentType });

        // Parse filename out of Content-Disposition if the gateway forwards it.
        const cd = (response.headers['content-disposition'] as string | undefined) ?? '';
        const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(cd);
        const filename = match?.[1] ?? `report-${id}.${format}`;
        return { blob, filename };
      }),
};
