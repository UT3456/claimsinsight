/**
 * Safe array unwrap for Promise.allSettled results.
 *
 * Guards against the "API returned 200 but with a non-array body" crash that
 * used to take down whole role dashboards: any service replying with `null`,
 * an error envelope, or a plain object would land in our `for (const x of …)`
 * loops and throw "not iterable", flipping the dashboard into its catch
 * branch with an opaque "Failed to load" toast.
 *
 *   const arr = settledArray<MyDto>(result);   // [] when rejected OR malformed
 */
export function settledArray<T>(r: PromiseSettledResult<T[] | unknown>): T[] {
  if (r.status !== 'fulfilled') return [];
  const v = r.value;
  return Array.isArray(v) ? (v as T[]) : [];
}
