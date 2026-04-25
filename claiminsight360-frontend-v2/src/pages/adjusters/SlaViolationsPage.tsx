import { useEffect, useMemo, useReducer, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Modal, Form, Input, DatePicker, Alert, Row, Col,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip as RTooltip, ResponsiveContainer,
  CartesianGrid, Cell,
} from 'recharts';
import { RefreshCw, Plus, AlertTriangle } from 'lucide-react';

import {
  PageHeader, KpiCard, DataCard, Badge, Chip, StatusDot,
  GhostButton, DarkButton, EmptyState, CHART,
} from '../../components/ui';
import type { BadgeTone, StatusTone } from '../../components/ui';
import { slaApi, type SlaViolation, type CreateSlaViolationRequest } from '../../api/adjustersApi';

/**
 * SlaViolationsPage — follows the adjusters table pattern.
 * Layout: PageHeader · 4 KpiCards · two-col charts · DataCard table.
 */

// ── Reducer ──────────────────────────────────────────────────────────────────

interface State {
  items: SlaViolation[];
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
  severityFilter: 'ALL' | 'HIGH' | 'MEDIUM' | 'LOW';
  escalatedOnly: boolean;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: SlaViolation[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'OPEN_MODAL' }
  | { type: 'CLOSE_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'SUBMIT_SUCCESS'; payload: SlaViolation }
  | { type: 'SUBMIT_ERROR' }
  | { type: 'SET_SEVERITY'; payload: State['severityFilter'] }
  | { type: 'TOGGLE_ESCALATED' };

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'FETCH_START':       return { ...s, loading: true, error: null };
    case 'FETCH_SUCCESS':     return { ...s, loading: false, items: a.payload };
    case 'FETCH_ERROR':       return { ...s, loading: false, error: a.payload };
    case 'OPEN_MODAL':        return { ...s, modalOpen: true };
    case 'CLOSE_MODAL':       return { ...s, modalOpen: false, submitting: false };
    case 'SUBMIT_START':      return { ...s, submitting: true };
    case 'SUBMIT_SUCCESS':    return { ...s, submitting: false, modalOpen: false, items: [a.payload, ...s.items] };
    case 'SUBMIT_ERROR':      return { ...s, submitting: false };
    case 'SET_SEVERITY':      return { ...s, severityFilter: a.payload };
    case 'TOGGLE_ESCALATED':  return { ...s, escalatedOnly: !s.escalatedOnly };
    default:                  return s;
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

const SEVERITY_META: Record<string, { tone: BadgeTone; status: StatusTone; color: string }> = {
  HIGH:   { tone: 'red',    status: 'danger',  color: CHART.red },
  MEDIUM: { tone: 'amber',  status: 'warning', color: CHART.amber },
  LOW:    { tone: 'blue',   status: 'info',    color: CHART.blue },
};

// ── Component ────────────────────────────────────────────────────────────────

export default function SlaViolationsPage() {
  const navigate = useNavigate();
  const [state, dispatch] = useReducer(reducer, {
    items: [], loading: false, error: null, modalOpen: false, submitting: false,
    severityFilter: 'ALL', escalatedOnly: false,
  });
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      dispatch({ type: 'FETCH_SUCCESS', payload: await slaApi.getAll() });
    } catch { dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load SLA violations.' }); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (values: Record<string, unknown>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const req: CreateSlaViolationRequest = {
        claimId:       Number(values.claimId),
        adjusterId:    Number(values.adjusterId),
        violationType: values.violationType as string,
        slaTargetDays: Number(values.slaTargetDays),
        actualDays:    Number(values.actualDays),
        violationDate: (values.violationDate as dayjs.Dayjs).format('YYYY-MM-DD'),
      };
      dispatch({ type: 'SUBMIT_SUCCESS', payload: await slaApi.create(req) });
      form.resetFields();
    } catch { dispatch({ type: 'SUBMIT_ERROR' }); }
  };

  // ── Derived ──────────────────────────────────────────────────────────────
  const high      = state.items.filter(i => i.severity === 'HIGH').length;
  const medium    = state.items.filter(i => i.severity === 'MEDIUM').length;
  const low       = state.items.filter(i => i.severity === 'LOW').length;
  const escalated = state.items.filter(i => i.escalated).length;
  const avgOverdue = state.items.length
    ? state.items.reduce((s, i) => s + (i.daysOverdue ?? 0), 0) / state.items.length
    : 0;

  const filtered = useMemo(() => {
    let list = state.items;
    if (state.severityFilter !== 'ALL') list = list.filter(i => i.severity === state.severityFilter);
    if (state.escalatedOnly) list = list.filter(i => i.escalated);
    return list;
  }, [state.items, state.severityFilter, state.escalatedOnly]);

  const byType = useMemo(() => (
    Object.entries(
      state.items.reduce<Record<string, number>>((acc, v) => {
        acc[v.violationType] = (acc[v.violationType] ?? 0) + 1;
        return acc;
      }, {}),
    )
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 6)
  ), [state.items]);

  const severityMix = useMemo(() => (
    [
      { name: 'HIGH',   value: high,   color: CHART.red },
      { name: 'MEDIUM', value: medium, color: CHART.amber },
      { name: 'LOW',    value: low,    color: CHART.blue },
    ].filter(s => s.value > 0)
  ), [high, medium, low]);

  const columns: ColumnsType<SlaViolation> = [
    {
      title: 'ID', dataIndex: 'violationId', key: 'violationId', width: 70,
      render: v => <span style={{ color: 'var(--ci-text-muted)' }}>#{v}</span>,
    },
    {
      title: 'Claim', dataIndex: 'claimId', key: 'claimId', width: 120,
      render: (v: number) => (
        <a
          onClick={() => navigate(`/claims/${v}`)}
          style={{ color: 'var(--ci-primary)', fontFamily: 'ui-monospace, SFMono-Regular, monospace', fontSize: 11 }}
        >CLM-{v}</a>
      ),
    },
    {
      title: 'Adjuster', dataIndex: 'adjusterId', key: 'adjusterId', width: 100,
      render: v => <span style={{ color: 'var(--ci-text-secondary)' }}>#{v}</span>,
    },
    {
      title: 'Type', dataIndex: 'violationType', key: 'violationType', width: 160,
      render: v => <Badge tone="amber">{v}</Badge>,
    },
    {
      title: 'Target', dataIndex: 'slaTargetDays', key: 'slaTargetDays', width: 80,
      render: v => <span style={{ color: 'var(--ci-text-muted)' }}>{v}d</span>,
    },
    {
      title: 'Actual', dataIndex: 'actualDays', key: 'actualDays', width: 80,
      sorter: (a, b) => a.actualDays - b.actualDays,
      render: v => <span style={{ fontWeight: 500 }}>{v}d</span>,
    },
    {
      title: 'Overdue', dataIndex: 'daysOverdue', key: 'daysOverdue', width: 90,
      sorter: (a, b) => (a.daysOverdue ?? 0) - (b.daysOverdue ?? 0),
      render: v => (
        <span style={{ fontWeight: 500, color: 'var(--ci-danger-text, #B22C2B)' }}>
          +{v ?? 0}d
        </span>
      ),
    },
    {
      title: 'Severity', dataIndex: 'severity', key: 'severity', width: 120,
      render: (v: string) => {
        if (!v) return <span style={{ color: 'var(--ci-text-muted)' }}>—</span>;
        const meta = SEVERITY_META[v] ?? { tone: 'neutral' as BadgeTone, status: 'muted' as StatusTone };
        return <StatusDot tone={meta.status}>{v}</StatusDot>;
      },
    },
    {
      title: 'Escalated', dataIndex: 'escalated', key: 'escalated', width: 100,
      render: v => v
        ? <Badge tone="red">Escalated</Badge>
        : <span style={{ color: 'var(--ci-text-muted)', fontSize: 11 }}>—</span>,
    },
    {
      title: 'Date', dataIndex: 'violationDate', key: 'violationDate', width: 110,
      render: v => <span style={{ color: 'var(--ci-text-secondary)' }}>{v?.split('T')[0] ?? '—'}</span>,
    },
  ];

  const sevCycle: State['severityFilter'][] = ['ALL', 'HIGH', 'MEDIUM', 'LOW'];

  return (
    <div>
      <PageHeader
        title="SLA violations"
        subtitle={`${state.items.length} breach${state.items.length === 1 ? '' : 'es'} · ${escalated} escalated · avg ${avgOverdue.toFixed(1)}d overdue`}
        actions={
          <>
            <Chip
              dropdown
              active={state.severityFilter !== 'ALL'}
              onClick={() => {
                const i = sevCycle.indexOf(state.severityFilter);
                dispatch({ type: 'SET_SEVERITY', payload: sevCycle[(i + 1) % sevCycle.length] });
              }}
            >
              {state.severityFilter === 'ALL' ? 'All severities' : state.severityFilter}
            </Chip>
            <Chip
              active={state.escalatedOnly}
              onClick={() => dispatch({ type: 'TOGGLE_ESCALATED' })}
            >
              {state.escalatedOnly ? 'Escalated ✓' : 'Escalated only'}
            </Chip>
            <GhostButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>
              Refresh
            </GhostButton>
            <DarkButton onClick={() => dispatch({ type: 'OPEN_MODAL' })} icon={<Plus size={12} strokeWidth={2} />}>
              Record violation
            </DarkButton>
          </>
        }
      />

      {/* KPI row */}
      <div style={styles.kpiRow}>
        <KpiCard label="Total violations" value={state.items.length.toLocaleString()} delta="Breach count" deltaDirection="flat" />
        <KpiCard label="High severity"    value={high.toLocaleString()} delta={high > 0 ? 'Action required' : 'None open'} deltaDirection={high > 0 ? 'up' : 'flat'} deltaTone={high > 0 ? 'down' : 'up'} tone={high > 0 ? 'danger' : 'default'} />
        <KpiCard label="Escalated"        value={escalated.toLocaleString()} delta={`${state.items.length ? Math.round((escalated / state.items.length) * 100) : 0}% of total`} deltaDirection="flat" tone={escalated > 0 ? 'warning' : 'default'} />
        <KpiCard label="Avg overdue"      value={`${avgOverdue.toFixed(1)}d`} delta={avgOverdue > 3 ? 'Above target' : 'Within tolerance'} deltaDirection={avgOverdue > 3 ? 'up' : 'down'} deltaTone={avgOverdue > 3 ? 'down' : 'up'} />
      </div>

      {state.error && <Alert type="error" showIcon message={state.error} style={{ marginBottom: 12, borderRadius: 8 }} closable />}

      {/* Chart row */}
      <div style={styles.chartRow}>
        <DataCard title="Violations by type" subtitle="Most-frequent breach categories (top 6)">
          {byType.length === 0 ? (
            <EmptyState title="No violations recorded" description="Breaches will appear as SLA misses are logged." tone="positive" />
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={byType} margin={{ top: 4, right: 16, bottom: 0, left: -8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--ci-border)" vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 10, fill: 'var(--ci-text-secondary)' }} axisLine={false} tickLine={false} />
                <YAxis allowDecimals={false} tick={{ fontSize: 10, fill: 'var(--ci-text-muted)' }} axisLine={false} tickLine={false} />
                <RTooltip
                  cursor={{ fill: 'var(--ci-bg-surface-2)' }}
                  contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8, fontSize: 12 }}
                />
                <Bar dataKey="count" radius={[5, 5, 0, 0]} barSize={28}>
                  {byType.map((_, i) => (
                    <Cell key={i} fill={[CHART.amber, CHART.red, CHART.purple, CHART.blue, CHART.teal, CHART.coral][i % 6]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          )}
        </DataCard>

        <DataCard title="Severity mix" subtitle="Breakdown across HIGH · MEDIUM · LOW">
          {severityMix.length === 0 ? (
            <EmptyState title="No data" description="Severity mix appears once violations exist." />
          ) : (
            <div style={styles.severityList}>
              {severityMix.map((s) => {
                const total = high + medium + low;
                const pct = total ? Math.round((s.value / total) * 100) : 0;
                return (
                  <div key={s.name} style={styles.severityRow}>
                    <div style={styles.severityHeader}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
                        <span style={{ width: 8, height: 8, borderRadius: '50%', background: s.color, display: 'inline-block' }} />
                        <span style={{ fontWeight: 500, fontSize: 12 }}>{s.name}</span>
                      </span>
                      <span style={{ fontSize: 11, color: 'var(--ci-text-muted)' }}>
                        {s.value} · {pct}%
                      </span>
                    </div>
                    <div style={styles.severityTrack}>
                      <div style={{ ...styles.severityFill, width: `${pct}%`, background: s.color }} />
                    </div>
                  </div>
                );
              })}
              {escalated > 0 && (
                <div style={styles.escalatedNote}>
                  <AlertTriangle size={12} strokeWidth={1.8} color="var(--ci-danger-text, #B22C2B)" />
                  <span>{escalated} case{escalated === 1 ? '' : 's'} escalated to management</span>
                </div>
              )}
            </div>
          )}
        </DataCard>
      </div>

      <DataCard padding={0}>
        {!state.loading && filtered.length === 0 ? (
          <EmptyState
            title={state.severityFilter !== 'ALL' || state.escalatedOnly ? 'No matching violations' : 'No SLA violations'}
            description={state.severityFilter !== 'ALL' || state.escalatedOnly ? 'Try clearing filters to view all breaches.' : 'All deadlines are being met — nice work.'}
            tone={state.severityFilter === 'ALL' && !state.escalatedOnly ? 'positive' : 'neutral'}
            actions={(state.severityFilter !== 'ALL' || state.escalatedOnly) ? (
              <GhostButton onClick={() => {
                dispatch({ type: 'SET_SEVERITY', payload: 'ALL' });
                if (state.escalatedOnly) dispatch({ type: 'TOGGLE_ESCALATED' });
              }}>Clear filters</GhostButton>
            ) : undefined}
          />
        ) : (
          <Table
            rowKey="violationId"
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
        title="Record SLA violation"
        open={state.modalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_MODAL' }); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={state.submitting}
        okText="Record violation"
        destroyOnClose
        width={520}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Row gutter={12}>
            <Col span={12}><Form.Item name="claimId" label="Claim ID (numeric)" rules={[{ required: true }]}><Input type="number" min={1} placeholder="1" /></Form.Item></Col>
            <Col span={12}><Form.Item name="adjusterId" label="Adjuster ID" rules={[{ required: true }]}><Input type="number" min={1} placeholder="1" /></Form.Item></Col>
            <Col span={12}><Form.Item name="violationType" label="Violation type" rules={[{ required: true }]}><Input placeholder="RESPONSE_TIME" /></Form.Item></Col>
            <Col span={12}><Form.Item name="violationDate" label="Violation date" rules={[{ required: true }]}><DatePicker style={{ width: '100%' }} /></Form.Item></Col>
            <Col span={12}><Form.Item name="slaTargetDays" label="SLA target (days)" rules={[{ required: true }]}><Input type="number" min={1} placeholder="5" /></Form.Item></Col>
            <Col span={12}><Form.Item name="actualDays" label="Actual days" rules={[{ required: true }]}><Input type="number" min={1} placeholder="8" /></Form.Item></Col>
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
  severityList: {
    display: 'flex', flexDirection: 'column', gap: 14, padding: '4px 2px',
  },
  severityRow: { display: 'flex', flexDirection: 'column', gap: 6 },
  severityHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  severityTrack: {
    height: 8, borderRadius: 4,
    background: 'var(--ci-bg-surface-2)', overflow: 'hidden',
  },
  severityFill: { height: '100%', borderRadius: 4, transition: 'width 0.3s' },
  escalatedNote: {
    display: 'flex', alignItems: 'center', gap: 6,
    marginTop: 6, padding: '8px 10px',
    background: 'var(--ci-bg-surface-2)',
    borderRadius: 6, fontSize: 11,
    color: 'var(--ci-text-secondary)',
  },
};
