import { useEffect, useState, useCallback } from 'react';
import { Badge, Popover, List, Tag, Button, Empty, Spin, Typography, Tooltip } from 'antd';
import { FiBell, FiCheck, FiCheckCircle, FiInbox } from 'react-icons/fi';
import { useNavigate }                    from 'react-router-dom';
import { useAuth }                        from '../../hooks/useAuth';
import { useNotificationPolling }         from '../../hooks/useNotificationPolling';
import { notificationsApi }               from '../../api/notificationsApi';
import type { Notification }              from '../../api/notificationsApi';

const { Text } = Typography;

const CATEGORY_COLORS: Record<string, string> = {
  RISK:        'red',
  DENIAL:      'orange',
  COST:        'gold',
  PERFORMANCE: 'blue',
  AGING:       'purple',
  SYSTEM:      'default',
};

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1)    return 'just now';
  if (mins < 60)   return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24)    return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  if (days < 7)    return `${days}d ago`;
  return new Date(iso).toLocaleDateString();
}

export default function NotificationBell() {
  const navigate        = useNavigate();
  const { userId }      = useAuth();
  const { unreadCount } = useNotificationPolling(userId);

  const [open, setOpen]       = useState(false);
  const [recent, setRecent]   = useState<Notification[]>([]);
  const [loading, setLoading] = useState(false);

  const loadRecent = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    try {
      const all = await notificationsApi.getForUser(userId);
      setRecent(all.slice(0, 6));
    } catch {
      setRecent([]);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    if (open) loadRecent();
  }, [open, loadRecent]);

  // Refresh preview when unread count changes (new notification arrived)
  useEffect(() => {
    if (open) loadRecent();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [unreadCount]);

  const markRead = async (id: number) => {
    try {
      await notificationsApi.updateStatus(id, 'READ');
      setRecent(prev => prev.map(n => n.notificationId === id ? { ...n, status: 'READ' } : n));
    } catch { /* ignore */ }
  };

  const markAllRead = async () => {
    if (!userId) return;
    try {
      await notificationsApi.markAllRead(userId);
      setRecent(prev => prev.map(n => ({ ...n, status: n.status === 'UNREAD' ? 'READ' : n.status })));
    } catch { /* ignore */ }
  };

  const content = (
    <div style={styles.popover}>
      <div style={styles.header}>
        <Text strong style={{ fontSize: 14, color: 'var(--ci-text-primary)' }}>
          Notifications
        </Text>
        {unreadCount > 0 && (
          <Button
            type="link"
            size="small"
            icon={<FiCheckCircle />}
            onClick={markAllRead}
            style={{ padding: 0, fontSize: 12, display: 'inline-flex', alignItems: 'center', gap: 4 }}
          >
            Mark all read
          </Button>
        )}
      </div>

      <div style={styles.list}>
        {loading ? (
          <div style={{ padding: 24, textAlign: 'center' }}><Spin size="small" /></div>
        ) : recent.length === 0 ? (
          <Empty
            image={<FiInbox style={{ fontSize: 36, color: 'var(--ci-text-muted, #94a3b8)' }} />}
            description={<Text style={{ color: 'var(--ci-text-secondary)', fontSize: 12 }}>No notifications</Text>}
            style={{ padding: 24 }}
          />
        ) : (
          <List
            size="small"
            dataSource={recent}
            renderItem={(n) => {
              const unread = n.status === 'UNREAD';
              return (
                <List.Item
                  style={{
                    ...styles.item,
                    background: unread ? 'var(--ci-bg-hover, rgba(37,99,235,0.06))' : 'transparent',
                    cursor: 'pointer',
                  }}
                  onClick={() => {
                    if (unread) markRead(n.notificationId);
                    setOpen(false);
                    navigate('/notifications');
                  }}
                  actions={unread ? [
                    <Tooltip title="Mark as read" key="mark">
                      <Button
                        type="text"
                        size="small"
                        icon={<FiCheck />}
                        onClick={(e) => { e.stopPropagation(); markRead(n.notificationId); }}
                      />
                    </Tooltip>,
                  ] : []}
                >
                  <List.Item.Meta
                    avatar={
                      <div style={{
                        ...styles.dot,
                        background: unread ? '#2563eb' : 'transparent',
                        border: unread ? 'none' : '1px solid var(--ci-border)',
                      }} />
                    }
                    title={
                      <div style={styles.titleRow}>
                        <Text
                          strong={unread}
                          style={{ fontSize: 13, color: 'var(--ci-text-primary)', flex: 1 }}
                          ellipsis
                        >
                          {n.title}
                        </Text>
                        <Tag color={CATEGORY_COLORS[n.category] ?? 'default'} style={{ marginRight: 0, fontSize: 10 }}>
                          {n.category}
                        </Tag>
                      </div>
                    }
                    description={
                      <div>
                        <Text style={{ fontSize: 12, color: 'var(--ci-text-secondary)', display: 'block' }} ellipsis>
                          {n.message}
                        </Text>
                        <Text style={{ fontSize: 10, color: 'var(--ci-text-muted, #94a3b8)' }}>
                          {timeAgo(n.createdDate)}
                        </Text>
                      </div>
                    }
                  />
                </List.Item>
              );
            }}
          />
        )}
      </div>

      <div style={styles.footer}>
        <Button
          type="link"
          size="small"
          block
          onClick={() => { setOpen(false); navigate('/notifications'); }}
        >
          View all notifications
        </Button>
      </div>
    </div>
  );

  return (
    <Popover
      content={content}
      trigger="click"
      open={open}
      onOpenChange={setOpen}
      placement="bottomRight"
      arrow={false}
      overlayInnerStyle={{ padding: 0, borderRadius: 12 }}
    >
      <Tooltip title={open ? '' : 'Notifications'} placement="bottom">
        <Badge
          count={unreadCount}
          overflowCount={99}
          offset={[-2, 2]}
          style={{ backgroundColor: '#F45B69' }}
        >
          <div style={styles.bell} aria-label="Open notifications">
            <FiBell style={{ fontSize: 20, color: 'var(--ci-text-secondary)' }} />
          </div>
        </Badge>
      </Tooltip>
    </Popover>
  );
}

const styles: Record<string, React.CSSProperties> = {
  bell: {
    width: 40, height: 40,
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    borderRadius: 10, cursor: 'pointer',
    transition: 'background 0.2s',
  },
  popover: {
    width: 380,
    background: 'var(--ci-bg-surface)',
    borderRadius: 12,
    overflow: 'hidden',
  },
  header: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '12px 16px',
    borderBottom: '1px solid var(--ci-border)',
  },
  list: {
    maxHeight: 420,
    overflowY: 'auto',
  },
  item: {
    padding: '10px 16px',
    borderBottom: '1px solid var(--ci-border)',
    transition: 'background 0.15s',
  },
  titleRow: {
    display: 'flex', alignItems: 'center', gap: 8,
    marginBottom: 2,
  },
  dot: {
    width: 8, height: 8, borderRadius: '50%',
    marginTop: 6, marginLeft: 4,
  },
  footer: {
    borderTop: '1px solid var(--ci-border)',
    padding: 4,
    background: 'var(--ci-bg-subtle, transparent)',
  },
};
