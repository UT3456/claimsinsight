import { vi, describe, it, expect, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./axiosInstance', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getApiErrorMessage: vi.fn(),
}));

import axiosInstance from './axiosInstance';
import { claimsApi } from './claimsApi';

const get    = axiosInstance.get    as Mock;
const post   = axiosInstance.post   as Mock;
const put    = axiosInstance.put    as Mock;
const del    = axiosInstance.delete as Mock;

beforeEach(() => { vi.clearAllMocks(); });

const kpi = { kpiId: 1, claimId: 'CLM-001', metricName: 'TAT', metricValue: 25, metricDate: '2026-01-01' };

// ── KPI endpoints ─────────────────────────────────────────────────────────────
describe('claimsApi.getAll()', () => {
  it('GETs /kpis and returns the list', async () => {
    get.mockResolvedValueOnce({ data: [kpi] });
    const result = await claimsApi.getAll();
    expect(get).toHaveBeenCalledWith('/kpis');
    expect(result).toEqual([kpi]);
  });
});

describe('claimsApi.getById()', () => {
  it('GETs /kpis/:id', async () => {
    get.mockResolvedValueOnce({ data: kpi });
    const result = await claimsApi.getById(1);
    expect(get).toHaveBeenCalledWith('/kpis/1');
    expect(result).toEqual(kpi);
  });
});

describe('claimsApi.getByClaim()', () => {
  it('GETs /kpis/claim/:claimId', async () => {
    get.mockResolvedValueOnce({ data: [kpi] });
    const result = await claimsApi.getByClaim('CLM-001');
    expect(get).toHaveBeenCalledWith('/kpis/claim/CLM-001');
    expect(result).toEqual([kpi]);
  });
});

describe('claimsApi.getByMetric()', () => {
  it('GETs /kpis/metric/:name', async () => {
    get.mockResolvedValueOnce({ data: [kpi] });
    await claimsApi.getByMetric('TAT');
    expect(get).toHaveBeenCalledWith('/kpis/metric/TAT');
  });
});

describe('claimsApi.getByDateRange()', () => {
  it('GETs /kpis/date-range with start and end params', async () => {
    get.mockResolvedValueOnce({ data: [kpi] });
    await claimsApi.getByDateRange('2026-01-01', '2026-01-31');
    expect(get).toHaveBeenCalledWith('/kpis/date-range', { params: { start: '2026-01-01', end: '2026-01-31' } });
  });
});

describe('claimsApi.create()', () => {
  it('POSTs to /kpis with the request body', async () => {
    const req = { claimId: 'CLM-001', metricName: 'TAT', metricValue: 25, metricDate: '2026-01-01' };
    post.mockResolvedValueOnce({ data: kpi });
    const result = await claimsApi.create(req);
    expect(post).toHaveBeenCalledWith('/kpis', req);
    expect(result).toEqual(kpi);
  });
});

describe('claimsApi.delete()', () => {
  it('DELETEs /kpis/:id', async () => {
    del.mockResolvedValueOnce({ data: undefined });
    await claimsApi.delete(1);
    expect(del).toHaveBeenCalledWith('/kpis/1');
  });
});

// ── calculate() — admin "Recalculate KPIs" action ───────────────────────
describe('claimsApi.calculate()', () => {
  it('POSTs /kpis/calculate/:claimId and returns the recomputed KPI rows', async () => {
    const recomputed = [
      { ...kpi, kpiId: 10, metricName: 'TAT' },
      { ...kpi, kpiId: 11, metricName: 'CYCLE_TIME' },
    ];
    post.mockResolvedValueOnce({ data: recomputed });
    const result = await claimsApi.calculate('CLM-2026-AUTO-001');
    expect(post).toHaveBeenCalledWith('/kpis/calculate/CLM-2026-AUTO-001');
    expect(result).toHaveLength(2);
  });
});

// ── Claim Status endpoints ────────────────────────────────────────────────────
describe('claimsApi.getAllClaimStatuses()', () => {
  it('GETs /claim-status', async () => {
    get.mockResolvedValueOnce({ data: { 'CLM-001': 'ACTIVE' } });
    const result = await claimsApi.getAllClaimStatuses();
    expect(get).toHaveBeenCalledWith('/claim-status');
    expect(result).toEqual({ 'CLM-001': 'ACTIVE' });
  });
});

describe('claimsApi.getClaimStatus()', () => {
  it('GETs /claim-status/:claimId', async () => {
    const resp = { claimId: 'CLM-001', status: 'ACTIVE', updatedAt: null };
    get.mockResolvedValueOnce({ data: resp });
    const result = await claimsApi.getClaimStatus('CLM-001');
    expect(get).toHaveBeenCalledWith('/claim-status/CLM-001');
    expect(result.status).toBe('ACTIVE');
  });
});

describe('claimsApi.updateClaimStatus()', () => {
  it('PUTs /claim-status/:claimId with the new status', async () => {
    const resp = { claimId: 'CLM-001', status: 'INACTIVE', updatedAt: '2026-01-01' };
    put.mockResolvedValueOnce({ data: resp });
    const result = await claimsApi.updateClaimStatus('CLM-001', 'INACTIVE');
    expect(put).toHaveBeenCalledWith('/claim-status/CLM-001', { status: 'INACTIVE' });
    expect(result.status).toBe('INACTIVE');
  });
});
