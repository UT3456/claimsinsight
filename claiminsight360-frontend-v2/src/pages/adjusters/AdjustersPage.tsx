import { useEffect, useMemo, useReducer, useCallback } from 'react';
import {
  Table, Modal, Form, Input, Alert, Tooltip, Popconfirm, Button, Row, Col,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip as RTooltip, ResponsiveContainer,
  CartesianGrid, ComposedChart, Line, Cell,
} from 'recharts';
import { RefreshCw, Plus, Trash2, Trophy, Search } from 'lucide-react';

import {
  PageHeader, KpiCard, DataCard, Badge, Chip, StatusDot, TintedAvatar,
  GhostButton, DarkButton, EmptyState, CHART,
} from '../../components/ui';
import type { BadgeTone, StatusTone } from '../../components/ui';
import { adjustersApi, type AdjusterPerformance, type CreateAdjusterRequest } from '../../api/adjustersApi';

/**
 * AdjustersPage — reference screen #06.
 * Layout: PageHeader · 4 KpiCards · two-col charts · DataCard table with avatars.
 */

// ── Reducer ──────────────────────────────────────────────────────────────────

interface State {
  items: AdjusterPerformance[];
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
  period: string;
  tierFilter: 'ALL' | 'TOP' | 'MID' | 'LOW';
  search: string;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: AdjusterPerformance[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'OPEN_MODAL' }
  | { type: 'CLOSE_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'SUBMIT_SUCCESS'; payload: AdjusterPerformance }
  | { type: 'SUBMIT_ERROR' }
  | { type: 'DELETE'; payload: number }
  | { type: 'SET_PERIOD'; payload: string }
  | { type: 'SET_TIER'; payload: State['tierFilter'] }
  | { type: 'SET_SEARCH'; payload: string };

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'FETCH_START':    return { ...s, loading: true, error: null };
    case 'FETCH_SUCCESS':  return { ...s, loading: false, items: a.payload };
    case 'FETCH_ERROR':    return { ...s, loading: false, error: a.payload };
    case 'OPEN_MODAL':     return { ...s, modalOpen: true };
    case 'CLOSE_MODAL':    return { ...s, modalOpen: false, submitting: false };
    case 'SUBMIT_START':   return { ...s, submitting: true };
    case 'SUBMIT_SUCCESS': return { ...s, submitting: false, modalOpen: false, items: [...s.items, a.payload] };
    case 'SUBMIT_ERROR':   return { ...s, submitting: false };
    case 'DELETE':         return { ...s, items: s.items.filter(i => i.perfId !== a.payload) };
    case 'SET_PERIOD':     return { ...s, period: a.payload };
    case 'SET_TIER':       return { ...s, tierFilter: a.payload };
    case 'SET_SEARCH':     return { ...s, search: a.payload };
    default:               return s;
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function tierOf(slaRate: number): 'TOP' | 'MID' | 'LOW' {
  if (slaRate >= 90) return 'TOP';
  if (slaRate >= 75) return 'MID';
  return 'LOW';
}

function flagMeta(flag: string): { tone: BadgeTone; status: StatusTone } {
  if (!flag) return { tone: 'neutral', status: 'muted' };
  const f = flag.toLowerCase();
  if (f.includes('high') || f.includes('good') || f.includes('top'))
    return { tone: 'green', status: 'healthy' };
  if (f.includes('low') || f.includes('poor'))
    return { tone: 'red', status: 'danger' };
  return { tone: 'amber', status: 'warning' };
}

// ── Component ────────────────────────────────────────────────────────────────

export default function AdjustersPage() {
  const [state, dispatch] = useReducer(reducer, {
    items: [], loading: false, error: null, modalOpen: false, submitting: false,
    period: '', tierFilter: 'ALL', search: '',
  });
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const data = await adjustersApi.getAll(state.period || undefined);
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load adjuster performance.' });
    }
  }, [state.period]);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (values: Record<string, unknown>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const req: CreateAdjusterRequest = {
        adjusterId:       Number(values.adjusterId),
        claimsHandled:    Number(values.claimsHandled ?? 0),
        totalDaysTaken:   Number(values.totalDaysTaken ?? 0),
        slaMetCount:      Number(values.slaMetCount ?? 0),
        slaBreachedCount: Number(values.slaBreachedCount ?? 0),
        deniedClaimsCount:Number(values.deniedClaimsCount ?? 0),
        errorRate:        Number(values.errorRate ?? 0),
        period:           values.period as string,
      };
      dispatch({ type: 'SUBMIT_SUCCESS', payload: await adjustersApi.create(req) });
      form.resetFields();
    } catch { dispatch({ type: 'SUBMIT_ERROR' }); }
  };

  const handleDelete = async (id: number) => {
    await adjustersApi.delete(id);
    dispatch({ type: 'DELETE', payload: id });
  };

  // ── Derived ──────────────────────────────────────────────────────────────
  const filtered = useMemo(() => {
    let list = state.items;
    if (state.tierFilter !== 'ALL') {
      list = list.filter(i => tierOf(i.slaComplianceRate ?? 0) === state.tierFilter);
    }
    if (state.search.trim()) {
      const q = state.search.trim().toLowerCase();
      list = list.filter(i => String(i.adjusterId).includes(q) || (i.period ?? '').toLowerCase().includes(q));
    }
    return list;
  }, [state.items, state.tierFilter, state.search]);

  const avgTat = state.items.length
    ? (state.items.reduce((s, i) => s + (i.avgTat ?? 0), 0) / state.items.length)
    : 0;
  const avgSla = state.items.length
    ? (state.items.reduce((s, i) => s + (i.slaComplianceRate ?? 0), 0) / state.items.length)
    : 0;
  const topCount = state.items.filter(i => (i.slaComplianceRate ?? 0) >= 90).length;

  const topAdjusters = useMemo(() => (
    [...state.items]
      .sort((a, b) => b.claimsHandled - a.claimsHandled)
      .slice(0, 10)
      .map(i => ({
        name: `#${i.adjusterId}`,
        adjusterId: i.adjusterId,
        claims: i.claimsHandled,
        avgTat: Number((i.avgTat ?? 0).toFixed(1)),
        slaRate: Number((i.slaComplianceRate ?? 0).toFixed(1)),
      }))
  ), [state.items]);

  const columns: ColumnsType<AdjusterPerformance> = [
    {
      title: 'Adjuster', dataIndex: 'adjusterId', key: 'adjusterId', width: 180,
      render: (id: number) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <TintedAvatar size={28} name={`A${id}`} colorKey={String(id)} />
          <div>
            <div style={{ fontWeight: 500, fontSize: 12 }}>Adjuster #{id}</div>
            <div style={{ fontSize: 10, color: 'var(--ci-text-muted)' }}>ID {id}</div>
          </div>
        </div>
      ),
    },
    {
      title: 'Period', dataIndex: 'period', key: 'period', width: 100,
      render: v => <Badge tone="purple">{v}</Badge>,
    },
    {
      title: 'Claims', dataIndex: 'claimsHandled', key: 'claimsHandled', width: 90,
      sorter: (a, b) => a.claimsHandled - b.claimsHandled,
      render: v => <span style={{ fontWeight: 500 }}>{v}</span>,
    },
    {
      title: 'Avg TAT', dataIndex: 'avgTat', key: 'avgTat', width: 90,
      sorter: (a, b) => (a.avgTat ?? 0) - (b.avgTat ?? 0),
      render: v => (
        <span style={{ color: (v ?? 0) > 10 ? 'var(--ci-warning-text)' : 'var(--ci-text-secondary)' }}>
          {Number(v ?? 0).toFixed(1)}d
        </span>
      ),
    },
    {
      title: 'SLA compliance', dataIndex: 'slaComplianceRate', key: 'slaComplianceRate', width: 180,
      sorter: (a, b) => (a.slaComplianceRate ?? 0) - (b.slaComplianceRate ?? 0),
      render: (v: number) => {
        const rate = Math.round(v ?? 0);
        const color = rate >= 90 ? CHART.teal : rate >= 75 ? CHART.amber : CHART.red;
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <div style={styles.slaBarTrack}>
              <div style={{ ...styles.slaBarFill, width: `${Math.min(rate, 100)}%`, background: color }} />
            </div>
            <span style={{ fontSize: 11, fontWeight: 500, width: 32, textAlign: 'right', color: 'var(--ci-text-secondary)' }}>{rate}%</span>
          </div>
        );
      },
    },
    {
      title: 'Error rate', dataIndex: 'errorRate', key: 'errorRate', width: 90,
      render: v => (
        <span style={{ color: (v ?? 0) > 5 ? 'var(--ci-danger-text, #B22C2B)' : 'var(--ci-text-secondary)' }}>
          {Number(v ?? 0).toFixed(1)}%
        </span>
      ),
    },
    {
      title: 'Performance', dataIndex: 'performanceFlag', key: 'performanceFlag', width: 140,
      render: (v: string) => {
        if (!v) return <span style={{ color: 'var(--ci-text-muted)' }}>—</span>;
        const meta = flagMeta(v);
        return <StatusDot tone={meta.status}>{v}</StatusDot>;
      },
    },
    {
      title: '', key: 'action', width: 40, align: 'right',
      render: (_, rec) => (
        <Popconfirm title="Delete this record?" onConfirm={() => handleDelete(rec.perfId)}>
          <Tooltip title="Delete">
            <Button type="text" size="small" icon={<Trash2 size={13} strokeWidth={1.6} />} style={{ color: 'var(--ci-text-muted)' }} />
          </Tooltip>
        </Popconfirm>
      ),
    },
  ];

  const tierCycle: State['tierFilter'][] = ['ALL', 'TOP', 'MID', 'LOW'];
  const tierLabel: Record<State['tierFilter'], string> = {
    ALL: 'All tiers', TOP: 'Top (≥90%)', MID: 'Mid (75–90%)', LOW: 'Low (<75%)',
  };

  return (
    <div>
      <PageHeader
        title="Adjusters"
        subtitle={`${state.items.length} record${state.items.length === 1 ? '' : 's'} · ${topCount} top performer${topCount === 1 ? '' : 's'}`}
        actions={
          <>
            <Chip
              dropdown
              active={state.tierFilter !== 'ALL'}
              onClick={() => {
                const i = tierCycle.indexOf(state.tierFilter);
                dispatch({ type: 'SET_TIER', payload: tierCycle[(i + 1) % tierCycle.length] });
              }}
            >
              {tierLabel[state.tierFilter]}
            </Chip>
            <GhostButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>
              Refresh
            </GhostButton>
            <DarkButton onClick={() => dispatch({ type: 'OPEN_MODAL' })} icon={<Plus size={12} strokeWidth={2} />}>
              Add record
            </DarkButton>
          </>
        }
      />

      {/* KPI row */}
      <div style={styles.kpiRow}>
        <KpiCard label="Adjusters tracked" value={state.items.length.toLocaleString()} delta="Active handlers" deltaDirection="flat" />
        <KpiCard label="Avg TAT"          value={`${avgTat.toFixed(1)}d`} delta={avgTat > 10 ? 'Above target' : 'On target'} deltaDirection={avgTat > 10 ? 'up' : 'down'} deltaTone={avgTat > 10 ? 'down' : 'up'} />
        <KpiCard label="Avg SLA"          value={`${avgSla.toFixed(0)}%`} delta={avgSla >= 85 ? 'Healthy' : 'Needs attention'} deltaDirection={avgSla >= 85 ? 'up' : 'down'} deltaTone={avgSla >= 85 ? 'up' : 'down'} />
        <KpiCard label="Top performers"   value={topCount.toLocaleString()} delta="SLA ≥ 90%" deltaDirection="flat" />
      </div>

      {state.error && <Alert type="error" showIcon message={state.error} style={{ marginBottom: 12, borderRadius: 8 }} closable />}

      {/* Chart row */}
      <div style={styles.chartRow}>
        <DataCard title="Top 10 by volume" subtitle="Claims handled (bars) vs. average TAT (line)">
          {topAdjusters.length === 0 ? (
            <EmptyState title="No data yet" description="Performance records will appear as they are recorded." />
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <ComposedChart data={topAdjusters} margin={{ top: 4, right: 16, bottom: 0, left: -8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--ci-border)" vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 10, fill: 'var(--ci-text-secondary)' }} axisLine={false} tickLine={false} />
                <YAxis yAxisId="left"  tick={{ fontSize: 10, fill: 'var(--ci-text-muted)' }} axisLine={false} tickLine={false} />
                <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 10, fill: 'var(--ci-text-muted)' }} axisLine={false} tickLine={false} />
                <RTooltip
                  cursor={{ fill: 'var(--ci-bg-surface-2)' }}
                  contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8, fontSize: 12 }}
                />
                <Bar  yAxisId="left"  dataKey="claims" fill={CHART.blue} radius={[5, 5, 0, 0]} name="Claims" barSize={22} />
                <Line yAxisId="right" type="monotone" dataKey="avgTat" stroke={CHART.red} strokeWidth={2} dot={{ r: 3, fill: CHART.red }} name="Avg TAT (d)" />
              </ComposedChart>
            </ResponsiveContainer>
          )}
        </DataCard>

        <DataCard title="SLA compliance" subtitle="Top 10 adjusters by volume">
          {topAdjusters.length === 0 ? (
            <EmptyState title="No data yet" description="SLA bars will populate once records exist." />
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={topAdjusters} layout="vertical" margin={{ top: 4, right: 16, bottom: 0, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--ci-border)" horizontal={false} />
                <XAxis type="number" domain={[0, 100]} tick={{ fontSize: 10, fill: 'var(--ci-text-muted)' }} unit="%" axisLine={false} tickLine={false} />
                <YAxis dataKey="name" type="category" width={50} tick={{ fontSize: 10, fill: 'var(--ci-text-secondary)' }} axisLine={false} tickLine={false} />
                <RTooltip
                  cursor={{ fill: 'var(--ci-bg-surface-2)' }}
                  formatter={(v) => `${Number(v)}%`}
                  contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8, fontSize: 12 }}
                />
                <Bar dataKey="slaRate" radius={[0, 5, 5, 0]} barSize={14}>
                  {topAdjusters.map((d) => {
                    const color = d.slaRate >= 90 ? CHART.teal : d.slaRate >= 75 ? CHART.amber : CHART.red;
                    return <Cell key={d.name} fill={color} />;
                  })}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </DataCard>
      </div>

      {/* Filter rail */}
      <div style={styles.filterRow}>
        <div style={styles.search}>
          <Search size={12} strokeWidth={1.8} color="var(--ci-text-muted)" />
          <input
            style={styles.searchInput}
            placeholder="Search adjuster ID or period"
            value={state.search}
            onChange={e => dispatch({ type: 'SET_SEARCH', payload: e.target.value })}
          />
        </div>
        <div style={styles.search}>
          <input
            style={{ ...styles.searchInput, width: 160 }}
            placeholder="Filter period (e.g. Q1-2024)"
            value={state.period}
            onChange={e => dispatch({ type: 'SET_PERIOD', payload: e.target.value })}
            onKeyDown={e => { if (e.key === 'Enter') load(); }}
          />
          <Trophy size={12} strokeWidth={1.8} color="var(--ci-text-muted)" />
        </div>
      </div>

      <DataCard padding={0}>
        {!state.loading && filtered.length === 0 ? (
          <EmptyState
            title={state.tierFilter !== 'ALL' || state.search ? 'No matching adjusters' : 'No adjuster records'}
            description={state.tierFilter !== 'ALL' || state.search ? 'Try clearing filters to view all records.' : 'Records will appear as performance data is captured.'}
            actions={(state.tierFilter !== 'ALL' || state.search) ? (
              <GhostButton onClick={() => { dispatch({ type: 'SET_TIER', payload: 'ALL' }); dispatch({ type: 'SET_SEARCH', payload: '' }); }}>
                Clear filters
              </GhostButton>
            ) : undefined}
          />
        ) : (
          <Table
            rowKey="perfId"
            columns={columns}
            dataSource={filtered}
            loading={state.loading}
            size="small"
            pagination={{ pageSize: 10, size: 'small', hideOnSinglePage: true }}
            scroll={{ x: 900 }}
          />
        )}
      </DataCard>

      <Modal
        title="Add performance record"
        open={state.modalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_MODAL' }); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={state.submitting}
        okText="Add record"
        destroyOnClose
        width={560}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Row gutter={12}>
            <Col span={12}><Form.Item name="adjusterId" label="Adjuster ID" rules={[{ required: true }]}><Input type="number" min={1} /></Form.Item></Col>
            <Col span={12}><Form.Item name="period" label="Period" rules={[{ required: true }]}><Input placeholder="Q1-2024" /></Form.Item></Col>
            <Col span={12}><Form.Item name="claimsHandled"    label="Claims handled"    ><Input type="number" min={0} defaultValue={0} /></Form.Item></Col>
            <Col span={12}><Form.Item name="totalDaysTaken"   label="Total days taken"  ><Input type="number" min={0} defaultValue={0} /></Form.Item></Col>
            <Col span={12}><Form.Item name="slaMetCount"      label="SLA met count"     ><Input type="number" min={0} defaultValue={0} /></Form.Item></Col>
            <Col span={12}><Form.Item name="slaBreachedCount" label="SLA breached"      ><Input type="number" min={0} defaultValue={0} /></Form.Item></Col>
            <Col span={12}><Form.Item name="deniedClaimsCount" label="Denied claims"    ><Input type="number" min={0} defaultValue={0} /></Form.Item></Col>
            <Col span={12}><Form.Item name="errorRate"        label="Error rate (%)"    ><Input type="number" min={0} max={100} defaultValue={0} /></Form.Item></Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  kpiRow: {
    display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 16,
  },
  chartRow: {
    display: 'grid', gridTemplateColumns: '1.3fr 1fr', gap: 12, marginBottom: 16,
  },
  filterRow: {
    display: 'flex', gap: 8, marginBottom: 12, flexWrap: 'wrap',
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
    width: 200,
  },
  slaBarTrack: {
    flex: 1, height: 6, borderRadius: 3,
    background: 'var(--ci-bg-surface-2)', overflow: 'hidden',
    maxWidth: 120,
  },
  slaBarFill: {
    height: '100%', borderRadius: 3, transition: 'width 0.3s',
  },
};
