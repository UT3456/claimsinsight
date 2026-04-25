import { useEffect, useReducer, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Alert, Tooltip, Popconfirm, Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { Plus, RefreshCw, Search, Trash2, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

import {
  PageHeader, DataCard, DarkButton, GhostButton, EmptyState,
} from '../../components/ui';
import {
  ingestApi,
  type IngestRequest,
  type RawClaim,
} from '../../api/dataIngestionApi';

const { Text } = Typography;

/**
 * RawClaimsPage — admin utility view of ingested claim payloads.
 * No dedicated reference screen; follows the same admin pattern as FeedsPage.
 */

interface State {
  items: RawClaim[];
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
  searchClaimId: string;
}

type Action =
  | { type: 'START' }
  | { type: 'SUCCESS'; payload: RawClaim[] }
  | { type: 'ERROR'; payload: string }
  | { type: 'OPEN_MODAL' }
  | { type: 'CLOSE_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'SUBMIT_SUCCESS'; payload: RawClaim }
  | { type: 'SUBMIT_ERROR' }
  | { type: 'DELETE'; payload: number }
  | { type: 'SET_SEARCH'; payload: string };

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'START':          return { ...s, loading: true, error: null };
    case 'SUCCESS':        return { ...s, loading: false, items: a.payload };
    case 'ERROR':          return { ...s, loading: false, error: a.payload };
    case 'OPEN_MODAL':     return { ...s, modalOpen: true };
    case 'CLOSE_MODAL':    return { ...s, modalOpen: false, submitting: false };
    case 'SUBMIT_START':   return { ...s, submitting: true };
    case 'SUBMIT_SUCCESS': return { ...s, submitting: false, modalOpen: false, items: [a.payload, ...s.items] };
    case 'SUBMIT_ERROR':   return { ...s, submitting: false };
    case 'DELETE':         return { ...s, items: s.items.filter(i => i.rawId !== a.payload) };
    case 'SET_SEARCH':     return { ...s, searchClaimId: a.payload };
    default:               return s;
  }
}

export default function RawClaimsPage() {
  const navigate = useNavigate();
  const [state, dispatch] = useReducer(reducer, {
    items: [], loading: false, error: null, modalOpen: false, submitting: false, searchClaimId: '',
  });
  const [form] = Form.useForm();

  const load = useCallback(async (claimId?: string) => {
    dispatch({ type: 'START' });
    try {
      const data = claimId?.trim()
        ? await ingestApi.getByClaim(claimId.trim())
        : await ingestApi.getAll();
      dispatch({ type: 'SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'ERROR', payload: 'Failed to load raw claims.' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleIngest = async (values: Omit<IngestRequest, 'feedId'> & { feedId: string }) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const record = await ingestApi.ingest({
        claimId:     values.claimId,
        feedId:      Number(values.feedId),
        payloadJson: values.payloadJson,
      });
      dispatch({ type: 'SUBMIT_SUCCESS', payload: record });
      form.resetFields();
    } catch {
      dispatch({ type: 'SUBMIT_ERROR' });
    }
  };

  const handleDelete = async (id: number) => {
    await ingestApi.delete(id);
    dispatch({ type: 'DELETE', payload: id });
  };

  const columns: ColumnsType<RawClaim> = [
    {
      title: 'Raw ID',
      dataIndex: 'rawId',
      key: 'rawId',
      width: 90,
      render: v => <span style={{ color: 'var(--ci-text-muted)' }}>#{v}</span>,
    },
    {
      title: 'Claim ID',
      dataIndex: 'claimId',
      key: 'claimId',
      width: '22%',
      render: (v: string) => (
        <a onClick={() => navigate(`/claims/${v}`)} style={{ fontWeight: 500, color: 'var(--ci-primary)' }}>
          {v}
        </a>
      ),
    },
    {
      title: 'Payload',
      dataIndex: 'payloadJson',
      key: 'payloadJson',
      ellipsis: true,
      render: v => <Text style={{ fontSize: 11, color: 'var(--ci-text-muted)', fontFamily: 'ui-monospace, SFMono-Regular, monospace' }}>{v}</Text>,
    },
    {
      title: 'Ingested',
      dataIndex: 'ingestedDate',
      key: 'ingestedDate',
      width: 130,
      render: v => <span style={{ color: 'var(--ci-text-secondary)' }}>{v?.replace('T', ' ').slice(0, 16) ?? '—'}</span>,
    },
    {
      title: '',
      key: 'action',
      width: 60,
      align: 'right',
      render: (_, rec) => (
        <Popconfirm title="Delete this raw claim?" onConfirm={() => handleDelete(rec.rawId)}>
          <Tooltip title="Delete">
            <Button type="text" size="small" icon={<Trash2 size={13} strokeWidth={1.6} />} style={{ color: 'var(--ci-text-muted)' }} />
          </Tooltip>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Raw claims"
        subtitle={`${state.items.length.toLocaleString()} ingested payload${state.items.length === 1 ? '' : 's'}`}
        actions={
          <>
            <div style={styles.search}>
              <Search size={12} strokeWidth={1.8} color="var(--ci-text-muted)" />
              <input
                style={styles.searchInput}
                placeholder="Search claim ID"
                value={state.searchClaimId}
                onChange={e => dispatch({ type: 'SET_SEARCH', payload: e.target.value })}
                onKeyDown={e => { if (e.key === 'Enter') load(state.searchClaimId); }}
              />
              {state.searchClaimId && (
                <X
                  size={12} strokeWidth={1.8} color="var(--ci-text-muted)"
                  style={{ cursor: 'pointer' }}
                  onClick={() => { dispatch({ type: 'SET_SEARCH', payload: '' }); load(); }}
                />
              )}
            </div>
            <GhostButton onClick={() => load()} icon={<RefreshCw size={12} strokeWidth={1.8} />}>
              Refresh
            </GhostButton>
            <DarkButton
              onClick={() => dispatch({ type: 'OPEN_MODAL' })}
              icon={<Plus size={12} strokeWidth={2} />}
            >
              Ingest claim
            </DarkButton>
          </>
        }
      />

      {state.error && (
        <Alert type="error" showIcon message={state.error} style={{ marginBottom: 12, borderRadius: 8 }} closable />
      )}

      <DataCard padding={0}>
        {!state.loading && state.items.length === 0 && !state.error ? (
          <EmptyState
            title={state.searchClaimId ? 'No matches' : 'No raw claims yet'}
            description={
              state.searchClaimId
                ? `No ingested payloads for claim “${state.searchClaimId}”.`
                : 'Ingested payloads from your registered feeds will appear here.'
            }
            actions={
              state.searchClaimId
                ? <GhostButton onClick={() => { dispatch({ type: 'SET_SEARCH', payload: '' }); load(); }}>Clear search</GhostButton>
                : <DarkButton onClick={() => dispatch({ type: 'OPEN_MODAL' })}>Ingest claim</DarkButton>
            }
          />
        ) : (
          <Table
            rowKey="rawId"
            columns={columns}
            dataSource={state.items}
            loading={state.loading}
            pagination={{ pageSize: 10, size: 'small', hideOnSinglePage: true }}
            size="small"
          />
        )}
      </DataCard>

      <Modal
        title="Ingest raw claim"
        open={state.modalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_MODAL' }); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={state.submitting}
        okText="Ingest"
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleIngest}>
          <Form.Item name="claimId" label="Claim ID" rules={[{ required: true, min: 3 }]}>
            <Input placeholder="CLM-2026-AUTO-001" />
          </Form.Item>
          <Form.Item name="feedId" label="Feed ID" rules={[{ required: true }]}>
            <Input type="number" placeholder="1" min={1} />
          </Form.Item>
          <Form.Item name="payloadJson" label="Payload JSON" rules={[{ required: true }]}>
            <Input.TextArea rows={5} placeholder='{"claimType":"Auto","amount":5000}' />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  search: {
    display: 'inline-flex', alignItems: 'center', gap: 6,
    height: 28, padding: '0 10px',
    border: '1px solid var(--ci-border-strong)',
    borderRadius: 'var(--ci-radius-input)',
    background: 'var(--ci-bg-surface)',
  },
  searchInput: {
    border: 'none', outline: 'none', background: 'transparent',
    fontSize: 11, color: 'var(--ci-text-primary)',
    width: 160,
  },
};
