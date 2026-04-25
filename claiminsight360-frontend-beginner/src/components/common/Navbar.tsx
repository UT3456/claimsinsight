import { Button, Dropdown, Avatar } from 'antd';
import type { MenuProps } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import NotificationBell from './NotificationBell';

interface NavbarProps {
  collapsed: boolean;
  onToggle: () => void;
}

export default function Navbar({ collapsed, onToggle }: NavbarProps) {
  const { username, roleLabel, logout } = useAuth();
  const navigate = useNavigate();

  const menuItems: MenuProps['items'] = [
    { key: 'profile', label: 'Profile', onClick: () => navigate('/profile') },
    { type: 'divider' },
    { key: 'logout', label: 'Logout', onClick: logout },
  ];

  // get first letter for avatar
  const firstLetter = username ? username.charAt(0).toUpperCase() : 'U';

  return (
    <div style={{
      height: 50,
      background: '#fff',
      borderBottom: '1px solid #ddd',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 16px',
    }}>
      <div>
        <Button onClick={onToggle} size="small">
          {collapsed ? '>' : '<'}
        </Button>
        <span style={{ marginLeft: 12, fontWeight: 'bold' }}>ClaimInsight</span>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <NotificationBell />

        <Dropdown menu={{ items: menuItems }} trigger={['click']}>
          <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8 }}>
            <Avatar size={28}>{firstLetter}</Avatar>
            <div style={{ fontSize: 12 }}>
              <div>{username}</div>
              <div style={{ color: '#999', fontSize: 11 }}>{roleLabel}</div>
            </div>
          </div>
        </Dropdown>
      </div>
    </div>
  );
}
