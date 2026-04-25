import { Menu } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

interface SidebarProps {
  collapsed: boolean;
}

export default function Sidebar({ collapsed }: SidebarProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { hasAccess, isAdmin } = useAuth();

  // build menu items - hide if user doesn't have access
  const items: any[] = [];

  if (hasAccess('/dashboard')) items.push({ key: '/dashboard', label: 'Dashboard' });
  if (hasAccess('/claims')) items.push({ key: '/claims', label: 'Claims' });
  if (hasAccess('/fraud-risk')) items.push({ key: '/fraud-risk', label: 'Fraud & Risk' });
  if (hasAccess('/denial-leakage')) items.push({ key: '/denial-leakage', label: 'Denials' });
  if (hasAccess('/adjusters')) items.push({ key: '/adjusters', label: 'Adjusters' });
  if (hasAccess('/sla-violations')) items.push({ key: '/sla-violations', label: 'SLA Violations' });
  if (hasAccess('/reserves')) items.push({ key: '/reserves', label: 'Reserves' });
  if (hasAccess('/costs')) items.push({ key: '/costs', label: 'Costs' });
  if (hasAccess('/aging')) items.push({ key: '/aging', label: 'Aging' });
  if (hasAccess('/reports')) items.push({ key: '/reports', label: 'Reports' });
  if (hasAccess('/notifications')) items.push({ key: '/notifications', label: 'Notifications' });

  if (isAdmin) {
    items.push({ key: '/ingestion/feeds', label: 'Data Feeds' });
    items.push({ key: '/ingestion/raw', label: 'Raw Claims' });
    items.push({ key: '/admin/kpi-definitions', label: 'KPI Config' });
    items.push({ key: '/admin/users', label: 'Users' });
    items.push({ key: '/admin/audit-logs', label: 'Audit Logs' });
  }

  // find currently selected key
  let selectedKey = '/dashboard';
  for (const item of items) {
    if (location.pathname === item.key || location.pathname.startsWith(item.key + '/')) {
      selectedKey = item.key;
    }
  }

  return (
    <div style={{
      width: collapsed ? 60 : 200,
      background: '#fff',
      borderRight: '1px solid #ddd',
      height: '100vh',
      overflowY: 'auto',
    }}>
      <Menu
        mode="inline"
        inlineCollapsed={collapsed}
        selectedKeys={[selectedKey]}
        items={items}
        onClick={(e) => navigate(e.key)}
        style={{ borderRight: 0 }}
      />
    </div>
  );
}
