import type { ReactNode } from 'react';
import { Empty } from 'antd';

// basic empty state using antd Empty

export interface EmptyStateProps {
  title: string;
  description?: ReactNode;
  icon?: ReactNode;
  tone?: 'neutral' | 'positive';
  actions?: ReactNode;
  style?: React.CSSProperties;
}

export default function EmptyState({ title, description, actions, style }: EmptyStateProps) {
  return (
    <div style={{ padding: 24, textAlign: 'center', ...style }}>
      <Empty description={title} />
      {description && <div style={{ fontSize: 12, color: '#999', marginTop: 8 }}>{description}</div>}
      {actions && <div style={{ marginTop: 12 }}>{actions}</div>}
    </div>
  );
}
