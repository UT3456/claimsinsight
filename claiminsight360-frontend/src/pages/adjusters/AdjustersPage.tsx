import { useEffect, useReducer, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Typography, Space, Tag,
  Alert, Tooltip, Popconfirm, Card, Row, Col, Statistic, Progress,
} from 'antd';
import { PlusOutlined, ReloadOutlined, DeleteOutlined, TrophyOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { adjustersApi, AdjusterPerformance, CreateAdjusterRequest } from '../../api/adjustersApi';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip as RTooltip, ResponsiveContainer,
  CartesianGrid, Legend, ComposedChart, Line,
} from 'recharts';
import ChartCard, { CHART_COLORS } from '../../components/charts/ChartCard';

const { Title, Text } = Typography;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  items: AdjusterPerformance[];
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
  period: string;
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
  | { type: 'DELETE_SUCCESS'; payload: number }
  | { type: 'SET_PERIOD'; payload: string };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'FETCH_START':    return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS':  return { ...state, loading: false, items: action.payload };
    case 'FETCH_ERROR':    return { ...state, loading: false, error: action.payload };
    case 'OPEN_MODAL':     return { ...state, modalOpen: true };
    case 'CLOSE_MODAL':    return { ...state, modalOpen: false, submitting: false };
    case 'SUBMIT_START':   return { ...state, submitting: true };
    case 'SUBMIT_SUCCESS': return { ...state, submitting: false, modalOpen: false, items: [...state.items, action.payload] };
    case 'SUBMIT_ERROR':   return { ...state, submitting: false };
    case 'DELETE_SUCCESS': return { ...state, items: state.items.filter(i => i.perfId !== action.payload) };
    case 'SET_PERIOD':     return { ...state, period: action.payload };
    default:               return state;
  }
}

// ── Component ─────────────────────────────────────────────────────────────────

function perfTag(flag: string) {
  if (!flag) return null;
  const color = flag.toLowerCase().includes('low') || flag.toLowerCase().includes('poor') ? 'red'
    : flag.toLowerCase().includes('high') || flag.toLowerCase().includes('good') ? 'green'
    : 'orange';
  return <Tag color={color}>{flag}</Tag>;
}

export default function AdjustersPage() {
  const [state, dispatch] = useReducer(reducer, {
    items: [], loading: false, error: null, modalOpen: false, submitting: false, period: '',
  });
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const data = await adjustersApi.getAll(state.period || undefined);
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load adjuster performance' });
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
      const record = await adjustersApi.create(req);
      dispatch({ type: 'SUBMIT_SUCCESS', payload: record });
      form.resetFields();
    } catch {
      dispatch({ type: 'SUBMIT_ERROR' });
    }
  };

  const handleDelete = async (id: number) => {
    await adjustersApi.delete(id);
    dispatch({ type: 'DELETE_SUCCESS', payload: id });
  };

  const avgTat = state.items.length
    ? (state.items.reduce((s, i) => s + (i.avgTat ?? 0), 0) / state.items.length).toFixed(1)
    : '—';

  // ── Chart data ────────────────────────────────────────────────────────────
  const topAdjusters = [...state.items]
    .sort((a, b) => b.claimsHandled - a.claimsHandled)
    .slice(0, 10)
    .map(i => ({
      name: `#${i.adjusterId}`,
      claims: i.claimsHandled,
      avgTat: Number((i.avgTat ?? 0).toFixed(1)),
      slaRate: Number((i.slaComplianceRate ?? 0).toFixed(1)),
    }));

  const columns: ColumnsType<AdjusterPerformance> = [
    { title: 'Adjuster ID',  dataIndex: 'adjusterId',        key: 'adjusterId',       width: 110 },
    { title: 'Period',       dataIndex: 'period',            key: 'period',            render: v => <Tag color="purple">{v}</Tag> },
    { title: 'Claims',       dataIndex: 'claimsHandled',     key: 'claimsHandled',     sorter: (a, b) => a.claimsHandled - b.claimsHandled },
    { title: 'Avg TAT',      dataIndex: 'avgTat',            key: 'avgTat',            render: v => `${Number(v ?? 0).toFixed(1)}d`, sorter: (a, b) => (a.avgTat ?? 0) - (b.avgTat ?? 0) },
    {
      title: 'SLA Compliance', dataIndex: 'slaComplianceRate', key: 'slaComplianceRate',
      render: v => <Progress percent={Math.round(v ?? 0)} size="small" style={{ width: 120 }} />,
      sorter: (a, b) => (a.slaComplianceRate ?? 0) - (b.slaComplianceRate ?? 0),
    },
    { title: 'Error Rate',   dataIndex: 'errorRate',         key: 'errorRate',         render: v => `${Number(v ?? 0).toFixed(1)}%` },
    { title: 'Performance',  dataIndex: 'performanceFlag',   key: 'performanceFlag',   render: v => perfTag(v) },
    { title: 'Productivity', dataIndex: 'productivityFlag',  key: 'productivityFlag',  render: v => perfTag(v) },
    {
      title: 'Action', key: 'action', width: 80,
      render: (_, rec) => (
        <Popconfirm title="Delete?" onConfirm={() => handleDelete(rec.perfId)}>
          <Tooltip title="Delete"><Button icon={<DeleteOutlined />} size="small" danger /></Tooltip>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0 }}>Adjuster Performance</Title>
          <Text type="secondary">Claims handler metrics and efficiency scores</Text>
        </div>
        <Space>
          <Input
            placeholder="Period (e.g. Q1-2024)"
            value={state.period}
            onChange={e => dispatch({ type: 'SET_PERIOD', payload: e.target.value })}
            style={{ width: 180 }}
            allowClear
          />
          <Button icon={<ReloadOutlined />} onClick={load}>Filter</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_MODAL' })}>
            Add Record
          </Button>
        </Space>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        {[
          { title: 'Total Records',  value: state.items.length,   icon: null },
          { title: 'Avg TAT (days)', value: avgTat,               icon: null },
          { title: 'Top Performers', value: state.items.filter(i => (i.slaComplianceRate ?? 0) >= 90).length, icon: <TrophyOutlined /> },
        ].map(c => (
          <Col xs={24} sm={8} key={c.title}>
            <Card style={styles.statCard}>
              <Statistic title={c.title} value={c.value} prefix={c.icon} />
            </Card>
          </Col>
        ))}
      </Row>

      {state.error && <Alert type="error" message={state.error} style={{ marginBottom: 16 }} />}

      {/* Charts */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={14}>
          <ChartCard
            title="Top 10 Adjusters — Claims vs. Avg TAT"
            subtitle="Bars = claims handled, line = average turnaround (days)"
            loading={state.loading}
            isEmpty={!state.loading && topAdjusters.length === 0}
          >
            <ResponsiveContainer width="100%" height="100%">
              <ComposedChart data={topAdjusters} margin={{ top: 10, right: 20, bottom: 0, left: -10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--ci-border)" />
                <XAxis dataKey="name" tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <YAxis yAxisId="left"  tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <RTooltip contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8 }} />
                <Legend wrapperStyle={{ fontSize: 12 }} />
                <Bar  yAxisId="left"  dataKey="claims" fill={CHART_COLORS.primary} radius={[6, 6, 0, 0]} name="Claims Handled" />
                <Line yAxisId="right" type="monotone" dataKey="avgTat" stroke={CHART_COLORS.danger}
                  strokeWidth={2.5} dot={{ r: 4 }} name="Avg TAT (days)" />
              </ComposedChart>
            </ResponsiveContainer>
          </ChartCard>
        </Col>

        <Col xs={24} lg={10}>
          <ChartCard
            title="SLA Compliance per Adjuster"
            subtitle="Top 10 adjusters by claim volume"
            loading={state.loading}
            isEmpty={!state.loading && topAdjusters.length === 0}
          >
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={topAdjusters} layout="vertical" margin={{ top: 10, right: 20, bottom: 0, left: 10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--ci-border)" />
                <XAxis type="number" domain={[0, 100]} tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} unit="%" />
                <YAxis dataKey="name" type="category" width={50} tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <RTooltip formatter={(v) => `${Number(v)}%`}
                  contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8 }} />
                <Bar dataKey="slaRate" fill={CHART_COLORS.success} radius={[0, 6, 6, 0]} name="SLA %" />
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </Col>
      </Row>

      <Table
        rowKey="perfId"
        columns={columns}
        dataSource={state.items}
        loading={state.loading}
        pagination={{ pageSize: 10 }}
        bordered size="middle"
        scroll={{ x: 900 }}
      />

      <Modal
        title="Add Adjuster Performance Record"
        open={state.modalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_MODAL' }); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={state.submitting}
        destroyOnClose
        width={560}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="adjusterId" label="Adjuster ID" rules={[{ required: true }]}>
                <Input type="number" min={1} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="period" label="Period" rules={[{ required: true }]}>
                <Input placeholder="Q1-2024" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="claimsHandled" label="Claims Handled">
                <Input type="number" min={0} defaultValue={0} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="totalDaysTaken" label="Total Days Taken">
                <Input type="number" min={0} defaultValue={0} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="slaMetCount" label="SLA Met Count">
                <Input type="number" min={0} defaultValue={0} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="slaBreachedCount" label="SLA Breached">
                <Input type="number" min={0} defaultValue={0} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="deniedClaimsCount" label="Denied Claims">
                <Input type="number" min={0} defaultValue={0} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="errorRate" label="Error Rate (%)">
                <Input type="number" min={0} max={100} defaultValue={0} />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  header:   { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24, flexWrap: 'wrap', gap: 12 },
  statCard: { borderRadius: 10, border: '1px solid #e2e8f0' },
};
