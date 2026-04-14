import { Result, Button }  from 'antd';
import { RocketOutlined }  from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';

export default function ComingSoon() {
  const navigate = useNavigate();
  const location = useLocation();

  const pageName = location.pathname
    .replace(/\//g, ' ')
    .replace(/-/g, ' ')
    .trim()
    .replace(/\b\w/g, (c) => c.toUpperCase());

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
      <Result
        icon={<RocketOutlined style={{ color: '#2563eb' }} />}
        title={pageName || 'Coming Soon'}
        subTitle="This page is part of Phase 2+ and will be implemented next."
        extra={
          <Button type="primary" onClick={() => navigate('/dashboard')}>
            Back to Dashboard
          </Button>
        }
      />
    </div>
  );
}
