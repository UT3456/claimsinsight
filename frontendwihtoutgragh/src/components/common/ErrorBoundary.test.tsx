import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ErrorBoundary from './ErrorBoundary';

// ── Helper: component that throws during render ────────────────────────────────
function Bomb({ shouldThrow }: { shouldThrow: boolean }) {
  if (shouldThrow) throw new Error('Test render error');
  return <div>Rendered OK</div>;
}

// Suppress React's error output to keep test logs clean
let consoleError: typeof console.error;
beforeEach(() => {
  consoleError = console.error;
  console.error = vi.fn();
});
afterEach(() => {
  console.error = consoleError;
});

// ── Normal (no-error) path ────────────────────────────────────────────────────
describe('ErrorBoundary — no error', () => {
  it('renders children when there is no error', () => {
    render(
      <ErrorBoundary>
        <Bomb shouldThrow={false} />
      </ErrorBoundary>,
    );
    expect(screen.getByText('Rendered OK')).toBeInTheDocument();
  });

  it('does not show the error panel when no error occurs', () => {
    render(
      <ErrorBoundary>
        <Bomb shouldThrow={false} />
      </ErrorBoundary>,
    );
    expect(screen.queryByText('Something went wrong')).toBeNull();
  });
});

// ── Error path ────────────────────────────────────────────────────────────────
describe('ErrorBoundary — with render error', () => {
  it('shows the "Something went wrong" heading', () => {
    render(
      <ErrorBoundary>
        <Bomb shouldThrow />
      </ErrorBoundary>,
    );
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });

  it('displays the error message in the detail block', () => {
    render(
      <ErrorBoundary>
        <Bomb shouldThrow />
      </ErrorBoundary>,
    );
    expect(screen.getByText('Test render error')).toBeInTheDocument();
  });

  it('hides the child content', () => {
    render(
      <ErrorBoundary>
        <Bomb shouldThrow />
      </ErrorBoundary>,
    );
    expect(screen.queryByText('Rendered OK')).toBeNull();
  });

  it('shows the "Try again without reload" button', () => {
    render(
      <ErrorBoundary>
        <Bomb shouldThrow />
      </ErrorBoundary>,
    );
    expect(screen.getByText('Try again without reload')).toBeInTheDocument();
  });

  it('shows the "Reload page" button', () => {
    render(
      <ErrorBoundary>
        <Bomb shouldThrow />
      </ErrorBoundary>,
    );
    expect(screen.getByText('Reload page')).toBeInTheDocument();
  });
});

// ── Reset (try-again) ─────────────────────────────────────────────────────────
describe('ErrorBoundary — reset via "Try again"', () => {
  it('clears the error state and re-renders children when they no longer throw', () => {
    // Use a closure variable to control throwing.
    // We flip it to false BEFORE clicking reset so that when the boundary
    // resets and tries to re-render its children, they succeed.
    let doThrow = true;

    function ClosureBomb() {
      if (doThrow) throw new Error('Test render error');
      return <div>Rendered OK</div>;
    }

    render(
      <ErrorBoundary>
        <ClosureBomb />
      </ErrorBoundary>,
    );
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();

    // Stop throwing before reset so the boundary recovers cleanly
    doThrow = false;
    fireEvent.click(screen.getByText('Try again without reload'));

    expect(screen.getByText('Rendered OK')).toBeInTheDocument();
    expect(screen.queryByText('Something went wrong')).toBeNull();
  });
});

// ── Custom fallback ────────────────────────────────────────────────────────────
describe('ErrorBoundary — custom fallback', () => {
  it('renders the custom fallback instead of the default panel', () => {
    const fallback = (error: Error, reset: () => void) => (
      <div>
        <p data-testid="custom-fallback">Custom: {error.message}</p>
        <button onClick={reset}>Reset</button>
      </div>
    );

    render(
      <ErrorBoundary fallback={fallback}>
        <Bomb shouldThrow />
      </ErrorBoundary>,
    );
    expect(screen.getByTestId('custom-fallback')).toBeInTheDocument();
    expect(screen.getByText('Custom: Test render error')).toBeInTheDocument();
    // Default panel must NOT appear
    expect(screen.queryByText('Something went wrong')).toBeNull();
  });

  it('custom fallback reset prop clears the error', () => {
    const fallback = (_error: Error, reset: () => void) => (
      <button data-testid="custom-reset" onClick={reset}>Reset</button>
    );

    let doThrow = true;
    function ClosureBomb() {
      if (doThrow) throw new Error('Test render error');
      return <div>Rendered OK</div>;
    }

    render(
      <ErrorBoundary fallback={fallback}>
        <ClosureBomb />
      </ErrorBoundary>,
    );
    expect(screen.getByTestId('custom-reset')).toBeInTheDocument();

    doThrow = false;
    fireEvent.click(screen.getByTestId('custom-reset'));

    expect(screen.getByText('Rendered OK')).toBeInTheDocument();
  });
});
