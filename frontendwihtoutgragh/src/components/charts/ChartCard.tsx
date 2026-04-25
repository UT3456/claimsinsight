import { Card, Typography, Spin, Empty } from 'antd';
import type { CSSProperties, ReactNode } from 'react';

const { Text } = Typography;

interface ChartCardProps {
  title:       string;
  subtitle?:   string;
  extra?:      ReactNode;
  loading?:    boolean;
  isEmpty?:    boolean;
  height?:     number;
  children:    ReactNode;
  style?:      CSSProperties;
}

/**
 * Themed wrapper for Recharts graphs. Uses CSS vars so charts respect
 * the active light/dark theme without manual color wiring.
 */
export default function ChartCard({
  title, subtitle, extra, loading, isEmpty, height = 280, children, style,
}: ChartCardProps) {
  return (
    <Card
      style={{
        borderRadius: 12,
        border: '1px solid var(--ci-border)',
        background: 'var(--ci-bg-surface)',
        boxShadow: 'var(--ci-shadow-sm)',
        ...style,
      }}
      bodyStyle={{ padding: 20 }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 14 }}>
        <div>
          <Text strong style={{ fontSize: 14, color: 'var(--ci-text-primary)', display: 'block' }}>
            {title}
          </Text>
          {subtitle && (
            <Text style={{ fontSize: 12, color: 'var(--ci-text-muted)' }}>{subtitle}</Text>
          )}
        </div>
        {extra}
      </div>

      <div style={{ width: '100%', height }}>
        {loading ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
            <Spin />
          </div>
        ) : isEmpty ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
            <Empty description={<Text style={{ color: 'var(--ci-text-muted)', fontSize: 12 }}>No data</Text>} />
          </div>
        ) : children}
      </div>
    </Card>
  );
}

/** Shared chart color palette — Color Hunt accents + primary blue. */
export const CHART_COLORS = {
  primary:  '#2563eb',
  success:  '#3CB371',
  warn:     '#F8E16C',
  danger:   '#F45B69',
  info:     '#526D82',
  accent1:  '#9333ea',
  accent2:  '#d97706',
  accent3:  '#0891b2',
  muted:    '#9DB2BF',
};

/** Sequential palette for categorical charts (bars, pies). */
export const CHART_PALETTE = [
  '#2563eb', '#3CB371', '#F45B69', '#F8E16C',
  '#9333ea', '#0891b2', '#d97706', '#526D82',
  '#F97316', '#14B8A6',
];
