import { Outlet }       from 'react-router-dom';
import { useSelector }  from 'react-redux';
import { Result, Button } from 'antd';
import { useNavigate }  from 'react-router-dom';
import {
  selectIsAuthenticated,
  selectIsAdmin,
} from '../store/slices/authSlice';

export default function AdminRoute() {
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const isAdmin         = useSelector(selectIsAdmin);
  const navigate        = useNavigate();

  if (!isAuthenticated) {
    navigate('/login', { replace: true });
    return null;
  }

  if (!isAdmin) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
        <Result
          status="403"
          title="403 — Access Denied"
          subTitle="You do not have permission to view this page. Admin role required."
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
