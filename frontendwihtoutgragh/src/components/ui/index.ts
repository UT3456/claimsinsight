export { default as PageHeader }    from './PageHeader';
export { default as KpiCard }       from './KpiCard';
export { default as DataCard }      from './DataCard';
export { default as Badge }         from './Badge';
export { default as Chip }          from './Chip';
export { default as StatusDot }     from './StatusDot';
export { default as TintedAvatar, initialsOf } from './TintedAvatar';
export { default as EmptyState }    from './EmptyState';
export { default as ErrorState }    from './ErrorState';
export { default as GhostButton }   from './GhostButton';
export { default as DarkButton }    from './DarkButton';

export type { PageHeaderProps }   from './PageHeader';
export type { KpiCardProps }      from './KpiCard';
export type { DataCardProps }     from './DataCard';
export type { BadgeProps, BadgeTone } from './Badge';
export type { ChipProps }         from './Chip';
export type { StatusDotProps, StatusTone } from './StatusDot';
export type { TintedAvatarProps } from './TintedAvatar';
export type { EmptyStateProps }   from './EmptyState';
export type { ErrorStateProps }   from './ErrorState';
export type { GhostButtonProps }  from './GhostButton';
export type { DarkButtonProps }   from './DarkButton';

/** Chart palette — use for Recharts strokes/fills to match the spec. */
export const CHART = {
  blue:   '#378ADD',
  teal:   '#5DCAA5',
  amber:  '#EF9F27',
  red:    '#E24B4A',
  purple: '#7F77DD',
  coral:  '#D85A30',
} as const;

export const CHART_PALETTE = [CHART.blue, CHART.teal, CHART.amber, CHART.red, CHART.purple, CHART.coral];
