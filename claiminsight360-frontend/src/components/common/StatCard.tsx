import type { CSSProperties } from 'react';

export type StatTone = 'up' | 'down' | 'muted' | 'danger' | 'warn' | 'default';

export interface StatCardProps {
  label:  string;
  value:  string | number;
  sub?:   string;
  tone?:  StatTone;
  /** Visual variant — 'default' grey, 'red' / 'yellow' tinted for dangerous stats */
  variant?: 'default' | 'red' | 'yellow';
  style?: CSSProperties;
}

const TONE_COLOR: Record<StatTone, string> = {
  up:      '#16a34a',
  down:    '#dc2626',
  muted:   '#888',
  danger:  '#991b1b',
  warn:    '#854d0e',
  default: '#888',
};

const VARIANT_STYLE: Record<NonNullable<StatCardProps['variant']>, CSSProperties> = {
  default: {},
  red:     { background: '#fee2e2', borderColor: '#fecaca' },
  yellow:  { background: '#fef3c7', borderColor: '#fde68a' },
};

const VARIANT_LABEL_COLOR: Record<NonNullable<StatCardProps['variant']>, string | undefined> = {
  default: undefined,
  red:     '#991b1b',
  yellow:  '#854d0e',
};

const VARIANT_VAL_COLOR: Record<NonNullable<StatCardProps['variant']>, string | undefined> = {
  default: undefined,
  red:     '#501313',
  yellow:  '#582f0e',
};

export default function StatCard({ label, value, sub, tone = 'default', variant = 'default', style }: StatCardProps) {
  return (
    <div className="ci-stat" style={{ ...VARIANT_STYLE[variant], ...style }}>
      <div className="ci-stat-lbl" style={{ color: VARIANT_LABEL_COLOR[variant] }}>{label}</div>
      <div className="ci-stat-val" style={{ color: VARIANT_VAL_COLOR[variant], fontSize: 20 }}>{value}</div>
      {sub && (
        <div style={{ fontSize: 10, marginTop: 2, color: VARIANT_LABEL_COLOR[variant] ?? TONE_COLOR[tone] }}>
          {sub}
        </div>
      )}
    </div>
  );
}
