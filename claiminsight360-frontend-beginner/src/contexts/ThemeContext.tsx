import type { ReactNode } from 'react';

// kept this file so imports dont break - we removed dark mode

export type ThemeMode = 'light' | 'dark';

export function ThemeProvider({ children }: { children: ReactNode }) {
  return <>{children}</>;
}

// just returns light always - no toggle anymore
export function useTheme() {
  return {
    theme: 'light' as ThemeMode,
    toggleTheme: () => {},
    setTheme: (_t: ThemeMode) => {},
  };
}
