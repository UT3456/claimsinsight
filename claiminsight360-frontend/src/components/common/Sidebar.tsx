import { Menu, Typography }              from 'antd';
import type { MenuProps }                from 'antd';
import { useNavigate, useLocation }      from 'react-router-dom';
import { useAuth }                       from '../../hooks/useAuth';
import {
  DashboardOutlined, BarChartOutlined, DatabaseOutlined,
  TeamOutlined, AlertOutlined, DollarOutlined,
  ClockCircleOutlined, StopOutlined, SafetyOutlined,
  FileTextOutlined, BellOutlined, AuditOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';

const { Text } = Typography;

interface SidebarProps { collapsed: boolean }
type MenuItem = Required<MenuProps>['items'][number];

// Helper — returns null if role can't access; used to filter items
function item(
  key: string,
  label: string,
  icon: React.ReactNode,
  allowed: boolean,
  children?: MenuItem[],
): MenuItem | null {
  if (!allowed) return null;
  return children
    ? { key, icon, label, children }
    : { key, icon, label };
}

export default function Sidebar({ collapsed }: SidebarProps) {
  const navigate  = useNavigate();
  const location  = useLocation();
  const { hasAccess, isAdmin } = useAuth();

  // Build menu — null entries are filtered out below
  const raw: (MenuItem | null)[] = [
    item('/dashboard', 'Overview', <DashboardOutlined />, hasAccess('/dashboard')),

    { type: 'divider' } as MenuItem,

    // Claims Analytics group
    (() => {
      const children = [
        item('/claims',         'KPI Dashboard', null, hasAccess('/claims')),
        item('/ingestion/feeds','Data Feeds',    null, hasAccess('/ingestion/feeds')),
        item('/ingestion/raw',  'Raw Claims',    null, hasAccess('/ingestion/raw')),
      ].filter(Boolean) as MenuItem[];
      return children.length
        ? { key: 'claims-group', icon: <BarChartOutlined />, label: 'Claims Analytics', children }
        : null;
    })(),

    // Adjuster Ops group
    (() => {
      const children = [
        item('/adjusters',      'Performance',    null, hasAccess('/adjusters')),
        item('/sla-violations', 'SLA Violations', null, hasAccess('/sla-violations')),
      ].filter(Boolean) as MenuItem[];
      return children.length
        ? { key: 'ops-group', icon: <TeamOutlined />, label: 'Adjuster Ops', children }
        : null;
    })(),

    // Financial group
    (() => {
      const children = [
        item('/costs',    'Cost Analysis', null, hasAccess('/costs')),
        item('/reserves', 'Reserves',      null, hasAccess('/reserves')),
        item('/aging',    'Aging',         null, hasAccess('/aging')),
      ].filter(Boolean) as MenuItem[];
      return children.length
        ? { key: 'financial-group', icon: <DollarOutlined />, label: 'Financial', children }
        : null;
    })(),

    // Risk Intelligence group
    (() => {
      const children = [
        item('/fraud-risk',     'Fraud Risk',       null, hasAccess('/fraud-risk')),
        item('/denial-leakage', 'Denial & Leakage', null, hasAccess('/denial-leakage')),
      ].filter(Boolean) as MenuItem[];
      return children.length
        ? { key: 'risk-group', icon: <SafetyOutlined />, label: 'Risk Intelligence', children }
        : null;
    })(),

    { type: 'divider' } as MenuItem,

    item('/reports',       'Reports',       <FileTextOutlined />, hasAccess('/reports')),
    item('/notifications', 'Notifications', <BellOutlined />,     hasAccess('/notifications')),

    // Admin only
    ...(isAdmin ? [
      { type: 'divider' } as MenuItem,
      item('/admin/audit-logs', 'Audit Logs', <AuditOutlined />, true),
    ] : []),
  ];

  const items = raw.filter(Boolean) as MenuItem[];

  const openKeys = ['claims-group', 'ops-group', 'financial-group', 'risk-group'].filter((g) => {
    const map: Record<string, string[]> = {
      'claims-group':    ['/claims', '/ingestion/feeds', '/ingestion/raw'],
      'ops-group':       ['/adjusters', '/sla-violations'],
      'financial-group': ['/costs', '/reserves', '/aging'],
      'risk-group':      ['/fraud-risk', '/denial-leakage'],
    };
    return map[g]?.some((r) => location.pathname.startsWith(r));
  });

  return (
    <aside style={{ ...styles.sidebar, width: collapsed ? 80 : 240 }}>
      {/* Logo */}
      {!collapsed ? (
        <div style={styles.logo}>
          <div style={styles.logoIcon}>
            <SafetyCertificateOutlined style={{ fontSize: 22, color: '#fff' }} />
          </div>
          <div>
            <Text strong style={{ fontSize: 15, color: '#0f172a', lineHeight: 1.2, display: 'block' }}>
              ClaimInsight<span style={{ color: '#2563eb' }}>360</span>
            </Text>
            <Text style={{ fontSize: 9, color: '#94a3b8', letterSpacing: '0.06em' }}>
              ANALYTICS PLATFORM
            </Text>
          </div>
        </div>
      ) : (
        <div style={styles.logoCollapsed}>
          <SafetyCertificateOutlined style={{ fontSize: 24, color: '#2563eb' }} />
        </div>
      )}

      {/* Nav */}
      <Menu
        mode="inline"
        inlineCollapsed={collapsed}
        selectedKeys={[location.pathname]}
        defaultOpenKeys={openKeys}
        items={items}
        onClick={({ key }) => navigate(key)}
        style={styles.menu}
      />

      {!collapsed && (
        <div style={styles.footer}>
          <AlertOutlined style={{ color: '#94a3b8', fontSize: 12, marginRight: 6 }} />
          <Text style={{ fontSize: 11, color: '#94a3b8' }}>All activity is monitored</Text>
        </div>
      )}
    </aside>
  );
}

const styles: Record<string, React.CSSProperties> = {
  sidebar: {
    height: '100vh', background: '#fff',
    borderRight: '1px solid #e2e8f0',
    display: 'flex', flexDirection: 'column',
    position: 'sticky', top: 0,
    transition: 'width 0.2s',
    overflow: 'hidden', flexShrink: 0,
  },
  logo: {
    display: 'flex', alignItems: 'center', gap: 12,
    padding: '18px 20px 14px',
    borderBottom: '1px solid #f1f5f9',
  },
  logoIcon: {
    width: 38, height: 38, borderRadius: 10,
    background: 'linear-gradient(135deg, #1d4ed8, #2563eb)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    flexShrink: 0, boxShadow: '0 2px 8px rgba(37,99,235,0.3)',
  },
  logoCollapsed: {
    display: 'flex', justifyContent: 'center',
    padding: '20px 0 14px',
    borderBottom: '1px solid #f1f5f9',
  },
  menu: { flex: 1, border: 'none', overflowY: 'auto', overflowX: 'hidden', paddingTop: 8 },
  footer: {
    padding: '12px 20px', borderTop: '1px solid #f1f5f9',
    display: 'flex', alignItems: 'center',
  },
};
