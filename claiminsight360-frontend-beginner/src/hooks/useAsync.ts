import { useReducer, useCallback } from 'react';

// ── Types ─────────────────────────────────────────────────────────────────────

export interface AsyncState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

type AsyncAction<T> =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: T }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'RESET' };

// ── Reducer ───────────────────────────────────────────────────────────────────

function asyncReducer<T>(
  state: AsyncState<T>,
  action: AsyncAction<T>,
): AsyncState<T> {
  switch (action.type) {
    case 'FETCH_START':
      return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS':
      return { data: action.payload, loading: false, error: null };
    case 'FETCH_ERROR':
      return { ...state, loading: false, error: action.payload };
    case 'RESET':
      return { data: null, loading: false, error: null };
    default:
      return state;
  }
}

// ── Hook ──────────────────────────────────────────────────────────────────────

/**
 * Generic useReducer-based async data hook.
 *
 * Usage:
 *   const { data, loading, error, run } = useAsync<ClaimKpi[]>();
 *   useEffect(() => { run(claimsApi.getAll()); }, [run]);
 */
export function useAsync<T>() {
  const [state, dispatch] = useReducer(
    asyncReducer as (state: AsyncState<T>, action: AsyncAction<T>) => AsyncState<T>,
    { data: null, loading: false, error: null },
  );

  const run = useCallback(async (promise: Promise<T>): Promise<T> => {
    dispatch({ type: 'FETCH_START' });
    try {
      const data = await promise;
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
      return data;
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        (err as Error)?.message ??
        'An error occurred';
      dispatch({ type: 'FETCH_ERROR', payload: msg });
      throw err;
    }
  }, []);

  const reset = useCallback(() => dispatch({ type: 'RESET' }), []);

  return { ...state, run, reset };
}
