import { useEffect, useReducer, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, DatePicker,
  Typography, Space, Tag, Alert, Tooltip, Popconfirm,
  Card, Row, Col, Statistic,
} from 'antd';
import { PlusOutlined, ReloadOutlined, DeleteOutlined, DollarOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { costsApi, ClaimCost, CreateCostRequest, COST_TYPES } from '../../api/financialApi';
import {
  PieChart, Pie, Cell, Legend, ResponsiveContainer, Tooltip as RTooltip,
  AreaChart, Area, XAxis, YAxis, CartesianGrid,
} from 'recharts';
import ChartCard, { CHART_COLORS, CHART_PALETTE } from '../../components/charts/ChartCard';

const { Title, Text } = Typography;
const { Option } = Select;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  items: ClaimCost[];
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: ClaimCost[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'OPEN_MODAL' }
  | { type: 'CLOSE_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'SUBMIT_SUCCESS'; payload: ClaimCost }
  | { type: 'SUBMIT_ERROR' }
  | { type: 'DELETE_SUCCESS'; payload: number };

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
    case 'DELETE_SUCCESS': return { ...state, items: state.items.filter(i => i.costId !== action.payload) };
    default:               return state;
  }
}

// ── Component ─────────────────────────────────────────────────────────────────

const COST_COLORS: Record<string, string> = { MEDICAL: 'red', LEGAL: 'purple', REPAIR: 'blue', SETTLEMENT: 'green' };

export default function CostsPage() {
  const [state, dispatch] = useReducer(reducer, {
    items: [], loading: false, error: null, modalOpen: false, submitting: false,
  });
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const data = await costsApi.getAll();
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load costs' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (values: Record<string, unknown>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const req: CreateCostRequest = {
        claimId:  values.claimId as string,
        costType: values.costType as string,
        amount:   Number(values.amount),
        costDate: (values.costDate as dayjs.Dayjs).format('YYYY-MM-DD'),
      };
      const record = await costsApi.create(req);
      dispatch({ type: 'SUBMIT_SUCCESS', payload: record });
      form.resetFields();
    } catch {
      dispatch({ type: 'SUBMIT_ERROR' });
    }
  };

  const handleDelete = async (id: number) => {
    await costsApi.delete(id);
    dispatch({ type: 'DELETE_SUCCESS', payload: id });
  };

  const totalAmount = state.items.reduce((s, i) => s + (i.amount ?? 0), 0);
  const byCostType = COST_TYPES.reduce((acc, t) => ({
    ...acc,
    [t]: state.items.filter(i => i.costType === t).reduce((s, i) => s + i.amount, 0),
  }), {} as Record<string, number>);

  // ── Chart data ────────────────────────────────────────────────────────────
  const costTypeData = COST_TYPES
    .map((t) => ({ name: t, value: Number((byCostType[t] ?? 0).toFixed(2)) }))
    .filter(d => d.value > 0);

  const monthlyTrend = Object.entries(
    state.items.reduce<Record<string, number>>((acc, i) => {
      const m = i.costDate?.slice(0, 7);
      if (!m) return acc;
      acc[m] = (acc[m] ?? 0) + i.amount;
      return acc;
    }, {}),
  )
    .map(([month, amount]) => ({ month, amount: Number(amount.toFixed(2)) }))
    .sort((a, b) => a.month.localeCompare(b.month));

  const columns: ColumnsType<ClaimCost> = [
    { title: 'Cost ID',  dataIndex: 'costId',   key: 'costId',  width: 90 },
    { title: 'Claim ID', dataIndex: 'claimId',  key: 'claimId', render: v => <Text code>{v}</Text> },
    {
      title: 'Type', dataIndex: 'costType', key: 'costType',
      render: v => <Tag color={COST_COLORS[v] ?? 'default'}>{v}</Tag>,
      filters: COST_TYPES.map(t => ({ text: t, value: t })),
      onFilter: (value, rec) => rec.costType === value,
    },
    {
      title: 'Amount', dataIndex: 'amount', key: 'amount',
      render: v => <Text strong>${Number(v).toLocaleString('en-US', { minimumFractionDigits: 2 })}</Text>,
      sorter: (a, b) => a.amount - b.amount,
    },
    { title: 'Cost Date', dataIndex: 'costDate', key: 'costDate', sorter: (a, b) => a.costDate.localeCompare(b.costDate) },
    {
      title: 'Action', key: 'action', width: 80,
      render: (_, rec) => (
        <Popconfirm title="Delete this cost?" onConfirm={() => handleDelete(rec.costId)}>
          <Tooltip title="Delete"><Button icon={<DeleteOutlined />} size="small" danger /></Tooltip>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0 }}>Claim Costs</Title>
          <Text type="secondary">Financial cost records per claim</Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_MODAL' })}>
            Add Cost
          </Button>
        </Space>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={6}>
          <Card style={styles.statCard}>
            <Statistic title="Total Amount" value={totalAmount}
              prefix={<DollarOutlined />}
              formatter={v => Number(v).toLocaleString('en-US', { minimumFractionDigits: 2 })} />
          </Card>
        </Col>
        {COST_TYPES.map(t => (
          <Col xs={12} sm={5} key={t} style={{ flex: 1 }}>
            <Card style={styles.statCard}>
              <Statistic title={t} value={byCostType[t] ?? 0}
                prefix="$"
                formatter={v => Number(v).toLocaleString('en-US', { minimumFractionDigits: 0 })}
                valueStyle={{ color: COST_COLORS[t] === 'red' ? '#dc2626' : undefined, fontSize: 18 }} />
            </Card>
          </Col>
        ))}
      </Row>

      {state.error && <Alert type="error" message={state.error} style={{ marginBottom: 16 }} />}

      {/* Charts */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={10}>
          <ChartCard
            title="Cost Mix by Type"
            subtitle="Total spend per cost category"
            loading={state.loading}
            isEmpty={!state.loading && costTypeData.length === 0}
          >
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={costTypeData} dataKey="value" nameKey="name"
                  cx="50%" cy="50%" innerRadius={55} outerRadius={95} paddingAngle={2}>
                  {costTypeData.map((_, i) => (
                    <Cell key={i} fill={CHART_PALETTE[i % CHART_PALETTE.length]} />
                  ))}
                </Pie>
                <Legend verticalAlign="bottom" iconType="circle" wrapperStyle={{ fontSize: 12 }} />
                <RTooltip formatter={(v) => `$${Number(v).toLocaleString()}`}
                  contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8 }} />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>
        </Col>

        <Col xs={24} lg={14}>
          <ChartCard
            title="Monthly Cost Trend"
            subtitle="Aggregate spend across all claims"
            loading={state.loading}
            isEmpty={!state.loading && monthlyTrend.length === 0}
          >
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={monthlyTrend} margin={{ top: 10, right: 10, bottom: 0, left: -10 }}>
                <defs>
                  <linearGradient id="costGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"  stopColor={CHART_COLORS.primary} stopOpacity={0.5} />
                    <stop offset="95%" stopColor={CHART_COLORS.primary} stopOpacity={0.05} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--ci-border)" />
                <XAxis dataKey="month" tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <YAxis tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <RTooltip formatter={(v) => `$${Number(v).toLocaleString()}`}
                  contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8 }} />
                <Area type="monotone" dataKey="amount" stroke={CHART_COLORS.primary}
                  strokeWidth={2.5} fill="url(#costGrad)" />
              </AreaChart>
            </ResponsiveContainer>
          </ChartCard>
        </Col>
      </Row>

      <Table
        rowKey="costId"
        columns={columns}
        dataSource={state.items}
        loading={state.loading}
        pagination={{ pageSize: 10 }}
        bordered size="middle"
      />

      <Modal
        title="Add Cost Record"
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
          <Form.Item name="costType" label="Cost Type" rules={[{ required: true }]}>
            <Select placeholder="Select type">
              {COST_TYPES.map(t => <Option key={t} value={t}><Tag color={COST_COLORS[t]}>{t}</Tag></Option>)}
            </Select>
          </Form.Item>
          <Form.Item name="amount" label="Amount ($)" rules={[{ required: true }]}>
            <Input type="number" min={0.01} step={0.01} placeholder="15000.00" />
          </Form.Item>
          <Form.Item name="costDate" label="Cost Date" rules={[{ required: true }]}>
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
