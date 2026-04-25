/** Minimal SVG line chart matching the reference. */
export interface SimpleLineDatum {
  label: string;
  value: number;
}

interface Props {
  data:    SimpleLineDatum[];
  height?: number;
  color?:  string;
}

export default function SimpleLine({ data, height = 120, color = '#5b21b6' }: Props) {
  const width     = 300;
  const leftPad   = 30;
  const rightPad  = 20;
  const topPad    = 10;
  const bottomPad = 25;
  const plotH     = height - bottomPad - topPad;
  const plotW     = width - leftPad - rightPad;

  const max = Math.max(...data.map(d => d.value), 1);
  const step = plotW / Math.max(data.length - 1, 1);

  const points = data.map((d, i) => {
    const x = leftPad + i * step;
    const y = topPad + (1 - d.value / max) * plotH;
    return { x, y, d };
  });

  const pathStr = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');

  return (
    <svg viewBox={`0 0 ${width} ${height}`} style={{ width: '100%', height }}>
      <line x1={leftPad} y1={height - bottomPad} x2={width - 10} y2={height - bottomPad} stroke="#ccc" strokeWidth="1" />
      <line x1={leftPad} y1={topPad}             x2={leftPad}    y2={height - bottomPad} stroke="#ccc" strokeWidth="1" />
      <line x1={leftPad} y1={topPad + plotH / 2} x2={width - 10} y2={topPad + plotH / 2} stroke="#eee" strokeWidth="1" />

      <path d={pathStr} fill="none" stroke={color} strokeWidth="2" />

      {points.map((p, i) => (
        <g key={i}>
          <circle cx={p.x} cy={p.y} r="3" fill={color} />
          <text x={p.x} y={height - bottomPad + 12} textAnchor="middle" fontSize="9" fill="#666">{p.d.label}</text>
        </g>
      ))}
    </svg>
  );
}
