/** Minimal SVG donut/pie matching the reference design. */
export interface SimpleDonutSlice {
  label: string;
  value: number;
  color: string;
}

interface Props {
  data:    SimpleDonutSlice[];
  size?:   number;
  /** Inner hole radius, 0 for full pie */
  inner?:  number;
}

export default function SimpleDonut({ data, size = 110, inner = 0 }: Props) {
  const total = data.reduce((s, d) => s + d.value, 0) || 1;
  const cx = size / 2;
  const cy = size / 2;
  const r  = size / 2 - 2;

  let angle = -Math.PI / 2; // start at top
  const slices = data.map(d => {
    const a0 = angle;
    const a1 = angle + (d.value / total) * Math.PI * 2;
    angle = a1;
    const x0 = cx + r * Math.cos(a0);
    const y0 = cy + r * Math.sin(a0);
    const x1 = cx + r * Math.cos(a1);
    const y1 = cy + r * Math.sin(a1);
    const large = a1 - a0 > Math.PI ? 1 : 0;
    return { ...d, path: `M ${cx} ${cy} L ${x0} ${y0} A ${r} ${r} 0 ${large} 1 ${x1} ${y1} Z` };
  });

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
      <svg viewBox={`0 0 ${size} ${size}`} style={{ width: size, height: size, flexShrink: 0 }}>
        {slices.map((s, i) => <path key={i} d={s.path} fill={s.color} />)}
        {inner > 0 && <circle cx={cx} cy={cy} r={inner} fill="#fff" />}
      </svg>
      <div>
        {data.map(d => {
          const pct = Math.round((d.value / total) * 100);
          return (
            <div key={d.label} style={{ fontSize: 12, marginBottom: 4 }}>
              <span style={{
                display: 'inline-block', width: 10, height: 10,
                marginRight: 6, verticalAlign: 'middle', background: d.color,
              }} />
              {d.label} {pct}%
            </div>
          );
        })}
      </div>
    </div>
  );
}
