import type { ReactNode } from 'react';

/**
 * Badge — inline state/category pill for tables.
 * Per spec: 11px, radius 10, no border, tinted bg + matching text.
 */
export type BadgeTone = 'green' | 'amber' | 'red' | 'blue' | 'neutral' | 'purple' | 'teal';

export interface BadgeProps {
  tone?: BadgeTone;
  children: ReactNode;
  style?: React.CSSProperties;
}

const TONES: Record<BadgeTone, React.CSSProperties> = {
  green:   { background: 'var(--ci-success-bg)', color: 'var(--ci-success-text)' },
  amber:   { background: 'var(--ci-warning-bg)', color: 'var(--ci-warning-text)' },
  red:     { background: 'var(--ci-danger-bg)',  color: 'var(--ci-danger-text)' },
  blue:    { background: 'var(--ci-info-bg)',    color: 'var(--ci-info-text)' },
  purple:  { background: '#EEEBF9',              color: '#3B2E7F' },
  teal:    { background: '#E0F2EC',              color: '#084D3D' },
  neutral: { background: '#EFEDE6',              color: '#5F5E5A' },
};

export default function Badge({ tone = 'neutral', children, style }: BadgeProps) {
  return (
    <span style={{ ...base, ...TONES[tone], ...style }}>{children}</span>
  );
}

const base: React.CSSProperties = {
  display: 'inline-block', padding: '2px 9px', borderRadius: 10,
  fontSize: 11, fontWeight: 400, lineHeight: 1.4, whiteSpace: 'nowrap',
};
