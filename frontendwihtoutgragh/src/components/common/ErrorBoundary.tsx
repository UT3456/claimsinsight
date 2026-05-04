import { Component, type ErrorInfo, type ReactNode } from 'react';
import { AlertTriangle, RotateCw } from 'lucide-react';

/**
 * Top-level safety net for render errors. Without this, any unhandled exception
 * inside any page (a bad selector, a null-dereference, a malformed prop) takes
 * the whole app down to a white screen — React 18 unmounts the tree.
 *
 * Wraps {@link AppRouter} so that:
 *   - one bad page doesn't blank the navbar / sidebar / theme toggle
 *   - the user sees a friendly message + reload button instead of nothing
 *   - the actual error is logged to console for the dev to debug
 *
 * NOT a substitute for catch blocks in async code — `componentDidCatch` does
 * NOT catch Promise rejections, event handler errors, or errors thrown
 * in setTimeout/setInterval callbacks. It catches RENDER errors only.
 */

interface Props {
  children: ReactNode;
  /** Optional custom fallback. If omitted, the default panel is shown. */
  fallback?: (error: Error, reset: () => void) => ReactNode;
}

interface State {
  error: Error | null;
}

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo): void {
    // Log so devs can find the original stack. In production this would feed
    // into Sentry / Datadog / wherever crash reports go.
    console.error('[ErrorBoundary] render crash:', error);
    console.error('[ErrorBoundary] component stack:', info.componentStack);
  }

  reset = (): void => {
    this.setState({ error: null });
  };

  render(): ReactNode {
    const { error } = this.state;
    if (!error) return this.props.children;

    if (this.props.fallback) {
      return this.props.fallback(error, this.reset);
    }

    return (
      <div style={styles.wrap}>
        <div style={styles.panel}>
          <div style={styles.iconCircle}>
            <AlertTriangle size={28} strokeWidth={1.6} />
          </div>
          <h2 style={styles.title}>Something went wrong</h2>
          <p style={styles.subtitle}>
            The page hit an unexpected error. The rest of the app is still
            running — try reloading this page.
          </p>
          {/* Show the message in a subtle code block so developers can quickly
              see what broke without exposing a stack trace to end users. */}
          <pre style={styles.detail}>{error.message || 'Unknown error'}</pre>
          <div style={styles.actions}>
            <button type="button" style={styles.primary} onClick={() => window.location.reload()}>
              <RotateCw size={13} strokeWidth={1.7} /> Reload page
            </button>
            <button type="button" style={styles.ghost} onClick={this.reset}>
              Try again without reload
            </button>
          </div>
        </div>
      </div>
    );
  }
}

const styles: Record<string, React.CSSProperties> = {
  wrap: {
    minHeight: '60vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 32,
  },
  panel: {
    maxWidth: 460,
    width: '100%',
    background: 'var(--ci-bg-surface)',
    border: '1px solid var(--ci-border)',
    borderRadius: 12,
    padding: '32px 28px',
    textAlign: 'center',
  },
  iconCircle: {
    width: 56, height: 56, borderRadius: '50%',
    display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
    background: 'var(--ci-danger-bg)',
    color: 'var(--ci-danger-text)',
    marginBottom: 16,
  },
  title:    { fontSize: 18, fontWeight: 600, margin: '0 0 8px', color: 'var(--ci-text-primary)' },
  subtitle: { fontSize: 13, color: 'var(--ci-text-secondary)', margin: '0 0 16px', lineHeight: 1.55 },
  detail: {
    fontSize: 11, fontFamily: 'Consolas, ui-monospace, monospace',
    background: 'var(--ci-bg-subtle)',
    color: 'var(--ci-text-muted)',
    padding: '8px 12px', borderRadius: 6,
    margin: '0 0 20px', textAlign: 'left',
    whiteSpace: 'pre-wrap', wordBreak: 'break-word',
    maxHeight: 120, overflow: 'auto',
  },
  actions: {
    display: 'flex', gap: 8, justifyContent: 'center',
  },
  primary: {
    display: 'inline-flex', alignItems: 'center', gap: 6,
    padding: '7px 14px', borderRadius: 6,
    fontSize: 12, fontWeight: 500,
    background: 'var(--ci-primary)', border: '1px solid var(--ci-primary)',
    color: '#fff', cursor: 'pointer',
  },
  ghost: {
    padding: '7px 14px', borderRadius: 6,
    fontSize: 12, fontWeight: 500,
    background: 'var(--ci-bg-surface)',
    border: '1px solid var(--ci-border-strong)',
    color: 'var(--ci-text-primary)', cursor: 'pointer',
  },
};
