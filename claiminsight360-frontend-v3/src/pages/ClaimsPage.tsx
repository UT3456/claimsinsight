import { useState, useEffect } from 'react';
import { Card, CardBody, FormInput, FormSelect, Table, Button, LoadingSpinner } from '@/components';
import { claimsService } from '@/services/claimsService';
import { Claim, ClaimStatus } from '@/types';

const ClaimsPage: React.FC = () => {
  const [claims, setClaims] = useState<Claim[]>([]);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState<{ status: ClaimStatus | '' }>({ status: '' });

  useEffect(() => {
    loadClaims();
  }, [filter]);

  const loadClaims = async () => {
    try {
      setLoading(true);
      const result = await claimsService.getClaims({
        page: 1,
        limit: 10,
        status: filter.status || undefined,
      });
      setClaims(result.data);
    } catch (error) {
      console.error('Failed to load claims:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="flex-between" style={{ marginBottom: '1.5rem' }}>
        <h1>Claims Management</h1>
        <Button>+ New Claim</Button>
      </div>

      <Card style={{ marginBottom: '1.5rem' }}>
        <CardBody>
          <div className="grid grid-2">
            <FormInput
              type="text"
              placeholder="Search claims..."
              label="Search"
            />
            <FormSelect
              label="Status"
              options={[
                { value: '', label: 'All Statuses' },
                { value: 'OPEN', label: 'Open' },
                { value: 'UNDER_REVIEW', label: 'Under Review' },
                { value: 'APPROVED', label: 'Approved' },
                { value: 'DENIED', label: 'Denied' },
                { value: 'CLOSED', label: 'Closed' },
              ]}
              value={filter.status}
              onChange={(e) => setFilter({ ...filter, status: e.target.value as ClaimStatus | '' })}
            />
          </div>
        </CardBody>
      </Card>

      {loading ? (
        <LoadingSpinner message="Loading claims..." />
      ) : (
        <Card>
          <CardBody>
            <Table
              headers={['Claim #', 'Claimant', 'Amount', 'Status', 'Date']}
              rows={claims.map((claim) => [
                claim.claimNumber,
                claim.claimantName,
                `$${claim.claimAmount.toLocaleString()}`,
                claim.claimStatus,
                new Date(claim.claimDate).toLocaleDateString(),
              ])}
            />
          </CardBody>
        </Card>
      )}
    </div>
  );
};

export default ClaimsPage;
