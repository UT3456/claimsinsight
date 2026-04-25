/**
 * Tinted avatar — 24px circle with initials, color stable per person across screens.
 * Per spec §Universal rule 6: tables use tinted avatars; bg + text from the same ramp.
 *
 * Ramp (background / text):
 *   amber   #FAEEDA / #633806
 *   blue    #E6F1FB / #0C447C
 *   green   #EAF3DE / #27500A
 *   purple  #EEEBF9 / #3B2E7F
 *   teal    #E0F2EC / #084D3D
 *   red     #FCEBEB / #791F1F
 */
const RAMP = [
  { bg: '#FAEEDA', fg: '#633806' }, // amber
  { bg: '#E6F1FB', fg: '#0C447C' }, // blue
  { bg: '#EAF3DE', fg: '#27500A' }, // green
  { bg: '#EEEBF9', fg: '#3B2E7F' }, // purple
  { bg: '#E0F2EC', fg: '#084D3D' }, // teal
  { bg: '#FCEBEB', fg: '#791F1F' }, // red
];

/** djb2 hash — deterministic color pick per key. */
function hash(key: string): number {
  let h = 5381;
  for (let i = 0; i < key.length; i++) h = ((h << 5) + h + key.charCodeAt(i)) | 0;
  return Math.abs(h);
}

export function initialsOf(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export interface TintedAvatarProps {
  name: string;
  /** Key that determines the color. Defaults to `name` — pass userId/email for stability across rename. */
  colorKey?: string;
  size?: number;
  style?: React.CSSProperties;
}

export default function TintedAvatar({ name, colorKey, size = 24, style }: TintedAvatarProps) {
  const { bg, fg } = RAMP[hash(colorKey ?? name) % RAMP.length];
  return (
    <span
      style={{
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        width: size, height: size, borderRadius: '50%',
        background: bg, color: fg, fontWeight: 500,
        fontSize: Math.round(size * 0.42), lineHeight: 1,
        flexShrink: 0, ...style,
      }}
      aria-label={name}
    >
      {initialsOf(name)}
    </span>
  );
}
