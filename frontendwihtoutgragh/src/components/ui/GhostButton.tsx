import { forwardRef, type ReactNode, type ButtonHTMLAttributes } from 'react';

/**
 * Ghost button — white bg, 1px border, used in empty-state actions and secondary controls.
 * Per spec §Component patterns: ghost is `#fff bg + 1px solid #D3D1C7`.
 *
 * Forwards all native <button> props + ref so parents like AntD <Dropdown>
 * can inject click/hover handlers and reference the DOM node.
 */
export interface GhostButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode;
  icon?: ReactNode;
}

const GhostButton = forwardRef<HTMLButtonElement, GhostButtonProps>(
  function GhostButton({ children, icon, style, disabled, type = 'button', ...rest }, ref) {
    return (
      <button
        ref={ref}
        type={type}
        disabled={disabled}
        style={{ ...base, ...(disabled ? disabledStyle : {}), ...style }}
        {...rest}
      >
        {icon}
        {children}
      </button>
    );
  },
);

export default GhostButton;

const base: React.CSSProperties = {
  display: 'inline-flex', alignItems: 'center', gap: 6,
  padding: '6px 12px', borderRadius: 'var(--ci-radius-btn)',
  fontSize: 12, fontWeight: 500, lineHeight: 1.3,
  background: 'var(--ci-bg-surface)',
  border: '1px solid var(--ci-border-strong)',
  color: 'var(--ci-text-primary)',
  cursor: 'pointer',
};
const disabledStyle: React.CSSProperties = { opacity: 0.5, cursor: 'not-allowed' };
