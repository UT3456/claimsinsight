import { vi, describe, it, expect, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./axiosInstance', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getApiErrorMessage: vi.fn(),
}));

import axiosInstance from './axiosInstance';
import { costsApi, reservesApi, agingApi } from './financialApi';

const get  = axiosInstance.get    as Mock;
const post = axiosInstance.post   as Mock;
const del  = axiosInstance.delete as Mock;

beforeEach(() => { vi.clearAllMocks(); });

const cost: import('./financialApi').ClaimCost = {
  costId: 1, claimId: 'CLM-001', costType: 'SETTLEMENT', amount: 10000, costDate: '2026-01-01',
};
const reserve: import('./financialApi').ClaimReserve = {
  reserveId: 1, claimId: 'CLM-001', reserveAmount: 12000, updatedDate: '2026-01-01',
};
const aging: import('./financialApi').AgingRecord = {
  agingId: 1, claimId: 'CLM-001', agingDays: 95, agingBucket: 'BUCKET_90_PLUS',
};

// ── costsApi ──────────────────────────────────────────────────────────────────
describe('costsApi.getAll()', () => {
  it('GETs /costs', async () => {
    get.mockResolvedValueOnce({ data: [cost] });
    const result = await costsApi.getAll();
    expect(get).toHaveBeenCalledWith('/costs');
    expect(result).toEqual([cost]);
  });
});

describe('costsApi.getById()', () => {
  it('GETs /costs/:id', async () => {
    get.mockResolvedValueOnce({ data: cost });
    await costsApi.getById(1);
    expect(get).toHaveBeenCalledWith('/costs/1');
  });
});

describe('costsApi.getByClaim()', () => {
  it('GETs /costs/claim/:claimId', async () => {
    get.mockResolvedValueOnce({ data: [cost] });
    await costsApi.getByClaim('CLM-001');
    expect(get).toHaveBeenCalledWith('/costs/claim/CLM-001');
  });
});

describe('costsApi.getSummaryByType()', () => {
  it('GETs /costs/summary/by-type', async () => {
    get.mockResolvedValueOnce({ data: [{ costType: 'SETTLEMENT', total: 10000 }] });
    const result = await costsApi.getSummaryByType();
    expect(get).toHaveBeenCalledWith('/costs/summary/by-type');
    expect(result[0].total).toBe(10000);
  });
});

describe('costsApi.getHighestCostClaim()', () => {
  it('GETs /costs/analytics/highest-cost-claim', async () => {
    get.mockResolvedValueOnce({ data: { claimId: 'CLM-001', total: 50000 } });
    const result = await costsApi.getHighestCostClaim();
    expect(get).toHaveBeenCalledWith('/costs/analytics/highest-cost-claim');
    expect(result.claimId).toBe('CLM-001');
  });
});

describe('costsApi.create()', () => {
  it('POSTs to /costs', async () => {
    const req = { claimId: 'CLM-001', costType: 'SETTLEMENT', amount: 10000, costDate: '2026-01-01' };
    post.mockResolvedValueOnce({ data: cost });
    await costsApi.create(req);
    expect(post).toHaveBeenCalledWith('/costs', req);
  });
});

describe('costsApi.delete()', () => {
  it('DELETEs /costs/:id', async () => {
    del.mockResolvedValueOnce({ data: undefined });
    await costsApi.delete(1);
    expect(del).toHaveBeenCalledWith('/costs/1');
  });
});

// ── reservesApi ───────────────────────────────────────────────────────────────
describe('reservesApi.getAll()', () => {
  it('GETs /reserves', async () => {
    get.mockResolvedValueOnce({ data: [reserve] });
    const result = await reservesApi.getAll();
    expect(get).toHaveBeenCalledWith('/reserves');
    expect(result).toEqual([reserve]);
  });
});

describe('reservesApi.getTotalAmount() — data shape handling', () => {
  it('returns the number directly when backend sends a plain number', async () => {
    get.mockResolvedValueOnce({ data: 99000 });
    const total = await reservesApi.getTotalAmount();
    expect(total).toBe(99000);
  });

  it('extracts totalReserveAmount from the backend object envelope', async () => {
    get.mockResolvedValueOnce({ data: { totalReserveAmount: 120000.50 } });
    const total = await reservesApi.getTotalAmount();
    expect(total).toBe(120000.50);
  });

  it('falls back to the "total" key if totalReserveAmount is absent', async () => {
    get.mockResolvedValueOnce({ data: { total: 55000 } });
    const total = await reservesApi.getTotalAmount();
    expect(total).toBe(55000);
  });

  it('returns 0 when object has no known key', async () => {
    get.mockResolvedValueOnce({ data: {} });
    const total = await reservesApi.getTotalAmount();
    expect(total).toBe(0);
  });

  it('returns 0 when data is null', async () => {
    get.mockResolvedValueOnce({ data: null });
    const total = await reservesApi.getTotalAmount();
    expect(total).toBe(0);
  });
});

describe('reservesApi.create()', () => {
  it('POSTs to /reserves', async () => {
    const req = { claimId: 'CLM-001', reserveAmount: 12000, updatedDate: '2026-01-01' };
    post.mockResolvedValueOnce({ data: reserve });
    await reservesApi.create(req);
    expect(post).toHaveBeenCalledWith('/reserves', req);
  });
});

// ── agingApi ──────────────────────────────────────────────────────────────────
describe('agingApi.getAll()', () => {
  it('GETs /aging', async () => {
    get.mockResolvedValueOnce({ data: [aging] });
    const result = await agingApi.getAll();
    expect(get).toHaveBeenCalledWith('/aging');
    expect(result).toEqual([aging]);
  });
});

describe('agingApi.getByBucket()', () => {
  it('GETs /aging/bucket/:bucket', async () => {
    get.mockResolvedValueOnce({ data: [aging] });
    await agingApi.getByBucket('BUCKET_90_PLUS');
    expect(get).toHaveBeenCalledWith('/aging/bucket/BUCKET_90_PLUS');
  });
});

describe('agingApi.getDistribution()', () => {
  it('GETs /aging/analytics/distribution', async () => {
    const dist = [{ agingBucket: 'BUCKET_0_30', count: 5 }];
    get.mockResolvedValueOnce({ data: dist });
    const result = await agingApi.getDistribution();
    expect(get).toHaveBeenCalledWith('/aging/analytics/distribution');
    expect(result).toEqual(dist);
  });
});

describe('agingApi.create()', () => {
  it('POSTs to /aging', async () => {
    const req = { claimId: 'CLM-001', agingDays: 95, agingBucket: 'BUCKET_90_PLUS' };
    post.mockResolvedValueOnce({ data: aging });
    await agingApi.create(req);
    expect(post).toHaveBeenCalledWith('/aging', req);
  });
});

describe('agingApi.delete()', () => {
  it('DELETEs /aging/:id', async () => {
    del.mockResolvedValueOnce({ data: undefined });
    await agingApi.delete(1);
    expect(del).toHaveBeenCalledWith('/aging/1');
  });
});
