import { ROLES, AppRole } from './roles';

export interface SidebarItem {
  label: string;
  /** Route the item navigates to. */
  route: string;
}

export interface SidebarSection {
  /** Section header, e.g. WORK / ANALYSIS */
  title: string;
  items: SidebarItem[];
}

/**
 * Reference-aligned menu per role.
 * Sections and items follow claiminsight360-simple.html exactly.
 * Multiple labels may map to the same route — that's intentional (the
 * reference shows menu entries like "My queue" + "All claims" that both
 * point at the Claims screen).
 */
export const SIDEBAR_MENUS: Record<AppRole, SidebarSection[]> = {
  [ROLES.CLAIMS_ANALYST]: [
    {
      title: 'WORK',
      items: [
        { label: 'My dashboard', route: '/dashboard' },
        { label: 'My queue',     route: '/claims' },
        { label: 'All claims',   route: '/claims' },
      ],
    },
    {
      title: 'ANALYSIS',
      items: [
        { label: 'Denials',       route: '/denial-leakage' },
        { label: 'Cost drivers',  route: '/costs' },
        { label: 'Notifications', route: '/notifications' },
      ],
    },
  ],

  [ROLES.CLAIMS_MANAGER]: [
    {
      title: 'OVERVIEW',
      items: [
        { label: 'Team dashboard', route: '/dashboard' },
        { label: 'All claims',     route: '/claims' },
      ],
    },
    {
      title: 'TEAM',
      items: [
        { label: 'Adjusters',    route: '/adjusters' },
        { label: 'Workload',     route: '/adjusters' },
        { label: 'SLA tracking', route: '/sla-violations' },
      ],
    },
    {
      title: 'ANALYSIS',
      items: [
        { label: 'Denials',       route: '/denial-leakage' },
        { label: 'Reports',       route: '/reports' },
        { label: 'Notifications', route: '/notifications' },
      ],
    },
  ],

  [ROLES.FRAUD_ANALYST]: [
    {
      title: 'RISK',
      items: [
        { label: 'Risk dashboard', route: '/dashboard' },
        { label: 'Review queue',   route: '/fraud-risk' },
        { label: 'Flagged claims', route: '/fraud-risk' },
      ],
    },
    {
      title: 'INVESTIGATE',
      items: [
        { label: 'Patterns',      route: '/denial-leakage' },
        { label: 'Indicators',    route: '/fraud-risk' },
        { label: 'Escalated',     route: '/fraud-risk' },
        { label: 'Notifications', route: '/notifications' },
      ],
    },
  ],

  [ROLES.ACTUARY]: [
    {
      title: 'PORTFOLIO',
      items: [
        { label: 'Analytics',   route: '/dashboard' },
        { label: 'Loss ratios', route: '/costs' },
        { label: 'Severity',    route: '/reserves' },
        { label: 'Frequency',   route: '/reserves' },
      ],
    },
    {
      title: 'RESERVES',
      items: [
        { label: 'Reserve trends', route: '/reserves' },
        { label: 'Aging',          route: '/aging' },
        { label: 'Reports',        route: '/reports' },
        { label: 'Notifications',  route: '/notifications' },
      ],
    },
  ],

  [ROLES.OPERATIONS_EXEC]: [
    {
      title: 'SUMMARY',
      items: [
        { label: 'Executive dashboard', route: '/dashboard' },
        { label: 'KPI scorecard',       route: '/reports' },
      ],
    },
    {
      title: 'PERFORMANCE',
      items: [
        { label: 'SLA compliance', route: '/sla-violations' },
        { label: 'Cost trends',    route: '/costs' },
        { label: 'Loss ratios',    route: '/costs' },
      ],
    },
    {
      title: 'REPORTS',
      items: [
        { label: 'Monthly',       route: '/reports' },
        { label: 'Quarterly',     route: '/reports' },
        { label: 'Notifications', route: '/notifications' },
      ],
    },
  ],

  [ROLES.ADMIN]: [
    {
      title: 'SYSTEM',
      items: [
        { label: 'System overview',  route: '/dashboard' },
        { label: 'Data feeds',       route: '/ingestion/feeds' },
        { label: 'KPI definitions',  route: '/admin/kpi-definitions' },
      ],
    },
    {
      title: 'ACCESS',
      items: [
        { label: 'Users & roles', route: '/admin/users' },
        { label: 'Audit log',     route: '/admin/audit-logs' },
      ],
    },
    {
      title: 'CONFIG',
      items: [
        { label: 'Integrations', route: '/ingestion/raw' },
        { label: 'Settings',     route: '/profile' },
      ],
    },
  ],
};

/** Short role-specific label shown under the brand, e.g. "Analyst workspace". */
export const ROLE_WORKSPACE_LABEL: Record<AppRole, string> = {
  [ROLES.CLAIMS_ANALYST]:  'Analyst workspace',
  [ROLES.CLAIMS_MANAGER]:  'Manager workspace',
  [ROLES.FRAUD_ANALYST]:   'Fraud workspace',
  [ROLES.ACTUARY]:         'Actuary workspace',
  [ROLES.OPERATIONS_EXEC]: 'Executive view',
  [ROLES.ADMIN]:           'Admin console',
};
