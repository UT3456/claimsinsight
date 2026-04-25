import { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Provider, useDispatch, useSelector } from 'react-redux';
import { store } from '@/store';
import { setUser } from '@/store/authSlice';
import { ProtectedRoute } from '@/middleware/ProtectedRoute';
import Layout from '@/components/Layout';
import { RootState } from '@/store';

// Pages
import LoginPage from '@/pages/LoginPage';
import DashboardPage from '@/pages/DashboardPage';
import ClaimsPage from '@/pages/ClaimsPage';
import ClaimDetailPage from '@/pages/ClaimDetailPage';
import DenialsPage from '@/pages/DenialsPage';
import FraudPage from '@/pages/FraudPage';
import ReservesPage from '@/pages/ReservesPage';
import ReportsPage from '@/pages/ReportsPage';
import AdjustersPage from '@/pages/AdjustersPage';
import NotificationsPage from '@/pages/NotificationsPage';
import AdminPage from '@/pages/AdminPage';
import NotFoundPage from '@/pages/NotFoundPage';

// Initialization component to restore auth state
const AppInitializer: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const dispatch = useDispatch();
  const { isAuthenticated } = useSelector((state: RootState) => state.auth);

  useEffect(() => {
    // Try to restore authentication from localStorage on app load
    const token = localStorage.getItem('authToken');
    const userStr = localStorage.getItem('user');

    if (token && userStr) {
      try {
        const user = JSON.parse(userStr);
        dispatch(setUser({ user, token }));
        console.log('Auth restored from localStorage');
      } catch (error) {
        console.error('Failed to restore auth:', error);
        localStorage.removeItem('authToken');
        localStorage.removeItem('user');
      }
    }
  }, [dispatch]);

  return <>{children}</>;
};

const AppRoutes: React.FC = () => {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        <Route
          path="/*"
          element={
            <ProtectedRoute>
              <Layout>
                <Routes>
                  <Route path="/" element={<DashboardPage />} />
                  <Route path="/claims" element={<ClaimsPage />} />
                  <Route path="/claims/:claimId" element={<ClaimDetailPage />} />
                  <Route path="/denials" element={<DenialsPage />} />
                  <Route path="/fraud" element={<FraudPage />} />
                  <Route path="/reserves" element={<ReservesPage />} />
                  <Route path="/reports" element={<ReportsPage />} />
                  <Route path="/adjusters" element={<AdjustersPage />} />
                  <Route path="/notifications" element={<NotificationsPage />} />
                  <Route path="/admin" element={<AdminPage />} />
                  <Route path="*" element={<NotFoundPage />} />
                </Routes>
              </Layout>
            </ProtectedRoute>
          }
        />
      </Routes>
    </Router>
  );
};

const App: React.FC = () => {
  return (
    <Provider store={store}>
      <AppInitializer>
        <AppRoutes />
      </AppInitializer>
    </Provider>
  );
};

export default App;
