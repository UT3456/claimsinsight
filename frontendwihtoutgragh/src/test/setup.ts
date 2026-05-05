import '@testing-library/jest-dom';

// ── localStorage mock ─────────────────────────────────────────────────────────
// Node 20+ ships a built-in localStorage that lacks clear() and other methods.
// Replace it with a full in-memory implementation so every test gets a
// spec-compliant store, and beforeEach({ localStorage.clear() }) works reliably.
const makeLocalStorageMock = () => {
  let store: Record<string, string> = {};
  return {
    getItem:    (key: string) => store[key] ?? null,
    setItem:    (key: string, value: string) => { store[key] = String(value); },
    removeItem: (key: string) => { delete store[key]; },
    clear:      () => { store = {}; },
    get length() { return Object.keys(store).length; },
    key:        (index: number) => Object.keys(store)[index] ?? null,
  };
};

Object.defineProperty(globalThis, 'localStorage', {
  value: makeLocalStorageMock(),
  writable: true,
  configurable: true,
});
