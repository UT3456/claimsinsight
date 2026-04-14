import { useEffect, useReducer, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, DatePicker, Tabs,
  Typography, Space, Tag, Alert, Tooltip, Popconfirm, Card, Row, Col, Statistic,
} from 'antd';
import { PlusOutlined, ReloadOutlined, DeleteOutlined, SafetyOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  riskScoresApi, riskIndicatorsApi,
  RiskScore, RiskIndicator,
  CreateRiskScoreRequest, CreateRiskIndicatorRequest,
  INDICATOR_TYPES, SEVERITIES,
} from '../../api/fraudRiskApi';

const { Title, Text } = Typography;
const { Option } = Select;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  scores: RiskScore[];
  indicators: RiskIndicator[];
  loadingScores: boolean;
  loadingIndicators: boolean;
  error: string | null;
  scoreModal: boolean;
  indicatorModal: boolean;
  submitting: boolean;
}

type Action =
  | { type: 'SCORES_START' }
  | { type: 'SCORES_SUCCESS'; payload: RiskScore[] }
  | { type: 'INDICATORS_START' }
  | { type: 'INDICATORS_SUCCESS'; payload: RiskIndicator[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'OPEN_SCORE_MODAL' }
  | { type: 'CLOSE_SCORE_MODAL' }
  | { type: 'OPEN_INDICATOR_MODAL' }
  | { type: 'CLOSE_INDICATOR_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'SCORE_CREATED'; payload: RiskScore }
  | { type: 'INDICATOR_CREATED'; payload: RiskIndicator }
  | { type: 'SUBMIT_ERROR' }
  | { type: 'DELETE_SCORE'; payload: number }
  | { type: 'DELETE_INDICATOR'; payload: number };

const initial: State = {
  scores: [], indicators: [],
  loadingScores: false, loadingIndicators: false,
  error: null, scoreModal: false, indicatorModal: false, submitting: false,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'SCORES_START':         return { ...state, loadingScores: true, error: null };
    case 'SCORES_SUCCESS':       return { ...state, loadingScores: false, scores: action.payload };
    case 'INDICATORS_START':     return { ...state, loadingIndicators: true, error: null };
    case 'INDICATORS_SUCCESS':   return { ...state, loadingIndicators: false, indicators: action.payload };
    case 'FETCH_ERROR':          return { ...state, loadingScores: false, loadingIndicators: false, error: action.payload };
    case 'OPEN_SCORE_MODAL':     return { ...state, scoreModal: true };
    case 'CLOSE_SCORE_MODAL':    return { ...state, scoreModal: false, submitting: false };
    case 'OPEN_INDICATOR_MODAL': return { ...state, indicatorModal: true };
    case 'CLOSE_INDICATOR_MODAL':return { ...state, indicatorModal: false, submitting: false };
    case 'SUBMIT_START':         return { ...state, submitting: true };
    case 'SCORE_CREATED':        return { ...state, submitting: false, scoreModal: false, scores: [action.payload, ...state.scores] };
    case 'INDICATOR_CREATED':    return { ...state, submitting: false, indicatorModal: false, indicators: [action.payload, ...state.indicators] };
    case 'SUBMIT_ERROR':         return { ...state, submitting: false };
    case 'DELETE_SCORE':         return { ...state, scores: state.scores.filter(s => s.scoreId !== action.payload) };
    case 'DELETE_INDICATOR':     return { ...state, indicators: state.indicators.filter(i => i.indicatorId !== action.payload) };
    default:                     return state;
  }
}

// ── Component ─────────────────────────────────────────────────────────────────

const SEV_COLOR: Record<string, string> = { HIGH: 'red', MEDIUM: 'orange', LOW: 'gold' };

export default function FraudRiskPage() {
  const [state, dispatch] = useReducer(reducer, initial);
  const [scoreForm]     = Form.useForm();
  const [indicatorForm] = Form.useForm();

  const loadScores = useCallback(async () => {
    dispatch({ type: 'SCORES_START' });
    try {
      dispatch({ type: 'SCORES_SUCCESS', payload: await riskScoresApi.getAll() });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load risk scores' });
    }
  }, []);

  const loadIndicators = useCallback(async () => {
    dispatch({ type: 'INDICATORS_START' });
    try {
      dispatch({ type: 'INDICATORS_SUCCESS', payload: await riskIndicatorsApi.getAll() });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load risk indicators' });
    }
  }, []);

  useEffect(() => { loadScores(); loadIndicators(); }, [loadScores, loadIndicators]);

  const handleCreateScore = async (values: Record<string, unknown>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const req: CreateRiskScoreRequest = {
        claimId:     values.claimId as string,
        scoreValue:  Number(values.scoreValue),
        computedDate:(values.computedDate as dayjs.Dayjs).format('YYYY-MM-DD'),
      };
      dispatch({ type: 'SCORE_CREATED', payload: await riskScoresApi.create(req) });
      scoreForm.resetFields();
    } catch { dispatch({ type: 'SUBMIT_ERROR' }); }
  };

  const handleCreateIndicator = async (values: Record<string, unknown>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const req: CreateRiskIndicatorRequest = {
        claimId:       values.claimId as string,
        indicatorType: values.indicatorType as string,
        severity:      values.severity as string,
        triggeredDate: (values.triggeredDate as dayjs.Dayjs).format('YYYY-MM-DD'),
      };
      dispatch({ type: 'INDICATOR_CREATED', payload: await riskIndicatorsApi.create(req) });
      indicatorForm.resetFields();
    } catch { dispatch({ type: 'SUBMIT_ERROR' }); }
  };

  const highRisk = state.scores.filter(s => s.scoreValue >= 75).length;
  const highSev  = state.indicators.filter(i => i.severity === 'HIGH').length;

  const scoreColumns: ColumnsType<RiskScore> = [
    { title: 'ID',      dataIndex: 'scoreId',     key: 'scoreId',     width: 80 },
    { title: 'Claim',   dataIndex: 'claimId',     key: 'claimId',     render: v => <Text code>{v}</Text> },
    {
      title: 'Score',   dataIndex: 'scoreValue',  key: 'scoreValue',
      sorter: (a, b) => a.scoreValue - b.scoreValue,
      render: v => {
        const n = Number(v);
        const color = n >= 75 ? '#dc2626' : n >= 50 ? '#d97706' : '#16a34a';
        return <Text strong style={{ color }}>{n.toFixed(1)}</Text>;
      },
    },
    { title: 'Date',    dataIndex: 'computedDate',key: 'computedDate' },
    {
      title: 'Action', key: 'action', width: 80,
      render: (_, rec) => (
        <Popconfirm title="Delete?" onConfirm={async () => { await riskScoresApi.delete(rec.scoreId); dispatch({ type: 'DELETE_SCORE', payload: rec.scoreId }); }}>
          <Tooltip title="Delete"><Button icon={<DeleteOutlined />} size="small" danger /></Tooltip>
        </Popconfirm>
      ),
    },
  ];

  const indicatorColumns: ColumnsType<RiskIndicator> = [
    { title: 'ID',           dataIndex: 'indicatorId',   key: 'indicatorId',   width: 80 },
    { title: 'Claim',        dataIndex: 'claimId',       key: 'claimId',       render: v => <Text code>{v}</Text> },
    { title: 'Type',         dataIndex: 'indicatorType', key: 'indicatorType', render: v => <Tag color="blue">{v}</Tag> },
    {
      title: 'Severity',     dataIndex: 'severity',      key: 'severity',
      render: v => <Tag color={SEV_COLOR[v] ?? 'default'}>{v}</Tag>,
      filters: SEVERITIES.map(s => ({ text: s, value: s })),
      onFilter: (value, rec) => rec.severity === value,
    },
    { title: 'Triggered',    dataIndex: 'triggeredDate', key: 'triggeredDate' },
    {
      title: 'Action', key: 'action', width: 80,
      render: (_, rec) => (
        <Popconfirm title="Delete?" onConfirm={async () => { await riskIndicatorsApi.delete(rec.indicatorId); dispatch({ type: 'DELETE_INDICATOR', payload: rec.indicatorId }); }}>
          <Tooltip title="Delete"><Button icon={<DeleteOutlined />} size="small" danger /></Tooltip>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0 }}>Fraud & Risk Intelligence</Title>
          <Text type="secondary">Risk scores and fraud indicators</Text>
        </div>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        {[
          { title: 'Risk Scores',       value: state.scores.length,     color: '#2563eb' },
          { title: 'High Risk (≥75)',   value: highRisk,                color: '#dc2626' },
          { title: 'Risk Indicators',   value: state.indicators.length, color: '#7c3aed' },
          { title: 'High Severity',     value: highSev,                 color: '#dc2626' },
        ].map(c => (
          <Col xs={12} sm={6} key={c.title}>
            <Card style={styles.statCard}>
              <Statistic title={c.title} value={c.value} valueStyle={{ color: c.color }}
                prefix={<SafetyOutlined />} />
            </Card>
          </Col>
        ))}
      </Row>

      {state.error && <Alert type="error" message={state.error} style={{ marginBottom: 16 }} />}

      <Tabs
        items={[
          {
            key: 'scores', label: `Risk Scores (${state.scores.length})`,
            children: (
              <>
                <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                  <Button icon={<ReloadOutlined />} onClick={loadScores}>Refresh</Button>
                  <Button type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_SCORE_MODAL' })}>
                    Add Score
                  </Button>
                </div>
                <Table rowKey="scoreId" columns={scoreColumns} dataSource={state.scores}
                  loading={state.loadingScores} pagination={{ pageSize: 10 }} bordered size="middle" />
              </>
            ),
          },
          {
            key: 'indicators', label: `Risk Indicators (${state.indicators.length})`,
            children: (
              <>
                <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
                  <Button icon={<ReloadOutlined />} onClick={loadIndicators}>Refresh</Button>
                  <Button type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_INDICATOR_MODAL' })}>
                    Add Indicator
                  </Button>
                </div>
                <Table rowKey="indicatorId" columns={indicatorColumns} dataSource={state.indicators}
                  loading={state.loadingIndicators} pagination={{ pageSize: 10 }} bordered size="middle" />
              </>
            ),
          },
        ]}
      />

      {/* Score Modal */}
      <Modal title="Add Risk Score" open={state.scoreModal}
        onCancel={() => { dispatch({ type: 'CLOSE_SCORE_MODAL' }); scoreForm.resetFields(); }}
        onOk={() => scoreForm.submit()} confirmLoading={state.submitting} destroyOnClose>
        <Form form={scoreForm} layout="vertical" onFinish={handleCreateScore}>
          <Form.Item name="claimId" label="Claim ID" rules={[{ required: true }]}>
            <Input placeholder="CLM-2026-AUTO-001" />
          </Form.Item>
          <Form.Item name="scoreValue" label="Score Value (0–100)" rules={[{ required: true }]}>
            <Input type="number" min={0} max={100} step={0.1} placeholder="75.5" />
          </Form.Item>
          <Form.Item name="computedDate" label="Computed Date" rules={[{ required: true }]}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Indicator Modal */}
      <Modal title="Add Risk Indicator" open={state.indicatorModal}
        onCancel={() => { dispatch({ type: 'CLOSE_INDICATOR_MODAL' }); indicatorForm.resetFields(); }}
        onOk={() => indicatorForm.submit()} confirmLoading={state.submitting} destroyOnClose>
        <Form form={indicatorForm} layout="vertical" onFinish={handleCreateIndicator}>
          <Form.Item name="claimId" label="Claim ID" rules={[{ required: true }]}>
            <Input placeholder="CLM-2026-AUTO-001" />
          </Form.Item>
          <Form.Item name="indicatorType" label="Indicator Type" rules={[{ required: true }]}>
            <Select placeholder="Select type">
              {INDICATOR_TYPES.map(t => <Option key={t} value={t}>{t}</Option>)}
            </Select>
          </Form.Item>
          <Form.Item name="severity" label="Severity" rules={[{ required: true }]}>
            <Select placeholder="Select severity">
              {SEVERITIES.map(s => <Option key={s} value={s}><Tag color={SEV_COLOR[s]}>{s}</Tag></Option>)}
            </Select>
          </Form.Item>
          <Form.Item name="triggeredDate" label="Triggered Date" rules={[{ required: true }]}>
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
