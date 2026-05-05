import { vi, describe, it, expect, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./axiosInstance', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getApiErrorMessage: vi.fn(),
}));

import axiosInstance from './axiosInstance';
import { adjustersApi, slaApi } from './adjustersApi';

const get  = axiosInstance.get    as Mock;
const post = axiosInstance.post   as Mock;
const del  = axiosInstance.delete as Mock;

beforeEach(() => { vi.clearAllMocks(); });

const perf: import('./adjustersApi').AdjusterPerformance = {
  perfId: 1, adjusterId: 101, claimsHandled: 50, totalDaysTaken: 800,
  avgTat: 16, qualityScore: 90, slaMetCount: 45, slaBreachedCount: 5,
  deniedClaimsCount: 3, errorRate: 0.06, period: '2026-01',
  slaComplianceRate: 0.9, denialRate: 0.06, performanceIndex: 88,
  productivityFlag: 'HIGH', performanceFlag: 'GOOD',
};

const sla: import('./adjustersApi').SlaViolation = {
  violationId: 1, claimId: 200, adjusterId: 101, violationType: 'OVERDUE',
  slaTargetDays: 30, actualDays: 45, violationDate: '2026-01-15',
  daysOverdue: 15, severity: 'MEDIUM', escalated: false,
};

// ── adjustersApi ──────────────────────────────────────────────────────────────
describe('adjustersApi.getAll()', () => {
  it('GETs /adjusters/performance without period param', async () => {
    get.mockResolvedValueOnce({ data: [perf] });
    const result = await adjustersApi.getAll();
    expect(get).toHaveBeenCalledWith('/adjusters/performance', { params: {} });
    expect(result).toEqual([perf]);
  });

  it('includes period param when provided', async () => {
    get.mockResolvedValueOnce({ data: [perf] });
    await adjustersApi.getAll('2026-01');
    expect(get).toHaveBeenCalledWith('/adjusters/performance', { params: { period: '2026-01' } });
  });
});

describe('adjustersApi.getByAdjuster()', () => {
  it('GETs /adjusters/:id/performance', async () => {
    get.mockResolvedValueOnce({ data: [perf] });
    await adjustersApi.getByAdjuster(101);
    expect(get).toHaveBeenCalledWith('/adjusters/101/performance', { params: {} });
  });

  it('includes period param when provided', async () => {
    get.mockResolvedValueOnce({ data: [perf] });
    await adjustersApi.getByAdjuster(101, '2026-01');
    expect(get).toHaveBeenCalledWith('/adjusters/101/performance', { params: { period: '2026-01' } });
  });
});

describe('adjustersApi.getTopPerformers()', () => {
  it('GETs /adjusters/top-performers', async () => {
    get.mockResolvedValueOnce({ data: [perf] });
    await adjustersApi.getTopPerformers();
    expect(get).toHaveBeenCalledWith('/adjusters/top-performers', { params: {} });
  });
});

describe('adjustersApi.create()', () => {
  it('POSTs to /adjusters/performance', async () => {
    const req = {
      adjusterId: 101, claimsHandled: 50, totalDaysTaken: 800,
      slaMetCount: 45, slaBreachedCount: 5, deniedClaimsCount: 3,
      errorRate: 0.06, period: '2026-01',
    };
    post.mockResolvedValueOnce({ data: perf });
    const result = await adjustersApi.create(req);
    expect(post).toHaveBeenCalledWith('/adjusters/performance', req);
    expect(result.adjusterId).toBe(101);
  });
});

describe('adjustersApi.delete()', () => {
  it('DELETEs /adjusters/performance/:perfId', async () => {
    del.mockResolvedValueOnce({ data: undefined });
    await adjustersApi.delete(1);
    expect(del).toHaveBeenCalledWith('/adjusters/performance/1');
  });
});

// ── slaApi ────────────────────────────────────────────────────────────────────
describe('slaApi.getAll()', () => {
  it('GETs /sla-violations', async () => {
    get.mockResolvedValueOnce({ data: [sla] });
    const result = await slaApi.getAll();
    expect(get).toHaveBeenCalledWith('/sla-violations');
    expect(result).toEqual([sla]);
  });
});

describe('slaApi.getByAdjuster()', () => {
  it('GETs /sla-violations/adjuster/:adjusterId', async () => {
    get.mockResolvedValueOnce({ data: [sla] });
    await slaApi.getByAdjuster(101);
    expect(get).toHaveBeenCalledWith('/sla-violations/adjuster/101');
  });
});

describe('slaApi.create()', () => {
  it('POSTs to /sla-violations', async () => {
    const req = {
      claimId: 200, adjusterId: 101, violationType: 'OVERDUE',
      slaTargetDays: 30, actualDays: 45, violationDate: '2026-01-15',
    };
    post.mockResolvedValueOnce({ data: sla });
    const result = await slaApi.create(req);
    expect(post).toHaveBeenCalledWith('/sla-violations', req);
    expect(result.daysOverdue).toBe(15);
  });
});
