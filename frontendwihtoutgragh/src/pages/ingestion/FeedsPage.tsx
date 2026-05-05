import { useEffect, useMemo, useReducer, useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Button, Modal, Form, Input, Select,
  Alert, Tooltip, Popconfirm, Dropdown,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { MenuProps } from 'antd';
import { RefreshCw, Trash2, Plus } from 'lucide-react';

import {
  PageHeader, DataCard, Badge, Chip, StatusDot, DarkButton, GhostButton, EmptyState, ErrorState,
} from '../../components/ui';
import type { StatusTone } from '../../components/ui';
import {
  feedsApi, FEED_TYPES, FEED_STATUSES,
  type DataFeed,
} from '../../api/dataIngestionApi';
import { ingestApi } from '../../api/dataIngestionApi';
import { useAuth } from '../../hooks/useAuth';
import { canEdit } from '../../utils/roles';

const { Option } = Select;

/**
 * FeedsPage — reference screen #11 "Data feeds".
 * Single card wrapping the ingestion-health table.
 * ACTIVE feed rows expose an "Ingest" link that opens a pre-filled modal
 * for inserting a raw claim payload into that feed.
 */

// ── Reducer ──────────────────────────────────────────────────────────────────

interface State {
  items:           DataFeed[];
  loading:         boolean;
  error:           string | null;
  statusFilter:    'ALL' | 'ACTIVE' | 'INACTIVE' | 'FAILED';
  // register-feed modal
  modalOpen:       boolean;
  submitting:      boolean;
  // ingest-claim modal
  ingestModalOpen: boolean;
  ingestTarget:    DataFeed | null;
  ingesting:       boolean;
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
  | { type: 'DELETE'; payload: number }
  | { type: 'OPEN_INGEST';  payload: DataFeed }
  | { type: 'CLOSE_INGEST' }
  | { type: 'INGEST_START' }
  | { type: 'INGEST_DONE' };

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
    case 'OPEN_INGEST':    return { ...s, ingestModalOpen: true, ingestTarget: a.payload };
    case 'CLOSE_INGEST':   return { ...s, ingestModalOpen: false, ingestTarget: null, ingesting: false };
    case 'INGEST_START':   return { ...s, ingesting: true };
    case 'INGEST_DONE':    return { ...s, ingesting: false, ingestModalOpen: false, ingestTarget: null };
    default:               return s;
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

const STATUS_LABELS: Record<string, { label: string; tone: StatusTone }> = {
  ACTIVE:   { label: 'Healthy', tone: 'healthy' },
  INACTIVE: { label: 'Idle',    tone: 'muted'   },
  FAILED:   { label: 'Error',   tone: 'danger'  },
};

function relativeSync(iso: string | null): { text: string; color?: string } {
  if (!iso) return { text: '—', color: 'var(--ci-text-muted)' };
  const then = new Date(iso).getTime();
  if (isNaN(then)) return { text: iso, color: 'var(--ci-text-muted)' };
  const diffMin = Math.max(0, Math.floor((Date.now() - then) / 60000));
  if (diffMin < 2)        return { text: 'just now' };
  if (diffMin < 60)       return { text: `${diffMin}m ago` };
  if (diffMin < 60 * 24) {
    const hrs   = Math.floor(diffMin / 60);
    const color = hrs >= 4 ? 'var(--ci-warning-text)' : undefined;
    return { text: `${hrs}h ago`, color };
  }
  const days = Math.floor(diffMin / (60 * 24));
  return { text: `${days}d ago`, color: 'var(--ci-warning-text)' };
}

/** Friendly display name for a feed, e.g. "feed-auto-01" */
function feedLabel(feed: DataFeed): string {
  return `feed-${(feed.feedType ?? 'unknown').toLowerCase()}-${String(feed.feedId).padStart(2, '0')}`;
}

// ── Component ────────────────────────────────────────────────────────────────

export default function FeedsPage() {
  const navigate = useNavigate();
  const { role } = useAuth();
  // Managers + Operations Execs see this page but cannot add / delete / ingest.
  const canWrite = canEdit(role, '/ingestion/feeds');
  const [state, dispatch] = useReducer(reducer, {
    items: [], loading: false, error: null,
    statusFilter: 'ALL',
    modalOpen: false, submitting: false,
    ingestModalOpen: false, ingestTarget: null, ingesting: false,
  });
  const [form]       = Form.useForm();
  const [ingestForm] = Form.useForm();

  // Claim counts per feedId — loaded separately so feeds appear instantly.
  const [claimCounts, setClaimCounts] = useState<Record<number, number>>({});

  const loadCounts = useCallback(async () => {
    try {
      const all = await ingestApi.getAll();
      const counts: Record<number, number> = {};
      for (const r of all) counts[r.feedId] = (counts[r.feedId] ?? 0) + 1;
      setClaimCounts(counts);
    } catch { /* non-fatal — counts just stay empty */ }
  }, []);

  const load = useCallback(async () => {
    dispatch({ type: 'START' });
    try {
      const data = await feedsApi.getAll();
      dispatch({ type: 'SUCCESS', payload: data });
    } catch (err) {
      const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to load data feeds.';
      dispatch({ type: 'ERROR', payload: msg });
    }
  }, []);

  useEffect(() => { load(); loadCounts(); }, [load, loadCounts]);

  // ── Register new feed ────────────────────────────────────────────────────
  const handleCreate = async (values: Record<string, string>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const record = await feedsApi.create(values as { feedType: string; sourceSystem: string; status: string });
      dispatch({ type: 'SUBMIT_SUCCESS', payload: record });
      form.resetFields();
    } catch (err) {
      const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to create feed.';
      dispatch({ type: 'SUBMIT_ERROR' });
      dispatch({ type: 'ERROR', payload: msg });
    }
  };

  // ── Ingest raw claim into active feed ────────────────────────────────────
  const handleIngest = async (values: { claimId: string; payloadJson: string }) => {
    if (!state.ingestTarget) return;
    const feedId = state.ingestTarget.feedId;
    dispatch({ type: 'INGEST_START' });
    try {
      await ingestApi.ingest({
        claimId:     values.claimId,
        feedId,
        payloadJson: values.payloadJson,
      });
      // Optimistically increment the claim count for this feed immediately.
      setClaimCounts(prev => ({ ...prev, [feedId]: (prev[feedId] ?? 0) + 1 }));
      dispatch({ type: 'INGEST_DONE' });
      ingestForm.resetFields();
    } catch (err) {
      const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to ingest claim.';
      dispatch({ type: 'INGEST_DONE' });
      dispatch({ type: 'ERROR', payload: msg });
    }
  };

  // ── Delete feed ──────────────────────────────────────────────────────────
  const handleDelete = async (id: number) => {
    try {
      await feedsApi.delete(id);
      dispatch({ type: 'DELETE', payload: id });
    } catch (err) {
      const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to delete feed.';
      dispatch({ type: 'ERROR', payload: msg });
    }
  };

  const filtered = useMemo(() => {
    if (state.statusFilter === 'ALL') return state.items;
    return state.items.filter(i => i.status === state.statusFilter);
  }, [state.items, state.statusFilter]);

  const activeCount = state.items.filter(i => i.status === 'ACTIVE').length;

  // ── Columns ──────────────────────────────────────────────────────────────
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
      title: 'Claims',
      key: 'claims',
      width: 80,
      align: 'right' as const,
      render: (_: unknown, rec: DataFeed) => {
        const count = claimCounts[rec.feedId] ?? 0;
        if (count === 0) {
          return <span style={{ color: 'var(--ci-text-muted)', fontSize: 11 }}>0</span>;
        }
        return (
          <a
            onClick={() => navigate(`/ingestion/raw-claims?feedId=${rec.feedId}`)}
            style={{ fontWeight: 600, fontSize: 12, color: 'var(--ci-primary)' }}
          >
            {count}
          </a>
        );
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
      width: '18%',
      align: 'right',
      render: (_, rec) => {
        // Read-only viewers (Managers etc.) see no row actions
        if (!canWrite) return null;

        const deleteBtn = (
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
        );

        if (rec.status === 'FAILED') {
          return <a onClick={() => load()} style={styles.retryLink}>Retry</a>;
        }
        if (rec.status === 'ACTIVE') {
          return (
            <div style={{ display: 'flex', gap: 6, justifyContent: 'flex-end', alignItems: 'center' }}>
              <a
                onClick={() => dispatch({ type: 'OPEN_INGEST', payload: rec })}
                style={styles.ingestLink}
              >
                Ingest claim
              </a>
              {deleteBtn}
            </div>
          );
        }
        return deleteBtn;
      },
    },
  ];

  return (
    <div>
      <PageHeader
        title="Data feeds"
        subtitle={`${activeCount} active source${activeCount === 1 ? '' : 's'} · ingestion health`}
        actions={
          <>
            <Dropdown
              menu={{
                items: [
                  { key: 'ALL',      label: 'All status' },
                  { key: 'ACTIVE',   label: STATUS_LABELS.ACTIVE.label },
                  { key: 'INACTIVE', label: STATUS_LABELS.INACTIVE.label },
                  { key: 'FAILED',   label: STATUS_LABELS.FAILED.label },
                ],
                selectable: true,
                selectedKeys: [state.statusFilter],
                onClick: ({ key }) => dispatch({ type: 'SET_FILTER', payload: key as State['statusFilter'] }),
              } satisfies MenuProps}
              trigger={['click']}
            >
              <Chip dropdown active={state.statusFilter !== 'ALL'}>
                {state.statusFilter === 'ALL' ? 'All status' : STATUS_LABELS[state.statusFilter]?.label ?? state.statusFilter}
              </Chip>
            </Dropdown>
            <GhostButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>
              Refresh
            </GhostButton>
            {canWrite && (
              <DarkButton
                onClick={() => dispatch({ type: 'OPEN_MODAL' })}
                icon={<Plus size={12} strokeWidth={2} />}
              >
                Add feed
              </DarkButton>
            )}
          </>
        }
      />

      {state.error && (
        <Alert type="error" showIcon message={state.error} style={{ marginBottom: 12, borderRadius: 8 }} closable />
      )}

      <DataCard padding={0}>
        {!state.loading && state.error && filtered.length === 0 ? (
          <ErrorState
            title="Couldn't load feeds"
            message={state.error}
            onRetry={load}
          />
        ) : !state.loading && filtered.length === 0 && !state.error ? (
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

      {/* ── Register data feed modal ───────────────────────────────────────── */}
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

      {/* ── Ingest raw claim into active feed modal ────────────────────────── */}
      <Modal
        title={
          state.ingestTarget
            ? `Ingest claim into ${feedLabel(state.ingestTarget)}`
            : 'Ingest claim'
        }
        open={state.ingestModalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_INGEST' }); ingestForm.resetFields(); }}
        onOk={() => ingestForm.submit()}
        confirmLoading={state.ingesting}
        okText="Ingest claim"
        destroyOnClose
      >
        <Form form={ingestForm} layout="vertical" onFinish={handleIngest}>
          {/* Feed context — read-only so the user knows exactly where the claim lands */}
          <Form.Item label="Feed ID">
            <Input value={state.ingestTarget?.feedId ?? ''} disabled />
          </Form.Item>
          <Form.Item label="Feed type">
            <Input value={state.ingestTarget?.feedType ?? ''} disabled />
          </Form.Item>
          <Form.Item label="Source system">
            <Input value={state.ingestTarget?.sourceSystem ?? ''} disabled />
          </Form.Item>

          {/* User-provided claim fields */}
          <Form.Item
            name="claimId"
            label="Claim ID"
            rules={[{ required: true, min: 3, message: 'Claim ID must be at least 3 characters' }]}
          >
            <Input placeholder="CLM-2026-AUTO-001" />
          </Form.Item>
          <Form.Item
            name="payloadJson"
            label="Payload JSON"
            rules={[{ required: true, message: 'Payload JSON is required' }]}
          >
            <Input.TextArea
              rows={5}
              placeholder='{"claimType":"Auto","amount":5000,"incidentDate":"2026-01-15"}'
            />
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
  ingestLink: {
    color: 'var(--ci-primary)', fontWeight: 500, fontSize: 11, cursor: 'pointer',
  },
};
