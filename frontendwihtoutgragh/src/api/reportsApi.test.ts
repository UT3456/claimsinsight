import { vi, describe, it, expect, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./axiosInstance', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getApiErrorMessage: vi.fn(),
}));

import axiosInstance from './axiosInstance';
import { reportsApi } from './reportsApi';

const get  = axiosInstance.get    as Mock;
const post = axiosInstance.post   as Mock;
const del  = axiosInstance.delete as Mock;

beforeEach(() => { vi.clearAllMocks(); });

const report: import('./reportsApi').AnalyticsReport = {
  reportId: 1, scope: 'PRODUCT', scopeValue: 'AUTO',
  metrics: 'TAT,SEVERITY', generatedDate: '2026-01-01',
  generatedBy: 'alice', reportData: '{"tat":25}',
};

// ── standard CRUD ─────────────────────────────────────────────────────────────
describe('reportsApi.getAll()', () => {
  it('GETs /reports', async () => {
    get.mockResolvedValueOnce({ data: [report] });
    const result = await reportsApi.getAll();
    expect(get).toHaveBeenCalledWith('/reports');
    expect(result).toEqual([report]);
  });
});

describe('reportsApi.getById()', () => {
  it('GETs /reports/:id', async () => {
    get.mockResolvedValueOnce({ data: report });
    await reportsApi.getById(1);
    expect(get).toHaveBeenCalledWith('/reports/1');
  });
});

describe('reportsApi.getByScope()', () => {
  it('GETs /reports/scope/:scope', async () => {
    get.mockResolvedValueOnce({ data: [report] });
    await reportsApi.getByScope('PRODUCT');
    expect(get).toHaveBeenCalledWith('/reports/scope/PRODUCT');
  });
});

describe('reportsApi.getByGeneratedBy()', () => {
  it('GETs /reports/generated-by/:user', async () => {
    get.mockResolvedValueOnce({ data: [report] });
    await reportsApi.getByGeneratedBy('alice');
    expect(get).toHaveBeenCalledWith('/reports/generated-by/alice');
  });
});

describe('reportsApi.getByDateRange()', () => {
  it('GETs /reports/date-range with start/end params', async () => {
    get.mockResolvedValueOnce({ data: [report] });
    await reportsApi.getByDateRange('2026-01-01', '2026-01-31');
    expect(get).toHaveBeenCalledWith('/reports/date-range', {
      params: { startDate: '2026-01-01', endDate: '2026-01-31' },
    });
  });
});

describe('reportsApi.getDashboardSummary()', () => {
  it('GETs /reports/analytics/dashboard-summary', async () => {
    get.mockResolvedValueOnce({ data: { totalReports: 5 } });
    const result = await reportsApi.getDashboardSummary();
    expect(get).toHaveBeenCalledWith('/reports/analytics/dashboard-summary');
    expect(result).toEqual({ totalReports: 5 });
  });
});

describe('reportsApi.create()', () => {
  it('POSTs to /reports', async () => {
    const req = {
      scope: 'PRODUCT', scopeValue: 'AUTO', metrics: 'TAT',
      generatedDate: '2026-01-01', generatedBy: 'alice',
    };
    post.mockResolvedValueOnce({ data: report });
    const result = await reportsApi.create(req);
    expect(post).toHaveBeenCalledWith('/reports', req);
    expect(result.scope).toBe('PRODUCT');
  });
});

describe('reportsApi.delete()', () => {
  it('DELETEs /reports/:id', async () => {
    del.mockResolvedValueOnce({ data: undefined });
    await reportsApi.delete(1);
    expect(del).toHaveBeenCalledWith('/reports/1');
  });
});

// ── reportsApi.export() — blob handling ───────────────────────────────────────
describe('reportsApi.export()', () => {
  it('GETs /reports/:id/export with format param and responseType blob', async () => {
    const fakeBlob = new Blob(['%PDF-data'], { type: 'application/pdf' });
    get.mockResolvedValueOnce({
      data: fakeBlob,
      headers: {
        'content-type': 'application/pdf',
        'content-disposition': 'attachment; filename="report-1.pdf"',
      },
    });
    const result = await reportsApi.export(1, 'pdf');
    expect(get).toHaveBeenCalledWith('/reports/1/export', {
      params: { format: 'pdf' },
      responseType: 'blob',
    });
    expect(result.blob).toBeInstanceOf(Blob);
    expect(result.filename).toBe('report-1.pdf');
  });

  it('defaults filename to report-{id}.{format} when no Content-Disposition header', async () => {
    const fakeBlob = new Blob(['claimId,status'], { type: 'text/csv' });
    get.mockResolvedValueOnce({
      data: fakeBlob,
      headers: { 'content-type': 'text/csv' },
    });
    const result = await reportsApi.export(5, 'csv');
    expect(result.filename).toBe('report-5.csv');
  });

  it('uses format as content-type fallback when no Content-Type header (pdf)', async () => {
    const fakeBlob = new Blob(['%PDF'], {});
    get.mockResolvedValueOnce({ data: fakeBlob, headers: {} });
    const result = await reportsApi.export(2, 'pdf');
    expect(result.blob.type).toBe('application/pdf');
  });

  it('uses text/csv as content-type fallback for csv format', async () => {
    const fakeBlob = new Blob(['data'], {});
    get.mockResolvedValueOnce({ data: fakeBlob, headers: {} });
    const result = await reportsApi.export(3, 'csv');
    expect(result.blob.type).toBe('text/csv');
  });
});
