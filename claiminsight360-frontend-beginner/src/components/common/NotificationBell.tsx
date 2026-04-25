import { Badge } from 'antd';
import { BellOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useNotificationPolling } from '../../hooks/useNotificationPolling';

// simple notification bell - clicking it takes you to notifications page
export default function NotificationBell() {
  const navigate = useNavigate();
  const { userId } = useAuth();
  const { unreadCount } = useNotificationPolling(userId);

  return (
    <div
      style={{ cursor: 'pointer', padding: 4 }}
      onClick={() => navigate('/notifications')}
    >
      <Badge count={unreadCount}>
        <BellOutlined style={{ fontSize: 20 }} />
      </Badge>
    </div>
  );
}
