import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/hooks';
import { authService } from '@/services/authService';
import { Button } from '@/components';

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [userMenuOpen, setUserMenuOpen] = useState(false);

  const handleLogout = async () => {
    try {
      await authService.logout();
      navigate('/login');
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  const getNavItems = () => {
    const commonItems = [
      { path: '/', label: 'Dashboard', icon: '📊' },
      { path: '/claims', label: 'Claims', icon: '📋' },
      { path: '/denials', label: 'Denials', icon: '❌' },
      { path: '/fraud', label: 'Fraud', icon: '⚠️' },
      { path: '/reserves', label: 'Reserves', icon: '💰' },
      { path: '/reports', label: 'Reports', icon: '📈' },
      { path: '/adjusters', label: 'Adjusters', icon: '👥' },
      { path: '/notifications', label: 'Notifications', icon: '🔔' },
    ];

    if (user?.role === 'ADMIN') {
      commonItems.push({ path: '/admin', label: 'Admin', icon: '⚙️' });
    }

    return commonItems;
  };

  const isActive = (path: string) => {
    if (path === '/') {
      return location.pathname === '/';
    }
    return location.pathname.startsWith(path);
  };

  return (
    <div style={{ display: 'flex', minHeight: '100vh', backgroundColor: '#f9fafb' }}>
      {/* Sidebar */}
      <aside
        style={{
          width: sidebarOpen ? '250px' : '0',
          backgroundColor: '#fff',
          borderRight: '1px solid #e5e7eb',
          transition: 'width 0.3s ease',
          overflow: 'hidden',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
        }}
      >
        <div style={{ padding: '1.5rem' }}>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#0066cc' }}>
            ClaimInsight360
          </h1>
        </div>

        <nav style={{ padding: '1rem 0' }}>
          {getNavItems().map((item) => (
            <Link
              key={item.path}
              to={item.path}
              style={{
                display: 'flex',
                alignItems: 'center',
                padding: '0.75rem 1rem',
                color: isActive(item.path) ? '#0066cc' : '#6b7280',
                textDecoration: 'none',
                borderLeft: isActive(item.path) ? '4px solid #0066cc' : '4px solid transparent',
                backgroundColor: isActive(item.path) ? '#e6f0ff' : 'transparent',
                fontSize: '0.875rem',
                fontWeight: isActive(item.path) ? '600' : '400',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
              }}
            >
              <span style={{ marginRight: '0.75rem', fontSize: '1.25rem' }}>{item.icon}</span>
              {item.label}
            </Link>
          ))}
        </nav>
      </aside>

      {/* Main Content */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {/* Header */}
        <header
          style={{
            backgroundColor: '#fff',
            borderBottom: '1px solid #e5e7eb',
            padding: '1rem 1.5rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
          }}
        >
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            style={{
              background: 'none',
              border: 'none',
              fontSize: '1.5rem',
              cursor: 'pointer',
            }}
          >
            ☰
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <span style={{ color: '#6b7280' }}>
              Welcome, <strong>{user?.firstName}</strong>
            </span>

            <div style={{ position: 'relative' }}>
              <button
                onClick={() => setUserMenuOpen(!userMenuOpen)}
                style={{
                  backgroundColor: '#e5e7eb',
                  border: 'none',
                  borderRadius: '50%',
                  width: '40px',
                  height: '40px',
                  cursor: 'pointer',
                  fontSize: '1rem',
                }}
              >
                {user?.firstName?.charAt(0).toUpperCase()}
              </button>

              {userMenuOpen && (
                <div
                  style={{
                    position: 'absolute',
                    right: 0,
                    top: '100%',
                    marginTop: '0.5rem',
                    backgroundColor: '#fff',
                    borderRadius: '0.375rem',
                    boxShadow: '0 10px 15px rgba(0,0,0,0.1)',
                    minWidth: '150px',
                    zIndex: 10,
                  }}
                >
                  <button
                    onClick={handleLogout}
                    style={{
                      width: '100%',
                      padding: '0.75rem 1rem',
                      background: 'none',
                      border: 'none',
                      textAlign: 'left',
                      cursor: 'pointer',
                      color: '#dc3545',
                      fontSize: '0.875rem',
                    }}
                  >
                    Logout
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main style={{ flex: 1, padding: '1.5rem', overflow: 'auto' }}>
          {children}
        </main>
      </div>
    </div>
  );
};

export default Layout;
