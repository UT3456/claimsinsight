import { classNames } from '@/utils/helpers';

interface BadgeProps {
  variant?: 'primary' | 'success' | 'warning' | 'danger';
  children: React.ReactNode;
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({ variant = 'primary', children, className }) => {
  return (
    <span className={classNames(`badge badge-${variant}`, className)}>
      {children}
    </span>
  );
};
