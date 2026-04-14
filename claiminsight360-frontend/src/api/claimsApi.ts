import axiosInstance from './axiosInstance';

export interface ClaimKpi {
  kpiId: number;
  claimId: string;
  metricName: string;
  metricValue: number;
  metricDate: string;
}

export interface CreateKpiRequest {
  claimId: string;
  metricName: string;
  metricValue: number;
  metricDate: string;
}

export const METRIC_NAMES = ['TAT', 'CYCLE_TIME', 'SEVERITY', 'FREQUENCY', 'LOSS_RATIO'] as const;
export type MetricName = typeof METRIC_NAMES[number];

export const claimsApi = {
  getAll: (): Promise<ClaimKpi[]> =>
    axiosInstance.get('/kpis').then(r => r.data),

  getById: (id: number): Promise<ClaimKpi> =>
    axiosInstance.get(`/kpis/${id}`).then(r => r.data),

  getByClaim: (claimId: string): Promise<ClaimKpi[]> =>
    axiosInstance.get(`/kpis/claim/${claimId}`).then(r => r.data),

  getByMetric: (metricName: string): Promise<ClaimKpi[]> =>
    axiosInstance.get(`/kpis/metric/${metricName}`).then(r => r.data),

  getByDateRange: (start: string, end: string): Promise<ClaimKpi[]> =>
    axiosInstance.get('/kpis/date-range', { params: { start, end } }).then(r => r.data),

  create: (data: CreateKpiRequest): Promise<ClaimKpi> =>
    axiosInstance.post('/kpis', data).then(r => r.data),

  calculateForClaim: (claimId: string): Promise<ClaimKpi[]> =>
    axiosInstance.post(`/kpis/calculate/${claimId}`).then(r => r.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/kpis/${id}`).then(r => r.data),
};
