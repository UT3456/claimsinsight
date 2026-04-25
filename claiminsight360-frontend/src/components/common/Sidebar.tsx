import { Typography }                     from 'antd';
import { useNavigate, useLocation }       from 'react-router-dom';
import { SafetyCertificateOutlined }      from '@ant-design/icons';
import { useAuth }                        from '../../hooks/useAuth';
import { SIDEBAR_MENUS, ROLE_WORKSPACE_LABEL } from '../../utils/sidebarMenus';
import type { AppRole }                   from '../../utils/roles';

const { Text } = Typography;

interface SidebarProps { collapsed: boolean }

export default function Sidebar({ collapsed }: SidebarProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { role, username, roleLabel, hasAccess } = useAuth();

  const sections = role ? (SIDEBAR_MENUS[role as AppRole] ?? []) : [];
  const workspace = role ? ROLE_WORKSPACE_LABEL[role as AppRole] : '';

  const isActive = (route: string) => {
    if (route === '/dashboard') return location.pathname === '/dashboard';
    return location.pathname === route || location.pathname.startsWith(route + '/');
  };

  return (
    <aside style={{
      width: collapsed ? 72 : 220,
      height: '100vh',
      background: 'var(--ci-bg-sidebar)',
      borderRight: '1px solid var(--ci-border)',
      display: 'flex', flexDirection: 'column',
      position: 'sticky', top: 0,
      transition: 'width 0.2s',
      overflow: 'hidden', flexShrink: 0,
    }}>
      {/* Brand */}
      <div style={{
        padding: collapsed ? '16px 0' : '14px 20px',
        borderBottom: '1px solid var(--ci-border)',
        display: 'flex', flexDirection: 'column',
        alignItems: collapsed ? 'center' : 'flex-start',
        gap: 2,
      }}>
        {collapsed ? (
          <SafetyCertificateOutlined style={{ fontSize: 20, color: '#2563eb' }} />
        ) : (
          <>
            <Text strong style={{ fontSize: 14, color: 'var(--ci-text-primary)', letterSpacing: '-0.01em' }}>
              ClaimInsight<span style={{ color: '#2563eb' }}>360</span>
            </Text>
            {workspace && (
              <Text style={{ fontSize: 10, color: 'var(--ci-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                {workspace}
              </Text>
            )}
          </>
        )}
      </div>

      {/* Nav sections */}
      <nav style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', padding: collapsed ? '8px 4px' : '10px 12px' }}>
        {sections.map(section => {
          const visibleItems = section.items.filter(it => hasAccess(it.route));
          if (!visibleItems.length) return null;
          return (
            <div key={section.title} style={{ marginBottom: 14 }}>
              {!collapsed && (
                <div style={{
                  fontSize: 10, fontWeight: 600, color: 'var(--ci-text-muted)',
                  letterSpacing: '0.08em', padding: '4px 8px 6px',
                }}>
                  {section.title}
                </div>
              )}
              {visibleItems.map((it, idx) => {
                const active = isActive(it.route);
                return (
                  <div
                    key={`${section.title}-${idx}-${it.label}`}
                    onClick={() => navigate(it.route)}
                    style={{
                      padding: collapsed ? '6px 4px' : '6px 10px',
                      margin: '1px 0',
                      borderRadius: 3,
                      cursor: 'pointer',
                      fontSize: 12,
                      color: active ? '#2563eb' : 'var(--ci-text-primary)',
                      background: active ? '#eff6ff' : 'transparent',
                      fontWeight: active ? 600 : 400,
                      textAlign: collapsed ? 'center' : 'left',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                    }}
                    onMouseEnter={e => { if (!active) (e.currentTarget as HTMLDivElement).style.background = '#f5f5f5'; }}
                    onMouseLeave={e => { if (!active) (e.currentTarget as HTMLDivElement).style.background = 'transparent'; }}
                  >
                    {collapsed ? it.label.charAt(0) : it.label}
                  </div>
                );
              })}
            </div>
          );
        })}
      </nav>

      {/* User tag */}
      {!collapsed && username && (
        <div style={{
          borderTop: '1px solid var(--ci-border)',
          padding: '10px 14px',
          display: 'flex', flexDirection: 'column', gap: 2,
        }}>
          <Text strong style={{ fontSize: 12, color: 'var(--ci-text-primary)' }}>
            {username}
          </Text>
          <Text style={{ fontSize: 10, color: 'var(--ci-text-muted)' }}>
            {roleLabel}
          </Text>
        </div>
      )}
    </aside>
  );
}
