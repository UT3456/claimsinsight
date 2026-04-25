import axiosInstance from './axiosInstance';

export interface AdjusterPerformance {
  perfId: number;
  adjusterId: number;
  claimsHandled: number;
  totalDaysTaken: number;
  avgTat: number;
  qualityScore: number;
  slaMetCount: number;
  slaBreachedCount: number;
  deniedClaimsCount: number;
  errorRate: number;
  period: string;
  slaComplianceRate: number;
  denialRate: number;
  performanceIndex: number;
  productivityFlag: string;
  performanceFlag: string;
}

export interface CreateAdjusterRequest {
  adjusterId: number;
  claimsHandled: number;
  totalDaysTaken: number;
  slaMetCount: number;
  slaBreachedCount: number;
  deniedClaimsCount: number;
  errorRate: number;
  period: string;
}

export interface SlaViolation {
  violationId: number;
  claimId: number;
  adjusterId: number;
  violationType: string;
  slaTargetDays: number;
  actualDays: number;
  violationDate: string;
  daysOverdue: number;
  severity: string;
  escalated: boolean;
}

export interface CreateSlaViolationRequest {
  claimId: number;
  adjusterId: number;
  violationType: string;
  slaTargetDays: number;
  actualDays: number;
  violationDate: string;
}

export const adjustersApi = {
  getAll: (period?: string): Promise<AdjusterPerformance[]> =>
    axiosInstance.get('/adjusters/performance', { params: period ? { period } : {} }).then(r => r.data),

  getByAdjuster: (id: number, period?: string): Promise<AdjusterPerformance[]> =>
    axiosInstance.get(`/adjusters/${id}/performance`, { params: period ? { period } : {} }).then(r => r.data),

  getTopPerformers: (period?: string): Promise<AdjusterPerformance[]> =>
    axiosInstance.get('/adjusters/top-performers', { params: period ? { period } : {} }).then(r => r.data),

  create: (data: CreateAdjusterRequest): Promise<AdjusterPerformance> =>
    axiosInstance.post('/adjusters/performance', data).then(r => r.data),

  delete: (perfId: number): Promise<void> =>
    axiosInstance.delete(`/adjusters/performance/${perfId}`).then(r => r.data),
};

export const slaApi = {
  getAll: (): Promise<SlaViolation[]> =>
    axiosInstance.get('/sla-violations').then(r => r.data),

  getByAdjuster: (adjusterId: number): Promise<SlaViolation[]> =>
    axiosInstance.get(`/sla-violations/adjuster/${adjusterId}`).then(r => r.data),

  create: (data: CreateSlaViolationRequest): Promise<SlaViolation> =>
    axiosInstance.post('/sla-violations', data).then(r => r.data),
};
