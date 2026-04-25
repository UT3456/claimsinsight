import type { ReactNode } from 'react';
import { Typography } from 'antd';

const { Title, Text } = Typography;

interface Props {
  title:     string;
  subtitle?: string;
  right?:    ReactNode;
}

export default function PageHeader({ title, subtitle, right }: Props) {
  return (
    <div style={{
      display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
      marginBottom: 16, flexWrap: 'wrap', gap: 12,
    }}>
      <div>
        <Title level={4} style={{ margin: 0, color: 'var(--ci-text-primary)' }}>{title}</Title>
        {subtitle && <Text style={{ fontSize: 12, color: 'var(--ci-text-muted)' }}>{subtitle}</Text>}
      </div>
      {right}
    </div>
  );
}
