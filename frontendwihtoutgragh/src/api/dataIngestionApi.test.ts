import { vi, describe, it, expect, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./axiosInstance', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getApiErrorMessage: vi.fn(),
}));

import axiosInstance from './axiosInstance';
import { feedsApi, ingestApi } from './dataIngestionApi';

const get  = axiosInstance.get    as Mock;
const post = axiosInstance.post   as Mock;
const put  = axiosInstance.put    as Mock;
const del  = axiosInstance.delete as Mock;

beforeEach(() => { vi.clearAllMocks(); });

const feed: import('./dataIngestionApi').DataFeed = {
  feedId: 1, feedType: 'CLAIM', sourceSystem: 'CoreSystem',
  status: 'ACTIVE', lastSyncDate: '2026-01-01T00:00:00', createdDate: '2026-01-01T00:00:00',
};

const raw: import('./dataIngestionApi').RawClaim = {
  rawId: 10, claimId: 'CLM-001', feedId: 1, feedType: 'CLAIM',
  payloadJson: '{}', ingestedDate: '2026-01-01T00:00:00',
};

// ── feedsApi ──────────────────────────────────────────────────────────────────
describe('feedsApi.getAll()', () => {
  it('GETs /feeds', async () => {
    get.mockResolvedValueOnce({ data: [feed] });
    const result = await feedsApi.getAll();
    expect(get).toHaveBeenCalledWith('/feeds');
    expect(result).toEqual([feed]);
  });
});

describe('feedsApi.getById()', () => {
  it('GETs /feeds/:id', async () => {
    get.mockResolvedValueOnce({ data: feed });
    await feedsApi.getById(1);
    expect(get).toHaveBeenCalledWith('/feeds/1');
  });
});

describe('feedsApi.create()', () => {
  it('POSTs to /feeds with request body', async () => {
    const req = { feedType: 'CLAIM', sourceSystem: 'CoreSystem', status: 'ACTIVE' };
    post.mockResolvedValueOnce({ data: feed });
    const result = await feedsApi.create(req);
    expect(post).toHaveBeenCalledWith('/feeds', req);
    expect(result).toEqual(feed);
  });
});

describe('feedsApi.updateStatus()', () => {
  it('PUTs /feeds/:id/status', async () => {
    put.mockResolvedValueOnce({ data: { ...feed, status: 'INACTIVE' } });
    const result = await feedsApi.updateStatus(1, 'INACTIVE');
    expect(put).toHaveBeenCalledWith('/feeds/1/status', { status: 'INACTIVE' });
    expect(result.status).toBe('INACTIVE');
  });
});

describe('feedsApi.delete()', () => {
  it('DELETEs /feeds/:id', async () => {
    del.mockResolvedValueOnce({ data: undefined });
    await feedsApi.delete(1);
    expect(del).toHaveBeenCalledWith('/feeds/1');
  });
});

// ── ingestApi ─────────────────────────────────────────────────────────────────
describe('ingestApi.getAll()', () => {
  it('GETs /ingest/raw-claims', async () => {
    get.mockResolvedValueOnce({ data: [raw] });
    const result = await ingestApi.getAll();
    expect(get).toHaveBeenCalledWith('/ingest/raw-claims');
    expect(result).toEqual([raw]);
  });
});

describe('ingestApi.getByClaim()', () => {
  it('GETs /ingest/raw-claims/:claimId', async () => {
    get.mockResolvedValueOnce({ data: [raw] });
    await ingestApi.getByClaim('CLM-001');
    expect(get).toHaveBeenCalledWith('/ingest/raw-claims/CLM-001');
  });
});

describe('ingestApi.getByFeed()', () => {
  it('GETs /ingest/raw-claims/feed/:feedId', async () => {
    get.mockResolvedValueOnce({ data: [raw] });
    await ingestApi.getByFeed(1);
    expect(get).toHaveBeenCalledWith('/ingest/raw-claims/feed/1');
  });
});

describe('ingestApi.ingest()', () => {
  it('POSTs to /ingest with request body', async () => {
    const req = { claimId: 'CLM-001', feedId: 1, payloadJson: '{"claimAmount":5000}' };
    post.mockResolvedValueOnce({ data: raw });
    const result = await ingestApi.ingest(req);
    expect(post).toHaveBeenCalledWith('/ingest', req);
    expect(result).toEqual(raw);
  });
});

describe('ingestApi.delete()', () => {
  it('DELETEs /ingest/raw-claims/:rawId', async () => {
    del.mockResolvedValueOnce({ data: undefined });
    await ingestApi.delete(10);
    expect(del).toHaveBeenCalledWith('/ingest/raw-claims/10');
  });
});
