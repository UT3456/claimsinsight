import type { CSSProperties } from 'react';

interface Props {
  label: string;
  right: string | number;
  /** 0–100 */
  pct:   number;
  color?: string;
  style?: CSSProperties;
}

export default function ProgressBar({ label, right, pct, color = '#2563eb', style }: Props) {
  return (
    <div className="ci-bar-wrap" style={style}>
      <div className="ci-bar-head">
        <span>{label}</span>
        <span style={{ color, fontWeight: 600 }}>{right}</span>
      </div>
      <div className="ci-bar">
        <div className="ci-bar-fill" style={{ width: `${Math.min(Math.max(pct, 0), 100)}%`, background: color }} />
      </div>
    </div>
  );
}
