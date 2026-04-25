// Mock data for admin-only pages (KPI Definitions, Users & Roles).

export interface KpiDefinition {
  name:     string;
  formula:  string;
  unit:     string;
  note:     string;
  active:   boolean;
}

export const kpiDefinitionsMock: KpiDefinition[] = [
  { name: 'Avg TAT',     formula: 'avg(settled - filed)',     unit: 'days', note: 'SLA: 17',        active: true },
  { name: 'Loss Ratio',  formula: 'paid / premium',           unit: '%',    note: 'Target: ≤70',    active: true },
  { name: 'Severity',    formula: 'avg(total_cost)',          unit: '₹',    note: 'By product',     active: true },
  { name: 'Risk Score',  formula: 'sum(weight * severity)',   unit: '0-100', note: 'Alert: ≥70',    active: true },
  { name: 'Denial Rate', formula: 'denied / total',           unit: '%',    note: 'Target: ≤12',    active: true },
  { name: 'Frequency',   formula: 'claims / policies',        unit: '%',    note: 'Per period',     active: false },
];

export interface AppUser {
  name:       string;
  email:      string;
  role:       string;
  status:     'Active' | 'Invited' | 'Suspended';
  lastActive: string;
}

export const usersMock: AppUser[] = [
  { name: 'Priya Menon', email: 'p.menon@acme.com',  role: 'Claims Manager', status: 'Active',  lastActive: 'now' },
  { name: 'Arjun Shah',  email: 'a.shah@acme.com',   role: 'Fraud Analyst',  status: 'Active',  lastActive: '5m' },
  { name: 'Lina Zhou',   email: 'l.zhou@acme.com',   role: 'Actuary',        status: 'Active',  lastActive: '2h' },
  { name: 'Omar Hassan', email: 'o.hassan@acme.com', role: 'Claims Analyst', status: 'Invited', lastActive: '—' },
  { name: 'Rajesh Iyer', email: 'r.iyer@acme.com',   role: 'Operations Exec', status: 'Active', lastActive: '1d' },
  { name: 'System Admin', email: 'admin@acme.com',   role: 'Admin',          status: 'Active',  lastActive: '10m' },
];
