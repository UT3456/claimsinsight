import { useParams } from 'react-router-dom';
import { Card, CardBody, CardHeader, Button } from '@/components';

const ClaimDetailPage: React.FC = () => {
  const { claimId } = useParams();

  return (
    <div>
      <div className="flex-between" style={{ marginBottom: '1.5rem' }}>
        <h1>Claim Details</h1>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <Button variant="secondary">Edit</Button>
          <Button>Approve</Button>
        </div>
      </div>

      <Card>
        <CardHeader>
          <h2>Claim #{claimId}</h2>
        </CardHeader>
        <CardBody>
          <p>Loading claim details...</p>
        </CardBody>
      </Card>
    </div>
  );
};

export default ClaimDetailPage;
