import type { ReactNode } from 'react';

/**
 * Dark button — the "Export" / "Apply" style from the reference.
 * Per spec §Component patterns: primary is #2C2C2A bg + white text,
 * OR #185FA5 for the main CTA. This component covers the #2C2C2A variant.
 * For the blue CTA, use AntD <Button type="primary"> which inherits colorPrimary=#185FA5.
 */
export interface DarkButtonProps {
  children: ReactNode;
  onClick?: () => void;
  icon?: ReactNode;
  style?: React.CSSProperties;
  disabled?: boolean;
  size?: 'sm' | 'md';
}

export default function DarkButton({
  children, onClick, icon, style, disabled, size = 'sm',
}: DarkButtonProps) {
  const pad = size === 'md' ? '7px 14px' : '5px 12px';
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      style={{ ...base, padding: pad, ...(disabled ? disabledStyle : {}), ...style }}
    >
      {icon}
      {children}
    </button>
  );
}

const base: React.CSSProperties = {
  // High-contrast inverted button. Uses CSS variables so the page's foreground
  // becomes the button's background — works in both light and dark themes.
  display: 'inline-flex', alignItems: 'center', gap: 6,
  borderRadius: 'var(--ci-radius-btn)',
  fontSize: 11, fontWeight: 500, lineHeight: 1.3,
  background: 'var(--ci-text-primary)',
  border: '1px solid var(--ci-text-primary)',
  color: 'var(--ci-bg-app)',
  cursor: 'pointer',
};
const disabledStyle: React.CSSProperties = { opacity: 0.5, cursor: 'not-allowed' };
