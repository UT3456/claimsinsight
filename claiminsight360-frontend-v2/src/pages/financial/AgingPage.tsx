import { useEffect, useReducer, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, Typography,
  Space, Tag, Alert, Tooltip, Popconfirm, Card, Row, Col, Statistic,
} from 'antd';
import { PlusOutlined, ReloadOutlined, DeleteOutlined, ClockCircleOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { agingApi, AgingRecord, CreateAgingRequest, AGING_BUCKETS, AGING_BUCKET_LABELS } from '../../api/financialApi';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip as RTooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend, CartesianGrid, LabelList,
} from 'recharts';
import ChartCard, { CHART_COLORS } from '../../components/charts/ChartCard';

const BUCKET_CHART_COLORS: Record<string, string> = {
  'BUCKET_0_30':   CHART_COLORS.success,
  'BUCKET_31_60':  CHART_COLORS.warn,
  'BUCKET_61_90':  CHART_COLORS.accent2,
  'BUCKET_90_PLUS':CHART_COLORS.danger,
};

const { Title, Text } = Typography;
const { Option } = Select;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  items: AgingRecord[];
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
  filterBucket: string | null;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: AgingRecord[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'OPEN_MODAL' }
  | { type: 'CLOSE_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'SUBMIT_SUCCESS'; payload: AgingRecord }
  | { type: 'SUBMIT_ERROR' }
  | { type: 'DELETE_SUCCESS'; payload: number }
  | { type: 'SET_FILTER'; payload: string | null };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'FETCH_START':    return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS':  return { ...state, loading: false, items: action.payload };
    case 'FETCH_ERROR':    return { ...state, loading: false, error: action.payload };
    case 'OPEN_MODAL':     return { ...state, modalOpen: true };
    case 'CLOSE_MODAL':    return { ...state, modalOpen: false, submitting: false };
    case 'SUBMIT_START':   return { ...state, submitting: true };
    case 'SUBMIT_SUCCESS': return { ...state, submitting: false, modalOpen: false, items: [action.payload, ...state.items] };
    case 'SUBMIT_ERROR':   return { ...state, submitting: false };
    case 'DELETE_SUCCESS': return { ...state, items: state.items.filter(i => i.agingId !== action.payload) };
    case 'SET_FILTER':     return { ...state, filterBucket: action.payload };
    default:               return state;
  }
}

// ── Component ─────────────────────────────────────────────────────────────────

const BUCKET_COLORS: Record<string, string> = {
  BUCKET_0_30: 'green', BUCKET_31_60: 'gold', BUCKET_61_90: 'orange', BUCKET_90_PLUS: 'red',
};

export default function AgingPage() {
  const [state, dispatch] = useReducer(reducer, {
    items: [], loading: false, error: null, modalOpen: false, submitting: false, filterBucket: null,
  });
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const data = state.filterBucket
        ? await agingApi.getByBucket(state.filterBucket)
        : await agingApi.getAll();
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load aging records' });
    }
  }, [state.filterBucket]);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (values: Record<string, unknown>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const req: CreateAgingRequest = {
        claimId:     values.claimId as string,
        agingDays:   Number(values.agingDays),
        agingBucket: values.agingBucket as string,
      };
      const record = await agingApi.create(req);
      dispatch({ type: 'SUBMIT_SUCCESS', payload: record });
      form.resetFields();
    } catch {
      dispatch({ type: 'SUBMIT_ERROR' });
    }
  };

  const handleDelete = async (id: number) => {
    await agingApi.delete(id);
    dispatch({ type: 'DELETE_SUCCESS', payload: id });
  };

  // Bucket distribution counts
  const bucketCounts = AGING_BUCKETS.reduce((acc, b) => ({
    ...acc, [b]: state.items.filter(i => i.agingBucket === b).length,
  }), {} as Record<string, number>);

  const columns: ColumnsType<AgingRecord> = [
    { title: 'ID',         dataIndex: 'agingId',     key: 'agingId',     width: 80 },
    { title: 'Claim ID',   dataIndex: 'claimId',     key: 'claimId',     render: v => <Text code>{v}</Text> },
    {
      title: 'Aging Days', dataIndex: 'agingDays',   key: 'agingDays',
      sorter: (a, b) => a.agingDays - b.agingDays,
      render: v => <Text strong>{v}</Text>,
    },
    {
      title: 'Bucket',     dataIndex: 'agingBucket', key: 'agingBucket',
      render: v => <Tag color={BUCKET_COLORS[v] ?? 'default'}>{AGING_BUCKET_LABELS[v] ?? v}</Tag>,
    },
    {
      title: 'Action', key: 'action', width: 80,
      render: (_, rec) => (
        <Popconfirm title="Delete this record?" onConfirm={() => handleDelete(rec.agingId)}>
          <Tooltip title="Delete"><Button icon={<DeleteOutlined />} size="small" danger /></Tooltip>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0 }}>Claim Aging</Title>
          <Text type="secondary">Age distribution of open claims</Text>
        </div>
        <Space>
          <Select
            allowClear
            placeholder="Filter by bucket"
            style={{ width: 160 }}
            value={state.filterBucket}
            onChange={v => dispatch({ type: 'SET_FILTER', payload: v ?? null })}
          >
            {AGING_BUCKETS.map(b => <Option key={b} value={b}>{AGING_BUCKET_LABELS[b]}</Option>)}
          </Select>
          <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_MODAL' })}>
            Add Record
          </Button>
        </Space>
      </div>

      {/* Distribution */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        {AGING_BUCKETS.map(b => (
          <Col xs={12} sm={6} key={b}>
            <Card style={{ borderRadius: 10, border: `1px solid`, borderColor: BUCKET_COLORS[b] === 'red' ? '#fecaca' : '#e2e8f0' }}>
              <Statistic
                title={<Tag color={BUCKET_COLORS[b]}>{AGING_BUCKET_LABELS[b]}</Tag>}
                value={bucketCounts[b] ?? 0}
                prefix={<ClockCircleOutlined />}
                valueStyle={{ color: BUCKET_COLORS[b] === 'red' ? '#dc2626' : BUCKET_COLORS[b] === 'orange' ? '#d97706' : undefined }}
              />
            </Card>
          </Col>
        ))}
      </Row>

      {(() => {
        const bucketData = AGING_BUCKETS.map(b => ({
          name:  AGING_BUCKET_LABELS[b] ?? b,
          count: bucketCounts[b] ?? 0,
          color: BUCKET_CHART_COLORS[b] ?? CHART_COLORS.info,
        }));
        const hasData = bucketData.some(d => d.count > 0);
        return (
          <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
            <Col xs={24} lg={14}>
              <ChartCard
                title="Aging Distribution"
                subtitle="Number of claims in each aging bucket"
                loading={state.loading}
                isEmpty={!state.loading && !hasData}
              >
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={bucketData} margin={{ top: 20, right: 10, bottom: 0, left: -20 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--ci-border)" />
                    <XAxis dataKey="name" tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                    <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                    <RTooltip contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8 }} />
                    <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                      <LabelList dataKey="count" position="top" style={{ fill: 'var(--ci-text-primary)', fontSize: 12 }} />
                      {bucketData.map((b, i) => <Cell key={i} fill={b.color} />)}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </ChartCard>
            </Col>

            <Col xs={24} lg={10}>
              <ChartCard
                title="Aging Share"
                subtitle="Proportional view of bucket mix"
                loading={state.loading}
                isEmpty={!state.loading && !hasData}
              >
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie data={bucketData.filter(b => b.count > 0)} dataKey="count" nameKey="name"
                      cx="50%" cy="50%" innerRadius={55} outerRadius={95} paddingAngle={2}>
                      {bucketData.filter(b => b.count > 0).map((b, i) => <Cell key={i} fill={b.color} />)}
                    </Pie>
                    <Legend verticalAlign="bottom" iconType="circle" wrapperStyle={{ fontSize: 12 }} />
                    <RTooltip contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8 }} />
                  </PieChart>
                </ResponsiveContainer>
              </ChartCard>
            </Col>
          </Row>
        );
      })()}

      {state.error && <Alert type="error" message={state.error} style={{ marginBottom: 16 }} />}

      <Table
        rowKey="agingId"
        columns={columns}
        dataSource={state.items}
        loading={state.loading}
        pagination={{ pageSize: 10 }}
        bordered size="middle"
      />

      <Modal
        title="Add Aging Record"
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
          <Form.Item name="agingDays" label="Aging Days" rules={[{ required: true }]}>
            <Input type="number" min={0} placeholder="45" />
          </Form.Item>
          <Form.Item name="agingBucket" label="Aging Bucket" rules={[{ required: true }]}>
            <Select placeholder="Select bucket">
              {AGING_BUCKETS.map(b => (
                <Option key={b} value={b}>
                  <Tag color={BUCKET_COLORS[b]}>{AGING_BUCKET_LABELS[b]}</Tag>
                </Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24, flexWrap: 'wrap', gap: 12 },
};
