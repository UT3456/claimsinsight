import axiosInstance from './axiosInstance';

// ── Risk Score ────────────────────────────────────────────────────────────────

export interface RiskScore {
  scoreId: number;
  claimId: string;
  scoreValue: number;
  computedDate: string;
}

export interface CreateRiskScoreRequest {
  claimId: string;
  scoreValue: number;
  computedDate: string;
}

// ── Risk Indicator ────────────────────────────────────────────────────────────

export interface RiskIndicator {
  indicatorId: number;
  claimId: string;
  indicatorType: string;
  severity: string;
  triggeredDate: string;
}

export interface CreateRiskIndicatorRequest {
  claimId: string;
  indicatorType: string;
  severity: string;
  triggeredDate: string;
}

export const INDICATOR_TYPES = ['HighCost', 'UnusualTiming', 'Pattern'] as const;
export const SEVERITIES       = ['HIGH', 'MEDIUM', 'LOW'] as const;

export const riskScoresApi = {
  getAll: (): Promise<RiskScore[]> =>
    axiosInstance.get('/risk-scores').then(r => r.data),

  getById: (id: number): Promise<RiskScore> =>
    axiosInstance.get(`/risk-scores/${id}`).then(r => r.data),

  getByClaim: (claimId: string): Promise<RiskScore[]> =>
    axiosInstance.get(`/risk-scores/claim/${claimId}`).then(r => r.data),

  getAboveThreshold: (threshold: number): Promise<RiskScore[]> =>
    axiosInstance.get(`/risk-scores/threshold/${threshold}`).then(r => r.data),

  create: (data: CreateRiskScoreRequest): Promise<RiskScore> =>
    axiosInstance.post('/risk-scores', data).then(r => r.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/risk-scores/${id}`).then(r => r.data),
};

export const riskIndicatorsApi = {
  getAll: (): Promise<RiskIndicator[]> =>
    axiosInstance.get('/risk-indicators').then(r => r.data),

  getById: (id: number): Promise<RiskIndicator> =>
    axiosInstance.get(`/risk-indicators/${id}`).then(r => r.data),

  getByClaim: (claimId: string): Promise<RiskIndicator[]> =>
    axiosInstance.get(`/risk-indicators/claim/${claimId}`).then(r => r.data),

  getBySeverity: (severity: string): Promise<RiskIndicator[]> =>
    axiosInstance.get(`/risk-indicators/severity/${severity}`).then(r => r.data),

  create: (data: CreateRiskIndicatorRequest): Promise<RiskIndicator> =>
    axiosInstance.post('/risk-indicators', data).then(r => r.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/risk-indicators/${id}`).then(r => r.data),
};
