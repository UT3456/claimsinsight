import type { ReactNode } from 'react';
import { Inbox } from 'lucide-react';

/**
 * Empty state — centered, icon circle + title + subtitle + up-to-two ghost actions.
 * Per spec §Universal rule 1: never show an empty state AND an error banner at the same time.
 * Per spec §Empty state: 48px gray/green circle, 14px title, 12px muted subtitle (max 340px wide).
 *
 * Tone:
 *   neutral — gray circle (default "no data yet")
 *   positive — green circle (success: "all clear", "all resolved")
 */
export interface EmptyStateProps {
  title: string;
  description?: ReactNode;
  icon?: ReactNode;
  tone?: 'neutral' | 'positive';
  actions?: ReactNode;   // one or two ghost buttons
  style?: React.CSSProperties;
}

export default function EmptyState({
  title, description, icon, tone = 'neutral', actions, style,
}: EmptyStateProps) {
  const iconBg = tone === 'positive' ? 'var(--ci-success-bg)' : '#EFEDE6';
  const iconFg = tone === 'positive' ? 'var(--ci-success-text)' : 'var(--ci-text-muted)';

  return (
    <div style={{ ...wrap, ...style }}>
      <div style={{ ...circle, background: iconBg, color: iconFg }}>
        {icon ?? <Inbox size={22} strokeWidth={1.6} />}
      </div>
      <div style={titleStyle}>{title}</div>
      {description && <div style={descStyle}>{description}</div>}
      {actions && <div style={actionsStyle}>{actions}</div>}
    </div>
  );
}

const wrap: React.CSSProperties = {
  display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center',
  padding: '32px 16px', gap: 10,
};
const circle: React.CSSProperties = {
  width: 48, height: 48, borderRadius: '50%',
  display: 'flex', alignItems: 'center', justifyContent: 'center',
};
const titleStyle: React.CSSProperties = { fontSize: 14, fontWeight: 500, color: 'var(--ci-text-primary)' };
const descStyle:  React.CSSProperties = { fontSize: 12, color: 'var(--ci-text-muted)', maxWidth: 340, lineHeight: 1.55 };
const actionsStyle: React.CSSProperties = { display: 'flex', gap: 8, marginTop: 4 };
