interface AlertProps {
  type?: 'success' | 'warning' | 'danger' | 'info';
  children: React.ReactNode;
  onClose?: () => void;
}

export const Alert: React.FC<AlertProps> = ({ type = 'info', children, onClose }) => {
  return (
    <div className={`alert alert-${type}`}>
      <div className="flex-1">{children}</div>
      {onClose && (
        <button onClick={onClose} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
          ✕
        </button>
      )}
    </div>
  );
};
