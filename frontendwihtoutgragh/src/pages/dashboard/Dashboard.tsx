import { lazy, Suspense, type ComponentType, type LazyExoticComponent } from 'react';
import { Spin } from 'antd';
import { useAuth } from '../../hooks/useAuth';
import { ROLES, type AppRole } from '../../utils/roles';

/**
 * Dashboard — thin role-based router.
 *
 * Per the SRS, every persona has different first-screen needs:
 *
 *   Claims Analyst    → denial trends, severity, cost drivers
 *   Claims Manager    → TAT vs SLA, adjuster workload, cycle time
 *   Fraud Analyst     → risk score histogram, fraud signals + Escalate SIU
 *   Actuary           → severity, frequency, reserves
 *   Operations Exec   → portfolio KPIs, MoM trends, SLA compliance
 *   Admin             → user count, feed status, recent activity
 *
 * Each variant lives in its own file under ./variants/ and is lazy-loaded so
 * a Fraud Analyst's session never has to download the Actuary's reserve
 * tables (and vice versa).
 *
 * Falls back to AnalystDashboard if the role is missing or unrecognised —
 * the analyst view is the most general and works as a sensible default.
 */

const AnalystDashboard = lazy(() => import('./variants/AnalystDashboard'));
const ManagerDashboard = lazy(() => import('./variants/ManagerDashboard'));
const FraudDashboard   = lazy(() => import('./variants/FraudDashboard'));
const ActuaryDashboard = lazy(() => import('./variants/ActuaryDashboard'));
const OpsExecDashboard = lazy(() => import('./variants/OpsExecDashboard'));
const AdminDashboard   = lazy(() => import('./variants/AdminDashboard'));

type DashboardVariant = LazyExoticComponent<ComponentType<object>>;

const VARIANT_BY_ROLE: Record<AppRole, DashboardVariant> = {
  [ROLES.CLAIMS_ANALYST]:   AnalystDashboard,
  [ROLES.CLAIMS_MANAGER]:   ManagerDashboard,
  [ROLES.FRAUD_ANALYST]:    FraudDashboard,
  [ROLES.ACTUARY]:          ActuaryDashboard,
  [ROLES.OPERATIONS_EXEC]:  OpsExecDashboard,
  [ROLES.ADMIN]:            AdminDashboard,
};

export default function Dashboard() {
  const { role } = useAuth();
  const Variant: DashboardVariant =
    (role ? VARIANT_BY_ROLE[role as AppRole] : undefined) ?? AnalystDashboard;

  return (
    <Suspense fallback={
      <div style={{ display: 'flex', justifyContent: 'center', padding: '40px 0' }}>
        <Spin size="large" />
      </div>
    }>
      <Variant />
    </Suspense>
  );
}
