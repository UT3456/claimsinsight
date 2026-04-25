import { useAuth } from '@/hooks';
import { ProtectedRoute } from '@/middleware';
import { Card, CardBody } from '@/components';

const AdminPage: React.FC = () => {
  return (
    <ProtectedRoute requiredRoles={['ADMIN']}>
      <div>
        <h1>Administration</h1>
        <Card style={{ marginTop: '1.5rem' }}>
          <CardBody>
            <p>Administration content coming soon...</p>
          </CardBody>
        </Card>
      </div>
    </ProtectedRoute>
  );
};

export default AdminPage;
