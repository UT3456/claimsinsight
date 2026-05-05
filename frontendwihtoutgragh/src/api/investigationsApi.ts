import axiosInstance from './axiosInstance';

/** Lifecycle of a SIU fraud investigation. */
export type InvestigationStatus = 'NEW' | 'UNDER_REVIEW' | 'CLOSED';

export interface Investigation {
  investigationId: number;
  claimId: string;
  riskScoreId: number | null;
  status: InvestigationStatus;
  assignedTo: string | null;
  openedBy: string;
  openedAt: string;
  closedAt: string | null;
  notes: string | null;
}

export interface CreateInvestigationRequest {
  claimId: string;
  riskScoreId?: number;
  notes?: string;
}

export interface UpdateInvestigationRequest {
  status?: InvestigationStatus;
  assignedTo?: string;
  notes?: string;
}

export const investigationsApi = {
  /** Open a new investigation — fired by the "Escalate SIU" button. */
  open: (request: CreateInvestigationRequest): Promise<Investigation> =>
    axiosInstance.post('/investigations', request).then(r => r.data),

  getAll: (status?: InvestigationStatus): Promise<Investigation[]> =>
    axiosInstance
      .get('/investigations', { params: status ? { status } : {} })
      .then(r => r.data),

  getById: (id: number): Promise<Investigation> =>
    axiosInstance.get(`/investigations/${id}`).then(r => r.data),

  getByClaim: (claimId: string): Promise<Investigation[]> =>
    axiosInstance.get(`/investigations/claim/${claimId}`).then(r => r.data),

  update: (id: number, request: UpdateInvestigationRequest): Promise<Investigation> =>
    axiosInstance.patch(`/investigations/${id}`, request).then(r => r.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/investigations/${id}`).then(() => undefined),
};
