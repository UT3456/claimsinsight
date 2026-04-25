import type { ReactNode } from 'react';

/**
 * Page header — every inner screen uses this.
 *
 * Layout (per DESIGN_SPEC §Component patterns):
 *   [Title 16/500]                  [chip] [chip] [primary btn]
 *   [Subtitle 11/400 muted]
 */
export interface PageHeaderProps {
  title: string;
  subtitle?: ReactNode;
  actions?: ReactNode;   // chips + primary button on the right
}

export default function PageHeader({ title, subtitle, actions }: PageHeaderProps) {
  return (
    <div style={styles.wrap}>
      <div style={styles.left}>
        <div className="ci360-page-title">{title}</div>
        {subtitle && <div className="ci360-page-sub" style={{ marginTop: 2 }}>{subtitle}</div>}
      </div>
      {actions && <div style={styles.right}>{actions}</div>}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  wrap:  { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
           gap: 12, marginBottom: 16, flexWrap: 'wrap' },
  left:  { minWidth: 0 },
  right: { display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' },
};
