/**
 * Minimal SVG bar chart matching the reference design.
 * No external libraries — just a lightweight visual.
 */
export interface SimpleBarDatum {
  name:  string;
  value: number;
  color?: string;
}

interface Props {
  data:     SimpleBarDatum[];
  height?:  number;
  /** Y axis ticks shown on the left (top to bottom). Leave empty for none. */
  yTicks?:  string[];
}

export default function SimpleBar({ data, height = 130, yTicks = [] }: Props) {
  const width     = 300;
  const leftPad   = 30;
  const bottomPad = 25;
  const topPad    = 10;
  const plotH     = height - bottomPad - topPad;

  const max = Math.max(...data.map(d => d.value), 1);
  const barW = Math.min(32, (width - leftPad - 20) / data.length - 8);
  const step = (width - leftPad - 20) / data.length;

  return (
    <svg viewBox={`0 0 ${width} ${height}`} style={{ width: '100%', height }}>
      {/* axes */}
      <line x1={leftPad} y1={height - bottomPad} x2={width - 10} y2={height - bottomPad} stroke="#ccc" strokeWidth="1" />
      <line x1={leftPad} y1={topPad}             x2={leftPad}    y2={height - bottomPad} stroke="#ccc" strokeWidth="1" />

      {/* y ticks */}
      {yTicks.map((t, i) => {
        const y = topPad + (plotH / Math.max(yTicks.length - 1, 1)) * i;
        return (
          <text key={t + i} x={leftPad - 6} y={y + 3} textAnchor="end" fontSize="9" fill="#888">{t}</text>
        );
      })}

      {/* bars */}
      {data.map((d, i) => {
        const h = (d.value / max) * plotH;
        const x = leftPad + 20 + i * step;
        const y = height - bottomPad - h;
        return (
          <g key={d.name + i}>
            <rect x={x} y={y} width={barW} height={h} fill={d.color ?? '#2563eb'} />
            <text x={x + barW / 2} y={height - bottomPad + 12} textAnchor="middle" fontSize="9" fill="#666">
              {d.name}
            </text>
          </g>
        );
      })}
    </svg>
  );
}
