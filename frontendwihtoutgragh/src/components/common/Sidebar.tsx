import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth }                   from '../../hooks/useAuth';
import { hasAdminSectionAccess }     from '../../utils/roles';
import { Shield } from 'lucide-react';

/**
 * Sidebar — flat, dot-prefixed nav per DESIGN_SPEC.md.
 * Warm tint bg (#F8F7F3), uppercase section headers, 7/10 padding, 12px text.
 * Active row: #E6F1FB bg + #0C447C text. Dot color encodes section tone.
 */
interface SidebarProps { collapsed: boolean }

interface NavItem {
  to: string;
  label: string;
  /** Extra paths that should also highlight this row. */
  match?: string[];
  allowed: boolean;
  /** Tailwind-esque tone keys for the leading dot. */
  tone?: 'blue' | 'teal' | 'amber' | 'purple' | 'coral' | 'red' | 'neutral';
}

interface NavSection {
  title: string;
  items: NavItem[];
}

export default function Sidebar({ collapsed }: SidebarProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { hasAccess, role } = useAuth();
  // Show "Admin" section if the user has access to any admin route — not just
  // ROLE_ADMIN. Managers/Actuaries/Fraud analysts now see read-only tabs.
  const showAdminSection = hasAdminSectionAccess(role);

  const sections: NavSection[] = [
    {
      title: 'Analytics',
      items: [
        { to: '/dashboard',      label: 'Overview',         allowed: hasAccess('/dashboard'),      tone: 'blue' },
        { to: '/claims',         label: 'Claims',           match: ['/claims'], allowed: hasAccess('/claims'), tone: 'blue' },
        { to: '/fraud-risk',     label: 'Fraud & risk',     allowed: hasAccess('/fraud-risk'),     tone: 'red' },
        { to: '/denial-leakage', label: 'Denials',          allowed: hasAccess('/denial-leakage'), tone: 'amber' },
      ],
    },
    {
      title: 'Operations',
      items: [
        { to: '/adjusters',      label: 'Adjusters',        match: ['/adjusters'], allowed: hasAccess('/adjusters'), tone: 'purple' },
        { to: '/sla-violations', label: 'SLA violations',   allowed: hasAccess('/sla-violations'), tone: 'amber' },
      ],
    },
    {
      title: 'Financial',
      items: [
        { to: '/reserves', label: 'Reserves', allowed: hasAccess('/reserves'), tone: 'teal' },
        { to: '/costs',    label: 'Costs',    allowed: hasAccess('/costs'),    tone: 'teal' },
        { to: '/aging',    label: 'Aging',    allowed: hasAccess('/aging'),    tone: 'coral' },
      ],
    },
    {
      title: 'Reporting',
      items: [
        { to: '/reports',       label: 'Reports',       allowed: hasAccess('/reports'),       tone: 'neutral' },
        { to: '/notifications', label: 'Notifications', allowed: hasAccess('/notifications'), tone: 'neutral' },
      ],
    },
    ...(showAdminSection ? [{
      title: 'Admin',
      items: [
        { to: '/ingestion/feeds',       label: 'Data feeds',  allowed: hasAccess('/ingestion/feeds'),       tone: 'neutral' as const },
        { to: '/ingestion/raw',         label: 'Raw claims',  allowed: hasAccess('/ingestion/raw'),         tone: 'neutral' as const },
        { to: '/admin/kpi-definitions', label: 'KPI config',  allowed: hasAccess('/admin/kpi-definitions'), tone: 'neutral' as const },
        { to: '/admin/users',           label: 'Users',       allowed: hasAccess('/admin/users'),           tone: 'neutral' as const },
        { to: '/admin/audit-logs',      label: 'Audit log',   allowed: hasAccess('/admin/audit-logs'),      tone: 'neutral' as const },
      ],
    }] : []),
  ];

  const isActive = (item: NavItem) => {
    const paths = [item.to, ...(item.match ?? [])];
    return paths.some((p) =>
      p === '/dashboard'
        ? location.pathname === p
        : location.pathname === p || location.pathname.startsWith(p + '/'),
    );
  };

  return (
    <aside style={{ ...styles.sidebar, width: collapsed ? 64 : 224 }}>
      {/* Brand */}
      <div style={{ ...styles.brand, justifyContent: collapsed ? 'center' : 'flex-start', padding: collapsed ? '18px 0' : '18px 16px' }}>
        <div style={styles.brandSquare}>
          <Shield size={14} strokeWidth={2} color="#ffffff" />
        </div>
        {!collapsed && (
          <div style={styles.brandText}>ClaimInsight</div>
        )}
      </div>

      {/* Nav */}
      <nav style={styles.nav}>
        {sections.map((section) => {
          const visible = section.items.filter((i) => i.allowed);
          if (visible.length === 0) return null;
          return (
            <div key={section.title} style={styles.section}>
              {!collapsed && (
                <div style={styles.sectionHeader}>{section.title}</div>
              )}
              {visible.map((item) => {
                const active = isActive(item);
                return (
                  <button
                    key={item.to}
                    onClick={() => navigate(item.to)}
                    title={collapsed ? item.label : undefined}
                    style={{
                      ...styles.navItem,
                      justifyContent: collapsed ? 'center' : 'flex-start',
                      padding: collapsed ? '7px 0' : '7px 10px',
                      background: active ? 'var(--ci-primary-soft, #E6F1FB)' : 'transparent',
                      color: active ? 'var(--ci-primary-hover, #0C447C)' : 'var(--ci-text-secondary)',
                      fontWeight: active ? 500 : 400,
                    }}
                  >
                    <span style={{
                      ...styles.dot,
                      background: active
                        ? 'var(--ci-primary, #185FA5)'
                        : DOT_COLOR.neutral,
                    }} />
                    {!collapsed && <span style={{ lineHeight: 1.3 }}>{item.label}</span>}
                  </button>
                );
              })}
            </div>
          );
        })}
      </nav>

      {/* Footer — monitored notice */}
      {!collapsed && (
        <div style={styles.footer}>All activity is monitored</div>
      )}
    </aside>
  );
}

// Per design spec rule 4: "Color for meaning, not decoration."
// Nav dots are neutral; the active state is indicated by the primary blue.
const DOT_COLOR = {
  neutral: '#B4B2A9',
} as const;

const styles: Record<string, React.CSSProperties> = {
  sidebar: {
    height: '100vh',
    background: 'var(--ci-bg-sidebar, #F8F7F3)',
    borderRight: '1px solid var(--ci-border)',
    display: 'flex', flexDirection: 'column',
    position: 'sticky', top: 0,
    transition: 'width 0.2s',
    overflow: 'hidden', flexShrink: 0,
  },
  brand: {
    display: 'flex', alignItems: 'center', gap: 10,
    borderBottom: '1px solid var(--ci-border)',
  },
  brandSquare: {
    width: 22, height: 22, borderRadius: 6,
    background: 'var(--ci-primary, #185FA5)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    flexShrink: 0,
  },
  brandText: {
    fontSize: 13, fontWeight: 500,
    color: 'var(--ci-text-primary)',
    letterSpacing: '-0.01em',
  },
  nav: { flex: 1, overflowY: 'auto', overflowX: 'hidden', padding: '8px 8px 16px' },
  section: { marginTop: 14 },
  sectionHeader: {
    fontSize: 10, fontWeight: 500,
    letterSpacing: '0.08em',
    color: 'var(--ci-text-muted)',
    textTransform: 'uppercase',
    padding: '6px 10px 4px',
  },
  navItem: {
    width: '100%',
    display: 'flex', alignItems: 'center', gap: 10,
    border: 'none', background: 'transparent',
    borderRadius: 'var(--ci-radius-btn, 6px)',
    fontSize: 12,
    cursor: 'pointer',
    textAlign: 'left',
    marginTop: 1,
    transition: 'background 0.12s',
  },
  dot: {
    width: 7, height: 7, borderRadius: '50%',
    flexShrink: 0,
  },
  footer: {
    padding: '10px 16px',
    borderTop: '1px solid var(--ci-border)',
    fontSize: 10, color: 'var(--ci-text-muted)',
    letterSpacing: '0.02em',
  },
};
