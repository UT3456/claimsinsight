import { useEffect, useMemo, useReducer, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table, Modal, Form, Input, Select, Alert, Tooltip, Popconfirm, Button,
  App as AntApp,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useSelector } from 'react-redux';
import { RefreshCw, Plus, Trash2, Check, CheckCheck, Bell, Search } from 'lucide-react';

import {
  PageHeader, KpiCard, DataCard, Badge, Chip, StatusDot,
  GhostButton, DarkButton, EmptyState,
} from '../../components/ui';
import type { BadgeTone, StatusTone } from '../../components/ui';
import {
  notificationsApi, NOTIFICATION_CATEGORIES, NOTIFICATION_STATUSES,
  type Notification, type CreateNotificationRequest,
} from '../../api/notificationsApi';
import { selectUserId } from '../../store/slices/authSlice';

const { Option } = Select;
const { TextArea } = Input;

/**
 * NotificationsPage — reference screen #10.
 *
 * IMPORTANT — bug fixes vs. prior version:
 *  1) Uses the REAL userId from auth (no `?? 1` fallback). Page waits for auth
 *     to hydrate before loading — previously the table queried user #1 on every
 *     first render, so users saw an empty table while the bell (correct userId)
 *     showed notifications.
 *  2) Category + status filters are applied client-side over one `getForUser`
 *     result, so both filters can be combined.
 *  3) "Mark all read" refetches so `readDate` is current and the unread count
 *     stays in sync with server state.
 */

// ── Reducer ──────────────────────────────────────────────────────────────────

interface State {
  items: Notification[];
  unreadCount: number;
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  submitting: boolean;
  filterCategory: string | null;
  filterStatus: string | null;
  search: string;
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
  | { type: 'DELETE'; payload: number }
  | { type: 'STATUS_UPDATED'; payload: Notification }
  | { type: 'SET_CATEGORY'; payload: string | null }
  | { type: 'SET_STATUS'; payload: string | null }
  | { type: 'SET_SEARCH'; payload: string };

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'FETCH_START':    return { ...s, loading: true, error: null };
    case 'FETCH_SUCCESS':  return { ...s, loading: false, items: a.payload };
    case 'FETCH_ERROR':    return { ...s, loading: false, error: a.payload };
    case 'SET_UNREAD':     return { ...s, unreadCount: a.payload };
    case 'OPEN_MODAL':     return { ...s, modalOpen: true };
    case 'CLOSE_MODAL':    return { ...s, modalOpen: false, submitting: false };
    case 'SUBMIT_START':   return { ...s, submitting: true };
    case 'SUBMIT_SUCCESS': return { ...s, submitting: false, modalOpen: false, items: [a.payload, ...s.items] };
    case 'SUBMIT_ERROR':   return { ...s, submitting: false };
    case 'DELETE':         return { ...s, items: s.items.filter(i => i.notificationId !== a.payload) };
    case 'STATUS_UPDATED': return {
      ...s,
      items: s.items.map(i => i.notificationId === a.payload.notificationId ? a.payload : i),
    };
    case 'SET_CATEGORY':   return { ...s, filterCategory: a.payload };
    case 'SET_STATUS':     return { ...s, filterStatus: a.payload };
    case 'SET_SEARCH':     return { ...s, search: a.payload };
    default:               return s;
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

const CAT_TONE: Record<string, BadgeTone> = {
  RISK: 'red', DENIAL: 'amber', COST: 'amber', PERFORMANCE: 'blue', AGING: 'purple', SYSTEM: 'neutral',
};
const STATUS_META: Record<string, { tone: BadgeTone; status: StatusTone }> = {
  UNREAD:    { tone: 'blue',   status: 'info'    },
  READ:      { tone: 'neutral', status: 'muted'  },
  DISMISSED: { tone: 'neutral', status: 'muted'  },
  ACTIONED:  { tone: 'green',  status: 'healthy' },
};

function timeAgo(iso: string): string {
  if (!iso) return '';
  const then = new Date(iso).getTime();
  if (isNaN(then)) return iso;
  const diff = Date.now() - then;
  const mins = Math.floor(diff / 60000);
  if (mins < 1)    return 'just now';
  if (mins < 60)   return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24)    return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  if (days < 30)   return `${days}d ago`;
  return new Date(iso).toLocaleDateString();
}

/** If the referenceId looks like a claim ID (CLM- prefix or numeric), build a link. */
function claimHref(ref: string | null): string | null {
  if (!ref) return null;
  if (/^CLM[-_]/i.test(ref) || /^\d+$/.test(ref)) return `/claims/${ref}`;
  return null;
}

// ── Component ────────────────────────────────────────────────────────────────

export default function NotificationsPage() {
  const navigate = useNavigate();
  const userId = useSelector(selectUserId);

  const [state, dispatch] = useReducer(reducer, {
    items: [], unreadCount: 0, loading: false, error: null,
    modalOpen: false, submitting: false,
    filterCategory: null, filterStatus: null, search: '',
  });
  const [form] = Form.useForm();
  // Scoped message API (AntApp) — theme-aware toasts for mutation feedback.
  const { message } = AntApp.useApp();

  const load = useCallback(async () => {
    if (!userId) {
      // Auth not ready yet — don't fall back to a wrong userId.
      dispatch({ type: 'FETCH_SUCCESS', payload: [] });
      return;
    }
    dispatch({ type: 'FETCH_START' });
    try {
      // Fetch ALL for the user once; filters are applied client-side so category
      // + status can combine (the API has separate endpoints per axis).
      const data = await notificationsApi.getForUser(userId);
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
      const count = await notificationsApi.getUnreadCount(userId).catch(() => 0);
      dispatch({ type: 'SET_UNREAD', payload: count });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load notifications.' });
    }
  }, [userId]);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async (values: Record<string, unknown>) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const req: CreateNotificationRequest = {
        userId:      Number(values.userId),
        title:       values.title as string,
        message:     values.message as string,
        category:    values.category as string,
        referenceId: (values.referenceId as string) || undefined,
      };
      dispatch({ type: 'SUBMIT_SUCCESS', payload: await notificationsApi.create(req) });
      form.resetFields();
      message.success('Notification sent');
    } catch {
      dispatch({ type: 'SUBMIT_ERROR' });
      message.error('Failed to send notification');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await notificationsApi.delete(id);
      dispatch({ type: 'DELETE', payload: id });
      message.success('Notification deleted');
    } catch {
      message.error('Failed to delete notification');
    }
  };

  const handleStatusChange = async (id: number, status: string) => {
    try {
      const updated = await notificationsApi.updateStatus(id, status);
      dispatch({ type: 'STATUS_UPDATED', payload: updated });
      // Re-pull unread count (server-side truth)
      if (userId) {
        const count = await notificationsApi.getUnreadCount(userId).catch(() => state.unreadCount);
        dispatch({ type: 'SET_UNREAD', payload: count });
      }
      message.success(`Marked ${status.toLowerCase()}`);
    } catch {
      message.error('Failed to update status');
    }
  };

  const handleMarkAllRead = async () => {
    if (!userId) return;
    try {
      await notificationsApi.markAllRead(userId);
      await load();  // refetch so readDate + unreadCount match server
      message.success('All notifications marked as read');
    } catch {
      message.error('Failed to mark all as read');
    }
  };

  // ── Derived ──────────────────────────────────────────────────────────────
  const filtered = useMemo(() => {
    let list = state.items;
    if (state.filterCategory) list = list.filter(i => i.category === state.filterCategory);
    if (state.filterStatus)   list = list.filter(i => i.status === state.filterStatus);
    if (state.search.trim()) {
      const q = state.search.trim().toLowerCase();
      list = list.filter(i =>
        i.title.toLowerCase().includes(q) ||
        i.message.toLowerCase().includes(q) ||
        (i.referenceId ?? '').toLowerCase().includes(q),
      );
    }
    return list;
  }, [state.items, state.filterCategory, state.filterStatus, state.search]);

  const readCount     = state.items.filter(i => i.status === 'READ').length;
  const actionedCount = state.items.filter(i => i.status === 'ACTIONED').length;

  const columns: ColumnsType<Notification> = [
    {
      title: '', dataIndex: 'status', key: 'unread-dot', width: 24,
      render: v => (
        <span style={{
          display: 'inline-block', width: 7, height: 7, borderRadius: '50%',
          background: v === 'UNREAD' ? 'var(--ci-primary, #185FA5)' : 'transparent',
          border: v === 'UNREAD' ? 'none' : '1px solid var(--ci-border)',
        }} />
      ),
    },
    {
      title: 'Category', dataIndex: 'category', key: 'category', width: 120,
      render: (v: string) => <Badge tone={CAT_TONE[v] ?? 'neutral'}>{v}</Badge>,
    },
    {
      title: 'Title', dataIndex: 'title', key: 'title', width: 240,
      render: (v: string, rec) => (
        <div>
          <div style={{ fontWeight: 500, fontSize: 12 }}>{v}</div>
          {rec.referenceId && (
            <div style={{ fontSize: 10, marginTop: 2 }}>
              {claimHref(rec.referenceId)
                ? <a onClick={() => navigate(claimHref(rec.referenceId)!)} style={{ color: 'var(--ci-primary)', fontFamily: 'ui-monospace, SFMono-Regular, monospace' }}>{rec.referenceId}</a>
                : <span style={{ color: 'var(--ci-text-muted)', fontFamily: 'ui-monospace, SFMono-Regular, monospace' }}>{rec.referenceId}</span>}
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Message', dataIndex: 'message', key: 'message', ellipsis: true,
      render: v => <span style={{ color: 'var(--ci-text-secondary)' }}>{v}</span>,
    },
    {
      title: 'Status', dataIndex: 'status', key: 'status', width: 140,
      render: (v: string, rec) => {
        const meta = STATUS_META[v] ?? { tone: 'neutral' as BadgeTone, status: 'muted' as StatusTone };
        return (
          <Select
            size="small" value={v} bordered={false}
            style={{ width: 120 }}
            onChange={s => handleStatusChange(rec.notificationId, s)}
            dropdownMatchSelectWidth={false}
          >
            {NOTIFICATION_STATUSES.map(s => (
              <Option key={s} value={s}>
                <StatusDot tone={STATUS_META[s]?.status ?? 'muted'}>{s}</StatusDot>
              </Option>
            ))}
          </Select>
        );
        void meta;
      },
    },
    {
      title: 'Created', dataIndex: 'createdDate', key: 'createdDate', width: 110,
      sorter: (a, b) => a.createdDate.localeCompare(b.createdDate),
      defaultSortOrder: 'descend',
      render: v => <span style={{ color: 'var(--ci-text-secondary)', fontSize: 11 }}>{timeAgo(v)}</span>,
    },
    {
      title: '', key: 'action', width: 70, align: 'right',
      render: (_, rec) => (
        <div style={{ display: 'inline-flex', gap: 2 }}>
          {rec.status === 'UNREAD' && (
            <Tooltip title="Mark as read">
              <Button
                type="text" size="small"
                icon={<Check size={13} strokeWidth={1.6} />}
                onClick={() => handleStatusChange(rec.notificationId, 'READ')}
                style={{ color: 'var(--ci-text-muted)' }}
              />
            </Tooltip>
          )}
          <Popconfirm title="Delete this notification?" onConfirm={() => handleDelete(rec.notificationId)}>
            <Tooltip title="Delete">
              <Button type="text" size="small" icon={<Trash2 size={13} strokeWidth={1.6} />} style={{ color: 'var(--ci-text-muted)' }} />
            </Tooltip>
          </Popconfirm>
        </div>
      ),
    },
  ];

  const catCycle: (string | null)[]    = [null, ...NOTIFICATION_CATEGORIES];
  const statusCycle: (string | null)[] = [null, ...NOTIFICATION_STATUSES];

  return (
    <div>
      <PageHeader
        title="Notifications"
        subtitle={
          !userId
            ? 'Sign in to see notifications'
            : `${state.items.length} total · ${state.unreadCount} unread`
        }
        actions={
          <>
            <Chip
              dropdown
              active={!!state.filterCategory}
              onClick={() => {
                const i = catCycle.indexOf(state.filterCategory);
                dispatch({ type: 'SET_CATEGORY', payload: catCycle[(i + 1) % catCycle.length] });
              }}
            >
              {state.filterCategory ?? 'All categories'}
            </Chip>
            <Chip
              dropdown
              active={!!state.filterStatus}
              onClick={() => {
                const i = statusCycle.indexOf(state.filterStatus);
                dispatch({ type: 'SET_STATUS', payload: statusCycle[(i + 1) % statusCycle.length] });
              }}
            >
              {state.filterStatus ?? 'All statuses'}
            </Chip>
            <GhostButton
              icon={<CheckCheck size={12} strokeWidth={1.8} />}
              onClick={handleMarkAllRead}
              disabled={!state.unreadCount}
            >
              Mark all read
            </GhostButton>
            <GhostButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>
              Refresh
            </GhostButton>
            <DarkButton onClick={() => dispatch({ type: 'OPEN_MODAL' })} icon={<Plus size={12} strokeWidth={2} />}>
              New
            </DarkButton>
          </>
        }
      />

      {/* KPI row */}
      <div style={styles.kpiRow}>
        <KpiCard label="Total"    value={state.items.length.toLocaleString()} delta="All notifications" deltaDirection="flat" />
        <KpiCard label="Unread"   value={state.unreadCount.toLocaleString()} delta={state.unreadCount > 0 ? 'Needs attention' : 'Inbox clear'} deltaDirection={state.unreadCount > 0 ? 'up' : 'flat'} deltaTone={state.unreadCount > 0 ? 'down' : 'up'} tone={state.unreadCount > 0 ? 'warning' : 'default'} />
        <KpiCard label="Read"     value={readCount.toLocaleString()} delta="Reviewed" deltaDirection="flat" />
        <KpiCard label="Actioned" value={actionedCount.toLocaleString()} delta="Resolved" deltaDirection="flat" />
      </div>

      {state.error && <Alert type="error" showIcon message={state.error} style={{ marginBottom: 12, borderRadius: 8 }} closable />}

      {/* Filter rail */}
      <div style={styles.filterRow}>
        <div style={styles.search}>
          <Search size={12} strokeWidth={1.8} color="var(--ci-text-muted)" />
          <input
            style={styles.searchInput}
            placeholder="Search title, message, or reference"
            value={state.search}
            onChange={e => dispatch({ type: 'SET_SEARCH', payload: e.target.value })}
          />
        </div>
      </div>

      <DataCard padding={0}>
        {!userId ? (
          <EmptyState
            title="Not signed in"
            description="Please sign in to view your notifications."
          />
        ) : !state.loading && filtered.length === 0 ? (
          <EmptyState
            title={(state.filterCategory || state.filterStatus || state.search) ? 'No matching notifications' : 'Inbox zero'}
            description={(state.filterCategory || state.filterStatus || state.search) ? 'Try clearing filters to view everything.' : 'You are all caught up — new alerts will appear here.'}
            tone={(state.filterCategory || state.filterStatus || state.search) ? 'neutral' : 'positive'}
            actions={(state.filterCategory || state.filterStatus || state.search) ? (
              <GhostButton onClick={() => {
                dispatch({ type: 'SET_CATEGORY', payload: null });
                dispatch({ type: 'SET_STATUS', payload: null });
                dispatch({ type: 'SET_SEARCH', payload: '' });
              }}>Clear filters</GhostButton>
            ) : undefined}
          />
        ) : (
          <Table
            rowKey="notificationId"
            columns={columns}
            dataSource={filtered}
            loading={state.loading}
            size="small"
            pagination={{ pageSize: 12, size: 'small', hideOnSinglePage: true }}
            rowClassName={rec => rec.status === 'UNREAD' ? 'ci-unread-row' : ''}
            onRow={(rec) => ({
              style: rec.status === 'UNREAD' ? { background: 'var(--ci-primary-soft, #E6F1FB)' } : undefined,
            })}
          />
        )}
      </DataCard>

      <Modal
        title={<span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}><Bell size={14} strokeWidth={1.8} /> New notification</span>}
        open={state.modalOpen}
        onCancel={() => { dispatch({ type: 'CLOSE_MODAL' }); form.resetFields(); }}
        onOk={() => form.submit()}
        confirmLoading={state.submitting}
        okText="Create"
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleCreate} initialValues={{ userId: userId ?? undefined }}>
          <Form.Item name="userId" label="User ID" rules={[{ required: true }]}>
            <Input type="number" min={1} />
          </Form.Item>
          <Form.Item name="title" label="Title" rules={[{ required: true }]}>
            <Input placeholder="Notification title" />
          </Form.Item>
          <Form.Item name="message" label="Message" rules={[{ required: true }]}>
            <TextArea rows={3} placeholder="Notification message…" />
          </Form.Item>
          <Form.Item name="category" label="Category" rules={[{ required: true }]}>
            <Select placeholder="Select category">
              {NOTIFICATION_CATEGORIES.map(c => <Option key={c} value={c}>{c}</Option>)}
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
  kpiRow: {
    display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 16,
  },
  filterRow: { display: 'flex', gap: 8, marginBottom: 12 },
  search: {
    display: 'inline-flex', alignItems: 'center', gap: 6,
    height: 28, padding: '0 10px',
    border: '1px solid var(--ci-border-strong)',
    borderRadius: 'var(--ci-radius-input)',
    background: 'var(--ci-bg-surface)',
  },
  searchInput: {
    border: 'none', outline: 'none', background: 'transparent',
    fontSize: 11, color: 'var(--ci-text-primary)',
    width: 280,
  },
};
