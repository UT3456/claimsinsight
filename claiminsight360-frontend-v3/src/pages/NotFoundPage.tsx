import { Card, CardBody } from '@/components';

const NotFoundPage: React.FC = () => {
  return (
    <div style={{ textAlign: 'center', padding: '3rem' }}>
      <h1>404 - Page Not Found</h1>
      <p style={{ marginTop: '1rem', color: '#6b7280' }}>
        The page you're looking for doesn't exist.
      </p>
    </div>
  );
};

export default NotFoundPage;
