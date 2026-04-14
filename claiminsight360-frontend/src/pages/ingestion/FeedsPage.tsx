import { useEffect, useReducer, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, Typography,
  Space, Tag, Alert, Tooltip, Popconfirm, Card, Row, Col, Statistic,
} from 'antd';
import { PlusOutlined, ReloadOutlined, DeleteOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { feedsApi, DataFeed, FEED_TYPES, FEED_STATUSES } from '../../api/dataIngestionApi';

const { Title, Text } = Typography;
const { Option } = Select;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  items: DataFeed[];
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: DataFeed[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'OPEN_MODAL' }
  | { type: 'CLOSE_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'SUBMIT_SUCCESS'; payload: DataFeed }
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
    case 'SUBMIT_SUCCESS': return { ...state, submitting: false, modalOpen: false, items: [...state.items, action.payload] };
    case 'SUBMIT_ERROR':   return { ...state, submitting: false };
    case 'DELETE_SUCCESS': return { ...state, items: state.items.filter(i => i.feedId !== action.payload) };
    default:               return state;
  }
}

// ── Component ─────────────────────────────────────────────────────────────────

const STATUS_COLOR: Record<string, string> = { ACTIVE: 'green', INACTIVE: 'default', FAILED: 'red' };

export default function FeedsPage() {
  const [state, dispatch] = useReducer(reducer, {
    items: [], loading: false, error: null, modalOpen: false, submitting: false,
  });
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const data = await feedsApi.getAll();
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load data feeds' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (values: Record<string, string>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const record = await feedsApi.create(values as { feedType: string; sourceSystem: string; status: string });
      dispatch({ type: 'SUBMIT_SUCCESS', payload: record });
      form.resetFields();
    } catch {
      dispatch({ type: 'SUBMIT_ERROR' });
    }
  };

  const handleDelete = async (id: number) => {
    await feedsApi.delete(id);
    dispatch({ type: 'DELETE_SUCCESS', payload: id });
  };

  const active   = state.items.filter(i => i.status === 'ACTIVE').length;
  const inactive = state.items.filter(i => i.status === 'INACTIVE').length;
  const failed   = state.items.filter(i => i.status === 'FAILED').length;

  const columns: ColumnsType<DataFeed> = [
    { title: 'Feed ID',       dataIndex: 'feedId',       key: 'feedId',       width: 90 },
    { title: 'Feed Type',     dataIndex: 'feedType',     key: 'feedType',     render: v => <Tag color="blue">{v}</Tag> },
    { title: 'Source System', dataIndex: 'sourceSystem', key: 'sourceSystem'  },
    {
      title: 'Status', dataIndex: 'status', key: 'status',
      render: v => <Tag color={STATUS_COLOR[v] ?? 'default'}>{v}</Tag>,
    },
    { title: 'Last Sync',     dataIndex: 'lastSyncDate', key: 'lastSyncDate', render: v => v ?? '—' },
    { title: 'Created',       dataIndex: 'createdDate',  key: 'createdDate',  render: v => v?.split('T')[0] ?? '—' },
    {
      title: 'Action', key: 'action', width: 80,
      render: (_, rec) => (
        <Popconfirm title="Delete this feed?" onConfirm={() => handleDelete(rec.feedId)}>
          <Tooltip title="Delete"><Button icon={<DeleteOutlined />} size="small" danger /></Tooltip>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0 }}>Data Feeds</Title>
          <Text type="secondary">Registered data feed sources</Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_MODAL' })}>
            Register Feed
          </Button>
        </Space>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        {[
          { title: 'Total Feeds',   value: state.items.length, color: '#2563eb' },
          { title: 'Active',        value: active,             color: '#16a34a' },
          { title: 'Inactive',      value: inactive,           color: '#6b7280' },
          { title: 'Failed',        value: failed,             color: '#dc2626' },
        ].map(c => (
          <Col xs={12} sm={6} key={c.title}>
            <Card style={styles.statCard}>
              <Statistic title={c.title} value={c.value} valueStyle={{ color: c.color }} />
            </Card>
          </Col>
        ))}
      </Row>

      {state.error && <Alert type="error" message={state.error} style={{ marginBottom: 16 }} />}

      <Table
        rowKey="feedId"
        columns={columns}
        dataSource={state.items}
        loading={state.loading}
        pagination={{ pageSize: 10 }}
        bordered size="middle"
      />

      <Modal
        title="Register Data Feed"
        open={state.modalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_MODAL' }); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={state.submitting}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="feedType" label="Feed Type" rules={[{ required: true }]}>
            <Select placeholder="Select type">
              {FEED_TYPES.map(t => <Option key={t} value={t}>{t}</Option>)}
            </Select>
          </Form.Item>
          <Form.Item name="sourceSystem" label="Source System" rules={[{ required: true, min: 2 }]}>
            <Input placeholder="e.g. SAP, Oracle, Guidewire" />
          </Form.Item>
          <Form.Item name="status" label="Status" rules={[{ required: true }]}>
            <Select placeholder="Select status">
              {FEED_STATUSES.map(s => <Option key={s} value={s}>{s}</Option>)}
            </Select>
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
