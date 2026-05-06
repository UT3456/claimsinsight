import { vi, describe, it, expect, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./axiosInstance', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
  getApiErrorMessage: vi.fn(),
}));

import axiosInstance from './axiosInstance';
import { investigationsApi, type Investigation } from './investigationsApi';

const get   = axiosInstance.get    as Mock;
const post  = axiosInstance.post   as Mock;
const patch = axiosInstance.patch  as Mock;
const del   = axiosInstance.delete as Mock;

beforeEach(() => { vi.clearAllMocks(); });

const inv: Investigation = {
  investigationId: 1,
  claimId: 'CLM-2026-PROP-007',
  riskScoreId: 42,
  status: 'NEW',
  assignedTo: null,
  openedBy: 'priya_fraud',
  openedAt: '2026-04-06T10:30:00',
  closedAt: null,
  notes: 'Auto-escalation from risk score 94.',
};

// ── open() — the SIU escalation entry point ────────────────────────────────
describe('investigationsApi.open()', () => {
  it('POSTs to /investigations with claimId, riskScoreId, and notes', async () => {
    post.mockResolvedValueOnce({ data: inv });
    const result = await investigationsApi.open({
      claimId: 'CLM-2026-PROP-007',
      riskScoreId: 42,
      notes: 'Auto-escalation from risk score 94.',
    });
    expect(post).toHaveBeenCalledWith('/investigations', {
      claimId: 'CLM-2026-PROP-007',
      riskScoreId: 42,
      notes: 'Auto-escalation from risk score 94.',
    });
    expect(result.investigationId).toBe(1);
    expect(result.status).toBe('NEW');
  });

  it('supports a minimal payload with just claimId', async () => {
    post.mockResolvedValueOnce({ data: { ...inv, riskScoreId: null, notes: null } });
    await investigationsApi.open({ claimId: 'CLM-2026-LIAB-001' });
    expect(post).toHaveBeenCalledWith('/investigations', { claimId: 'CLM-2026-LIAB-001' });
  });

  it('propagates 409 conflict so the page can show the duplicate-escalation toast', async () => {
    const err = Object.assign(new Error('Conflict'), {
      response: { status: 409 },
      userMessage: 'Claim CLM-X already has an open investigation.',
    });
    post.mockRejectedValueOnce(err);
    await expect(investigationsApi.open({ claimId: 'CLM-X' })).rejects.toMatchObject({
      response: { status: 409 },
    });
  });
});

// ── list / filters ─────────────────────────────────────────────────────────
describe('investigationsApi.getAll()', () => {
  it('GETs /investigations with no params when status is omitted', async () => {
    get.mockResolvedValueOnce({ data: [inv] });
    const result = await investigationsApi.getAll();
    expect(get).toHaveBeenCalledWith('/investigations', { params: {} });
    expect(result).toHaveLength(1);
  });

  it('GETs /investigations?status=NEW when filtering for new escalations', async () => {
    get.mockResolvedValueOnce({ data: [inv] });
    await investigationsApi.getAll('NEW');
    expect(get).toHaveBeenCalledWith('/investigations', { params: { status: 'NEW' } });
  });

  it('GETs /investigations?status=UNDER_REVIEW for active investigations', async () => {
    get.mockResolvedValueOnce({ data: [] });
    await investigationsApi.getAll('UNDER_REVIEW');
    expect(get).toHaveBeenCalledWith('/investigations', { params: { status: 'UNDER_REVIEW' } });
  });
});

describe('investigationsApi.getById() / getByClaim()', () => {
  it('GETs /investigations/:id', async () => {
    get.mockResolvedValueOnce({ data: inv });
    const result = await investigationsApi.getById(1);
    expect(get).toHaveBeenCalledWith('/investigations/1');
    expect(result.investigationId).toBe(1);
  });

  it('GETs /investigations/claim/:claimId for the case history of a claim', async () => {
    get.mockResolvedValueOnce({ data: [inv] });
    const result = await investigationsApi.getByClaim('CLM-2026-PROP-007');
    expect(get).toHaveBeenCalledWith('/investigations/claim/CLM-2026-PROP-007');
    expect(result).toHaveLength(1);
  });
});

// ── update / close ─────────────────────────────────────────────────────────
describe('investigationsApi.update()', () => {
  it('PATCHes /investigations/:id with a status change to CLOSED', async () => {
    const closed = { ...inv, status: 'CLOSED' as const, closedAt: '2026-05-01T16:00:00' };
    patch.mockResolvedValueOnce({ data: closed });
    const result = await investigationsApi.update(1, { status: 'CLOSED' });
    expect(patch).toHaveBeenCalledWith('/investigations/1', { status: 'CLOSED' });
    expect(result.status).toBe('CLOSED');
    expect(result.closedAt).toBeTruthy();
  });

  it('PATCHes with an assigneeTo update', async () => {
    const assigned = { ...inv, assignedTo: 'fraud_manager_jim', status: 'UNDER_REVIEW' as const };
    patch.mockResolvedValueOnce({ data: assigned });
    await investigationsApi.update(1, { status: 'UNDER_REVIEW', assignedTo: 'fraud_manager_jim' });
    expect(patch).toHaveBeenCalledWith('/investigations/1', {
      status: 'UNDER_REVIEW', assignedTo: 'fraud_manager_jim',
    });
  });
});

describe('investigationsApi.delete()', () => {
  it('DELETEs /investigations/:id and resolves to undefined', async () => {
    del.mockResolvedValueOnce({ data: { id: 1, deleted: true } });
    const result = await investigationsApi.delete(1);
    expect(del).toHaveBeenCalledWith('/investigations/1');
    expect(result).toBeUndefined();
  });
});
