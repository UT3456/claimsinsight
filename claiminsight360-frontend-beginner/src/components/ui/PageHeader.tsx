import type { ReactNode } from 'react';

// page header - shows title and any action buttons

export interface PageHeaderProps {
  title: string;
  subtitle?: ReactNode;
  actions?: ReactNode;
}

export default function PageHeader({ title, subtitle, actions }: PageHeaderProps) {
  return (
    <div style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      marginBottom: 16,
    }}>
      <div>
        <h2 style={{ fontSize: 18, margin: 0 }}>{title}</h2>
        {subtitle && <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>{subtitle}</div>}
      </div>
      {actions && <div>{actions}</div>}
    </div>
  );
}
