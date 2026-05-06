import { describe, it, expect } from 'vitest';
import {
  ROLES,
  ROLE_LABELS,
  ROLE_COLORS,
  ROUTE_ROLES,
  WRITE_ROLES,
  canAccess,
  canEdit,
  hasAdminSectionAccess,
} from './roles';

describe('ROLES constant', () => {
  it('contains all six system roles', () => {
    const keys = Object.keys(ROLES);
    expect(keys).toHaveLength(6);
    expect(ROLES.ADMIN).toBe('ROLE_ADMIN');
    expect(ROLES.CLAIMS_ANALYST).toBe('ROLE_CLAIMS_ANALYST');
    expect(ROLES.FRAUD_ANALYST).toBe('ROLE_FRAUD_ANALYST');
    expect(ROLES.ACTUARY).toBe('ROLE_ACTUARY');
    expect(ROLES.OPERATIONS_EXEC).toBe('ROLE_OPERATIONS_EXEC');
    expect(ROLES.CLAIMS_MANAGER).toBe('ROLE_CLAIMS_MANAGER');
  });
});

describe('ROLE_LABELS', () => {
  it('provides a human-readable label for every role', () => {
    Object.values(ROLES).forEach((role) => {
      expect(ROLE_LABELS[role]).toBeTruthy();
    });
  });

  it('returns the correct label for ROLE_ADMIN', () => {
    expect(ROLE_LABELS['ROLE_ADMIN']).toBe('Admin');
  });

  it('returns the correct label for ROLE_FRAUD_ANALYST', () => {
    expect(ROLE_LABELS['ROLE_FRAUD_ANALYST']).toBe('Fraud Analyst');
  });
});

describe('ROLE_COLORS', () => {
  it('provides a color for every role', () => {
    Object.values(ROLES).forEach((role) => {
      expect(ROLE_COLORS[role]).toBeTruthy();
    });
  });

  it('assigns red to ADMIN', () => {
    expect(ROLE_COLORS['ROLE_ADMIN']).toBe('red');
  });

  it('assigns volcano to FRAUD_ANALYST', () => {
    expect(ROLE_COLORS['ROLE_FRAUD_ANALYST']).toBe('volcano');
  });
});

describe('ROUTE_ROLES', () => {
  it('defines access for /dashboard', () => {
    expect(ROUTE_ROLES['/dashboard']).toContain(ROLES.CLAIMS_ANALYST);
    expect(ROUTE_ROLES['/dashboard']).toContain(ROLES.ADMIN);
  });

  it('restricts /admin/users to ADMIN only', () => {
    expect(ROUTE_ROLES['/admin/users']).toEqual([ROLES.ADMIN]);
  });

  it('locks /admin/audit-logs to ADMIN only — no other role may even view it', () => {
    expect(ROUTE_ROLES['/admin/audit-logs']).toEqual([ROLES.ADMIN]);
  });

  it('opens /ingestion/feeds to Manager (read) + Analyst + Admin', () => {
    const allowed = ROUTE_ROLES['/ingestion/feeds'];
    expect(allowed).toContain(ROLES.CLAIMS_ANALYST);
    expect(allowed).toContain(ROLES.CLAIMS_MANAGER);
    expect(allowed).toContain(ROLES.ADMIN);
    expect(allowed).not.toContain(ROLES.ACTUARY);
  });

  it('restricts /fraud-risk to FRAUD_ANALYST and ADMIN only', () => {
    const allowed = ROUTE_ROLES['/fraud-risk'];
    expect(allowed).toContain(ROLES.FRAUD_ANALYST);
    expect(allowed).toContain(ROLES.ADMIN);
    expect(allowed).not.toContain(ROLES.CLAIMS_ANALYST);
    expect(allowed).not.toContain(ROLES.ACTUARY);
  });
});

// ── WRITE_ROLES — write vs view separation (security-critical) ─────────────
describe('WRITE_ROLES', () => {
  it('locks /admin/users writes to ADMIN only', () => {
    expect(WRITE_ROLES['/admin/users']).toEqual([ROLES.ADMIN]);
  });

  it('locks /admin/audit-logs writes (clear/delete) to ADMIN only', () => {
    expect(WRITE_ROLES['/admin/audit-logs']).toEqual([ROLES.ADMIN]);
  });

  it('locks /admin/kpi-definitions writes to ADMIN only', () => {
    expect(WRITE_ROLES['/admin/kpi-definitions']).toEqual([ROLES.ADMIN]);
  });

  it('allows /ingestion/feeds writes for ANALYST + ADMIN (not MANAGER)', () => {
    const writers = WRITE_ROLES['/ingestion/feeds'];
    expect(writers).toContain(ROLES.CLAIMS_ANALYST);
    expect(writers).toContain(ROLES.ADMIN);
    expect(writers).not.toContain(ROLES.CLAIMS_MANAGER);
  });
});

describe('canAccess()', () => {
  it('returns false when role is null', () => {
    expect(canAccess(null, '/dashboard')).toBe(false);
  });

  it('returns false when role is empty string', () => {
    expect(canAccess('', '/dashboard')).toBe(false);
  });

  it('returns true for ADMIN on every defined route', () => {
    Object.keys(ROUTE_ROLES).forEach((route) => {
      expect(canAccess(ROLES.ADMIN, route)).toBe(true);
    });
  });

  it('returns true for ADMIN on an undefined/unknown route (catch-all)', () => {
    expect(canAccess(ROLES.ADMIN, '/some/unknown/route')).toBe(true);
  });

  it('returns false for CLAIMS_ANALYST on admin-only route', () => {
    expect(canAccess(ROLES.CLAIMS_ANALYST, '/admin/audit-logs')).toBe(false);
  });

  it('returns true for CLAIMS_ANALYST on /claims', () => {
    expect(canAccess(ROLES.CLAIMS_ANALYST, '/claims')).toBe(true);
  });

  it('returns false for FRAUD_ANALYST on /costs', () => {
    expect(canAccess(ROLES.FRAUD_ANALYST, '/costs')).toBe(false);
  });

  it('returns true for FRAUD_ANALYST on /fraud-risk', () => {
    expect(canAccess(ROLES.FRAUD_ANALYST, '/fraud-risk')).toBe(true);
  });

  it('returns false for ACTUARY on /adjusters', () => {
    expect(canAccess(ROLES.ACTUARY, '/adjusters')).toBe(false);
  });

  it('returns true for ACTUARY on /reserves', () => {
    expect(canAccess(ROLES.ACTUARY, '/reserves')).toBe(true);
  });

  it('returns false for any role on a completely unknown route', () => {
    expect(canAccess(ROLES.CLAIMS_ANALYST, '/no-such-page')).toBe(false);
  });
});

// ── canEdit() — most security-critical helper ─────────────────────────────
// Disabling the Add/Delete buttons in the UI uses canEdit. If this returns
// the wrong answer, a Manager could see (and click) admin-only delete buttons.
describe('canEdit()', () => {
  it('returns false when role is null or empty', () => {
    expect(canEdit(null, '/admin/users')).toBe(false);
    expect(canEdit('', '/admin/users')).toBe(false);
  });

  it('returns true for ADMIN on every WRITE_ROLES route', () => {
    Object.keys(WRITE_ROLES).forEach((route) => {
      expect(canEdit(ROLES.ADMIN, route)).toBe(true);
    });
  });

  it('returns true for ADMIN on routes without an explicit WRITE_ROLES entry', () => {
    expect(canEdit(ROLES.ADMIN, '/dashboard')).toBe(true);
    expect(canEdit(ROLES.ADMIN, '/claims')).toBe(true);
  });

  it('returns false for MANAGER on /ingestion/feeds (read-only viewer)', () => {
    // Manager has READ access via ROUTE_ROLES, but NOT WRITE access. The Add Feed
    // button must stay hidden.
    expect(canAccess(ROLES.CLAIMS_MANAGER, '/ingestion/feeds')).toBe(true);
    expect(canEdit(ROLES.CLAIMS_MANAGER,   '/ingestion/feeds')).toBe(false);
  });

  it('returns false for FRAUD_ANALYST on /admin/audit-logs (cannot view at all)', () => {
    // Audit logs are admin-only — fraud analysts never see them.
    expect(canAccess(ROLES.FRAUD_ANALYST, '/admin/audit-logs')).toBe(false);
    expect(canEdit(ROLES.FRAUD_ANALYST,   '/admin/audit-logs')).toBe(false);
  });

  it('returns true for CLAIMS_ANALYST on /ingestion/raw (delete allowed)', () => {
    expect(canEdit(ROLES.CLAIMS_ANALYST, '/ingestion/raw')).toBe(true);
  });

  it('falls back to canAccess() for routes with no WRITE_ROLES entry', () => {
    // /dashboard has no write restriction defined → anyone with read access can edit
    expect(canEdit(ROLES.CLAIMS_ANALYST, '/dashboard')).toBe(true);
    expect(canEdit(ROLES.FRAUD_ANALYST,  '/dashboard')).toBe(true);
  });

  it('returns false on a completely unknown route for non-admin roles', () => {
    expect(canEdit(ROLES.CLAIMS_ANALYST, '/no-such-route')).toBe(false);
  });
});

// ── hasAdminSectionAccess() — Sidebar gating ──────────────────────────────
describe('hasAdminSectionAccess()', () => {
  it('returns false for null', () => {
    expect(hasAdminSectionAccess(null)).toBe(false);
  });

  it('returns true for ADMIN', () => {
    expect(hasAdminSectionAccess(ROLES.ADMIN)).toBe(true);
  });

  it('returns true for CLAIMS_ANALYST (has /ingestion/feeds + /ingestion/raw)', () => {
    expect(hasAdminSectionAccess(ROLES.CLAIMS_ANALYST)).toBe(true);
  });

  it('returns false for FRAUD_ANALYST (no admin tabs at all)', () => {
    // Fraud Analyst has no /ingestion/raw access (they investigate specific
    // claims via Fraud & Risk), no audit log access (admin-only), no KPI
    // config, no users. The Admin sidebar section is hidden entirely.
    expect(hasAdminSectionAccess(ROLES.FRAUD_ANALYST)).toBe(false);
  });

  it('returns true for CLAIMS_MANAGER (has /ingestion/feeds + /admin/kpi-definitions)', () => {
    expect(hasAdminSectionAccess(ROLES.CLAIMS_MANAGER)).toBe(true);
  });

  it('returns true for ACTUARY (has /admin/kpi-definitions)', () => {
    expect(hasAdminSectionAccess(ROLES.ACTUARY)).toBe(true);
  });

  it('returns true for OPERATIONS_EXEC (has /admin/kpi-definitions)', () => {
    expect(hasAdminSectionAccess(ROLES.OPERATIONS_EXEC)).toBe(true);
  });
});
