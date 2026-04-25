// basic avatar with initials

export function initialsOf(name: string): string {
  if (!name) return '?';
  const parts = name.trim().split(' ');
  if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export interface TintedAvatarProps {
  name: string;
  colorKey?: string; // not used anymore
  size?: number;
  style?: React.CSSProperties;
}

export default function TintedAvatar({ name, size = 24, style }: TintedAvatarProps) {
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: size,
        height: size,
        borderRadius: '50%',
        background: '#ddd',
        color: '#333',
        fontSize: 11,
        ...style,
      }}
    >
      {initialsOf(name)}
    </span>
  );
}
