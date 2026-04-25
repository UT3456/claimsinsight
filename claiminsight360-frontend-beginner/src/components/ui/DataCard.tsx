import type { ReactNode } from 'react';

/**
 * Data card — the wrapper used for charts, lists, tables.
 * Per spec: bg #fff, 1px solid #E8E6DF, radius 10, padding 14/16.
 * Head row: title left, subtitle / link / legend right.
 */
export interface DataCardProps {
  title?: ReactNode;
  subtitle?: ReactNode;
  headerRight?: ReactNode;   // legend, link, chip — anything that sits on the right of head row
  padding?: number | string; // allow override for tables that render flush
  children: ReactNode;
  style?: React.CSSProperties;
}

export default function DataCard({
  title, subtitle, headerRight, padding = '14px 16px', children, style,
}: DataCardProps) {
  const hasHead = title || subtitle || headerRight;
  return (
    <div style={{ ...styles.card, padding, ...style }}>
      {hasHead && (
        <div style={styles.head}>
          <div style={styles.headLeft}>
            {title    && <div className="ci360-section-title">{title}</div>}
            {subtitle && <div className="ci360-page-sub" style={{ marginTop: 2 }}>{subtitle}</div>}
          </div>
          {headerRight && <div style={styles.headRight}>{headerRight}</div>}
        </div>
      )}
      {children}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  card:      { background: 'var(--ci-bg-surface)', border: '1px solid var(--ci-border)',
               borderRadius: 'var(--ci-radius-card)' },
  head:      { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
               gap: 12, marginBottom: 12 },
  headLeft:  { minWidth: 0 },
  headRight: { display: 'flex', alignItems: 'center', gap: 10, flexShrink: 0 },
};
