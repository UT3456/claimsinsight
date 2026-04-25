import { useEffect, useReducer, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, DatePicker, Tabs,
  Typography, Space, Tag, Alert, Tooltip, Popconfirm, Card, Row, Col, Statistic,
} from 'antd';
import { PlusOutlined, ReloadOutlined, DeleteOutlined, WarningOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip as RTooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend, CartesianGrid,
} from 'recharts';
import ChartCard, { CHART_COLORS, CHART_PALETTE } from '../../components/charts/ChartCard';
import {
  denialPatternsApi, leakageFlagsApi,
  DenialPattern, LeakageFlag,
  CreateDenialPatternRequest, CreateLeakageFlagRequest,
  LEAKAGE_TYPES,
} from '../../api/denialLeakageApi';

const { Title, Text } = Typography;
const { Option } = Select;
const { TextArea } = Input;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  patterns: DenialPattern[];
  flags: LeakageFlag[];
  loadingPatterns: boolean;
  loadingFlags: boolean;
  error: string | null;
  patternModal: boolean;
  flagModal: boolean;
  submitting: boolean;
}

type Action =
  | { type: 'PATTERNS_START' }
  | { type: 'PATTERNS_SUCCESS'; payload: DenialPattern[] }
  | { type: 'FLAGS_START' }
  | { type: 'FLAGS_SUCCESS'; payload: LeakageFlag[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'OPEN_PATTERN_MODAL' }
  | { type: 'CLOSE_PATTERN_MODAL' }
  | { type: 'OPEN_FLAG_MODAL' }
  | { type: 'CLOSE_FLAG_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'PATTERN_CREATED'; payload: DenialPattern }
  | { type: 'FLAG_CREATED'; payload: LeakageFlag }
  | { type: 'SUBMIT_ERROR' }
  | { type: 'DELETE_PATTERN'; payload: number }
  | { type: 'DELETE_FLAG'; payload: number };

const initial: State = {
  patterns: [], flags: [],
  loadingPatterns: false, loadingFlags: false,
  error: null, patternModal: false, flagModal: false, submitting: false,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'PATTERNS_START':      return { ...state, loadingPatterns: true, error: null };
    case 'PATTERNS_SUCCESS':    return { ...state, loadingPatterns: false, patterns: action.payload };
    case 'FLAGS_START':         return { ...state, loadingFlags: true, error: null };
    case 'FLAGS_SUCCESS':       return { ...state, loadingFlags: false, flags: action.payload };
    case 'FETCH_ERROR':         return { ...state, loadingPatterns: false, loadingFlags: false, error: action.payload };
    case 'OPEN_PATTERN_MODAL':  return { ...state, patternModal: true };
    case 'CLOSE_PATTERN_MODAL': return { ...state, patternModal: false, submitting: false };
    case 'OPEN_FLAG_MODAL':     return { ...state, flagModal: true };
    case 'CLOSE_FLAG_MODAL':    return { ...state, flagModal: false, submitting: false };
    case 'SUBMIT_START':        return { ...state, submitting: true };
    case 'PATTERN_CREATED':     return { ...state, submitting: false, patternModal: false, patterns: [action.payload, ...state.patterns] };
    case 'FLAG_CREATED':        return { ...state, submitting: false, flagModal: false, flags: [action.payload, ...state.flags] };
    case 'SUBMIT_ERROR':        return { ...state, submitting: false };
    case 'DELETE_PATTERN':      return { ...state, patterns: state.patterns.filter(p => p.patternId !== action.payload) };
    case 'DELETE_FLAG':         return { ...state, flags: state.flags.filter(f => f.leakageId !== action.payload) };
    default:                    return state;
  }
}

// ── Component ─────────────────────────────────────────────────────────────────

const LEAKAGE_COLORS: Record<string, string> = { Overpayment: 'red', Delay: 'orange', Error: 'gold' };

export default function DenialLeakagePage() {
  const [state, dispatch] = useReducer(reducer, initial);
  const [patternForm] = Form.useForm();
  const [flagForm]    = Form.useForm();

  const loadPatterns = useCallback(async () => {
    dispatch({ type: 'PATTERNS_START' });
    try {
      dispatch({ type: 'PATTERNS_SUCCESS', payload: await denialPatternsApi.getAll() });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load denial patterns' });
    }
  }, []);

  const loadFlags = useCallback(async () => {
    dispatch({ type: 'FLAGS_START' });
    try {
      dispatch({ type: 'FLAGS_SUCCESS', payload: await leakageFlagsApi.getAll() });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load leakage flags' });
    }
  }, []);

  useEffect(() => { loadPatterns(); loadFlags(); }, [loadPatterns, loadFlags]);

  const handleCreatePattern = async (values: Record<string, unknown>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const req: CreateDenialPatternRequest = {
        claimId:        values.claimId as string,
        denialCode:     values.denialCode as string,
        reason:         values.reason as string,
        occurrenceDate: (values.occurrenceDate as dayjs.Dayjs).format('YYYY-MM-DD'),
      };
      dispatch({ type: 'PATTERN_CREATED', payload: await denialPatternsApi.create(req) });
      patternForm.resetFields();
    } catch { dispatch({ type: 'SUBMIT_ERROR' }); }
  };

  const handleCreateFlag = async (values: Record<string, unknown>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const req: CreateLeakageFlagRequest = {
        claimId:        values.claimId as string,
        leakageType:    values.leakageType as string,
        estimatedLoss:  Number(values.estimatedLoss),
        identifiedDate: (values.identifiedDate as dayjs.Dayjs).format('YYYY-MM-DD'),
      };
      dispatch({ type: 'FLAG_CREATED', payload: await leakageFlagsApi.create(req) });
      flagForm.resetFields();
    } catch { dispatch({ type: 'SUBMIT_ERROR' }); }
  };

  const totalLeakage = state.flags.reduce((s, f) => s + (f.estimatedLoss ?? 0), 0);

  // ── Chart data ────────────────────────────────────────────────────────────
  const denialByCode = Object.entries(
    state.patterns.reduce<Record<string, number>>((acc, p) => {
      acc[p.denialCode] = (acc[p.denialCode] ?? 0) + 1;
      return acc;
    }, {}),
  )
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 8);

  const leakageByType = Object.entries(
    state.flags.reduce<Record<string, number>>((acc, f) => {
      acc[f.leakageType] = (acc[f.leakageType] ?? 0) + f.estimatedLoss;
      return acc;
    }, {}),
  ).map(([name, value]) => ({ name, value: Number(value.toFixed(2)) }));

  const patternColumns: ColumnsType<DenialPattern> = [
    { title: 'ID',          dataIndex: 'patternId',      key: 'patternId',      width: 80 },
    { title: 'Claim',       dataIndex: 'claimId',        key: 'claimId',        render: v => <Text code>{v}</Text> },
    { title: 'Denial Code', dataIndex: 'denialCode',     key: 'denialCode',     render: v => <Tag color="red">{v}</Tag> },
    { title: 'Reason',      dataIndex: 'reason',         key: 'reason',         ellipsis: true },
    { title: 'Date',        dataIndex: 'occurrenceDate', key: 'occurrenceDate' },
    {
      title: 'Action', key: 'action', width: 80,
      render: (_, rec) => (
        <Popconfirm title="Delete?" onConfirm={async () => { await denialPatternsApi.delete(rec.patternId); dispatch({ type: 'DELETE_PATTERN', payload: rec.patternId }); }}>
          <Tooltip title="Delete"><Button icon={<DeleteOutlined />} size="small" danger /></Tooltip>
        </Popconfirm>
      ),
    },
  ];

  const flagColumns: ColumnsType<LeakageFlag> = [
    { title: 'ID',             dataIndex: 'leakageId',     key: 'leakageId',     width: 80 },
    { title: 'Claim',          dataIndex: 'claimId',       key: 'claimId',       render: v => <Text code>{v}</Text> },
    {
      title: 'Type',           dataIndex: 'leakageType',   key: 'leakageType',
      render: v => <Tag color={LEAKAGE_COLORS[v] ?? 'default'}>{v}</Tag>,
      filters: LEAKAGE_TYPES.map(t => ({ text: t, value: t })),
      onFilter: (value, rec) => rec.leakageType === value,
    },
    {
      title: 'Estimated Loss', dataIndex: 'estimatedLoss', key: 'estimatedLoss',
      sorter: (a, b) => a.estimatedLoss - b.estimatedLoss,
      render: v => <Text strong style={{ color: '#dc2626' }}>${Number(v).toLocaleString('en-US', { minimumFractionDigits: 2 })}</Text>,
    },
    { title: 'Identified',     dataIndex: 'identifiedDate', key: 'identifiedDate' },
    {
      title: 'Action', key: 'action', width: 80,
      render: (_, rec) => (
        <Popconfirm title="Delete?" onConfirm={async () => { await leakageFlagsApi.delete(rec.leakageId); dispatch({ type: 'DELETE_FLAG', payload: rec.leakageId }); }}>
          <Tooltip title="Delete"><Button icon={<DeleteOutlined />} size="small" danger /></Tooltip>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0 }}>Denial & Leakage</Title>
          <Text type="secondary">Denial patterns and financial leakage flags</Text>
        </div>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        {[
          { title: 'Denial Patterns', value: state.patterns.length, color: '#dc2626' },
          { title: 'Leakage Flags',   value: state.flags.length,    color: '#7c3aed' },
          { title: 'Total Leakage ($)', value: `$${totalLeakage.toLocaleString('en-US', { minimumFractionDigits: 0 })}`, color: '#dc2626', isText: true },
          { title: 'Avg Loss ($)', value: state.flags.length ? `$${(totalLeakage / state.flags.length).toLocaleString('en-US', { minimumFractionDigits: 0 })}` : '$0', color: '#d97706', isText: true },
        ].map(c => (
          <Col xs={12} sm={6} key={c.title}>
            <Card style={styles.statCard}>
              <Statistic
                title={c.title}
                value={c.isText ? c.value : Number(c.value)}
                valueStyle={{ color: c.color }}
                prefix={<WarningOutlined />}
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
            title="Top Denial Codes"
            subtitle="Most-frequent denial reasons (top 8)"
            loading={state.loadingPatterns}
            isEmpty={!state.loadingPatterns && denialByCode.length === 0}
          >
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={denialByCode} layout="vertical" margin={{ top: 10, right: 20, bottom: 0, left: 10 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--ci-border)" />
                <XAxis type="number" allowDecimals={false} tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} />
                <YAxis dataKey="name" type="category" tick={{ fontSize: 11, fill: 'var(--ci-text-secondary)' }} width={70} />
                <RTooltip contentStyle={{ background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)', borderRadius: 8 }} />
                <Bar dataKey="count" fill={CHART_COLORS.danger} radius={[0, 6, 6, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </Col>

        <Col xs={24} lg={10}>
          <ChartCard
            title="Leakage by Type"
            subtitle="Total estimated loss ($) per leakage category"
            loading={state.loadingFlags}
            isEmpty={!state.loadingFlags && leakageByType.length === 0}
          >
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={leakageByType} dataKey="value" nameKey="name"
                  cx="50%" cy="50%" innerRadius={50} outerRadius={90} paddingAngle={2}
                  label={(e) => `$${Number(e.value).toLocaleString()}`}>
                  {leakageByType.map((_, i) => (
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
      </Row>

      <Tabs
        items={[
          {
            key: 'patterns', label: `Denial Patterns (${state.patterns.length})`,
            children: (
              <>
                <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                  <Button icon={<ReloadOutlined />} onClick={loadPatterns}>Refresh</Button>
                  <Button type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_PATTERN_MODAL' })}>
                    Add Pattern
                  </Button>
                </div>
                <Table rowKey="patternId" columns={patternColumns} dataSource={state.patterns}
                  loading={state.loadingPatterns} pagination={{ pageSize: 10 }} bordered size="middle" />
              </>
            ),
          },
          {
            key: 'flags', label: `Leakage Flags (${state.flags.length})`,
            children: (
              <>
                <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                  <Button icon={<ReloadOutlined />} onClick={loadFlags}>Refresh</Button>
                  <Button type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_FLAG_MODAL' })}>
                    Add Flag
                  </Button>
                </div>
                <Table rowKey="leakageId" columns={flagColumns} dataSource={state.flags}
                  loading={state.loadingFlags} pagination={{ pageSize: 10 }} bordered size="middle" />
              </>
            ),
          },
        ]}
      />

      {/* Pattern Modal */}
      <Modal title="Add Denial Pattern" open={state.patternModal}
        onCancel={() => { dispatch({ type: 'CLOSE_PATTERN_MODAL' }); patternForm.resetFields(); }}
        onOk={() => patternForm.submit()} confirmLoading={state.submitting} destroyOnClose>
        <Form form={patternForm} layout="vertical" onFinish={handleCreatePattern}>
          <Form.Item name="claimId" label="Claim ID" rules={[{ required: true }]}>
            <Input placeholder="CLM-2026-AUTO-001" />
          </Form.Item>
          <Form.Item name="denialCode" label="Denial Code" rules={[{ required: true }]}>
            <Input placeholder="D001" />
          </Form.Item>
          <Form.Item name="reason" label="Reason" rules={[{ required: true }]}>
            <TextArea rows={3} placeholder="Describe the denial reason..." />
          </Form.Item>
          <Form.Item name="occurrenceDate" label="Occurrence Date" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Flag Modal */}
      <Modal title="Add Leakage Flag" open={state.flagModal}
        onCancel={() => { dispatch({ type: 'CLOSE_FLAG_MODAL' }); flagForm.resetFields(); }}
        onOk={() => flagForm.submit()} confirmLoading={state.submitting} destroyOnClose>
        <Form form={flagForm} layout="vertical" onFinish={handleCreateFlag}>
          <Form.Item name="claimId" label="Claim ID" rules={[{ required: true }]}>
            <Input placeholder="CLM-2026-AUTO-001" />
          </Form.Item>
          <Form.Item name="leakageType" label="Leakage Type" rules={[{ required: true }]}>
            <Select placeholder="Select type">
              {LEAKAGE_TYPES.map(t => <Option key={t} value={t}><Tag color={LEAKAGE_COLORS[t]}>{t}</Tag></Option>)}
            </Select>
          </Form.Item>
          <Form.Item name="estimatedLoss" label="Estimated Loss ($)" rules={[{ required: true }]}>
            <Input type="number" min={0.01} step={0.01} placeholder="5000.00" />
          </Form.Item>
          <Form.Item name="identifiedDate" label="Identified Date" rules={[{ required: true }]}>
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
