import { useEffect } from 'react';
import { useAuth } from '@/hooks';
import { Card, CardBody, LoadingSpinner } from '@/components';

const DashboardPage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div>
      <h1>Dashboard</h1>
      <p>Welcome, {user?.firstName}! Your role is: {user?.role}</p>

      <div className="grid grid-3" style={{ marginTop: '2rem' }}>
        <Card>
          <CardBody>
            <h3>Total Claims</h3>
            <p style={{ fontSize: '2rem', fontWeight: 'bold', color: '#0066cc' }}>1,234</p>
          </CardBody>
        </Card>

        <Card>
          <CardBody>
            <h3>Pending Review</h3>
            <p style={{ fontSize: '2rem', fontWeight: 'bold', color: '#ffc107' }}>56</p>
          </CardBody>
        </Card>

        <Card>
          <CardBody>
            <h3>High Risk</h3>
            <p style={{ fontSize: '2rem', fontWeight: 'bold', color: '#dc3545' }}>12</p>
          </CardBody>
        </Card>
      </div>

      <Card style={{ marginTop: '2rem' }}>
        <CardBody>
          <h3>Recent Activity</h3>
          <p>Dashboard content coming soon...</p>
        </CardBody>
      </Card>
    </div>
  );
};

export default DashboardPage;
