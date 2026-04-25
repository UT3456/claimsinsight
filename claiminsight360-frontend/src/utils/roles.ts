/** All roles in the system — must match backend Role.java enum exactly. */
export const ROLES = {
  CLAIMS_ANALYST:   'ROLE_CLAIMS_ANALYST',
  CLAIMS_MANAGER:   'ROLE_CLAIMS_MANAGER',
  FRAUD_ANALYST:    'ROLE_FRAUD_ANALYST',
  ACTUARY:          'ROLE_ACTUARY',
  OPERATIONS_EXEC:  'ROLE_OPERATIONS_EXEC',
  ADMIN:            'ROLE_ADMIN',
} as const;

export type AppRole = typeof ROLES[keyof typeof ROLES];

/** Human-readable label for each role. */
export const ROLE_LABELS: Record<AppRole, string> = {
  ROLE_CLAIMS_ANALYST:  'Claims Analyst',
  ROLE_CLAIMS_MANAGER:  'Claims Manager',
  ROLE_FRAUD_ANALYST:   'Fraud Analyst',
  ROLE_ACTUARY:         'Actuary',
  ROLE_OPERATIONS_EXEC: 'Operations Executive',
  ROLE_ADMIN:           'Admin',
};

/** Ant Design tag colour per role. */
export const ROLE_COLORS: Record<AppRole, string> = {
  ROLE_CLAIMS_ANALYST:  'blue',
  ROLE_CLAIMS_MANAGER:  'geekblue',
  ROLE_FRAUD_ANALYST:   'volcano',
  ROLE_ACTUARY:         'cyan',
  ROLE_OPERATIONS_EXEC: 'purple',
  ROLE_ADMIN:           'red',
};

/**
 * Which roles can access each route.
 * ADMIN implicitly has access to everything — checked separately.
 */
export const ROUTE_ROLES: Record<string, AppRole[]> = {
  '/dashboard':        [ROLES.CLAIMS_ANALYST, ROLES.CLAIMS_MANAGER, ROLES.FRAUD_ANALYST, ROLES.ACTUARY, ROLES.OPERATIONS_EXEC, ROLES.ADMIN],
  '/claims':           [ROLES.CLAIMS_ANALYST, ROLES.CLAIMS_MANAGER, ROLES.ACTUARY, ROLES.OPERATIONS_EXEC, ROLES.ADMIN],
  '/ingestion/feeds':  [ROLES.CLAIMS_ANALYST, ROLES.ADMIN],
  '/ingestion/raw':    [ROLES.CLAIMS_ANALYST, ROLES.ADMIN],
  '/adjusters':        [ROLES.CLAIMS_MANAGER, ROLES.OPERATIONS_EXEC, ROLES.ADMIN],
  '/sla-violations':   [ROLES.CLAIMS_MANAGER, ROLES.OPERATIONS_EXEC, ROLES.ADMIN],
  '/costs':            [ROLES.ACTUARY, ROLES.CLAIMS_MANAGER, ROLES.CLAIMS_ANALYST, ROLES.OPERATIONS_EXEC, ROLES.ADMIN],
  '/reserves':         [ROLES.ACTUARY, ROLES.CLAIMS_MANAGER, ROLES.OPERATIONS_EXEC, ROLES.ADMIN],
  '/aging':            [ROLES.ACTUARY, ROLES.CLAIMS_MANAGER, ROLES.ADMIN],
  '/fraud-risk':       [ROLES.FRAUD_ANALYST, ROLES.ADMIN],
  '/denial-leakage':   [ROLES.CLAIMS_ANALYST, ROLES.CLAIMS_MANAGER, ROLES.FRAUD_ANALYST, ROLES.ADMIN],
  '/reports':          [ROLES.CLAIMS_ANALYST, ROLES.CLAIMS_MANAGER, ROLES.FRAUD_ANALYST, ROLES.ACTUARY, ROLES.OPERATIONS_EXEC, ROLES.ADMIN],
  '/notifications':    [ROLES.CLAIMS_ANALYST, ROLES.CLAIMS_MANAGER, ROLES.FRAUD_ANALYST, ROLES.ACTUARY, ROLES.OPERATIONS_EXEC, ROLES.ADMIN],
  '/admin/audit-logs':      [ROLES.ADMIN],
  '/admin/kpi-definitions': [ROLES.ADMIN],
  '/admin/users':           [ROLES.ADMIN],
};

/** Returns true if the given role can access the given route. */
export function canAccess(role: string | null, route: string): boolean {
  if (!role) return false;
  if (role === ROLES.ADMIN) return true;
  const allowed = ROUTE_ROLES[route] ?? [];
  return allowed.includes(role as AppRole);
}
