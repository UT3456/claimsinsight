import { describe, it, expect } from 'vitest';
import {
  ROLES,
  ROLE_LABELS,
  ROLE_COLORS,
  ROUTE_ROLES,
  canAccess,
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

  it('restricts /admin/audit-logs to ADMIN only', () => {
    expect(ROUTE_ROLES['/admin/audit-logs']).toEqual([ROLES.ADMIN]);
  });

  it('restricts /fraud-risk to FRAUD_ANALYST and ADMIN only', () => {
    const allowed = ROUTE_ROLES['/fraud-risk'];
    expect(allowed).toContain(ROLES.FRAUD_ANALYST);
    expect(allowed).toContain(ROLES.ADMIN);
    expect(allowed).not.toContain(ROLES.CLAIMS_ANALYST);
    expect(allowed).not.toContain(ROLES.ACTUARY);
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
