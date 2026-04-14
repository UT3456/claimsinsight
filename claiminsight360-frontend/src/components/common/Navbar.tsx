import { Avatar, Dropdown, Tag, Typography }  from 'antd';
import type { MenuProps }                      from 'antd';
import {
  UserOutlined, LogoutOutlined,
  MenuFoldOutlined, MenuUnfoldOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import { useNavigate }       from 'react-router-dom';
import { useAuth }           from '../../hooks/useAuth';
import NotificationBell      from './NotificationBell';

const { Text } = Typography;

interface NavbarProps {
  collapsed: boolean;
  onToggle:  () => void;
}

export default function Navbar({ collapsed, onToggle }: NavbarProps) {
  const { username, roleLabel, roleColor, logout } = useAuth();
  const navigate = useNavigate();

  const menuItems: MenuProps['items'] = [
    {
      key:   'profile',
      icon:  <UserOutlined />,
      label: 'Profile',
      onClick: () => navigate('/profile'),
    },
    { type: 'divider' },
    {
      key:     'logout',
      icon:    <LogoutOutlined />,
      label:   'Sign Out',
      danger:  true,
      onClick: logout,
    },
  ];

  return (
    <header style={styles.navbar}>
      {/* Left — toggle + brand */}
      <div style={styles.left}>
        <button style={styles.toggleBtn} onClick={onToggle} aria-label="Toggle sidebar">
          {collapsed
            ? <MenuUnfoldOutlined style={{ fontSize: 18, color: '#475569' }} />
            : <MenuFoldOutlined   style={{ fontSize: 18, color: '#475569' }} />}
        </button>

        {collapsed && (
          <div style={styles.brand}>
            <SafetyCertificateOutlined style={{ color: '#2563eb', fontSize: 20 }} />
            <Text strong style={{ color: '#0f172a', fontSize: 16, marginLeft: 8 }}>
              ClaimInsight<span style={{ color: '#2563eb' }}>360</span>
            </Text>
          </div>
        )}
      </div>

      {/* Right — bell + user */}
      <div style={styles.right}>
        <NotificationBell />

        <div style={styles.divider} />

        <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
          <div style={styles.userArea}>
            <Avatar
              style={{ backgroundColor: '#1d4ed8', flexShrink: 0 }}
              icon={<UserOutlined />}
              size={34}
            />
            <div style={styles.userInfo}>
              <Text strong style={{ fontSize: 13, color: '#0f172a', display: 'block', lineHeight: 1.3 }}>
                {username}
              </Text>
              <Tag color={roleColor} style={{ fontSize: 10, padding: '0 6px', lineHeight: '18px', marginTop: 1 }}>
                {roleLabel}
              </Tag>
            </div>
          </div>
        </Dropdown>
      </div>
    </header>
  );
}

const styles: Record<string, React.CSSProperties> = {
  navbar: {
    height: 64, background: '#fff',
    borderBottom: '1px solid #e2e8f0',
    display: 'flex', alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 20px 0 8px',
    position: 'sticky', top: 0, zIndex: 100,
    boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
  },
  left:      { display: 'flex', alignItems: 'center', gap: 12 },
  right:     { display: 'flex', alignItems: 'center', gap: 8 },
  toggleBtn: {
    background: 'none', border: 'none', cursor: 'pointer',
    width: 40, height: 40, borderRadius: 8, display: 'flex',
    alignItems: 'center', justifyContent: 'center',
  },
  brand:    { display: 'flex', alignItems: 'center' },
  divider:  { width: 1, height: 28, background: '#e2e8f0', margin: '0 4px' },
  userArea: {
    display: 'flex', alignItems: 'center', gap: 10,
    cursor: 'pointer', padding: '4px 8px',
    borderRadius: 10, transition: 'background 0.2s',
  },
  userInfo: { display: 'flex', flexDirection: 'column' },
};
