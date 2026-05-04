import type { ReactNode } from 'react';
import { AlertTriangle, RotateCw } from 'lucide-react';
import GhostButton from './GhostButton';

/**
 * Error state — paired sibling of EmptyState. Renders inside a DataCard or
 * full panel when a fetch fails. Optional retry slot.
 *
 * Spec: 48px red-tinted icon circle, 14px title, 12px muted message,
 * single ghost retry button (max 340px width).
 */
export interface ErrorStateProps {
  title?: string;
  message?: ReactNode;
  onRetry?: () => void;
  retryLabel?: string;
  icon?: ReactNode;
  style?: React.CSSProperties;
}

export default function ErrorState({
  title = 'Something went wrong',
  message,
  onRetry,
  retryLabel = 'Try again',
  icon,
  style,
}: ErrorStateProps) {
  return (
    <div style={{ ...wrap, ...style }}>
      <div style={circle}>
        {icon ?? <AlertTriangle size={22} strokeWidth={1.6} />}
      </div>
      <div style={titleStyle}>{title}</div>
      {message && <div style={descStyle}>{message}</div>}
      {onRetry && (
        <div style={actionsStyle}>
          <GhostButton onClick={onRetry} icon={<RotateCw size={13} strokeWidth={1.7} />}>
            {retryLabel}
          </GhostButton>
        </div>
      )}
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
  background: 'var(--ci-danger-bg, #FCEBEA)',
  color: 'var(--ci-danger-text, #B83A39)',
};
const titleStyle: React.CSSProperties = { fontSize: 14, fontWeight: 500, color: 'var(--ci-text-primary)' };
const descStyle:  React.CSSProperties = { fontSize: 12, color: 'var(--ci-text-muted)', maxWidth: 340, lineHeight: 1.55 };
const actionsStyle: React.CSSProperties = { display: 'flex', gap: 8, marginTop: 4 };
