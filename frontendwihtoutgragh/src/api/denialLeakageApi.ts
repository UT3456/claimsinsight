import axiosInstance from './axiosInstance';

// ── Denial Pattern ────────────────────────────────────────────────────────────

export interface DenialPattern {
  patternId: number;
  claimId: string;
  denialCode: string;
  reason: string;
  occurrenceDate: string;
}

export interface CreateDenialPatternRequest {
  claimId: string;
  denialCode: string;
  reason: string;
  occurrenceDate: string;
}

// ── Leakage Flag ──────────────────────────────────────────────────────────────

export interface LeakageFlag {
  leakageId: number;
  claimId: string;
  leakageType: string;
  estimatedLoss: number;
  identifiedDate: string;
}

export interface CreateLeakageFlagRequest {
  claimId: string;
  leakageType: string;
  estimatedLoss: number;
  identifiedDate: string;
}

export const LEAKAGE_TYPES = ['Overpayment', 'Delay', 'Error'] as const;

export const denialPatternsApi = {
  getAll: (): Promise<DenialPattern[]> =>
    axiosInstance.get('/denial-patterns').then(r => r.data),

  getById: (id: number): Promise<DenialPattern> =>
    axiosInstance.get(`/denial-patterns/${id}`).then(r => r.data),

  getByClaim: (claimId: string): Promise<DenialPattern[]> =>
    axiosInstance.get(`/denial-patterns/claim/${claimId}`).then(r => r.data),

  getByCode: (code: string): Promise<DenialPattern[]> =>
    axiosInstance.get(`/denial-patterns/code/${code}`).then(r => r.data),

  create: (data: CreateDenialPatternRequest): Promise<DenialPattern> =>
    axiosInstance.post('/denial-patterns', data).then(r => r.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/denial-patterns/${id}`).then(r => r.data),
};

export const leakageFlagsApi = {
  getAll: (): Promise<LeakageFlag[]> =>
    axiosInstance.get('/leakage-flags').then(r => r.data),

  getById: (id: number): Promise<LeakageFlag> =>
    axiosInstance.get(`/leakage-flags/${id}`).then(r => r.data),

  getByClaim: (claimId: string): Promise<LeakageFlag[]> =>
    axiosInstance.get(`/leakage-flags/claim/${claimId}`).then(r => r.data),

  getByType: (type: string): Promise<LeakageFlag[]> =>
    axiosInstance.get(`/leakage-flags/type/${type}`).then(r => r.data),

  create: (data: CreateLeakageFlagRequest): Promise<LeakageFlag> =>
    axiosInstance.post('/leakage-flags', data).then(r => r.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/leakage-flags/${id}`).then(r => r.data),
};
