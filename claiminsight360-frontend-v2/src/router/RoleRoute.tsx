import { Outlet, useLocation } from 'react-router-dom';
import { useSelector }         from 'react-redux';
import { Result, Button }      from 'antd';
import { useNavigate }         from 'react-router-dom';
import { selectRole, selectIsAuthenticated } from '../store/slices/authSlice';
import { canAccess, ROLE_LABELS, type AppRole } from '../utils/roles';

/**
 * Wraps a route and checks whether the current user's role has access.
 * Usage in AppRouter:  <Route element={<RoleRoute />}> ... </Route>
 *
 * The allowed roles are derived from ROUTE_ROLES keyed on the current pathname.
 */
export default function RoleRoute() {
  const navigate        = useNavigate();
  const location        = useLocation();
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const role            = useSelector(selectRole);

  if (!isAuthenticated) {
    navigate('/login', { replace: true });
    return null;
  }

  // Find the best matching route key (strip dynamic segments like :id)
  const routeKey = '/' + location.pathname.split('/').slice(1, 3).join('/');

  if (!canAccess(role, routeKey)) {
    const roleLabel = role ? (ROLE_LABELS[role as AppRole] ?? role) : 'Unknown';
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
        <Result
          status="403"
          title="Access Denied"
          subTitle={
            <>
              Your role <strong>{roleLabel}</strong> does not have permission to view this page.
              <br />
              Please contact your administrator if you need access.
            </>
          }
          extra={
            <Button type="primary" onClick={() => navigate('/dashboard')}>
              Back to Dashboard
            </Button>
          }
        />
      </div>
    );
  }

  return <Outlet />;
}
