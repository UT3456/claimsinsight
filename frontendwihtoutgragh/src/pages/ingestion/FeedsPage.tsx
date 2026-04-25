import { useEffect, useMemo, useReducer, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Select,
  Alert, Tooltip, Popconfirm,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { RefreshCw, Trash2, Plus } from 'lucide-react';

import {
  PageHeader, DataCard, Badge, Chip, StatusDot, DarkButton, GhostButton, EmptyState,
} from '../../components/ui';
import type { StatusTone } from '../../components/ui';
import {
  feedsApi, FEED_TYPES, FEED_STATUSES,
  type DataFeed,
} from '../../api/dataIngestionApi';

const { Option } = Select;

/**
 * FeedsPage — reference screen #11 "Data feeds".
 * Single card wrapping the ingestion-health table. No stat tiles (reference
 * opts for a minimal toolbar — counts are implicit from the "Healthy" rows).
 */

// ── Reducer ──────────────────────────────────────────────────────────────────

interface State {
  items: DataFeed[];
  loading: boolean;
  error: string | null;
  statusFilter: 'ALL' | 'ACTIVE' | 'INACTIVE' | 'FAILED';
  modalOpen: boolean;
  submitting: boolean;
}

type Action =
  | { type: 'START' }
  | { type: 'SUCCESS'; payload: DataFeed[] }
  | { type: 'ERROR';   payload: string }
  | { type: 'SET_FILTER'; payload: State['statusFilter'] }
  | { type: 'OPEN_MODAL' }
  | { type: 'CLOSE_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'SUBMIT_SUCCESS'; payload: DataFeed }
  | { type: 'SUBMIT_ERROR' }
  | { type: 'DELETE'; payload: number };

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'START':          return { ...s, loading: true, error: null };
    case 'SUCCESS':        return { ...s, loading: false, items: a.payload };
    case 'ERROR':          return { ...s, loading: false, error: a.payload };
    case 'SET_FILTER':     return { ...s, statusFilter: a.payload };
    case 'OPEN_MODAL':     return { ...s, modalOpen: true };
    case 'CLOSE_MODAL':    return { ...s, modalOpen: false, submitting: false };
    case 'SUBMIT_START':   return { ...s, submitting: true };
    case 'SUBMIT_SUCCESS': return { ...s, submitting: false, modalOpen: false, items: [a.payload, ...s.items] };
    case 'SUBMIT_ERROR':   return { ...s, submitting: false };
    case 'DELETE':         return { ...s, items: s.items.filter(i => i.feedId !== a.payload) };
    default:               return s;
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

const STATUS_LABELS: Record<string, { label: string; tone: StatusTone }> = {
  ACTIVE:   { label: 'Healthy',  tone: 'healthy' },
  INACTIVE: { label: 'Idle',     tone: 'muted' },
  FAILED:   { label: 'Error',    tone: 'danger' },
};

function relativeSync(iso: string | null): { text: string; color?: string } {
  if (!iso) return { text: '—', color: 'var(--ci-text-muted)' };
  const then = new Date(iso).getTime();
  if (isNaN(then)) return { text: iso, color: 'var(--ci-text-muted)' };
  const diffMin = Math.max(0, Math.floor((Date.now() - then) / 60000));
  if (diffMin < 2)   return { text: 'just now' };
  if (diffMin < 60)  return { text: `${diffMin}m ago` };
  if (diffMin < 60 * 24) {
    const hrs = Math.floor(diffMin / 60);
    const color = hrs >= 4 ? 'var(--ci-warning-text)' : undefined;
    return { text: `${hrs}h ago`, color };
  }
  const days = Math.floor(diffMin / (60 * 24));
  return { text: `${days}d ago`, color: 'var(--ci-warning-text)' };
}

// ── Component ────────────────────────────────────────────────────────────────

export default function FeedsPage() {
  const [state, dispatch] = useReducer(reducer, {
    items: [], loading: false, error: null,
    statusFilter: 'ALL',
    modalOpen: false, submitting: false,
  });
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    dispatch({ type: 'START' });
    try {
      const data = await feedsApi.getAll();
      dispatch({ type: 'SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'ERROR', payload: 'Failed to load data feeds.' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (values: Record<string, string>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const record = await feedsApi.create(values as { feedType: string; sourceSystem: string; status: string });
      dispatch({ type: 'SUBMIT_SUCCESS', payload: record });
      form.resetFields();
    } catch {
      dispatch({ type: 'SUBMIT_ERROR' });
    }
  };

  const handleDelete = async (id: number) => {
    await feedsApi.delete(id);
    dispatch({ type: 'DELETE', payload: id });
  };

  const filtered = useMemo(() => {
    if (state.statusFilter === 'ALL') return state.items;
    return state.items.filter(i => i.status === state.statusFilter);
  }, [state.items, state.statusFilter]);

  const activeCount = state.items.filter(i => i.status === 'ACTIVE').length;

  const columns: ColumnsType<DataFeed> = [
    {
      title: 'Feed',
      dataIndex: 'feedId',
      key: 'feedId',
      width: '20%',
      render: (id, rec) => (
        <span style={{ fontWeight: 500 }}>
          feed-{rec.feedType?.toLowerCase()}-{String(id).padStart(2, '0')}
        </span>
      ),
    },
    {
      title: 'Type',
      dataIndex: 'feedType',
      key: 'feedType',
      width: '14%',
      render: v => <Badge tone="blue">{v}</Badge>,
    },
    {
      title: 'Source',
      dataIndex: 'sourceSystem',
      key: 'sourceSystem',
      width: '20%',
      render: v => <span style={{ color: 'var(--ci-text-secondary)' }}>{v}</span>,
    },
    {
      title: 'Last sync',
      dataIndex: 'lastSyncDate',
      key: 'lastSyncDate',
      width: '18%',
      render: (v, rec) => {
        if (rec.status === 'FAILED') {
          return <span style={{ color: 'var(--ci-danger-text)' }}>Failed {relativeSync(v).text}</span>;
        }
        const { text, color } = relativeSync(v);
        return <span style={{ color: color ?? 'var(--ci-text-secondary)' }}>{text}</span>;
      },
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: '15%',
      render: (v: string) => {
        const meta = STATUS_LABELS[v] ?? { label: v, tone: 'muted' as StatusTone };
        return <StatusDot tone={meta.tone}>{meta.label}</StatusDot>;
      },
    },
    {
      title: '',
      key: 'action',
      width: '13%',
      align: 'right',
      render: (_, rec) => (
        rec.status === 'FAILED'
          ? <a onClick={() => load()} style={styles.retryLink}>Retry</a>
          : (
            <Popconfirm title="Delete this feed?" onConfirm={() => handleDelete(rec.feedId)}>
              <Tooltip title="Delete">
                <Button
                  type="text"
                  size="small"
                  icon={<Trash2 size={13} strokeWidth={1.6} />}
                  style={{ color: 'var(--ci-text-muted)' }}
                />
              </Tooltip>
            </Popconfirm>
          )
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title="Data feeds"
        subtitle={`${activeCount} active source${activeCount === 1 ? '' : 's'} · ingestion health`}
        actions={
          <>
            <Chip
              dropdown
              active={state.statusFilter !== 'ALL'}
              onClick={() => {
                const next = state.statusFilter === 'ALL' ? 'ACTIVE'
                           : state.statusFilter === 'ACTIVE' ? 'FAILED'
                           : state.statusFilter === 'FAILED' ? 'INACTIVE'
                           : 'ALL';
                dispatch({ type: 'SET_FILTER', payload: next });
              }}
            >
              {state.statusFilter === 'ALL' ? 'All status' : STATUS_LABELS[state.statusFilter]?.label ?? state.statusFilter}
            </Chip>
            <GhostButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>
              Refresh
            </GhostButton>
            <DarkButton
              onClick={() => dispatch({ type: 'OPEN_MODAL' })}
              icon={<Plus size={12} strokeWidth={2} />}
            >
              Add feed
            </DarkButton>
          </>
        }
      />

      {state.error && (
        <Alert type="error" showIcon message={state.error} style={{ marginBottom: 12, borderRadius: 8 }} closable />
      )}

      <DataCard padding={0}>
        {!state.loading && filtered.length === 0 && !state.error ? (
          <EmptyState
            title={state.statusFilter === 'ALL' ? 'No feeds registered' : `No ${STATUS_LABELS[state.statusFilter]?.label.toLowerCase() ?? 'matching'} feeds`}
            description={
              state.statusFilter === 'ALL'
                ? 'Register a source system to begin ingesting claims.'
                : 'Try a different filter to view other feeds.'
            }
            actions={
              state.statusFilter === 'ALL'
                ? <DarkButton onClick={() => dispatch({ type: 'OPEN_MODAL' })}>Add feed</DarkButton>
                : <GhostButton onClick={() => dispatch({ type: 'SET_FILTER', payload: 'ALL' })}>Show all</GhostButton>
            }
          />
        ) : (
          <Table
            rowKey="feedId"
            columns={columns}
            dataSource={filtered}
            loading={state.loading}
            pagination={{ pageSize: 10, size: 'small', hideOnSinglePage: true }}
            size="small"
          />
        )}
      </DataCard>

      <Modal
        title="Register data feed"
        open={state.modalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_MODAL' }); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={state.submitting}
        okText="Add feed"
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="feedType" label="Feed type" rules={[{ required: true }]}>
            <Select placeholder="Select type">
              {FEED_TYPES.map(t => <Option key={t} value={t}>{t}</Option>)}
            </Select>
          </Form.Item>
          <Form.Item name="sourceSystem" label="Source system" rules={[{ required: true, min: 2 }]}>
            <Input placeholder="e.g. Guidewire, PAS core, Finance DW" />
          </Form.Item>
          <Form.Item name="status" label="Initial status" rules={[{ required: true }]} initialValue="ACTIVE">
            <Select>
              {FEED_STATUSES.map(s => <Option key={s} value={s}>{s}</Option>)}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  retryLink: {
    color: 'var(--ci-primary)', fontWeight: 500, fontSize: 11, cursor: 'pointer',
  },
};
