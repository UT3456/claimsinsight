import { vi, describe, it, expect, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./axiosInstance', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getApiErrorMessage: vi.fn(),
}));

import axiosInstance from './axiosInstance';
import { riskScoresApi, riskIndicatorsApi } from './fraudRiskApi';

const get  = axiosInstance.get    as Mock;
const post = axiosInstance.post   as Mock;
const del  = axiosInstance.delete as Mock;

beforeEach(() => { vi.clearAllMocks(); });

const score: import('./fraudRiskApi').RiskScore = {
  scoreId: 1, claimId: 'CLM-001', scoreValue: 82.5, computedDate: '2026-01-01',
};

const indicator: import('./fraudRiskApi').RiskIndicator = {
  indicatorId: 5, claimId: 'CLM-001', indicatorType: 'HighCost',
  severity: 'HIGH', triggeredDate: '2026-01-01',
};

// ── riskScoresApi ─────────────────────────────────────────────────────────────
describe('riskScoresApi.getAll()', () => {
  it('GETs /risk-scores', async () => {
    get.mockResolvedValueOnce({ data: [score] });
    const result = await riskScoresApi.getAll();
    expect(get).toHaveBeenCalledWith('/risk-scores');
    expect(result).toEqual([score]);
  });
});

describe('riskScoresApi.getById()', () => {
  it('GETs /risk-scores/:id', async () => {
    get.mockResolvedValueOnce({ data: score });
    await riskScoresApi.getById(1);
    expect(get).toHaveBeenCalledWith('/risk-scores/1');
  });
});

describe('riskScoresApi.getByClaim()', () => {
  it('GETs /risk-scores/claim/:claimId', async () => {
    get.mockResolvedValueOnce({ data: [score] });
    await riskScoresApi.getByClaim('CLM-001');
    expect(get).toHaveBeenCalledWith('/risk-scores/claim/CLM-001');
  });
});

describe('riskScoresApi.getAboveThreshold()', () => {
  it('GETs /risk-scores/threshold/:threshold', async () => {
    get.mockResolvedValueOnce({ data: [score] });
    await riskScoresApi.getAboveThreshold(75);
    expect(get).toHaveBeenCalledWith('/risk-scores/threshold/75');
  });

  it('returns only scores that are above threshold (from mocked data)', async () => {
    get.mockResolvedValueOnce({ data: [score] });
    const result = await riskScoresApi.getAboveThreshold(75);
    expect(result[0].scoreValue).toBeGreaterThan(75);
  });
});

describe('riskScoresApi.create()', () => {
  it('POSTs to /risk-scores', async () => {
    const req = { claimId: 'CLM-001', scoreValue: 82.5, computedDate: '2026-01-01' };
    post.mockResolvedValueOnce({ data: score });
    const result = await riskScoresApi.create(req);
    expect(post).toHaveBeenCalledWith('/risk-scores', req);
    expect(result).toEqual(score);
  });
});

describe('riskScoresApi.delete()', () => {
  it('DELETEs /risk-scores/:id', async () => {
    del.mockResolvedValueOnce({ data: undefined });
    await riskScoresApi.delete(1);
    expect(del).toHaveBeenCalledWith('/risk-scores/1');
  });
});

// ── riskIndicatorsApi ─────────────────────────────────────────────────────────
describe('riskIndicatorsApi.getAll()', () => {
  it('GETs /risk-indicators', async () => {
    get.mockResolvedValueOnce({ data: [indicator] });
    const result = await riskIndicatorsApi.getAll();
    expect(get).toHaveBeenCalledWith('/risk-indicators');
    expect(result).toEqual([indicator]);
  });
});

describe('riskIndicatorsApi.getByClaim()', () => {
  it('GETs /risk-indicators/claim/:claimId', async () => {
    get.mockResolvedValueOnce({ data: [indicator] });
    await riskIndicatorsApi.getByClaim('CLM-001');
    expect(get).toHaveBeenCalledWith('/risk-indicators/claim/CLM-001');
  });
});

describe('riskIndicatorsApi.getBySeverity()', () => {
  it('GETs /risk-indicators/severity/:severity', async () => {
    get.mockResolvedValueOnce({ data: [indicator] });
    await riskIndicatorsApi.getBySeverity('HIGH');
    expect(get).toHaveBeenCalledWith('/risk-indicators/severity/HIGH');
  });
});

describe('riskIndicatorsApi.create()', () => {
  it('POSTs to /risk-indicators with the request body', async () => {
    const req = { claimId: 'CLM-001', indicatorType: 'HighCost', severity: 'HIGH', triggeredDate: '2026-01-01' };
    post.mockResolvedValueOnce({ data: indicator });
    const result = await riskIndicatorsApi.create(req);
    expect(post).toHaveBeenCalledWith('/risk-indicators', req);
    expect(result.indicatorType).toBe('HighCost');
  });
});

describe('riskIndicatorsApi.delete()', () => {
  it('DELETEs /risk-indicators/:id', async () => {
    del.mockResolvedValueOnce({ data: undefined });
    await riskIndicatorsApi.delete(5);
    expect(del).toHaveBeenCalledWith('/risk-indicators/5');
  });
});
