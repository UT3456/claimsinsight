import { Dropdown }  from 'antd';
import type { MenuProps } from 'antd';
import { useNavigate }    from 'react-router-dom';
import { Menu, PanelLeftClose, PanelLeftOpen, Shield, User, LogOut, ChevronDown } from 'lucide-react';
import { useAuth }        from '../../hooks/useAuth';
import NotificationBell   from './NotificationBell';
import ThemeToggle        from './ThemeToggle';
import { TintedAvatar }   from '../ui';

interface NavbarProps {
  collapsed: boolean;
  onToggle:  () => void;
}

/**
 * Navbar — flat top bar. 48px tall, 1px bottom border, no shadow.
 * Brand only shown when sidebar is collapsed (otherwise brand lives in sidebar).
 */
export default function Navbar({ collapsed, onToggle }: NavbarProps) {
  const { username, roleLabel, logout } = useAuth();
  const navigate = useNavigate();

  const menuItems: MenuProps['items'] = [
    {
      key:   'profile',
      icon:  <User size={14} strokeWidth={1.8} />,
      label: 'Profile',
      onClick: () => navigate('/profile'),
    },
    { type: 'divider' },
    {
      key:     'logout',
      icon:    <LogOut size={14} strokeWidth={1.8} />,
      label:   'Sign out',
      danger:  true,
      onClick: logout,
    },
  ];

  const ToggleIcon = collapsed ? PanelLeftOpen : PanelLeftClose;

  return (
    <header style={styles.navbar}>
      {/* Left */}
      <div style={styles.left}>
        <button
          style={styles.toggleBtn}
          onClick={onToggle}
          aria-label="Toggle sidebar"
          onMouseOver={(e) => (e.currentTarget.style.background = 'var(--ci-bg-surface-2)')}
          onMouseOut={(e)  => (e.currentTarget.style.background = 'transparent')}
        >
          <ToggleIcon size={16} strokeWidth={1.8} color="var(--ci-text-secondary)" />
        </button>

        {collapsed && (
          <div style={styles.brand}>
            <div style={styles.brandSquare}>
              <Shield size={12} strokeWidth={2} color="#ffffff" />
            </div>
            <span style={styles.brandText}>ClaimInsight</span>
          </div>
        )}
      </div>

      {/* Right */}
      <div style={styles.right}>
        <ThemeToggle />
        <NotificationBell />

        <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
          <div
            style={styles.userArea}
            onMouseOver={(e) => (e.currentTarget.style.background = 'var(--ci-bg-surface-2)')}
            onMouseOut={(e)  => (e.currentTarget.style.background = 'transparent')}
          >
            <TintedAvatar name={username || 'User'} colorKey={username || undefined} size={26} />
            <div style={styles.userInfo}>
              <span style={styles.userName}>{username}</span>
              <span style={styles.userRole}>{roleLabel}</span>
            </div>
            <ChevronDown size={14} strokeWidth={1.6} color="var(--ci-text-muted)" />
          </div>
        </Dropdown>
      </div>
    </header>
  );
}

const styles: Record<string, React.CSSProperties> = {
  navbar: {
    height: 48,
    background: 'var(--ci-bg-surface)',
    borderBottom: '1px solid var(--ci-border)',
    display: 'flex', alignItems: 'center',
    justifyContent: 'space-between',
    padding: '0 16px 0 10px',
    position: 'sticky', top: 0, zIndex: 100,
  },
  left:  { display: 'flex', alignItems: 'center', gap: 10 },
  right: { display: 'flex', alignItems: 'center', gap: 6 },
  toggleBtn: {
    background: 'transparent', border: 'none', cursor: 'pointer',
    width: 30, height: 30, borderRadius: 6,
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    transition: 'background 0.12s',
  },
  brand: { display: 'flex', alignItems: 'center', gap: 8 },
  brandSquare: {
    width: 20, height: 20, borderRadius: 5,
    background: 'var(--ci-primary, #185FA5)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
  },
  brandText: {
    fontSize: 13, fontWeight: 500,
    color: 'var(--ci-text-primary)',
    letterSpacing: '-0.01em',
  },
  userArea: {
    display: 'flex', alignItems: 'center', gap: 8,
    cursor: 'pointer', padding: '3px 8px 3px 4px',
    borderRadius: 6, transition: 'background 0.12s',
  },
  userInfo: { display: 'flex', flexDirection: 'column', lineHeight: 1.2 },
  userName: {
    fontSize: 12, fontWeight: 500,
    color: 'var(--ci-text-primary)',
  },
  userRole: {
    fontSize: 10, fontWeight: 400,
    color: 'var(--ci-text-muted)',
  },
};
