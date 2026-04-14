import { useEffect, useReducer, useCallback } from 'react';
import {
  Table, Button, Modal, Form, Input, Select,
  Typography, Space, Tag, Alert, Tooltip, Popconfirm, Card, Row, Col, Statistic, Badge,
} from 'antd';
import { PlusOutlined, ReloadOutlined, DeleteOutlined, BellOutlined, CheckOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import {
  notificationsApi, Notification, CreateNotificationRequest,
  NOTIFICATION_CATEGORIES, NOTIFICATION_STATUSES,
} from '../../api/notificationsApi';
import { useSelector } from 'react-redux';
import { selectUserId } from '../../store/slices/authSlice';

const { Title, Text } = Typography;
const { Option } = Select;
const { TextArea } = Input;

// ── Reducer ───────────────────────────────────────────────────────────────────

interface State {
  items: Notification[];
  unreadCount: number;
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
  filterCategory: string | null;
  filterStatus: string | null;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: Notification[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'SET_UNREAD'; payload: number }
  | { type: 'OPEN_MODAL' }
  | { type: 'CLOSE_MODAL' }
  | { type: 'SUBMIT_START' }
  | { type: 'SUBMIT_SUCCESS'; payload: Notification }
  | { type: 'SUBMIT_ERROR' }
  | { type: 'DELETE_SUCCESS'; payload: number }
  | { type: 'STATUS_UPDATED'; payload: Notification }
  | { type: 'MARK_ALL_READ' }
  | { type: 'SET_CATEGORY'; payload: string | null }
  | { type: 'SET_STATUS'; payload: string | null };

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'FETCH_START':    return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS':  return { ...state, loading: false, items: action.payload };
    case 'FETCH_ERROR':    return { ...state, loading: false, error: action.payload };
    case 'SET_UNREAD':     return { ...state, unreadCount: action.payload };
    case 'OPEN_MODAL':     return { ...state, modalOpen: true };
    case 'CLOSE_MODAL':    return { ...state, modalOpen: false, submitting: false };
    case 'SUBMIT_START':   return { ...state, submitting: true };
    case 'SUBMIT_SUCCESS': return { ...state, submitting: false, modalOpen: false, items: [action.payload, ...state.items] };
    case 'SUBMIT_ERROR':   return { ...state, submitting: false };
    case 'DELETE_SUCCESS': return { ...state, items: state.items.filter(i => i.notificationId !== action.payload) };
    case 'STATUS_UPDATED': return {
      ...state,
      items: state.items.map(i => i.notificationId === action.payload.notificationId ? action.payload : i),
    };
    case 'MARK_ALL_READ':  return {
      ...state, unreadCount: 0,
      items: state.items.map(i => i.status === 'UNREAD' ? { ...i, status: 'READ' } : i),
    };
    case 'SET_CATEGORY':   return { ...state, filterCategory: action.payload };
    case 'SET_STATUS':     return { ...state, filterStatus: action.payload };
    default:               return state;
  }
}

// ── Component ─────────────────────────────────────────────────────────────────

const CAT_COLORS: Record<string, string> = {
  RISK: 'red', DENIAL: 'orange', COST: 'gold', PERFORMANCE: 'blue', AGING: 'purple', SYSTEM: 'default',
};
const STATUS_COLORS: Record<string, string> = {
  UNREAD: 'blue', READ: 'default', DISMISSED: 'default', ACTIONED: 'green',
};

export default function NotificationsPage() {
  const userId = useSelector(selectUserId) ?? 1;

  const [state, dispatch] = useReducer(reducer, {
    items: [], unreadCount: 0, loading: false, error: null,
    modalOpen: false, submitting: false, filterCategory: null, filterStatus: null,
  });
  const [form] = Form.useForm();

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      let data: Notification[];
      if (state.filterCategory) {
        data = await notificationsApi.getByCategory(userId, state.filterCategory);
      } else if (state.filterStatus) {
        data = await notificationsApi.getByStatus(userId, state.filterStatus);
      } else {
        data = await notificationsApi.getForUser(userId);
      }
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
      const count = await notificationsApi.getUnreadCount(userId).catch(() => 0);
      dispatch({ type: 'SET_UNREAD', payload: count });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load notifications' });
    }
  }, [userId, state.filterCategory, state.filterStatus]);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (values: Record<string, unknown>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const req: CreateNotificationRequest = {
        userId:      Number(values.userId),
        title:       values.title as string,
        message:     values.message as string,
        category:    values.category as string,
        referenceId: values.referenceId as string | undefined,
      };
      dispatch({ type: 'SUBMIT_SUCCESS', payload: await notificationsApi.create(req) });
      form.resetFields();
    } catch {
      dispatch({ type: 'SUBMIT_ERROR' });
    }
  };

  const handleDelete = async (id: number) => {
    await notificationsApi.delete(id);
    dispatch({ type: 'DELETE_SUCCESS', payload: id });
  };

  const handleMarkAllRead = async () => {
    await notificationsApi.markAllRead(userId);
    dispatch({ type: 'MARK_ALL_READ' });
  };

  const handleStatusChange = async (id: number, status: string) => {
    const updated = await notificationsApi.updateStatus(id, status);
    dispatch({ type: 'STATUS_UPDATED', payload: updated });
  };

  const columns: ColumnsType<Notification> = [
    { title: 'ID',       dataIndex: 'notificationId', key: 'notificationId', width: 80 },
    {
      title: 'Status',   dataIndex: 'status',          key: 'status',
      render: (v, rec) => (
        <Select size="small" value={v} style={{ width: 110 }}
          onChange={s => handleStatusChange(rec.notificationId, s)}>
          {NOTIFICATION_STATUSES.map(s => (
            <Option key={s} value={s}><Tag color={STATUS_COLORS[s]}>{s}</Tag></Option>
          ))}
        </Select>
      ),
      filters: NOTIFICATION_STATUSES.map(s => ({ text: s, value: s })),
      onFilter: (value, rec) => rec.status === value,
    },
    {
      title: 'Category', dataIndex: 'category',        key: 'category',
      render: v => <Tag color={CAT_COLORS[v] ?? 'default'}>{v}</Tag>,
      filters: NOTIFICATION_CATEGORIES.map(c => ({ text: c, value: c })),
      onFilter: (value, rec) => rec.category === value,
    },
    { title: 'Title',   dataIndex: 'title',            key: 'title',         ellipsis: true },
    { title: 'Message', dataIndex: 'message',          key: 'message',       ellipsis: true },
    { title: 'Created', dataIndex: 'createdDate',      key: 'createdDate',   sorter: (a, b) => a.createdDate.localeCompare(b.createdDate) },
    {
      title: 'Action', key: 'action', width: 80,
      render: (_, rec) => (
        <Popconfirm title="Delete notification?" onConfirm={() => handleDelete(rec.notificationId)}>
          <Tooltip title="Delete"><Button icon={<DeleteOutlined />} size="small" danger /></Tooltip>
        </Popconfirm>
      ),
    },
  ];

  const readCount      = state.items.filter(i => i.status === 'READ').length;
  const actionedCount  = state.items.filter(i => i.status === 'ACTIONED').length;

  return (
    <div>
      <div style={styles.header}>
        <div>
          <Title level={3} style={{ margin: 0 }}>
            <Badge count={state.unreadCount} offset={[10, 0]}>
              Notifications
            </Badge>
          </Title>
          <Text type="secondary">System and workflow notifications</Text>
        </div>
        <Space wrap>
          <Select allowClear placeholder="Category" style={{ width: 140 }}
            value={state.filterCategory}
            onChange={v => dispatch({ type: 'SET_CATEGORY', payload: v ?? null })}>
            {NOTIFICATION_CATEGORIES.map(c => <Option key={c} value={c}>{c}</Option>)}
          </Select>
          <Select allowClear placeholder="Status" style={{ width: 130 }}
            value={state.filterStatus}
            onChange={v => dispatch({ type: 'SET_STATUS', payload: v ?? null })}>
            {NOTIFICATION_STATUSES.map(s => <Option key={s} value={s}>{s}</Option>)}
          </Select>
          <Button icon={<CheckOutlined />} onClick={handleMarkAllRead}>Mark All Read</Button>
          <Button icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_MODAL' })}>
            New
          </Button>
        </Space>
      </div>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        {[
          { title: 'Total',    value: state.items.length,  color: '#2563eb' },
          { title: 'Unread',   value: state.unreadCount,   color: '#dc2626' },
          { title: 'Read',     value: readCount,           color: '#16a34a' },
          { title: 'Actioned', value: actionedCount,       color: '#7c3aed' },
        ].map(c => (
          <Col xs={12} sm={6} key={c.title}>
            <Card style={styles.statCard}>
              <Statistic title={c.title} value={c.value}
                valueStyle={{ color: c.color }} prefix={<BellOutlined />} />
            </Card>
          </Col>
        ))}
      </Row>

      {state.error && <Alert type="error" message={state.error} style={{ marginBottom: 16 }} />}

      <Table
        rowKey="notificationId"
        columns={columns}
        dataSource={state.items}
        loading={state.loading}
        pagination={{ pageSize: 10 }}
        bordered size="middle"
        rowClassName={rec => rec.status === 'UNREAD' ? 'unread-row' : ''}
      />

      <Modal
        title="Create Notification"
        open={state.modalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_MODAL' }); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={state.submitting}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}
          initialValues={{ userId }}>
          <Form.Item name="userId" label="User ID" rules={[{ required: true }]}>
            <Input type="number" min={1} />
          </Form.Item>
          <Form.Item name="title" label="Title" rules={[{ required: true }]}>
            <Input placeholder="Notification title" />
          </Form.Item>
          <Form.Item name="message" label="Message" rules={[{ required: true }]}>
            <TextArea rows={3} placeholder="Notification message..." />
          </Form.Item>
          <Form.Item name="category" label="Category" rules={[{ required: true }]}>
            <Select placeholder="Select category">
              {NOTIFICATION_CATEGORIES.map(c => (
                <Option key={c} value={c}><Tag color={CAT_COLORS[c]}>{c}</Tag></Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="referenceId" label="Reference ID (optional)">
            <Input placeholder="CLM-2026-AUTO-001" />
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
