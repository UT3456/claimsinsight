import { Routes, Route, Navigate } from 'react-router-dom';
import { Suspense, lazy }          from 'react';
import { Spin }                    from 'antd';
import ProtectedRoute              from './ProtectedRoute';
import RoleRoute                   from './RoleRoute';
import AppLayout                   from '../layouts/AppLayout';

// ── Lazy-loaded pages ─────────────────────────────────────────────────────────
const LoginPage         = lazy(() => import('../pages/auth/LoginPage'));
const RegisterPage      = lazy(() => import('../pages/auth/RegisterPage'));
const DashboardRouter   = lazy(() => import('../pages/dashboard/DashboardRouter'));

// Claims Analytics
const ClaimsPage        = lazy(() => import('../pages/claims/ClaimsPage'));

// Data Ingestion
const FeedsPage         = lazy(() => import('../pages/ingestion/FeedsPage'));
const RawClaimsPage     = lazy(() => import('../pages/ingestion/RawClaimsPage'));

// Adjuster Operations
const AdjustersPage     = lazy(() => import('../pages/adjusters/AdjustersPage'));
const SlaViolationsPage = lazy(() => import('../pages/adjusters/SlaViolationsPage'));

// Financial
const CostsPage         = lazy(() => import('../pages/financial/CostsPage'));
const ReservesPage      = lazy(() => import('../pages/financial/ReservesPage'));
const AgingPage         = lazy(() => import('../pages/financial/AgingPage'));

// Risk & Denial
const FraudRiskPage     = lazy(() => import('../pages/risk/FraudRiskPage'));
const DenialLeakagePage = lazy(() => import('../pages/risk/DenialLeakagePage'));

// Reports & Notifications
const ReportsPage       = lazy(() => import('../pages/reports/ReportsPage'));
const NotificationsPage = lazy(() => import('../pages/notifications/NotificationsPage'));

// Admin
const AuditLogsPage       = lazy(() => import('../pages/admin/AuditLogsPage'));
const KpiDefinitionsPage  = lazy(() => import('../pages/admin/KpiDefinitionsPage'));
const UsersRolesPage      = lazy(() => import('../pages/admin/UsersRolesPage'));

// Profile
const ProfilePage       = lazy(() => import('../pages/profile/ProfilePage'));

const PageLoader = (
  <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
    <Spin size="large" />
  </div>
);

export default function AppRouter() {
  return (
    <Suspense fallback={PageLoader}>
      <Routes>

        {/* ── Public ── */}
        <Route path="/login"    element={<LoginPage />}    />
        <Route path="/register" element={<RegisterPage />} />

        {/* ── All authenticated users inside AppLayout shell ── */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>

            {/* Dashboard — role-specific component picked by DashboardRouter */}
            <Route path="/dashboard" element={<DashboardRouter />} />

            {/* Claims Analytics — CLAIMS_ANALYST, CLAIMS_MANAGER, ACTUARY, OPERATIONS_EXEC, ADMIN */}
            <Route element={<RoleRoute />}>
              <Route path="/claims"          element={<ClaimsPage />} />
              <Route path="/claims/:claimId" element={<ClaimsPage />} />
            </Route>

            {/* Data Ingestion — CLAIMS_ANALYST, ADMIN */}
            <Route element={<RoleRoute />}>
              <Route path="/ingestion/feeds" element={<FeedsPage />} />
              <Route path="/ingestion/raw"   element={<RawClaimsPage />} />
            </Route>

            {/* Adjuster Ops — CLAIMS_MANAGER, OPERATIONS_EXEC, ADMIN */}
            <Route element={<RoleRoute />}>
              <Route path="/adjusters"      element={<AdjustersPage />} />
              <Route path="/adjusters/:id"  element={<AdjustersPage />} />
              <Route path="/sla-violations" element={<SlaViolationsPage />} />
            </Route>

            {/* Financial — ACTUARY, CLAIMS_MANAGER, ADMIN */}
            <Route element={<RoleRoute />}>
              <Route path="/costs"    element={<CostsPage />} />
              <Route path="/reserves" element={<ReservesPage />} />
              <Route path="/aging"    element={<AgingPage />} />
            </Route>

            {/* Risk Intelligence — FRAUD_ANALYST (+CLAIMS_ANALYST for denial), ADMIN */}
            <Route element={<RoleRoute />}>
              <Route path="/fraud-risk"     element={<FraudRiskPage />} />
              <Route path="/denial-leakage" element={<DenialLeakagePage />} />
            </Route>

            {/* Reports & Notifications — all roles */}
            <Route path="/reports"       element={<ReportsPage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route path="/profile"       element={<ProfilePage />} />

            {/* Admin only */}
            <Route element={<RoleRoute />}>
              <Route path="/admin/audit-logs"      element={<AuditLogsPage />} />
              <Route path="/admin/kpi-definitions" element={<KpiDefinitionsPage />} />
              <Route path="/admin/users"           element={<UsersRolesPage />} />
            </Route>

          </Route>
        </Route>

        {/* ── Fallback ── */}
        <Route path="/"  element={<Navigate to="/dashboard" replace />} />
        <Route path="*"  element={<Navigate to="/login"     replace />} />

      </Routes>
    </Suspense>
  );
}
