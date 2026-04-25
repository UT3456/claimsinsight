import type { ReactNode } from 'react';
import { ArrowUp, ArrowDown } from 'lucide-react';

/**
 * KPI card — label, value, delta line.
 * Per spec: bg #F8F7F3, no border, radius 8, padding 12/14.
 *
 * Delta semantics:
 *   - direction='up'/'down' renders the arrow
 *   - tone='up' (green) = improvement, tone='down' (red) = deterioration
 *     (these are independent: a rising number can still be a 'down' tone)
 *   - if delta is missing, we render "No change" in muted gray
 *
 * `tone='danger' | 'warning'` also paints the whole card tinted (used on Fraud & Risk).
 */
export interface KpiCardProps {
  label: string;
  value: ReactNode;
  delta?: ReactNode;
  deltaDirection?: 'up' | 'down' | 'flat';
  deltaTone?: 'up' | 'down';
  tone?: 'default' | 'danger' | 'warning';
}

export default function KpiCard({
  label, value, delta, deltaDirection, deltaTone, tone = 'default',
}: KpiCardProps) {
  const toneStyles = TONE[tone];
  const deltaColor = !delta
    ? 'var(--ci-text-muted)'
    : deltaTone === 'up'   ? 'var(--ci-success-text)'
    : deltaTone === 'down' ? 'var(--ci-danger-text)'
    : 'var(--ci-text-muted)';

  const Icon = deltaDirection === 'up' ? ArrowUp
             : deltaDirection === 'down' ? ArrowDown
             : null;

  return (
    <div style={{ ...styles.card, ...toneStyles.card }}>
      <div style={{ ...styles.label, color: toneStyles.labelColor }}>{label}</div>
      <div style={{ ...styles.value, color: toneStyles.valueColor }}>{value}</div>
      <div style={{ ...styles.delta, color: deltaColor }}>
        {Icon && <Icon size={11} strokeWidth={2} />}
        {delta ?? 'No change'}
      </div>
    </div>
  );
}

const TONE = {
  default: { card: { background: 'var(--ci-bg-surface-2)' },
             labelColor: 'var(--ci-text-secondary)', valueColor: 'var(--ci-text-primary)' },
  danger:  { card: { background: 'var(--ci-danger-bg)' },
             labelColor: 'var(--ci-danger-text)',    valueColor: 'var(--ci-danger-text)' },
  warning: { card: { background: 'var(--ci-warning-bg)' },
             labelColor: 'var(--ci-warning-text)',   valueColor: 'var(--ci-warning-text)' },
} as const;

const styles: Record<string, React.CSSProperties> = {
  card:  { borderRadius: 8, padding: '12px 14px', border: 'none', minWidth: 0 },
  label: { fontSize: 11, marginBottom: 6, lineHeight: 1.3 },
  value: { fontSize: 22, fontWeight: 500, lineHeight: 1.1, letterSpacing: '-0.2px' },
  delta: { fontSize: 11, marginTop: 4, display: 'inline-flex', alignItems: 'center', gap: 3 },
};
