import { Badge, Tooltip }                from 'antd';
import { BellOutlined }                   from '@ant-design/icons';
import { useNavigate }                    from 'react-router-dom';
import { useAuth }                        from '../../hooks/useAuth';
import { useNotificationPolling }         from '../../hooks/useNotificationPolling';

export default function NotificationBell() {
  const navigate           = useNavigate();
  const { userId }         = useAuth();
  const { unreadCount }    = useNotificationPolling(userId);

  return (
    <Tooltip title="Notifications">
      <Badge
        count={unreadCount}
        overflowCount={99}
        offset={[-2, 2]}
        style={{ backgroundColor: '#ef4444' }}
      >
        <div
          onClick={() => navigate('/notifications')}
          style={styles.bell}
          aria-label="Open notifications"
        >
          <BellOutlined style={{ fontSize: 20, color: '#475569' }} />
        </div>
      </Badge>
    </Tooltip>
  );
}

const styles: Record<string, React.CSSProperties> = {
  bell: {
    width: 40, height: 40,
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    borderRadius: '10px', cursor: 'pointer',
    transition: 'background 0.2s',
  },
};
