import type { ReactNode } from 'react';

interface Props {
  tone:  'red' | 'yellow' | 'green' | 'blue';
  children: ReactNode;
  style?: React.CSSProperties;
}

const TONE_CLS: Record<Props['tone'], string> = {
  red:    'ci-alert ci-alert-red',
  yellow: 'ci-alert ci-alert-yellow',
  blue:   'ci-alert ci-alert-blue',
  green:  'ci-alert',
};

const TONE_GREEN: React.CSSProperties = {
  background: '#dcfce7', color: '#166534', border: '1px solid #bbf7d0',
};

export default function AlertBanner({ tone, children, style }: Props) {
  return (
    <div
      className={TONE_CLS[tone]}
      style={{ ...(tone === 'green' ? TONE_GREEN : undefined), ...style }}
    >
      {children}
    </div>
  );
}
