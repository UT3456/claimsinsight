import { useEffect, useMemo, useReducer, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Modal, Form, Input, DatePicker, Alert, Tooltip, Popconfirm, Button,
  App as AntApp,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { RefreshCw, Plus, Trash2, Search } from 'lucide-react';

import {
  PageHeader, KpiCard, DataCard, Chip,
  GhostButton, DarkButton, EmptyState, CHART,
} from '../../components/ui';
import { reservesApi, type ClaimReserve, type CreateReserveRequest } from '../../api/financialApi';

/**
 * ReservesPage — reference screen #07.
 * Layout: PageHeader · 3 KpiCards · two-col (monthly trend area · top-10 bars) · DataCard table.
 */

// ── Reducer ──────────────────────────────────────────────────────────────────

interface State {
  items: ClaimReserve[];
  total: number;
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
  search: string;
  bucketFilter: 'ALL' | 'LARGE' | 'MID' | 'SMALL';
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: ClaimReserve[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'SET_TOTAL'; payload: number }
  | { type: 'OPEN_MODAL' }
  | { type: 'CLOSE_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'SUBMIT_SUCCESS'; payload: ClaimReserve }
  | { type: 'SUBMIT_ERROR' }
  | { type: 'DELETE'; payload: number }
  | { type: 'SET_SEARCH'; payload: string }
  | { type: 'SET_BUCKET'; payload: State['bucketFilter'] };

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'FETCH_START':    return { ...s, loading: true, error: null };
    case 'FETCH_SUCCESS':  return { ...s, loading: false, items: a.payload };
    case 'FETCH_ERROR':    return { ...s, loading: false, error: a.payload };
    case 'SET_TOTAL':      return { ...s, total: a.payload };
    case 'OPEN_MODAL':     return { ...s, modalOpen: true };
    case 'CLOSE_MODAL':    return { ...s, modalOpen: false, submitting: false };
    case 'SUBMIT_START':   return { ...s, submitting: true };
    case 'SUBMIT_SUCCESS': return { ...s, submitting: false, modalOpen: false, items: [a.payload, ...s.items] };
    case 'SUBMIT_ERROR':   return { ...s, submitting: false };
    case 'DELETE':         return { ...s, items: s.items.filter(i => i.reserveId !== a.payload) };
    case 'SET_SEARCH':     return { ...s, search: a.payload };
    case 'SET_BUCKET':     return { ...s, bucketFilter: a.payload };
    default:               return s;
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

const fmtUSD = (n: number) => `$${n.toLocaleString('en-US', { maximumFractionDigits: 0 })}`;

function bucketOf(amount: number): 'LARGE' | 'MID' | 'SMALL' {
  if (amount >= 100_000) return 'LARGE';
  if (amount >= 10_000)  return 'MID';
  return 'SMALL';
}

// ── Component ────────────────────────────────────────────────────────────────

export default function ReservesPage() {
  const navigate = useNavigate();
  const [state, dispatch] = useReducer(reducer, {
    items: [], total: 0, loading: false, error: null,
    modalOpen: false, submitting: false,
    search: '', bucketFilter: 'ALL',
  });
  const [form] = Form.useForm();
  const { message } = AntApp.useApp();

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const [data, total] = await Promise.all([
        reservesApi.getAll(),
        reservesApi.getTotalAmount().catch(() => 0),
      ]);
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
      dispatch({ type: 'SET_TOTAL', payload: total });
    } catch { dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load reserves.' }); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (values: Record<string, unknown>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const req: CreateReserveRequest = {
        claimId:       values.claimId as string,
        reserveAmount: Number(values.reserveAmount),
        updatedDate:   (values.updatedDate as dayjs.Dayjs).format('YYYY-MM-DD'),
      };
      dispatch({ type: 'SUBMIT_SUCCESS', payload: await reservesApi.create(req) });
      form.resetFields();
      message.success('Reserve added');
    } catch (err) {
      dispatch({ type: 'SUBMIT_ERROR' });
      const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to add reserve.';
      message.error(msg);
    }
  };

  const handleDelete = async (id: number) => {
    await reservesApi.delete(id);
    dispatch({ type: 'DELETE', payload: id });
  };

  // ── Derived ──────────────────────────────────────────────────────────────
  const totalAmount = state.total || state.items.reduce((s, i) => s + i.reserveAmount, 0);
  const avgReserve = state.items.length ? totalAmount / state.items.length : 0;
  const largeCount = state.items.filter(i => bucketOf(i.reserveAmount) === 'LARGE').length;

  const filtered = useMemo(() => {
    let list = state.items;
    if (state.bucketFilter !== 'ALL') {
      list = list.filter(i => bucketOf(i.reserveAmount) === state.bucketFilter);
    }
    if (state.search.trim()) {
      const q = state.search.trim().toLowerCase();
      list = list.filter(i => i.claimId.toLowerCase().includes(q));
    }
    return list;
  }, [state.items, state.bucketFilter, state.search]);

  const reserveTrend = useMemo(() => (
    Object.entries(
      state.items.reduce<Record<string, number>>((acc, r) => {
        const m = r.updatedDate?.slice(0, 7);
        if (!m) return acc;
        acc[m] = (acc[m] ?? 0) + r.reserveAmount;
        return acc;
      }, {}),
    )
      .map(([month, total]) => ({ month, total: Number(total.toFixed(0)) }))
      .sort((a, b) => a.month.localeCompare(b.month))
  ), [state.items]);

  const columns: ColumnsType<ClaimReserve> = [
    {
      title: 'ID', dataIndex: 'reserveId', key: 'reserveId', width: 70,
      render: v => <span style={{ color: 'var(--ci-text-muted)' }}>#{v}</span>,
    },
    {
      title: 'Claim', dataIndex: 'claimId', key: 'claimId', width: 220,
      render: v => (
        <a
          onClick={() => navigate(`/claims/${v}`)}
          style={{ color: 'var(--ci-primary)', fontFamily: 'ui-monospace, SFMono-Regular, monospace', fontSize: 11 }}
        >{v}</a>
      ),
    },
    {
      title: 'Reserve amount', dataIndex: 'reserveAmount', key: 'reserveAmount', width: 200,
      sorter: (a, b) => a.reserveAmount - b.reserveAmount,
      render: (v: number) => {
        const bucket = bucketOf(v);
        const color = bucket === 'LARGE' ? CHART.red : bucket === 'MID' ? CHART.amber : CHART.teal;
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ width: 6, height: 6, borderRadius: '50%', background: color, display: 'inline-block' }} />
            <span style={{ fontWeight: 500 }}>{fmtUSD(Number(v))}</span>
          </div>
        );
      },
    },
    {
      title: 'Updated', dataIndex: 'updatedDate', key: 'updatedDate', width: 120,
      sorter: (a, b) => a.updatedDate.localeCompare(b.updatedDate),
      render: v => <span style={{ color: 'var(--ci-text-secondary)' }}>{v}</span>,
    },
    {
      title: '', key: 'action', width: 40, align: 'right',
      render: (_, rec) => (
        <Popconfirm title="Delete this reserve?" onConfirm={() => handleDelete(rec.reserveId)}>
          <Tooltip title="Delete">
            <Button type="text" size="small" icon={<Trash2 size={13} strokeWidth={1.6} />} style={{ color: 'var(--ci-text-muted)' }} />
          </Tooltip>
        </Popconfirm>
      ),
    },
  ];

  const bucketCycle: State['bucketFilter'][] = ['ALL', 'LARGE', 'MID', 'SMALL'];
  const bucketLabel: Record<State['bucketFilter'], string> = {
    ALL: 'All sizes', LARGE: 'Large (≥$100K)', MID: 'Mid ($10K–100K)', SMALL: 'Small (<$10K)',
  };

  // Build trend delta (last vs. prior month) for the delta line
  const trendDelta = useMemo(() => {
    if (reserveTrend.length < 2) return null;
    const last = reserveTrend[reserveTrend.length - 1].total;
    const prev = reserveTrend[reserveTrend.length - 2].total;
    if (!prev) return null;
    return ((last - prev) / prev) * 100;
  }, [reserveTrend]);

  return (
    <div>
      <PageHeader
        title="Claim reserves"
        subtitle={`${state.items.length} reserve${state.items.length === 1 ? '' : 's'} · ${fmtUSD(totalAmount)} committed`}
        actions={
          <>
            <Chip
              dropdown
              active={state.bucketFilter !== 'ALL'}
              onClick={() => {
                const i = bucketCycle.indexOf(state.bucketFilter);
                dispatch({ type: 'SET_BUCKET', payload: bucketCycle[(i + 1) % bucketCycle.length] });
              }}
            >
              {bucketLabel[state.bucketFilter]}
            </Chip>
            <GhostButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>
              Refresh
            </GhostButton>
            <DarkButton onClick={() => dispatch({ type: 'OPEN_MODAL' })} icon={<Plus size={12} strokeWidth={2} />}>
              Add reserve
            </DarkButton>
          </>
        }
      />

      {/* KPI row */}
      <div style={styles.kpiRow}>
        <KpiCard
          label="Total reserve" value={fmtUSD(totalAmount)}
          delta={trendDelta !== null ? `${trendDelta >= 0 ? '+' : ''}${trendDelta.toFixed(1)}% vs prior month` : 'No prior data'}
          deltaDirection={trendDelta === null ? 'flat' : trendDelta >= 0 ? 'up' : 'down'}
          deltaTone={trendDelta === null ? undefined : trendDelta >= 0 ? 'down' : 'up'}
        />
        <KpiCard label="Records"       value={state.items.length.toLocaleString()} delta="Active reserves" deltaDirection="flat" />
        <KpiCard label="Avg reserve"   value={fmtUSD(avgReserve)} delta={`${largeCount} large (≥$100K)`} deltaDirection="flat" />
      </div>

      {state.error && <Alert type="error" showIcon message={state.error} style={{ marginBottom: 12, borderRadius: 8 }} closable />}

      {/* Filter rail */}
      <div style={styles.filterRow}>
        <div style={styles.search}>
          <Search size={12} strokeWidth={1.8} color="var(--ci-text-muted)" />
          <input
            style={styles.searchInput}
            placeholder="Search claim ID"
            value={state.search}
            onChange={e => dispatch({ type: 'SET_SEARCH', payload: e.target.value })}
          />
        </div>
      </div>

      <DataCard padding={0}>
        {!state.loading && filtered.length === 0 ? (
          <EmptyState
            title={state.bucketFilter !== 'ALL' || state.search ? 'No matching reserves' : 'No reserves booked'}
            description={state.bucketFilter !== 'ALL' || state.search ? 'Try clearing filters to view all reserves.' : 'Reserves will appear as they are added.'}
            actions={(state.bucketFilter !== 'ALL' || state.search) ? (
              <GhostButton onClick={() => { dispatch({ type: 'SET_BUCKET', payload: 'ALL' }); dispatch({ type: 'SET_SEARCH', payload: '' }); }}>
                Clear filters
              </GhostButton>
            ) : undefined}
          />
        ) : (
          <Table
            rowKey="reserveId"
            columns={columns}
            dataSource={filtered}
            loading={state.loading}
            size="small"
            pagination={{ pageSize: 10, size: 'small', hideOnSinglePage: true }}
          />
        )}
      </DataCard>

      <Modal
        title="Add reserve record"
        open={state.modalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_MODAL' }); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={state.submitting}
        okText="Add reserve"
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="claimId" label="Claim ID" rules={[{ required: true }]}>
            <Input placeholder="CLM-2026-AUTO-001" />
          </Form.Item>
          <Form.Item name="reserveAmount" label="Reserve amount ($)" rules={[{ required: true }]}>
            <Input type="number" min={0.01} step={0.01} placeholder="50000.00" />
          </Form.Item>
          <Form.Item name="updatedDate" label="Updated date" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  kpiRow: {
    display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginBottom: 16,
  },
  filterRow: {
    display: 'flex', gap: 8, marginBottom: 12,
  },
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
    width: 220,
  },
};
