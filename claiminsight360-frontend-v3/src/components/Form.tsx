interface FormGroupProps {
  label?: string;
  error?: string;
  hint?: string;
  children: React.ReactNode;
}

export const FormGroup: React.FC<FormGroupProps> = ({ label, error, hint, children }) => {
  return (
    <div className="form-group">
      {label && <label>{label}</label>}
      {children}
      {hint && <div className="form-hint">{hint}</div>}
      {error && <div className="form-error">{error}</div>}
    </div>
  );
};

interface FormInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const FormInput: React.FC<FormInputProps> = ({ label, error, hint, ...props }) => {
  return (
    <FormGroup label={label} error={error} hint={hint}>
      <input {...props} />
    </FormGroup>
  );
};

interface FormSelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  hint?: string;
  options: Array<{ value: string; label: string }>;
}

export const FormSelect: React.FC<FormSelectProps> = ({ label, error, hint, options, ...props }) => {
  return (
    <FormGroup label={label} error={error} hint={hint}>
      <select {...props}>
        <option value="">Select an option</option>
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
    </FormGroup>
  );
};

interface FormTextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const FormTextarea: React.FC<FormTextareaProps> = ({ label, error, hint, ...props }) => {
  return (
    <FormGroup label={label} error={error} hint={hint}>
      <textarea {...props} />
    </FormGroup>
  );
};
