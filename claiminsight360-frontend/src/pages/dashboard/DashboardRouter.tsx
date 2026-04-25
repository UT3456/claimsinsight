import { useSelector } from 'react-redux';
import { selectRole } from '../../store/slices/authSlice';
import { ROLES } from '../../utils/roles';

import AnalystDashboard from './AnalystDashboard';
import ManagerDashboard from './ManagerDashboard';
import FraudDashboard   from './FraudDashboard';
import ActuaryDashboard from './ActuaryDashboard';
import OpsExecDashboard from './OpsExecDashboard';
import AdminDashboard   from './AdminDashboard';

/**
 * Routes `/dashboard` to the role-specific dashboard.
 * If the role is missing/unknown, falls back to the analyst view.
 */
export default function DashboardRouter() {
  const role = useSelector(selectRole);

  switch (role) {
    case ROLES.CLAIMS_MANAGER:  return <ManagerDashboard />;
    case ROLES.FRAUD_ANALYST:   return <FraudDashboard />;
    case ROLES.ACTUARY:         return <ActuaryDashboard />;
    case ROLES.OPERATIONS_EXEC: return <OpsExecDashboard />;
    case ROLES.ADMIN:           return <AdminDashboard />;
    case ROLES.CLAIMS_ANALYST:
    default:                    return <AnalystDashboard />;
  }
}
