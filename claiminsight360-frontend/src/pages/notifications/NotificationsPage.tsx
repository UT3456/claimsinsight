import { useEffect, useReducer, useCallback } from 'react';
import {
  Button, Modal, Form, Input, Select,
  Typography, Space, Tag, Popconfirm, Card, Row, Col,
} from 'antd';
import { PlusOutlined, ReloadOutlined, DeleteOutlined, CheckOutlined } from '@ant-design/icons';
import {
  notificationsApi, Notification, CreateNotificationRequest,
  NOTIFICATION_CATEGORIES, NOTIFICATION_STATUSES,
} from '../../api/notificationsApi';
import { useSelector } from 'react-redux';
import { selectUserId } from '../../store/slices/authSlice';

const { Title, Text } = Typography;
const { Option } = Select;
const { TextArea } = Input;

// ── Category dot colours ──────────────────────────────────────────────────────
const CAT_DOT: Record<string, string> = {
  RISK: '#dc2626', DENIAL: '#f59e0b', COST: '#2563eb',
  PERFORMANCE: '#7c3aed', AGING: '#d97706', SYSTEM: '#6b7280',
};
const CAT_TAG: Record<string, string> = {
  RISK: 'red', DENIAL: 'orange', COST: 'blue',
  PERFORMANCE: 'purple', AGING: 'gold', SYSTEM: 'default',
};

function relativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1)  return 'just now';
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
}

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

  const readCount     = state.items.filter(i => i.status === 'READ').length;
  const actionedCount = state.items.filter(i => i.status === 'ACTIONED').length;

  return (
    <div>
      {/* ── Header ── */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20, flexWrap: 'wrap', gap: 12 }}>
        <div>
          <Title level={4} style={{ margin: 0, color: 'var(--ci-text-primary)' }}>
            Notifications
            {state.unreadCount > 0 && (
              <span style={{
                marginLeft: 8, fontSize: 11, fontWeight: 600,
                background: '#dc2626', color: '#fff',
                borderRadius: 10, padding: '1px 7px',
              }}>
                {state.unreadCount}
              </span>
            )}
          </Title>
          <Text style={{ fontSize: 12, color: 'var(--ci-text-muted)' }}>System and workflow notifications</Text>
        </div>
        <Space wrap size={8}>
          <Select allowClear placeholder="Category" style={{ width: 130, fontSize: 12 }}
            value={state.filterCategory}
            onChange={v => dispatch({ type: 'SET_CATEGORY', payload: v ?? null })}>
            {NOTIFICATION_CATEGORIES.map(c => <Option key={c} value={c}>{c}</Option>)}
          </Select>
          <Select allowClear placeholder="Status" style={{ width: 120, fontSize: 12 }}
            value={state.filterStatus}
            onChange={v => dispatch({ type: 'SET_STATUS', payload: v ?? null })}>
            {NOTIFICATION_STATUSES.map(s => <Option key={s} value={s}>{s}</Option>)}
          </Select>
          <Button size="small" icon={<CheckOutlined />} onClick={handleMarkAllRead}>Mark All Read</Button>
          <Button size="small" icon={<ReloadOutlined />} onClick={load}>Refresh</Button>
          <Button size="small" type="primary" icon={<PlusOutlined />} onClick={() => dispatch({ type: 'OPEN_MODAL' })}>
            New
          </Button>
        </Space>
      </div>

      {/* ── Stat row ── */}
      <Row gutter={10} style={{ marginBottom: 20 }}>
        {[
          { label: 'Total',    value: state.items.length, color: '#2563eb' },
          { label: 'Unread',   value: state.unreadCount,  color: '#dc2626' },
          { label: 'Read',     value: readCount,          color: '#16a34a' },
          { label: 'Actioned', value: actionedCount,      color: '#7c3aed' },
        ].map(c => (
          <Col xs={12} sm={6} key={c.label}>
            <div className="ci-stat">
              <div className="ci-stat-lbl">{c.label}</div>
              <div className="ci-stat-val" style={{ color: c.color }}>{c.value}</div>
            </div>
          </Col>
        ))}
      </Row>

      {state.error && (
        <div className="ci-alert ci-alert-red" style={{ marginBottom: 14 }}>{state.error}</div>
      )}

      {/* ── Notification list ── */}
      <Card
        style={{ border: '1px solid var(--ci-border)', borderRadius: 4, boxShadow: 'none' }}
        bodyStyle={{ padding: 0 }}
        loading={state.loading}
      >
        {state.items.length === 0 && !state.loading && (
          <div style={{ padding: '32px 0', textAlign: 'center', color: 'var(--ci-text-muted)', fontSize: 13 }}>
            No notifications found
          </div>
        )}
        {state.items.map(n => (
          <div
            key={n.notificationId}
            className={`ci-notif-item${n.status === 'UNREAD' ? ' unread' : ''}`}
          >
            {/* coloured dot */}
            <div
              className="ci-notif-dot"
              style={{
                background: n.status === 'READ' || n.status === 'DISMISSED'
                  ? '#d1d5db'
                  : (CAT_DOT[n.category] ?? '#6b7280'),
              }}
            />

            {/* body */}
            <div style={{ flex: 1, minWidth: 0 }}>
              <div className="ci-notif-title">{n.title}</div>
              <div className="ci-notif-message">{n.message}</div>
              <div style={{ marginTop: 4, display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
                <Tag color={CAT_TAG[n.category] ?? 'default'} style={{ fontSize: 10, margin: 0 }}>{n.category}</Tag>
                <Select
                  size="small"
                  value={n.status}
                  style={{ width: 110, fontSize: 11 }}
                  onChange={s => handleStatusChange(n.notificationId, s)}
                >
                  {NOTIFICATION_STATUSES.map(s => <Option key={s} value={s}>{s}</Option>)}
                </Select>
              </div>
            </div>

            {/* time + delete */}
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 6, flexShrink: 0 }}>
              <span className="ci-notif-time">{relativeTime(n.createdDate)}</span>
              <Popconfirm title="Delete notification?" onConfirm={() => handleDelete(n.notificationId)}>
                <Button icon={<DeleteOutlined />} size="small" type="text" danger />
              </Popconfirm>
            </div>
          </div>
        ))}
      </Card>

      {/* ── Create modal ── */}
      <Modal
        title="Create Notification"
        open={state.modalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_MODAL' }); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={state.submitting}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleCreate} initialValues={{ userId }}>
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
                <Option key={c} value={c}><Tag color={CAT_TAG[c]}>{c}</Tag></Option>
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
