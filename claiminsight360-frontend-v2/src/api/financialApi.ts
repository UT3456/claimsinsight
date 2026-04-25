import axiosInstance from './axiosInstance';

// ── Cost ─────────────────────────────────────────────────────────────────────

export interface ClaimCost {
  costId: number;
  claimId: string;
  costType: string;
  amount: number;
  costDate: string;
}

export interface CostSummary {
  costType: string;
  total: number;
}

export interface CreateCostRequest {
  claimId: string;
  costType: string;
  amount: number;
  costDate: string;
}

export const COST_TYPES = ['MEDICAL', 'LEGAL', 'REPAIR', 'SETTLEMENT'] as const;
export type CostType = typeof COST_TYPES[number];

export const costsApi = {
  getAll: (): Promise<ClaimCost[]> =>
    axiosInstance.get('/costs').then(r => r.data),

  getById: (id: number): Promise<ClaimCost> =>
    axiosInstance.get(`/costs/${id}`).then(r => r.data),

  getByClaim: (claimId: string): Promise<ClaimCost[]> =>
    axiosInstance.get(`/costs/claim/${claimId}`).then(r => r.data),

  getSummaryByType: (): Promise<CostSummary[]> =>
    axiosInstance.get('/costs/summary/by-type').then(r => r.data),

  getHighestCostClaim: (): Promise<{ claimId: string; total: number }> =>
    axiosInstance.get('/costs/analytics/highest-cost-claim').then(r => r.data),

  create: (data: CreateCostRequest): Promise<ClaimCost> =>
    axiosInstance.post('/costs', data).then(r => r.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/costs/${id}`).then(r => r.data),
};

// ── Reserve ───────────────────────────────────────────────────────────────────

export interface ClaimReserve {
  reserveId: number;
  claimId: string;
  reserveAmount: number;
  updatedDate: string;
}

export interface CreateReserveRequest {
  claimId: string;
  reserveAmount: number;
  updatedDate: string;
}

export const reservesApi = {
  getAll: (): Promise<ClaimReserve[]> =>
    axiosInstance.get('/reserves').then(r => r.data),

  getById: (id: number): Promise<ClaimReserve> =>
    axiosInstance.get(`/reserves/${id}`).then(r => r.data),

  getByClaim: (claimId: string): Promise<ClaimReserve[]> =>
    axiosInstance.get(`/reserves/claim/${claimId}`).then(r => r.data),

  getTotalAmount: (): Promise<number> =>
    axiosInstance.get('/reserves/analytics/total').then(r => {
      const d = r.data;
      // Backend returns { totalReserveAmount: 0.00 } — extract the number
      if (typeof d === 'number') return d;
      if (d && typeof d === 'object') {
        const val = (d as Record<string, unknown>).totalReserveAmount
                 ?? (d as Record<string, unknown>).total
                 ?? 0;
        return Number(val) || 0;
      }
      return Number(d) || 0;
    }),

  getSummary: (): Promise<Record<string, number>> =>
    axiosInstance.get('/reserves/analytics/summary').then(r => r.data),

  create: (data: CreateReserveRequest): Promise<ClaimReserve> =>
    axiosInstance.post('/reserves', data).then(r => r.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/reserves/${id}`).then(r => r.data),
};

// ── Aging ─────────────────────────────────────────────────────────────────────

export interface AgingRecord {
  agingId: number;
  claimId: string;
  agingDays: number;
  agingBucket: string;
}

export interface AgingDistribution {
  agingBucket: string;
  count: number;
}

export interface CreateAgingRequest {
  claimId: string;
  agingDays: number;
  agingBucket: string;
}

export const AGING_BUCKETS = ['BUCKET_0_30', 'BUCKET_31_60', 'BUCKET_61_90', 'BUCKET_90_PLUS'] as const;
export type AgingBucket = typeof AGING_BUCKETS[number];

export const AGING_BUCKET_LABELS: Record<string, string> = {
  BUCKET_0_30:  '0–30 days',
  BUCKET_31_60: '31–60 days',
  BUCKET_61_90: '61–90 days',
  BUCKET_90_PLUS: '90+ days',
};

export const agingApi = {
  getAll: (): Promise<AgingRecord[]> =>
    axiosInstance.get('/aging').then(r => r.data),

  getById: (id: number): Promise<AgingRecord> =>
    axiosInstance.get(`/aging/${id}`).then(r => r.data),

  getByClaim: (claimId: string): Promise<AgingRecord[]> =>
    axiosInstance.get(`/aging/claim/${claimId}`).then(r => r.data),

  getByBucket: (bucket: string): Promise<AgingRecord[]> =>
    axiosInstance.get(`/aging/bucket/${bucket}`).then(r => r.data),

  getDistribution: (): Promise<AgingDistribution[]> =>
    axiosInstance.get('/aging/analytics/distribution').then(r => r.data),

  create: (data: CreateAgingRequest): Promise<AgingRecord> =>
    axiosInstance.post('/aging', data).then(r => r.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/aging/${id}`).then(r => r.data),
};
