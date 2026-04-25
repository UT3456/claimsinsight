import type { ReactNode } from 'react';

// simple KPI card - shows a label, value and delta

export interface KpiCardProps {
  label: string;
  value: ReactNode;
  delta?: ReactNode;
  deltaDirection?: 'up' | 'down' | 'flat';
  deltaTone?: 'up' | 'down';
  tone?: 'default' | 'danger' | 'warning';
}

export default function KpiCard({ label, value, delta }: KpiCardProps) {
  return (
    <div style={{
      background: '#fff',
      border: '1px solid #ddd',
      borderRadius: 4,
      padding: 12,
    }}>
      <div style={{ fontSize: 12, color: '#666', marginBottom: 6 }}>{label}</div>
      <div style={{ fontSize: 20, fontWeight: 'bold' }}>{value}</div>
      {delta && <div style={{ fontSize: 11, color: '#999', marginTop: 4 }}>{delta}</div>}
    </div>
  );
}
