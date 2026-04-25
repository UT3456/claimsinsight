import { useEffect, useReducer, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, DatePicker, Typography,
  Space, Alert, Tooltip, Popconfirm, Card, Row, Col, Statistic,
} from 'antd';
import { PlusOutlined, ReloadOutlined, DeleteOutlined, BankOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { reservesApi, ClaimReserve, CreateReserveRequest } from '../../api/financialApi';
import {
  LineChart, Line, XAxis, YAxis, Tooltip as RTooltip, ResponsiveContainer,
  CartesianGrid, BarChart, Bar, Cell,
} from 'recharts';
import ChartCard, { CHART_COLORS, CHART_PALETTE } from '../../components/charts/ChartCard';

const { Title, Text } = Typography;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  items: ClaimReserve[];
  total: number;
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
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
  | { type: 'DELETE_SUCCESS'; payload: number };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'FETCH_START':    return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS':  return { ...state, loading: false, items: action.payload };
    case 'FETCH_ERROR':    return { ...state, loading: false, error: action.payload };
    case 'SET_TOTAL':      return { ...state, total: action.payload };
    case 'OPEN_MODAL':     return { ...state, modalOpen: true };
    case 'CLOSE_MODAL':    return { ...state, modalOpen: false, submitting: false };
    case 'SUBMIT_START':   return { ...state, submitting: true };
    case 'SUBMIT_SUCCESS': return { ...state, submitting: false, modalOpen: false, items: [action.payload, ...state.items] };
    case 'SUBMIT_ERROR':   return { ...state, submitting: false };
    case 'DELETE_SUCCESS': return { ...state, items: state.items.filter(i => i.reserveId !== action.payload) };
    default:               return state;
  }
}

// ── Component ─────────────────────────────────────────────────────────────────

export default function ReservesPage() {
  const [state, dispatch] = useReducer(reducer, {
    items: [], total: 0, loading: false, error: null, modalOpen: false, submitting: false,
  });
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const [data, total] = await Promise.all([
        reservesApi.getAll(),
        reservesApi.getTotalAmount().catch(() => 0),
      ]);
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
      dispatch({ type: 'SET_TOTAL', payload: total });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load reserves' });
    }
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
      const record = await reservesApi.create(req);
      dispatch({ type: 'SUBMIT_SUCCESS', payload: record });
      form.resetFields();
    } catch {
      dispatch({ type: 'SUBMIT_ERROR' });
    }
  };

  const handleDelete = async (id: number) => {
    await reservesApi.delete(id);
    dispatch({ type: 'DELETE_SUCCESS', payload: id });
  };

  const avgReserve = state.items.length
    ? (state.items.reduce((s, i) => s + i.reserveAmount, 0) / state.items.length)
    : 0;

  // ── Chart data ────────────────────────────────────────────────────────────
  const reserveTrend = Object.entries(
    state.items.reduce<Record<string, number>>((acc, r) => {
      const m = r.updatedDate?.slice(0, 7);
      if (!m) return acc;
      acc[m] = (acc[m] ?? 0) + r.reserveAmount;
      return acc;
    }, {}),
  )
    .map(([month, total]) => ({ month, total: Number(total.toFixed(2)) }))
    .sort((a, b) => a.month.localeCompare(b.month));

  const topClaims = [...state.items]
    .sort((a, b) => b.reserveAmount - a.reserveAmount)
    .slice(0, 10)
    .map(r => ({ name: r.claimId, amount: Number(r.reserveAmount.toFixed(0)) }));

  const columns: ColumnsType<ClaimReserve> = [
    { title: 'Reserve ID',     dataIndex: 'reserveId',     key: 'reserveId',     width: 110 },
    { title: 'Claim ID',       dataIndex: 'claimId',       key: 'claimId',       render: v => <Text code>{v}</Text> },
    {
      title: 'Reserve Amount', dataIndex: 'reserveAmount', key: 'reserveAmount',
      render: v => <Text strong>${Number(v).toLocaleString('en-US', { minimumFractionDigits: 2 })}</Text>,
      sorter: (a, b) => a.reserveAmount - b.reserveAmount,
    },
    { title: 'Updated Date',   dataIndex: 'updatedDate',   key: 'updatedDate',   sorter: (a, b) => a.updatedDate.localeCompare(b.updatedDate) },
    {
      title: 'Action', key: 'action', width: 80,
      render: (_, rec) => (
        <Popconfirm title="Delete this reserve?" onConfirm={() => handleDelete(rec.reserveId)}>
          <Tooltip title="Delete"><Button icon={<DeleteOutlined />} size="small" danger /></Tooltip>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0 }}>Claim Reserves</Title>
          <Text type="secondary">Reserve amounts set aside per claim</Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_MODAL' })}>
            Add Reserve
          </Button>
        </Space>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        {[
          { title: 'Total Reserve', value: state.total,           prefix: '$', fmt: true },
          { title: 'Records',       value: state.items.length,    prefix: undefined, fmt: false },
          { title: 'Avg Reserve',   value: avgReserve,            prefix: '$', fmt: true },
        ].map(c => (
          <Col xs={24} sm={8} key={c.title}>
            <Card style={styles.statCard}>
              <Statistic
                title={c.title}
                value={c.value}
                prefix={c.prefix ? <BankOutlined /> : undefined}
                formatter={v => c.fmt
                  ? `$${Number(v).toLocaleString('en-US', { minimumFractionDigits: 2 })}`
                  : v as string}
              />
            </Card>
          </Col>
        ))}
      </Row>

      {state.error && <Alert type="error" message={state.error} style={{ marginBottom: 16 }} />}

      {/* Charts */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={14}>
          <ChartCard
            title="Reserve Trend"
            subtitle="Total reserve set aside per month"
            loading={state.loading}
            isEmpty={!state.loading && reserveTrend.length === 0}
          >
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={reserveTrend} margin={{ top: 10, right: 10, bottom: 0, left: -10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--ci-border)" />
                <XAxis dataKey="month" tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <YAxis tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <RTooltip formatter={(v) => `$${Number(v).toLocaleString()}`}
                  contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8 }} />
                <Line type="monotone" dataKey="total" stroke={CHART_COLORS.success}
                  strokeWidth={2.5} dot={{ r: 4, fill: CHART_COLORS.success }} />
              </LineChart>
            </ResponsiveContainer>
          </ChartCard>
        </Col>

        <Col xs={24} lg={10}>
          <ChartCard
            title="Top 10 Reserves by Claim"
            subtitle="Highest reserve amounts booked"
            loading={state.loading}
            isEmpty={!state.loading && topClaims.length === 0}
          >
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={topClaims} layout="vertical" margin={{ top: 10, right: 20, bottom: 0, left: 10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--ci-border)" />
                <XAxis type="number" tick={{ fontSize: 10, fill: 'var(--ci-text-secondary)' }} />
                <YAxis dataKey="name" type="category" width={100} tick={{ fontSize: 10, fill: 'var(--ci-text-secondary)' }} />
                <RTooltip formatter={(v) => `$${Number(v).toLocaleString()}`}
                  contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8 }} />
                <Bar dataKey="amount" radius={[0, 6, 6, 0]}>
                  {topClaims.map((_, i) => <Cell key={i} fill={CHART_PALETTE[i % CHART_PALETTE.length]} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </Col>
      </Row>

      <Table
        rowKey="reserveId"
        columns={columns}
        dataSource={state.items}
        loading={state.loading}
        pagination={{ pageSize: 10 }}
        bordered size="middle"
      />

      <Modal
        title="Add Reserve Record"
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
          <Form.Item name="reserveAmount" label="Reserve Amount ($)" rules={[{ required: true }]}>
            <Input type="number" min={0.01} step={0.01} placeholder="50000.00" />
          </Form.Item>
          <Form.Item name="updatedDate" label="Updated Date" rules={[{ required: true }]}>
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
