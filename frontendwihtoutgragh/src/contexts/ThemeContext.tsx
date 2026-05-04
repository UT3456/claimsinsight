import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';

export type ThemeMode = 'light' | 'dark';

interface ThemeContextValue {
  theme:       ThemeMode;
  toggleTheme: () => void;
  setTheme:    (t: ThemeMode) => void;
}

const STORAGE_KEY = 'ci360.theme';

const ThemeContext = createContext<ThemeContextValue | null>(null);

/**
 * The toggle has been removed from the UI — the app is light-mode only.
 * We still keep the ThemeContext + the data-theme attribute writer because
 * AntD's algorithm + every page's CSS-variable consumers depend on it being
 * set. Any leftover 'dark' value from a previous session is wiped here.
 */
function resolveInitialTheme(): ThemeMode {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch { /* localStorage may be blocked */ }
  return 'light';
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemeMode>(resolveInitialTheme);

  // Apply theme to <html> element so CSS variables cascade everywhere
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    document.documentElement.style.colorScheme = theme;
    try { localStorage.setItem(STORAGE_KEY, theme); } catch { /* noop */ }
  }, [theme]);

  const setTheme    = (t: ThemeMode) => setThemeState(t);
  const toggleTheme = () => setThemeState(prev => (prev === 'light' ? 'dark' : 'light'));

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used inside ThemeProvider');
  return ctx;
}
