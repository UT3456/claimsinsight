import { useEffect, useReducer, useCallback, useState } from 'react';
import {
  Table, Button, Modal, Form, Input, Select,
  DatePicker, Typography, Space, Tag, Alert,
  Tooltip, Popconfirm, Card, Row, Col, Statistic,
  App as AntApp,
} from 'antd';
import {
  PlusOutlined, ReloadOutlined, DeleteOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { claimsApi, ClaimKpi, METRIC_NAMES, type ClaimStatus } from '../../api/claimsApi';
import {
  LineChart, Line, XAxis, YAxis, Tooltip as RTooltip, ResponsiveContainer,
  CartesianGrid, Legend, BarChart, Bar, Cell,
} from 'recharts';
import ChartCard, { CHART_PALETTE } from '../../components/charts/ChartCard';

const { Title, Text } = Typography;
const { Option } = Select;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  items: ClaimKpi[];
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
  filterMetric: string | null;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: ClaimKpi[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'OPEN_MODAL' }
  | { type: 'CLOSE_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'SUBMIT_SUCCESS'; payload: ClaimKpi }
  | { type: 'SUBMIT_ERROR' }
  | { type: 'DELETE_SUCCESS'; payload: number }
  | { type: 'SET_FILTER'; payload: string | null };

const initial: State = {
  items: [], loading: false, error: null,
  modalOpen: false, submitting: false,
  filterMetric: null,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'FETCH_START':        return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS':      return { ...state, loading: false, items: action.payload };
    case 'FETCH_ERROR':        return { ...state, loading: false, error: action.payload };
    case 'OPEN_MODAL':         return { ...state, modalOpen: true };
    case 'CLOSE_MODAL':        return { ...state, modalOpen: false, submitting: false };
    case 'SUBMIT_START':       return { ...state, submitting: true };
    case 'SUBMIT_SUCCESS':     return { ...state, submitting: false, modalOpen: false, items: [...state.items, action.payload] };
    case 'SUBMIT_ERROR':       return { ...state, submitting: false };
    case 'DELETE_SUCCESS':     return { ...state, items: state.items.filter(i => i.kpiId !== action.payload) };
    case 'SET_FILTER':         return { ...state, filterMetric: action.payload };
    default:                   return state;
  }
}

// ── Component ─────────────────────────────────────────────────────────────────

const METRIC_COLORS: Record<string, string> = {
  TAT: 'blue', CYCLE_TIME: 'purple', SEVERITY: 'red',
  FREQUENCY: 'orange', LOSS_RATIO: 'green',
};

// ── Claim status (persisted by claims-metrics-service; localStorage is cache) ─
const STATUS_STORAGE_KEY = 'ci360.claimStatus.v1';

function loadStatusMap(): Record<string, ClaimStatus> {
  try {
    const raw = localStorage.getItem(STATUS_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Record<string, ClaimStatus>) : {};
  } catch { return {}; }
}

function saveStatusMap(map: Record<string, ClaimStatus>) {
  try { localStorage.setItem(STATUS_STORAGE_KEY, JSON.stringify(map)); } catch { /* ignore */ }
}

export default function ClaimsPage() {
  const [state, dispatch] = useReducer(reducer, initial);
  const [form] = Form.useForm();
  const [statusMap, setStatusMap] = useState<Record<string, ClaimStatus>>(() => loadStatusMap());
  // Scoped message API (AntApp) — theme-aware and correctly placed in the tree.
  const { message } = AntApp.useApp();

  // Hydrate from backend on mount; localStorage acts as optimistic cache until the fetch resolves.
  useEffect(() => {
    let cancelled = false;
    claimsApi.getAllClaimStatuses()
      .then(remote => {
        if (cancelled) return;
        setStatusMap(remote);
        saveStatusMap(remote);
      })
      .catch(() => { /* keep cached map */ });
    return () => { cancelled = true; };
  }, []);

  const updateStatus = useCallback((claimId: string, status: ClaimStatus) => {
    // Optimistic update + write-through cache; persist to backend.
    setStatusMap(prev => {
      const next = { ...prev, [claimId]: status };
      saveStatusMap(next);
      return next;
    });
    claimsApi.updateClaimStatus(claimId, status)
      .then(() => message.success(`Claim ${claimId} marked ${status.toLowerCase()}`))
      .catch(() => {
        // Revert on failure.
        setStatusMap(prev => {
          const next = { ...prev };
          delete next[claimId];
          saveStatusMap(next);
          return next;
        });
        message.error(`Failed to update ${claimId} — change reverted`);
      });
  }, [message]);

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const data = state.filterMetric
        ? await claimsApi.getByMetric(state.filterMetric)
        : await claimsApi.getAll();
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load KPIs' });
    }
  }, [state.filterMetric]);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (values: Record<string, unknown>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const record = await claimsApi.create({
        claimId:     values.claimId as string,
        metricName:  values.metricName as string,
        metricValue: values.metricValue as number,
        metricDate:  (values.metricDate as dayjs.Dayjs).format('YYYY-MM-DD'),
      });
      dispatch({ type: 'SUBMIT_SUCCESS', payload: record });
      form.resetFields();
      message.success(`KPI added for ${record.claimId}`);
    } catch {
      dispatch({ type: 'SUBMIT_ERROR' });
      message.error('Failed to add KPI. Check the fields and try again.');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await claimsApi.delete(id);
      dispatch({ type: 'DELETE_SUCCESS', payload: id });
      message.success('KPI deleted');
    } catch {
      message.error('Failed to delete KPI');
    }
  };

  // Stat summary
  const uniqueClaimIds = Array.from(new Set(state.items.map(i => i.claimId)));
  const uniqueClaims   = uniqueClaimIds.length;
  const activeClaims   = uniqueClaimIds.filter(id => (statusMap[id] ?? 'ACTIVE') === 'ACTIVE').length;
  const inactiveClaims = uniqueClaims - activeClaims;
  const avgValue = state.items.length
    ? (state.items.reduce((s, i) => s + i.metricValue, 0) / state.items.length).toFixed(2)
    : '—';

  // ── Chart data ────────────────────────────────────────────────────────────
  // Average value per metric across all claims
  const metricAverages = METRIC_NAMES.map(m => {
    const vals = state.items.filter(i => i.metricName === m);
    const avg = vals.length ? vals.reduce((s, i) => s + i.metricValue, 0) / vals.length : 0;
    return { name: m, avg: Number(avg.toFixed(2)), count: vals.length };
  }).filter(x => x.count > 0);

  // Trend per metric over time (grouped by date)
  const dates = Array.from(new Set(state.items.map(i => i.metricDate))).sort();
  const trendData = dates.map(d => {
    const row: Record<string, string | number> = { date: d };
    METRIC_NAMES.forEach(m => {
      const matches = state.items.filter(i => i.metricDate === d && i.metricName === m);
      if (matches.length) {
        row[m] = Number((matches.reduce((s, v) => s + v.metricValue, 0) / matches.length).toFixed(2));
      }
    });
    return row;
  });

  const columns: ColumnsType<ClaimKpi> = [
    { title: 'KPI ID',       dataIndex: 'kpiId',       key: 'kpiId',       width: 80 },
    { title: 'Claim ID',     dataIndex: 'claimId',     key: 'claimId',     render: v => <Text code>{v}</Text> },
    {
      title: 'Metric',       dataIndex: 'metricName',  key: 'metricName',
      render: v => <Tag color={METRIC_COLORS[v] ?? 'default'}>{v}</Tag>,
    },
    {
      title: 'Value',        dataIndex: 'metricValue', key: 'metricValue',
      render: v => <Text strong>{Number(v).toLocaleString()}</Text>,
      sorter: (a, b) => a.metricValue - b.metricValue,
    },
    { title: 'Date',         dataIndex: 'metricDate',  key: 'metricDate'   },
    {
      title: 'Status', key: 'status', width: 140,
      filters: [
        { text: 'Active',   value: 'ACTIVE' },
        { text: 'Inactive', value: 'INACTIVE' },
      ],
      onFilter: (val, rec) => (statusMap[rec.claimId] ?? 'ACTIVE') === val,
      render: (_, rec) => {
        const current: ClaimStatus = statusMap[rec.claimId] ?? 'ACTIVE';
        return (
          <Select
            size="small"
            value={current}
            style={{ width: 110 }}
            onChange={(v) => updateStatus(rec.claimId, v as ClaimStatus)}
            options={[
              { value: 'ACTIVE',   label: <span><span style={{ color: '#27500A' }}>●</span> Active</span> },
              { value: 'INACTIVE', label: <span><span style={{ color: '#888780' }}>●</span> Inactive</span> },
            ]}
          />
        );
      },
    },
    {
      title: 'Action', key: 'action', width: 80,
      render: (_, rec) => (
        <Popconfirm title="Delete this KPI?" onConfirm={() => handleDelete(rec.kpiId)}>
          <Tooltip title="Delete"><Button icon={<DeleteOutlined />} size="small" danger /></Tooltip>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0 }}>Claims KPIs</Title>
          <Text type="secondary">Performance metrics across all claims</Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_MODAL' })}>
            Add KPI
          </Button>
        </Space>
      </div>

      {/* Stats */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        {[
          { title: 'Total Records',   value: state.items.length },
          { title: 'Unique Claims',   value: uniqueClaims },
          { title: 'Active Claims',   value: activeClaims,   valueStyle: { color: '#27500A' } },
          { title: 'Inactive Claims', value: inactiveClaims, valueStyle: { color: '#888780' } },
          { title: 'Average Value',   value: avgValue },
        ].map(c => (
          <Col xs={24} sm={8} lg={4} key={c.title}>
            <Card style={styles.statCard}>
              <Statistic title={c.title} value={c.value} valueStyle={c.valueStyle} />
            </Card>
          </Col>
        ))}
      </Row>

      {/* Filter */}
      <Space style={{ marginBottom: 16 }}>
        <Text>Filter by metric:</Text>
        <Select
          allowClear
          placeholder="All metrics"
          style={{ width: 180 }}
          value={state.filterMetric}
          onChange={v => dispatch({ type: 'SET_FILTER', payload: v ?? null })}
        >
          {METRIC_NAMES.map(m => <Option key={m} value={m}>{m}</Option>)}
        </Select>
      </Space>

      {state.error && <Alert type="error" message={state.error} style={{ marginBottom: 16 }} />}

      {/* Charts */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={14}>
          <ChartCard
            title="KPI Trends Over Time"
            subtitle="Average metric value per date (all metrics)"
            loading={state.loading}
            isEmpty={!state.loading && trendData.length === 0}
          >
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={trendData} margin={{ top: 10, right: 10, bottom: 0, left: -10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--ci-border)" />
                <XAxis dataKey="date" tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <YAxis tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <RTooltip contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8 }} />
                <Legend wrapperStyle={{ fontSize: 12 }} />
                {METRIC_NAMES.map((m, i) => (
                  <Line key={m} type="monotone" dataKey={m}
                    stroke={CHART_PALETTE[i % CHART_PALETTE.length]}
                    strokeWidth={2} dot={{ r: 3 }} connectNulls />
                ))}
              </LineChart>
            </ResponsiveContainer>
          </ChartCard>
        </Col>

        <Col xs={24} lg={10}>
          <ChartCard
            title="Average KPI Value by Metric"
            subtitle="Mean across all recorded claims"
            loading={state.loading}
            isEmpty={!state.loading && metricAverages.length === 0}
          >
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={metricAverages} margin={{ top: 10, right: 10, bottom: 0, left: -20 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--ci-border)" />
                <XAxis dataKey="name" tick={{ fontSize: 10, fill: 'var(--ci-text-secondary)' }} />
                <YAxis tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <RTooltip contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8 }} />
                <Bar dataKey="avg" radius={[6, 6, 0, 0]}>
                  {metricAverages.map((_, i) => (
                    <Cell key={i} fill={CHART_PALETTE[i % CHART_PALETTE.length]} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </Col>
      </Row>

      <Table
        rowKey="kpiId"
        columns={columns}
        dataSource={state.items}
        loading={state.loading}
        pagination={{ pageSize: 10 }}
        bordered
        size="middle"
      />

      {/* Create Modal */}
      <Modal
        title="Add KPI Record"
        open={state.modalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_MODAL' }); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={state.submitting}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="claimId" label="Claim ID" rules={[{ required: true }]}>
            <Input placeholder="CLM-2026-AUTO-001" />
          </Form.Item>
          <Form.Item name="metricName" label="Metric Name" rules={[{ required: true }]}>
            <Select placeholder="Select metric">
              {METRIC_NAMES.map(m => <Option key={m} value={m}>{m}</Option>)}
            </Select>
          </Form.Item>
          <Form.Item name="metricValue" label="Metric Value" rules={[{ required: true }]}>
            <Input type="number" placeholder="0.00" />
          </Form.Item>
          <Form.Item name="metricDate" label="Metric Date" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  header:   { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24, flexWrap: 'wrap', gap: 12 },
  statCard: { borderRadius: 10, border: '1px solid #e2e8f0' },
};
