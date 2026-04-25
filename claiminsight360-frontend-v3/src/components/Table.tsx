interface TableProps {
  headers: string[];
  rows: (string | React.ReactNode)[][];
  onRowClick?: (rowIndex: number) => void;
}

export const Table: React.FC<TableProps> = ({ headers, rows, onRowClick }) => {
  return (
    <table className="table">
      <thead>
        <tr>
          {headers.map((header, idx) => (
            <th key={idx}>{header}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, rowIdx) => (
          <tr key={rowIdx} onClick={() => onRowClick?.(rowIdx)} style={onRowClick ? { cursor: 'pointer' } : {}}>
            {row.map((cell, cellIdx) => (
              <td key={cellIdx}>{cell}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
};
