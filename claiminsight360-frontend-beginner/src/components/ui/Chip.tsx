import type { ReactNode } from 'react';
import { ChevronDown } from 'lucide-react';

/**
 * Chip — interactive filter control.
 * Per spec: 11px, radius 6, 1px border, ghost bg. Dropdown arrow when it opens a picker.
 *
 * Variants:
 *   default — border #D3D1C7, white bg, secondary text
 *   active  — blue-tinted (used in Reports left rail)
 */
export interface ChipProps {
  children: ReactNode;
  active?: boolean;
  dropdown?: boolean;
  onClick?: () => void;
  style?: React.CSSProperties;
}

export default function Chip({ children, active, dropdown, onClick, style }: ChipProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      style={{
        ...base,
        ...(active ? activeStyle : defaultStyle),
        cursor: onClick ? 'pointer' : 'default',
        ...style,
      }}
    >
      <span>{children}</span>
      {dropdown && <ChevronDown size={12} strokeWidth={1.6} style={{ marginLeft: 2 }} />}
    </button>
  );
}

const base: React.CSSProperties = {
  display: 'inline-flex', alignItems: 'center', gap: 4,
  padding: '5px 10px', borderRadius: 'var(--ci-radius-chip)',
  fontSize: 11, fontWeight: 400, lineHeight: 1.3, whiteSpace: 'nowrap',
  background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border-strong)',
  color: 'var(--ci-text-secondary)',
};

const defaultStyle: React.CSSProperties = {};
const activeStyle: React.CSSProperties = {
  background: 'var(--ci-info-bg)',
  borderColor: 'var(--ci-info-bg)',
  color: 'var(--ci-info-text)',
  fontWeight: 500,
};
