import type { ReactNode } from 'react';

/**
 * Status dot + label.
 * Per spec §Universal rule 7: status is a dot-prefix, NOT a box.
 *   ● Healthy (green)  ● Lagging (amber)  ● Error (red)  ● Info (blue)
 *
 * Dot color is filled; label takes semantic text color.
 */
export type StatusTone = 'healthy' | 'warning' | 'danger' | 'info' | 'muted';

export interface StatusDotProps {
  tone: StatusTone;
  children: ReactNode;
  style?: React.CSSProperties;
}

const TONES: Record<StatusTone, { dot: string; text: string }> = {
  healthy: { dot: '#4C8521', text: 'var(--ci-success-text)' },
  warning: { dot: '#B67A1F', text: 'var(--ci-warning-text)' },
  danger:  { dot: '#C23B3B', text: 'var(--ci-danger-text)' },
  info:    { dot: '#185FA5', text: 'var(--ci-info-text)' },
  muted:   { dot: '#B4B2A9', text: 'var(--ci-text-muted)' },
};

export default function StatusDot({ tone, children, style }: StatusDotProps) {
  const t = TONES[tone];
  return (
    <span style={{ ...wrap, color: t.text, ...style }}>
      <span style={{ ...dot, background: t.dot }} />
      {children}
    </span>
  );
}

const wrap: React.CSSProperties = {
  display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 12,
};
const dot: React.CSSProperties = {
  width: 7, height: 7, borderRadius: '50%', flexShrink: 0,
};
