import { useEffect, useReducer, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, Typography,
  Space, Alert, Tooltip, Popconfirm, Tag, Card, Row, Col, Statistic,
} from 'antd';
import { PlusOutlined, ReloadOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { ingestApi, RawClaim, IngestRequest } from '../../api/dataIngestionApi';

const { Title, Text } = Typography;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  items: RawClaim[];
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
  searchClaimId: string;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: RawClaim[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'OPEN_MODAL' }
  | { type: 'CLOSE_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'SUBMIT_SUCCESS'; payload: RawClaim }
  | { type: 'SUBMIT_ERROR' }
  | { type: 'DELETE_SUCCESS'; payload: number }
  | { type: 'SET_SEARCH'; payload: string };

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
    case 'DELETE_SUCCESS': return { ...state, items: state.items.filter(i => i.rawId !== action.payload) };
    case 'SET_SEARCH':     return { ...state, searchClaimId: action.payload };
    default:               return state;
  }
}

// ── Component ─────────────────────────────────────────────────────────────────

export default function RawClaimsPage() {
  const [state, dispatch] = useReducer(reducer, {
    items: [], loading: false, error: null, modalOpen: false, submitting: false, searchClaimId: '',
  });
  const [form] = Form.useForm();

  const load = useCallback(async (claimId?: string) => {
    dispatch({ type: 'FETCH_START' });
    try {
      const data = claimId?.trim()
        ? await ingestApi.getByClaim(claimId.trim())
        : await ingestApi.getAll();
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load raw claims' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleIngest = async (values: Omit<IngestRequest, 'feedId'> & { feedId: string }) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const record = await ingestApi.ingest({
        claimId:     values.claimId,
        feedId:      Number(values.feedId),
        payloadJson: values.payloadJson,
      });
      dispatch({ type: 'SUBMIT_SUCCESS', payload: record });
      form.resetFields();
    } catch {
      dispatch({ type: 'SUBMIT_ERROR' });
    }
  };

  const handleDelete = async (id: number) => {
    await ingestApi.delete(id);
    dispatch({ type: 'DELETE_SUCCESS', payload: id });
  };

  const columns: ColumnsType<RawClaim> = [
    { title: 'Raw ID',   dataIndex: 'rawId',        key: 'rawId',       width: 90 },
    { title: 'Claim ID', dataIndex: 'claimId',       key: 'claimId',     render: v => <Text code>{v}</Text> },
    {
      title: 'Payload',  dataIndex: 'payloadJson',   key: 'payloadJson',
      ellipsis: true,
      render: v => <Text type="secondary" style={{ fontSize: 12 }}>{v}</Text>,
    },
    {
      title: 'Ingested', dataIndex: 'ingestedDate',  key: 'ingestedDate',
      render: v => v?.split('T')[0] ?? '—',
    },
    {
      title: 'Action', key: 'action', width: 80,
      render: (_, rec) => (
        <Popconfirm title="Delete this raw claim?" onConfirm={() => handleDelete(rec.rawId)}>
          <Tooltip title="Delete"><Button icon={<DeleteOutlined />} size="small" danger /></Tooltip>
        </Popconfirm>
      ),
    },
  ];

  return (
    <div>
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0 }}>Raw Claims</Title>
          <Text type="secondary">Ingested raw claim payloads</Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => load()}>Refresh</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_MODAL' })}>
            Ingest Claim
          </Button>
        </Space>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={8}>
          <Card style={styles.statCard}>
            <Statistic title="Total Ingested" value={state.items.length} />
          </Card>
        </Col>
        <Col xs={24} sm={16}>
          <Card style={styles.statCard}>
            <Space>
              <Text strong>Search by Claim ID:</Text>
              <Input
                placeholder="CLM-2026-AUTO-001"
                value={state.searchClaimId}
                onChange={e => dispatch({ type: 'SET_SEARCH', payload: e.target.value })}
                style={{ width: 240 }}
                allowClear
              />
              <Button
                icon={<SearchOutlined />}
                onClick={() => load(state.searchClaimId)}
              >
                Search
              </Button>
              <Button onClick={() => { dispatch({ type: 'SET_SEARCH', payload: '' }); load(); }}>
                Clear
              </Button>
            </Space>
          </Card>
        </Col>
      </Row>

      {state.error && <Alert type="error" message={state.error} style={{ marginBottom: 16 }} />}

      <Table
        rowKey="rawId"
        columns={columns}
        dataSource={state.items}
        loading={state.loading}
        pagination={{ pageSize: 10 }}
        bordered size="middle"
      />

      <Modal
        title="Ingest Raw Claim"
        open={state.modalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_MODAL' }); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={state.submitting}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleIngest}>
          <Form.Item name="claimId" label="Claim ID" rules={[{ required: true, min: 3 }]}>
            <Input placeholder="CLM-2026-AUTO-001" />
          </Form.Item>
          <Form.Item name="feedId" label="Feed ID" rules={[{ required: true }]}>
            <Input type="number" placeholder="1" min={1} />
          </Form.Item>
          <Form.Item name="payloadJson" label="Payload JSON" rules={[{ required: true }]}>
            <Input.TextArea
              rows={4}
              placeholder='{"claimType":"Auto","amount":5000}'
            />
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
