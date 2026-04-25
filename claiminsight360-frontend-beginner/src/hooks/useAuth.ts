import { useSelector, useDispatch } from 'react-redux';
import { useNavigate }              from 'react-router-dom';
import {
  selectIsAuthenticated,
  selectCurrentUser,
  selectRole,
  selectUserId,
  selectIsAdmin,
  clearCredentials,
} from '../store/slices/authSlice';
import { canAccess, ROLES, ROLE_LABELS, ROLE_COLORS, type AppRole } from '../utils/roles';

export function useAuth() {
  const dispatch        = useDispatch();
  const navigate        = useNavigate();
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const username        = useSelector(selectCurrentUser);
  const role            = useSelector(selectRole);
  const userId          = useSelector(selectUserId);
  const isAdmin         = useSelector(selectIsAdmin);

  const logout = () => {
    dispatch(clearCredentials());
    navigate('/login', { replace: true });
  };

  // Role-check helpers
  const isClaimsAnalyst   = role === ROLES.CLAIMS_ANALYST;
  const isClaimsManager   = role === ROLES.CLAIMS_MANAGER;
  const isFraudAnalyst    = role === ROLES.FRAUD_ANALYST;
  const isActuary         = role === ROLES.ACTUARY;
  const isOperationsExec  = role === ROLES.OPERATIONS_EXEC;

  const hasAccess = (route: string) => canAccess(role, route);

  const roleLabel = role ? (ROLE_LABELS[role as AppRole] ?? role) : '';
  const roleColor = role ? (ROLE_COLORS[role as AppRole] ?? 'default') : 'default';

  return {
    isAuthenticated,
    username,
    role,
    userId,
    isAdmin,
    isClaimsAnalyst,
    isClaimsManager,
    isFraudAnalyst,
    isActuary,
    isOperationsExec,
    roleLabel,
    roleColor,
    hasAccess,
    logout,
  };
}
