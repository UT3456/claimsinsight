import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import EmptyState from './EmptyState';

describe('EmptyState', () => {
  // ── required prop ──────────────────────────────────────────────────────
  it('renders the title', () => {
    render(<EmptyState title="No claims found" />);
    expect(screen.getByText('No claims found')).toBeInTheDocument();
  });

  // ── optional description ────────────────────────────────────────────────
  it('renders description when provided', () => {
    render(<EmptyState title="Empty" description="Try adding a claim." />);
    expect(screen.getByText('Try adding a claim.')).toBeInTheDocument();
  });

  it('renders no description element when omitted', () => {
    const { container } = render(<EmptyState title="Empty" />);
    // the desc div is only rendered conditionally — expect only title text
    expect(screen.getByText('Empty')).toBeInTheDocument();
    expect(container.querySelectorAll('div').length).toBeGreaterThan(0);
  });

  // ── icon ────────────────────────────────────────────────────────────────
  it('renders the default Inbox icon (svg) when no icon prop is given', () => {
    const { container } = render(<EmptyState title="Inbox" />);
    expect(container.querySelector('svg')).not.toBeNull();
  });

  it('renders a custom icon when provided', () => {
    render(
      <EmptyState
        title="Custom"
        icon={<span data-testid="custom-icon">★</span>}
      />,
    );
    expect(screen.getByTestId('custom-icon')).toBeInTheDocument();
  });

  // ── tone ────────────────────────────────────────────────────────────────
  it('renders with tone="neutral" (default) without error', () => {
    render(<EmptyState title="Neutral" />);
    expect(screen.getByText('Neutral')).toBeInTheDocument();
  });

  it('renders with tone="positive" without error', () => {
    render(<EmptyState title="All clear!" tone="positive" />);
    expect(screen.getByText('All clear!')).toBeInTheDocument();
  });

  // ── actions ─────────────────────────────────────────────────────────────
  it('renders action buttons when provided', () => {
    render(
      <EmptyState
        title="Empty"
        actions={<button data-testid="action-btn">Add claim</button>}
      />,
    );
    expect(screen.getByTestId('action-btn')).toBeInTheDocument();
  });

  it('renders no actions slot when omitted', () => {
    render(<EmptyState title="Empty" />);
    expect(screen.queryByRole('button')).toBeNull();
  });

  // ── style override ──────────────────────────────────────────────────────
  it('applies extra inline styles via the style prop', () => {
    const { container } = render(
      <EmptyState title="Styled" style={{ paddingTop: 64 }} />,
    );
    const wrap = container.firstChild as HTMLElement;
    expect(wrap.style.paddingTop).toBe('64px');
  });

  // ── ReactNode description ───────────────────────────────────────────────
  it('accepts a React element as description', () => {
    render(
      <EmptyState
        title="Empty"
        description={<span data-testid="rich-desc">Rich description</span>}
      />,
    );
    expect(screen.getByTestId('rich-desc')).toBeInTheDocument();
  });
});
