import { useEffect, useReducer, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, DatePicker, Typography,
  Space, Tag, Alert, Tooltip, Card, Row, Col, Statistic,
} from 'antd';
import { PlusOutlined, ReloadOutlined, WarningOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { slaApi, SlaViolation, CreateSlaViolationRequest } from '../../api/adjustersApi';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip as RTooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend, CartesianGrid,
} from 'recharts';
import ChartCard, { CHART_COLORS, CHART_PALETTE } from '../../components/charts/ChartCard';

const { Title, Text } = Typography;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  items: SlaViolation[];
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: SlaViolation[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'OPEN_MODAL' }
  | { type: 'CLOSE_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'SUBMIT_SUCCESS'; payload: SlaViolation }
  | { type: 'SUBMIT_ERROR' };

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
    default:               return state;
  }
}

// ── Component ─────────────────────────────────────────────────────────────────

const SEVERITY_COLOR: Record<string, string> = { HIGH: 'red', MEDIUM: 'orange', LOW: 'gold' };

export default function SlaViolationsPage() {
  const [state, dispatch] = useReducer(reducer, {
    items: [], loading: false, error: null, modalOpen: false, submitting: false,
  });
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const data = await slaApi.getAll();
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load SLA violations' });
    }
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
      const record = await slaApi.create(req);
      dispatch({ type: 'SUBMIT_SUCCESS', payload: record });
      form.resetFields();
    } catch {
      dispatch({ type: 'SUBMIT_ERROR' });
    }
  };

  const high   = state.items.filter(i => i.severity === 'HIGH').length;
  const medium = state.items.filter(i => i.severity === 'MEDIUM').length;
  const escalated = state.items.filter(i => i.escalated).length;

  // ── Chart data ────────────────────────────────────────────────────────────
  const byType = Object.entries(
    state.items.reduce<Record<string, number>>((acc, v) => {
      acc[v.violationType] = (acc[v.violationType] ?? 0) + 1;
      return acc;
    }, {}),
  )
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count);

  const severityMix = [
    { name: 'HIGH',   value: high,   color: CHART_COLORS.danger },
    { name: 'MEDIUM', value: medium, color: CHART_COLORS.accent2 },
    { name: 'LOW',    value: state.items.filter(i => i.severity === 'LOW').length, color: CHART_COLORS.warn },
  ].filter(s => s.value > 0);

  const columns: ColumnsType<SlaViolation> = [
    { title: 'ID',            dataIndex: 'violationId',   key: 'violationId',  width: 80 },
    { title: 'Claim ID',      dataIndex: 'claimId',       key: 'claimId',      render: v => <Text code>{v}</Text> },
    { title: 'Adjuster ID',   dataIndex: 'adjusterId',    key: 'adjusterId'    },
    { title: 'Violation',     dataIndex: 'violationType', key: 'violationType', render: v => <Tag color="orange">{v}</Tag> },
    { title: 'Target (d)',    dataIndex: 'slaTargetDays', key: 'slaTargetDays' },
    { title: 'Actual (d)',    dataIndex: 'actualDays',    key: 'actualDays',   sorter: (a, b) => a.actualDays - b.actualDays },
    { title: 'Overdue (d)',   dataIndex: 'daysOverdue',   key: 'daysOverdue',  render: v => <Text type="danger">{v ?? '—'}</Text> },
    {
      title: 'Severity', dataIndex: 'severity', key: 'severity',
      render: v => v ? <Tag color={SEVERITY_COLOR[v] ?? 'default'}>{v}</Tag> : '—',
    },
    {
      title: 'Escalated', dataIndex: 'escalated', key: 'escalated',
      render: v => v ? <Tag color="red">YES</Tag> : <Tag>NO</Tag>,
    },
    { title: 'Date', dataIndex: 'violationDate', key: 'violationDate', render: v => v?.split('T')[0] ?? '—' },
  ];

  return (
    <div>
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0 }}>SLA Violations</Title>
          <Text type="secondary">Missed service-level agreement deadlines</Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_MODAL' })}>
            Record Violation
          </Button>
        </Space>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        {[
          { title: 'Total Violations', value: state.items.length,     color: '#0f172a' },
          { title: 'High Severity',    value: high,                   color: '#dc2626' },
          { title: 'Medium Severity',  value: medium,                 color: '#d97706' },
          { title: 'Escalated',        value: escalated,              color: '#7c3aed' },
        ].map(c => (
          <Col xs={12} sm={6} key={c.title}>
            <Card style={styles.statCard}>
              <Statistic title={c.title} value={c.value} valueStyle={{ color: c.color }}
                prefix={c.title === 'High Severity' ? <WarningOutlined /> : undefined} />
            </Card>
          </Col>
        ))}
      </Row>

      {state.error && <Alert type="error" message={state.error} style={{ marginBottom: 16 }} />}

      {/* Charts */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={14}>
          <ChartCard
            title="Violations by Type"
            subtitle="Frequency of each SLA breach category"
            loading={state.loading}
            isEmpty={!state.loading && byType.length === 0}
          >
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={byType} margin={{ top: 10, right: 10, bottom: 0, left: -20 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--ci-border)" />
                <XAxis dataKey="name" tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <RTooltip contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8 }} />
                <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                  {byType.map((_, i) => <Cell key={i} fill={CHART_PALETTE[i % CHART_PALETTE.length]} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </Col>

        <Col xs={24} lg={10}>
          <ChartCard
            title="Severity Mix"
            subtitle="Distribution across HIGH / MEDIUM / LOW"
            loading={state.loading}
            isEmpty={!state.loading && severityMix.length === 0}
          >
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={severityMix} dataKey="value" nameKey="name"
                  cx="50%" cy="50%" innerRadius={55} outerRadius={95} paddingAngle={2}>
                  {severityMix.map((s, i) => <Cell key={i} fill={s.color} />)}
                </Pie>
                <Legend verticalAlign="bottom" iconType="circle" wrapperStyle={{ fontSize: 12 }} />
                <RTooltip contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8 }} />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>
        </Col>
      </Row>

      <Table
        rowKey="violationId"
        columns={columns}
        dataSource={state.items}
        loading={state.loading}
        pagination={{ pageSize: 10 }}
        bordered size="middle"
        scroll={{ x: 900 }}
      />

      <Modal
        title="Record SLA Violation"
        open={state.modalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_MODAL' }); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={state.submitting}
        destroyOnClose
        width={520}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item name="claimId" label="Claim ID (numeric)" rules={[{ required: true }]}>
                <Input type="number" min={1} placeholder="1" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="adjusterId" label="Adjuster ID" rules={[{ required: true }]}>
                <Input type="number" min={1} placeholder="1" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="violationType" label="Violation Type" rules={[{ required: true }]}>
                <Input placeholder="RESPONSE_TIME" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="violationDate" label="Violation Date" rules={[{ required: true }]}>
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="slaTargetDays" label="SLA Target (days)" rules={[{ required: true }]}>
                <Input type="number" min={1} placeholder="5" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="actualDays" label="Actual Days" rules={[{ required: true }]}>
                <Input type="number" min={1} placeholder="8" />
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
