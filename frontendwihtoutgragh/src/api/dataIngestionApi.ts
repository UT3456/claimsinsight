import axiosInstance from './axiosInstance';

export interface DataFeed {
  feedId: number;
  feedType: string;
  sourceSystem: string;
  status: string;
  lastSyncDate: string | null;
  createdDate: string;
}

export interface RawClaim {
  rawId: number;
  claimId: string;
  payloadJson: string;
  ingestedDate: string;
}

export interface CreateFeedRequest {
  feedType: string;
  sourceSystem: string;
  status: string;
}

export interface IngestRequest {
  claimId: string;
  feedId: number;
  payloadJson: string;
}

export const FEED_TYPES   = ['CLAIM', 'POLICY', 'PAYMENT', 'RESERVE'] as const;
export const FEED_STATUSES = ['ACTIVE', 'INACTIVE', 'FAILED'] as const;

export const feedsApi = {
  getAll: (): Promise<DataFeed[]> =>
    axiosInstance.get('/feeds').then(r => r.data),

  getById: (id: number): Promise<DataFeed> =>
    axiosInstance.get(`/feeds/${id}`).then(r => r.data),

  create: (data: CreateFeedRequest): Promise<DataFeed> =>
    axiosInstance.post('/feeds', data).then(r => r.data),

  updateStatus: (id: number, status: string): Promise<DataFeed> =>
    axiosInstance.put(`/feeds/${id}/status`, { status }).then(r => r.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/feeds/${id}`).then(r => r.data),
};

export const ingestApi = {
  getAll: (): Promise<RawClaim[]> =>
    axiosInstance.get('/ingest/raw-claims').then(r => r.data),

  getByClaim: (claimId: string): Promise<RawClaim[]> =>
    axiosInstance.get(`/ingest/raw-claims/${claimId}`).then(r => r.data),

  getByFeed: (feedId: number): Promise<RawClaim[]> =>
    axiosInstance.get(`/ingest/raw-claims/feed/${feedId}`).then(r => r.data),

  ingest: (data: IngestRequest): Promise<RawClaim> =>
    axiosInstance.post('/ingest', data).then(r => r.data),

  delete: (rawId: number): Promise<void> =>
    axiosInstance.delete(`/ingest/raw-claims/${rawId}`).then(r => r.data),
};
