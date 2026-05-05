import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Chip from './Chip';

describe('Chip', () => {
  // ── rendering ──────────────────────────────────────────────────────────
  it('renders its children', () => {
    render(<Chip>Filter</Chip>);
    expect(screen.getByText('Filter')).toBeInTheDocument();
  });

  it('renders a <button> element', () => {
    const { container } = render(<Chip>X</Chip>);
    expect(container.querySelector('button')).not.toBeNull();
  });

  it('has type="button" so it does not submit forms', () => {
    const { container } = render(<Chip>X</Chip>);
    expect(container.querySelector('button')?.type).toBe('button');
  });

  // ── dropdown arrow ─────────────────────────────────────────────────────
  it('renders a chevron icon when dropdown=true', () => {
    const { container } = render(<Chip dropdown>Status</Chip>);
    expect(container.querySelector('svg')).not.toBeNull();
  });

  it('renders no chevron icon when dropdown is omitted', () => {
    const { container } = render(<Chip>Status</Chip>);
    expect(container.querySelector('svg')).toBeNull();
  });

  // ── active state ───────────────────────────────────────────────────────
  it('renders in active state without crashing', () => {
    render(<Chip active>Active Filter</Chip>);
    expect(screen.getByText('Active Filter')).toBeInTheDocument();
  });

  it('renders in inactive state without crashing', () => {
    render(<Chip active={false}>Inactive</Chip>);
    expect(screen.getByText('Inactive')).toBeInTheDocument();
  });

  // ── click handler ──────────────────────────────────────────────────────
  it('calls onClick when clicked', async () => {
    const handler = vi.fn();
    render(<Chip onClick={handler}>Click me</Chip>);
    await userEvent.click(screen.getByText('Click me'));
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it('does not throw when clicked with no onClick prop', async () => {
    render(<Chip>No handler</Chip>);
    await userEvent.click(screen.getByText('No handler'));
    // no assertion needed — just must not throw
  });

  // ── style override ─────────────────────────────────────────────────────
  it('applies extra inline styles via the style prop', () => {
    const { container } = render(<Chip style={{ marginTop: 8 }}>Styled</Chip>);
    const btn = container.querySelector('button') as HTMLElement;
    expect(btn.style.marginTop).toBe('8px');
  });
});
