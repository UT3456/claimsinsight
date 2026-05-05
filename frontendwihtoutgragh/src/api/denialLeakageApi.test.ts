import { vi, describe, it, expect, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./axiosInstance', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getApiErrorMessage: vi.fn(),
}));

import axiosInstance from './axiosInstance';
import { denialPatternsApi, leakageFlagsApi } from './denialLeakageApi';

const get  = axiosInstance.get    as Mock;
const post = axiosInstance.post   as Mock;
const del  = axiosInstance.delete as Mock;

beforeEach(() => { vi.clearAllMocks(); });

const pattern: import('./denialLeakageApi').DenialPattern = {
  patternId: 1, claimId: 'CLM-001', denialCode: 'CO-4',
  reason: 'Service not covered', occurrenceDate: '2026-01-01',
};

const flag: import('./denialLeakageApi').LeakageFlag = {
  leakageId: 2, claimId: 'CLM-001', leakageType: 'Overpayment',
  estimatedLoss: 3000, identifiedDate: '2026-01-01',
};

// ── denialPatternsApi ─────────────────────────────────────────────────────────
describe('denialPatternsApi.getAll()', () => {
  it('GETs /denial-patterns', async () => {
    get.mockResolvedValueOnce({ data: [pattern] });
    const result = await denialPatternsApi.getAll();
    expect(get).toHaveBeenCalledWith('/denial-patterns');
    expect(result).toEqual([pattern]);
  });
});

describe('denialPatternsApi.getById()', () => {
  it('GETs /denial-patterns/:id', async () => {
    get.mockResolvedValueOnce({ data: pattern });
    await denialPatternsApi.getById(1);
    expect(get).toHaveBeenCalledWith('/denial-patterns/1');
  });
});

describe('denialPatternsApi.getByClaim()', () => {
  it('GETs /denial-patterns/claim/:claimId', async () => {
    get.mockResolvedValueOnce({ data: [pattern] });
    await denialPatternsApi.getByClaim('CLM-001');
    expect(get).toHaveBeenCalledWith('/denial-patterns/claim/CLM-001');
  });
});

describe('denialPatternsApi.getByCode()', () => {
  it('GETs /denial-patterns/code/:code', async () => {
    get.mockResolvedValueOnce({ data: [pattern] });
    await denialPatternsApi.getByCode('CO-4');
    expect(get).toHaveBeenCalledWith('/denial-patterns/code/CO-4');
  });
});

describe('denialPatternsApi.create()', () => {
  it('POSTs to /denial-patterns', async () => {
    const req = { claimId: 'CLM-001', denialCode: 'CO-4', reason: 'Not covered', occurrenceDate: '2026-01-01' };
    post.mockResolvedValueOnce({ data: pattern });
    const result = await denialPatternsApi.create(req);
    expect(post).toHaveBeenCalledWith('/denial-patterns', req);
    expect(result.denialCode).toBe('CO-4');
  });
});

describe('denialPatternsApi.delete()', () => {
  it('DELETEs /denial-patterns/:id', async () => {
    del.mockResolvedValueOnce({ data: undefined });
    await denialPatternsApi.delete(1);
    expect(del).toHaveBeenCalledWith('/denial-patterns/1');
  });
});

// ── leakageFlagsApi ───────────────────────────────────────────────────────────
describe('leakageFlagsApi.getAll()', () => {
  it('GETs /leakage-flags', async () => {
    get.mockResolvedValueOnce({ data: [flag] });
    const result = await leakageFlagsApi.getAll();
    expect(get).toHaveBeenCalledWith('/leakage-flags');
    expect(result).toEqual([flag]);
  });
});

describe('leakageFlagsApi.getByClaim()', () => {
  it('GETs /leakage-flags/claim/:claimId', async () => {
    get.mockResolvedValueOnce({ data: [flag] });
    await leakageFlagsApi.getByClaim('CLM-001');
    expect(get).toHaveBeenCalledWith('/leakage-flags/claim/CLM-001');
  });
});

describe('leakageFlagsApi.getByType()', () => {
  it('GETs /leakage-flags/type/:type', async () => {
    get.mockResolvedValueOnce({ data: [flag] });
    await leakageFlagsApi.getByType('Overpayment');
    expect(get).toHaveBeenCalledWith('/leakage-flags/type/Overpayment');
  });
});

describe('leakageFlagsApi.create()', () => {
  it('POSTs to /leakage-flags', async () => {
    const req = { claimId: 'CLM-001', leakageType: 'Overpayment', estimatedLoss: 3000, identifiedDate: '2026-01-01' };
    post.mockResolvedValueOnce({ data: flag });
    const result = await leakageFlagsApi.create(req);
    expect(post).toHaveBeenCalledWith('/leakage-flags', req);
    expect(result.estimatedLoss).toBe(3000);
  });
});

describe('leakageFlagsApi.delete()', () => {
  it('DELETEs /leakage-flags/:id', async () => {
    del.mockResolvedValueOnce({ data: undefined });
    await leakageFlagsApi.delete(2);
    expect(del).toHaveBeenCalledWith('/leakage-flags/2');
  });
});
