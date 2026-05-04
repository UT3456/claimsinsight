import { useEffect, useReducer, useCallback } from 'react';
import {
  Table, Button, Select,
  Alert, Tooltip, Popconfirm, Typography, Space, Tag, Card, Row, Col, Statistic,
  App as AntApp,
} from 'antd';
import {
  ReloadOutlined, DeleteOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { claimsApi, type ClaimKpi, METRIC_NAMES } from '../../api/claimsApi';

const { Title, Text } = Typography;
const { Option } = Select;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  items:        ClaimKpi[];
  loading:      boolean;
  error:        string | null;
  filterMetric: string | null;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: ClaimKpi[] }
  | { type: 'FETCH_ERROR';   payload: string }
  | { type: 'DELETE_SUCCESS'; payload: number }
  | { type: 'SET_FILTER';    payload: string | null };

const initial: State = {
  items: [], loading: false, error: null,
  filterMetric: null,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'FETCH_START':    return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS':  return { ...state, loading: false, items: action.payload };
    case 'FETCH_ERROR':    return { ...state, loading: false, error: action.payload };
    case 'DELETE_SUCCESS': return { ...state, items: state.items.filter(i => i.kpiId !== action.payload) };
    case 'SET_FILTER':     return { ...state, filterMetric: action.payload };
    default:               return state;
  }
}

// ── Metric tag colours ────────────────────────────────────────────────────────

const METRIC_COLORS: Record<string, string> = {
  TAT: 'blue', CYCLE_TIME: 'purple', SEVERITY: 'red',
  FREQUENCY: 'orange', LOSS_RATIO: 'green', SETTLEMENT_TIME: 'cyan',
};

// ── Component ─────────────────────────────────────────────────────────────────

export default function ClaimsPage() {
  const [state, dispatch] = useReducer(reducer, initial);
  const { message } = AntApp.useApp();

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

  const handleDelete = async (id: number) => {
    try {
      await claimsApi.delete(id);
      dispatch({ type: 'DELETE_SUCCESS', payload: id });
      message.success('KPI deleted');
    } catch {
      message.error('Failed to delete KPI');
    }
  };

  // ── Summary stats ─────────────────────────────────────────────────────────
  const uniqueClaims = new Set(state.items.map(i => i.claimId)).size;
  const avgValue = state.items.length
    ? (state.items.reduce((s, i) => s + i.metricValue, 0) / state.items.length).toFixed(2)
    : '—';

  // ── Table columns ─────────────────────────────────────────────────────────
  const columns: ColumnsType<ClaimKpi> = [
    { title: 'KPI ID',   dataIndex: 'kpiId',      key: 'kpiId',      width: 80 },
    { title: 'Claim ID', dataIndex: 'claimId',     key: 'claimId',    render: v => <Text code>{v}</Text> },
    {
      title: 'Metric',   dataIndex: 'metricName',  key: 'metricName',
      render: v => <Tag color={METRIC_COLORS[v] ?? 'default'}>{v}</Tag>,
    },
    {
      title: 'Value',    dataIndex: 'metricValue', key: 'metricValue',
      render: v => <Text strong>{Number(v).toLocaleString()}</Text>,
      sorter: (a, b) => a.metricValue - b.metricValue,
    },
    { title: 'Date',    dataIndex: 'metricDate',   key: 'metricDate' },
    {
      title: 'Action',   key: 'action', width: 80,
      render: (_, rec) => (
        <Popconfirm title="Delete this KPI?" onConfirm={() => handleDelete(rec.kpiId)}>
          <Tooltip title="Delete">
            <Button icon={<DeleteOutlined />} size="small" danger />
          </Tooltip>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      {/* Header */}
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0 }}>Claims KPIs</Title>
          <Text type="secondary">Performance metrics across all claims</Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
      </div>

      {/* Stats */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        {[
          { title: 'Total Records', value: state.items.length },
          { title: 'Unique Claims', value: uniqueClaims },
          { title: 'Average Value', value: avgValue },
        ].map(c => (
          <Col xs={24} sm={8} key={c.title}>
            <Card style={styles.statCard}>
              <Statistic title={c.title} value={c.value} />
            </Card>
          </Col>
        ))}
      </Row>

      {/* Metric filter */}
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

      {state.error && (
        <Alert type="error" message={state.error} style={{ marginBottom: 16 }} />
      )}

      <Table
        rowKey="kpiId"
        columns={columns}
        dataSource={state.items}
        loading={state.loading}
        pagination={{ pageSize: 10 }}
        bordered
        size="middle"
      />
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  header: {
    display: 'flex', justifyContent: 'space-between',
    alignItems: 'flex-start', marginBottom: 24,
    flexWrap: 'wrap', gap: 12,
  },
  statCard: { borderRadius: 10, border: '1px solid #e2e8f0' },
};
