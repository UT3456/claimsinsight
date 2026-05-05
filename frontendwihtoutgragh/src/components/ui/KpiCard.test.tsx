import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import KpiCard from './KpiCard';

describe('KpiCard', () => {
  // ── basic rendering ──────────────────────────────────────────────────────
  it('renders the label', () => {
    render(<KpiCard label="TAT" value="42 days" />);
    expect(screen.getByText('TAT')).toBeInTheDocument();
  });

  it('renders the value', () => {
    render(<KpiCard label="TAT" value="42 days" />);
    expect(screen.getByText('42 days')).toBeInTheDocument();
  });

  // ── delta ────────────────────────────────────────────────────────────────
  it('renders the delta text when provided', () => {
    render(<KpiCard label="Loss Ratio" value="1.2" delta="+0.2 vs last month" />);
    expect(screen.getByText('+0.2 vs last month')).toBeInTheDocument();
  });

  it('renders "No change" when delta is omitted', () => {
    render(<KpiCard label="Frequency" value="5" />);
    expect(screen.getByText('No change')).toBeInTheDocument();
  });

  it('renders ArrowUp icon when deltaDirection="up"', () => {
    const { container } = render(
      <KpiCard label="KPI" value="10" delta="+5%" deltaDirection="up" />,
    );
    // lucide renders an svg
    const svg = container.querySelector('svg');
    expect(svg).not.toBeNull();
  });

  it('renders ArrowDown icon when deltaDirection="down"', () => {
    const { container } = render(
      <KpiCard label="KPI" value="10" delta="-5%" deltaDirection="down" />,
    );
    const svg = container.querySelector('svg');
    expect(svg).not.toBeNull();
  });

  it('renders no icon when deltaDirection="flat"', () => {
    const { container } = render(
      <KpiCard label="KPI" value="10" delta="flat" deltaDirection="flat" />,
    );
    expect(container.querySelector('svg')).toBeNull();
  });

  it('renders no icon when deltaDirection is omitted', () => {
    const { container } = render(<KpiCard label="KPI" value="10" delta="stable" />);
    expect(container.querySelector('svg')).toBeNull();
  });

  // ── tone ─────────────────────────────────────────────────────────────────
  it('renders with tone="default" without error', () => {
    render(<KpiCard label="L" value="V" tone="default" />);
    expect(screen.getByText('L')).toBeInTheDocument();
  });

  it('renders with tone="danger" without error', () => {
    render(<KpiCard label="Fraud Score" value="85" tone="danger" />);
    expect(screen.getByText('Fraud Score')).toBeInTheDocument();
    expect(screen.getByText('85')).toBeInTheDocument();
  });

  it('renders with tone="warning" without error', () => {
    render(<KpiCard label="Risk Level" value="Medium" tone="warning" />);
    expect(screen.getByText('Risk Level')).toBeInTheDocument();
  });

  // ── ReactNode values ────────────────────────────────────────────────────
  it('accepts a React element as the value prop', () => {
    render(
      <KpiCard
        label="Severity"
        value={<strong data-testid="strong-val">8.5</strong>}
      />,
    );
    expect(screen.getByTestId('strong-val')).toBeInTheDocument();
    expect(screen.getByText('8.5')).toBeInTheDocument();
  });
});
