import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Badge from './Badge';

describe('Badge', () => {
  it('renders its children as text', () => {
    render(<Badge>OPEN</Badge>);
    expect(screen.getByText('OPEN')).toBeInTheDocument();
  });

  it('renders a <span> element', () => {
    const { container } = render(<Badge>test</Badge>);
    expect(container.firstChild?.nodeName).toBe('SPAN');
  });

  it('defaults to the neutral tone (no crash, children visible)', () => {
    render(<Badge tone="neutral">Neutral</Badge>);
    expect(screen.getByText('Neutral')).toBeInTheDocument();
  });

  const tones = ['green', 'amber', 'red', 'blue', 'purple', 'teal', 'neutral'] as const;
  tones.forEach((tone) => {
    it(`renders without error for tone="${tone}"`, () => {
      const { container } = render(<Badge tone={tone}>{tone}</Badge>);
      expect(container.firstChild).not.toBeNull();
      expect(screen.getByText(tone)).toBeInTheDocument();
    });
  });

  it('applies extra style via the style prop', () => {
    const { container } = render(<Badge style={{ fontWeight: 700 }}>Bold</Badge>);
    const span = container.firstChild as HTMLElement;
    expect(span.style.fontWeight).toBe('700');
  });

  it('renders complex React children (not just strings)', () => {
    render(
      <Badge>
        <span data-testid="inner">inner</span>
      </Badge>,
    );
    expect(screen.getByTestId('inner')).toBeInTheDocument();
  });
});
