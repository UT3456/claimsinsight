# Frontend (Without Graph) — Interview Q&A

This document contains interview questions and answers about `frontendwihtoutgragh` codebase.

---

## Q1: What is the entry point of this frontend application?

**Answer:**

The entry point is `src/main.tsx`.

```typescript
import ReactDOM from 'react-dom/client';
import { Provider } from 'react-redux';
import { store } from './store';
import App from './App';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <Provider store={store}>
    <App />
  </Provider>,
);
```

This file:
1. Imports Redux store
2. Wraps the entire app in a Redux Provider so all components can access Redux state
3. Renders the `App` component into the DOM

The app then mounts at the `#root` div in `index.html`.

---

## Q2: What is the role of `App.tsx`?

**Answer:**

`App.tsx` is the root app component that sets up:

1. **Theme configuration** — uses `ThemeContext` and Ant Design's `ConfigProvider`
2. **Routing** — wraps `AppRouter` inside `BrowserRouter`
3. **Error boundary** — catches render-time errors
4. **Theme tokens** — defines the ClaimInsight360 color scheme:
   - Primary blue: `#185FA5`
   - Success green: `#27500A`
   - Error red: `#791F1F`
   - Warning orange: `#633806`

The structure is:
```
<ThemeProvider>
  <ThemedApp>
    <ConfigProvider theme={antTheme}>
      <AntApp>
        <BrowserRouter>
          <ErrorBoundary>
            <AppRouter />
          </ErrorBoundary>
        </BrowserRouter>
      </AntApp>
    </ConfigProvider>
  </ThemedApp>
</ThemeProvider>
```

---

## Q3: Explain the Redux store structure. What data is stored?

**Answer:**

The Redux store is configured in `src/store/index.ts`:

```typescript
export const store = configureStore({
  reducer: {
    auth: authReducer,
  },
});
```

Currently, only **auth state** is stored in Redux:

From `authSlice.ts`, the auth state contains:
- `token` — JWT access token
- `username` — logged-in user's username
- `role` — user role (e.g., `ROLE_CLAIMS_ANALYST`, `ROLE_ADMIN`)
- `userId` — numeric user ID
- `expiresIn` — token expiration time
- `isAuthenticated` — boolean flag

### Key actions:
- `setCredentials(response)` — stores auth data after login
- `clearCredentials()` — wipes auth on logout
- Selectors like `selectRole`, `selectUserId`, `selectIsAuthenticated`

### Why Redux is used:
To share auth state across the entire app without prop drilling. Every page, hook, and component can access the current user/role without passing props down.

---

## Q4: How does authentication work in this frontend?

**Answer:**

### Login flow:

1. User fills the login form on `LoginPage.tsx`
2. Form submits to `authApi.login({ username, password })`
3. Backend returns JWT response:
   ```json
   {
     "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
     "refreshToken": "...",
     "username": "alice",
     "role": "ROLE_CLAIMS_ANALYST",
     "userId": 42,
     "expiresIn": 3600
   }
   ```
4. Frontend dispatches `setCredentials(response)` to Redux
5. Redux stores token + role + username in state and localStorage
6. User is navigated to dashboard

### Token attachment:
Every API request uses `axiosInstance`, which has a **request interceptor** that:
- reads the token from Redux state
- attaches `Authorization: Bearer <token>` header

### Token refresh:
If a request returns **401 Unauthorized**:
- `axiosInstance` tries to refresh using the refresh token
- If refresh succeeds, the original request is retried
- If refresh fails, user is redirected to login

### Logout:
`useAuth().logout()` dispatches `clearCredentials()`, which:
- clears Redux state
- removes token from localStorage
- navigates to `/login`

---

## Q5: How does role-based access control (RBAC) work?

**Answer:**

### Role types:
```typescript
export const ROLES = {
  CLAIMS_ANALYST:   'ROLE_CLAIMS_ANALYST',
  CLAIMS_MANAGER:   'ROLE_CLAIMS_MANAGER',
  FRAUD_ANALYST:    'ROLE_FRAUD_ANALYST',
  ACTUARY:          'ROLE_ACTUARY',
  OPERATIONS_EXEC:  'ROLE_OPERATIONS_EXEC',
  ADMIN:            'ROLE_ADMIN',
};
```

### Route-based access:
In `src/utils/roles.ts`, a `ROUTE_ROLES` map defines which roles can view each route:

```typescript
export const ROUTE_ROLES: Record<string, AppRole[]> = {
  '/dashboard':        [all roles],
  '/claims':           [CLAIMS_ANALYST, CLAIMS_MANAGER, ACTUARY, OPS_EXEC, ADMIN],
  '/fraud-risk':       [FRAUD_ANALYST, ADMIN],
  '/admin/users':      [ADMIN],
  '/reports':          [all roles],
};
```

**ADMIN role implicitly has access to all routes.**

### Route protection:
In `AppRouter.tsx`, routes are wrapped with `RoleRoute`:

```tsx
<Route element={<RoleRoute />}>
  <Route path="/fraud-risk" element={<FraudRiskPage />} />
</Route>
```

`RoleRoute` checks:
1. Is the user authenticated?
2. Does the user's role have access to this route?
3. If not, show 403 Access Denied page

### Page-level access:
Pages use the `useAuth()` hook to check role:

```typescript
const { hasAccess, isFraudAnalyst, isAdmin } = useAuth();

if (!hasAccess('/some-route')) {
  // Show error or disable feature
}
```

### Sidebar filtering:
The sidebar uses `hasAccess()` to only show menu items the user can access.

---

## Q6: What is the `useAuth()` hook and how is it used?

**Answer:**

`useAuth()` is a custom hook in `src/hooks/useAuth.ts` that provides auth convenience methods:

```typescript
export function useAuth() {
  const dispatch        = useDispatch();
  const navigate        = useNavigate();
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const username        = useSelector(selectCurrentUser);
  const role            = useSelector(selectRole);
  const userId          = useSelector(selectUserId);
  const isAdmin         = useSelector(selectIsAdmin);

  const logout = () => {
    dispatch(clearCredentials());
    navigate('/login', { replace: true });
  };

  const isClaimsAnalyst = role === ROLES.CLAIMS_ANALYST;
  const isClaimsManager = role === ROLES.CLAIMS_MANAGER;
  const isFraudAnalyst  = role === ROLES.FRAUD_ANALYST;
  // ... other role checks

  const hasAccess = (route: string) => canAccess(role, route);

  return {
    isAuthenticated, username, role, userId, isAdmin,
    isClaimsAnalyst, isClaimsManager, isFraudAnalyst, isActuary, isOperationsExec,
    roleLabel, roleColor, hasAccess, logout,
  };
}
```

### Usage example:
```typescript
export default function SomeComponent() {
  const { isAdmin, isFraudAnalyst, logout, username } = useAuth();

  return (
    <>
      <p>Hello {username}</p>
      {isAdmin && <button>Admin controls</button>}
      {isFraudAnalyst && <button>Fraud controls</button>}
      <button onClick={logout}>Logout</button>
    </>
  );
}
```

---

## Q7: How is data fetched from the backend? Explain the API layer pattern.

**Answer:**

### Pattern:
Each domain has its own API file in `src/api/`.

Examples:
- `claimsApi.ts` — KPI endpoints
- `fraudRiskApi.ts` — risk scores & indicators
- `reportsApi.ts` — analytics reports
- `authApi.ts` — login/refresh
- `notificationsApi.ts` — notification endpoints

### Example: `claimsApi.ts`

```typescript
import axiosInstance from './axiosInstance';

export interface ClaimKpi {
  kpiId: number;
  claimId: string;
  metricName: string;
  metricValue: number;
  metricDate: string;
}

export const claimsApi = {
  getAll: (): Promise<ClaimKpi[]> =>
    axiosInstance.get('/kpis').then(r => r.data),

  getByClaim: (claimId: string): Promise<ClaimKpi[]> =>
    axiosInstance.get(`/kpis/claim/${claimId}`).then(r => r.data),

  create: (data: CreateKpiRequest): Promise<ClaimKpi> =>
    axiosInstance.post('/kpis', data).then(r => r.data),

  delete: (id: number): Promise<void> =>
    axiosInstance.delete(`/kpis/${id}`).then(r => r.data),
};
```

### Benefits:
- **Separation of concerns** — HTTP logic separate from UI logic
- **Reusability** — any component can import `claimsApi`
- **TypeScript types** — request/response types are defined
- **Consistency** — all API calls follow the same pattern

### Usage in pages:
```typescript
export default function ClaimsPage() {
  const [state, dispatch] = useReducer(reducer, initial);

  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const data = await claimsApi.getAll();
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load KPIs' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // render table with state.items
}
```

---

## Q8: What is `axiosInstance.ts` and why is it important?

**Answer:**

`axiosInstance.ts` is the **centralized HTTP client** that:

1. **Sets base URL** — points to the API Gateway
2. **Attaches JWT** — adds `Authorization: Bearer <token>` to every request
3. **Handles token refresh** — if 401, tries to refresh silently
4. **Normalizes errors** — extracts human-readable error messages
5. **Handles 8s timeout** — gives lazy services time to wake up

### Key features:

#### Request interceptor (attach JWT):
```typescript
axiosInstance.interceptors.request.use((config) => {
  const isAuthEndpoint = config.url?.startsWith('/auth');
  if (!isAuthEndpoint) {
    const token = store.getState().auth.token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});
```

#### Response interceptor (handle 401):
```typescript
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Try to refresh token
      const refreshToken = localStorage.getItem('ci360_refresh');
      const { data } = await axiosInstance.post('/auth/refresh', { refreshToken });
      store.dispatch(setCredentials(data));
      // Retry original request with new token
      return axiosInstance(originalRequest);
    }
    // Extract error message
    (error as any).userMessage = getApiErrorMessage(error);
    return Promise.reject(error);
  }
);
```

### Why centralized:
If every page called axios directly, we'd have to:
- attach JWT in every call
- handle 401 in every call
- normalize errors in every call

Instead, `axiosInstance` does this once globally.

---

## Q9: Explain the reducer pattern used in pages like `ClaimsPage.tsx`.

**Answer:**

Pages use React's `useReducer` hook to manage complex state. Example:

```typescript
interface State {
  items: ClaimKpi[];
  loading: boolean;
  error: string | null;
  filterMetric: string | null;
}

type Action =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: ClaimKpi[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'DELETE_SUCCESS'; payload: number }
  | { type: 'SET_FILTER'; payload: string | null };

const initial: State = {
  items: [], loading: false, error: null, filterMetric: null,
};

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'FETCH_START':    return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS':  return { ...state, loading: false, items: action.payload };
    case 'FETCH_ERROR':    return { ...state, loading: false, error: action.payload };
    case 'DELETE_SUCCESS': return { ...state, items: state.items.filter(i => i.kpiId !== action.payload) };
    case 'SET_FILTER':     return { ...state, filterMetric: action.payload };
    default:               return state;
  }
}
```

Usage in component:

```typescript
export default function ClaimsPage() {
  const [state, dispatch] = useReducer(reducer, initial);

  const load = async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const data = await claimsApi.getAll();
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load KPIs' });
    }
  };

  useEffect(() => { load(); }, [load]);

  return (
    <div>
      {state.loading && <Spinner />}
      {state.error && <Alert type="error">{state.error}</Alert>}
      <Table dataSource={state.items} />
    </div>
  );
}
```

### Why use reducer instead of useState?
- **Multiple related states** — items, loading, error all change together
- **Type safety** — TypeScript ensures action types match state shape
- **Predictable updates** — all state changes go through the reducer
- **Easier to test** — pure function

---

## Q10: How does the notification system work?

**Answer:**

### Notification polling hook:
In `src/hooks/useNotificationPolling.ts`:

```typescript
export function useNotificationPolling(userId: number | null) {
  const [unreadCount, setUnreadCount] = useState(0);
  const failureCount = useRef(0);

  useEffect(() => {
    if (!userId) return;

    const fetchCount = async () => {
      try {
        const count = await notificationsApi.getUnreadCount(userId);
        setUnreadCount(count);
        failureCount.current = 0; // recovery: reset backoff
        scheduleNext(30_000); // poll every 30s
      } catch {
        // Service down? slow down polling
        failureCount.current += 1;
        const delay = failureCount.current >= 3 ? 120_000 : 30_000;
        scheduleNext(delay);
      }
    };

    // Initial poll after 5s (don't compete with Dashboard load)
    const timeoutId = setTimeout(fetchCount, 5_000);

    return () => clearTimeout(timeoutId);
  }, [userId]);

  return { unreadCount };
}
```

### Features:
1. **Polling** — every 30 seconds, asks backend for unread count
2. **Backoff** — if NotificationService is down, slow to 2-minute polling
3. **Stale badge** — if a poll fails, keep showing the last-known count (don't reset to 0)
4. **Delayed start** — wait 5 seconds so it doesn't compete with Dashboard's data loads

### Usage in NotificationBell:
```typescript
export default function NotificationBell() {
  const { userId } = useAuth();
  const { unreadCount } = useNotificationPolling(userId);

  return (
    <button>
      <Bell size={18} />
      {unreadCount > 0 && <span className="badge">{unreadCount}</span>}
    </button>
  );
}
```

---

## Q11: What is the Sidebar component and how does it filter based on role?

**Answer:**

The `Sidebar` component in `src/components/common/Sidebar.tsx`:

1. **Defines nav sections** — Analytics, Operations, Financial, Reporting, Admin
2. **Filters by role** — only shows menu items the user can access
3. **Marks active route** — highlights the current page

### Example nav structure:
```typescript
const sections: NavSection[] = [
  {
    title: 'Analytics',
    items: [
      { to: '/dashboard', label: 'Overview', allowed: hasAccess('/dashboard'), tone: 'blue' },
      { to: '/claims', label: 'Claims', allowed: hasAccess('/claims'), tone: 'blue' },
      { to: '/fraud-risk', label: 'Fraud & risk', allowed: hasAccess('/fraud-risk'), tone: 'red' },
    ],
  },
  {
    title: 'Operations',
    items: [
      { to: '/adjusters', label: 'Adjusters', allowed: hasAccess('/adjusters'), tone: 'purple' },
    ],
  },
];
```

### Rendering:
```typescript
{sections.map((section) => {
  // Only show section if it has any visible items
  const visible = section.items.filter((i) => i.allowed);
  if (visible.length === 0) return null;
  
  return (
    <div key={section.title}>
      <div className="section-header">{section.title}</div>
      {visible.map((item) => (
        <button
          className={isActive(item) ? 'active' : ''}
          onClick={() => navigate(item.to)}
        >
          <span className={`dot dot-${item.tone}`} />
          {item.label}
        </button>
      ))}
    </div>
  );
})}
```

### Key feature: Admin section only shows if user has any admin access:
```typescript
const showAdminSection = hasAdminSectionAccess(role);
```

So a fraud analyst who has read-only access to `/admin/audit-logs` will see the Admin section.

---

## Q12: What is the ThemeContext and how does it work?

**Answer:**

`ThemeContext` in `src/contexts/ThemeContext.tsx` manages the app's theme.

```typescript
export type ThemeMode = 'light' | 'dark';

interface ThemeContextValue {
  theme: ThemeMode;
  toggleTheme: () => void;
  setTheme: (t: ThemeMode) => void;
}
```

### Provider:
```typescript
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemeMode>(resolveInitialTheme);

  // Apply theme to <html> element so CSS variables cascade everywhere
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    document.documentElement.style.colorScheme = theme;
    localStorage.setItem(STORAGE_KEY, theme);
  }, [theme]);

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}
```

### Hook to use theme:
```typescript
export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useTheme must be used inside ThemeProvider');
  return ctx;
}
```

### Current state:
**The app is light-mode only.** The toggle was removed from the UI, and dark values in localStorage are wiped on init.

```typescript
function resolveInitialTheme(): ThemeMode {
  try {
    localStorage.removeItem(STORAGE_KEY); // wipe any old 'dark' setting
  } catch { /* noop */ }
  return 'light';
}
```

---

## Q13: How does the LoginPage work? What validation does it use?

**Answer:**

`LoginPage.tsx` uses:
- **Zod** for schema validation
- **react-hook-form** for form state
- **Ant Design Form** components for UI

### Validation schema:
```typescript
const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
  remember: z.boolean().optional(),
});
```

### Form setup:
```typescript
const { control, handleSubmit, formState: { errors } } = useForm<LoginFormValues>({
  resolver: zodResolver(loginSchema),
  defaultValues: { username: '', password: '', remember: true },
});
```

### Submission:
```typescript
const onSubmit = async (values: LoginFormValues) => {
  setLoading(true);
  setApiError(null);
  try {
    const response = await authApi.login({
      username: values.username,
      password: values.password,
    });
    dispatch(setCredentials(response));
    
    // Redirect to intended page or dashboard
    let redirectTo = '/dashboard';
    try {
      const stored = sessionStorage.getItem('ci360.redirectAfterLogin');
      if (stored && stored.startsWith('/') && !stored.startsWith('/login')) {
        redirectTo = stored;
      }
      sessionStorage.removeItem('ci360.redirectAfterLogin');
    } catch { /* noop */ }
    
    navigate(redirectTo, { replace: true });
  } catch (err: unknown) {
    const msg = (err as { userMessage?: string }).userMessage
              ?? 'Invalid username or password.';
    setApiError(msg);
  } finally {
    setLoading(false);
  }
};
```

### Redirect-after-login:
When a user tries to access `/fraud-risk` but is not logged in:
1. `ProtectedRoute` redirects to `/login`
2. It stores the intended path in `sessionStorage`
3. After login, the user is sent back to `/fraud-risk`

---

## Q14: What is the AppLayout component and how does it structure the page?

**Answer:**

`AppLayout.tsx` is the main page shell for authenticated users.

```typescript
export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  useServiceWarmup();

  return (
    <div style={styles.shell}>
      <Sidebar collapsed={collapsed} />
      
      <div style={styles.main}>
        <Navbar
          collapsed={collapsed}
          onToggle={() => setCollapsed((c) => !c)}
        />
        <main style={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
```

### Layout structure:
```
┌─────────────────────────────────┐
│ Navbar (with toggle button)     │
├─────────┬───────────────────────┤
│         │                       │
│ Sidebar │   <Outlet />          │
│         │  (page content)       │
│         │                       │
└─────────┴───────────────────────┘
```

### Features:
1. **Sidebar collapse toggle** — `[collapsed]` state passed to Navbar
2. **Navbar toggle** — calls `setCollapsed((c) => !c)`
3. **useServiceWarmup()** — sends lightweight pings to backend services on mount
4. **Outlet** — React Router renders the current page here

### Flexbox styling:
```typescript
const styles = {
  shell: {
    display: 'flex',
    minHeight: '100vh',
    background: 'var(--ci-bg-app)',
  },
  main: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    minWidth: 0, // prevents flex overflow
    overflow: 'hidden',
  },
  content: {
    flex: 1,
    overflowY: 'auto',
    padding: '24px',
  },
};
```

---

## Q15: What are the main pages in this application?

**Answer:**

Pages are in `src/pages/` organized by domain:

### Auth pages:
- `auth/LoginPage.tsx` — login form
- `auth/RegisterPage.tsx` — registration (if enabled)

### Dashboard:
- `dashboard/Dashboard.tsx` — main overview
- `dashboard/variants/AnalystDashboard.tsx` — Claims Analyst view
- `dashboard/variants/ManagerDashboard.tsx` — Claims Manager view
- `dashboard/variants/FraudDashboard.tsx` — Fraud Analyst view
- `dashboard/variants/ActuaryDashboard.tsx` — Actuary view
- `dashboard/variants/OpsExecDashboard.tsx` — Operations Executive view
- `dashboard/variants/AdminDashboard.tsx` — Admin view

### Claims Analytics:
- `claims/ClaimsPage.tsx` — KPI table

### Data Ingestion (Admin):
- `ingestion/FeedsPage.tsx` — data feed sources
- `ingestion/RawClaimsPage.tsx` — raw claim records

### Operations:
- `adjusters/AdjustersPage.tsx` — adjuster list
- `adjusters/SlaViolationsPage.tsx` — SLA violations

### Financial:
- `financial/CostsPage.tsx` — claim costs
- `financial/ReservesPage.tsx` — reserve amounts
- `financial/AgingPage.tsx` — aging buckets

### Risk Intelligence:
- `risk/FraudRiskPage.tsx` — fraud risk scores & indicators
- `risk/DenialLeakagePage.tsx` — denial patterns & leakage

### Reporting:
- `reports/ReportsPage.tsx` — analytics reports (CSV/PDF export)
- `notifications/NotificationsPage.tsx` — notification history

### Admin:
- `admin/AuditLogsPage.tsx` — audit log viewer
- `admin/KpiDefinitionsPage.tsx` — KPI configuration
- `admin/UsersRolesPage.tsx` — user role management

### Other:
- `profile/ProfilePage.tsx` — user profile
- `landing/LandingPage.tsx` — landing page

---

## Q16: How does `useReducer` differ from `useState` in this codebase?

**Answer:**

### useState — for simple state:
```typescript
const [collapsed, setCollapsed] = useState(false);

// Toggle
setCollapsed(c => !c);
```

Good for a single boolean or independent states.

### useReducer — for complex, related state:
```typescript
interface State {
  items: ClaimKpi[];
  loading: boolean;
  error: string | null;
  filterMetric: string | null;
}

const [state, dispatch] = useReducer(reducer, initial);

// Update items AND clear error at the same time
dispatch({ type: 'FETCH_SUCCESS', payload: data });

// Instead of:
// setItems(data);
// setLoading(false);
// setError(null);
```

### When to use useReducer:
- Multiple states that change together
- Complex state transitions
- Type-safe updates (TypeScript ensures action types)
- Easier to test (pure reducer function)

**In this codebase, pages use `useReducer` for page state (items, loading, error, filters), and `useState` for UI state (modal open, form values).**

---

## Q17: What happens when a user with FRAUD_ANALYST role logs in?

**Answer:**

1. **Login** → user submits username/password
2. **Backend responds** with JWT containing role: `ROLE_FRAUD_ANALYST`
3. **Redux stores** the role in authSlice
4. **User navigated** to `/dashboard`
5. **Dashboard chooses** which variant to show:
   ```typescript
   if (role === ROLES.FRAUD_ANALYST) {
     return <FraudDashboard />;
   }
   ```
6. **Sidebar rendered** — only shows sections user can access:
   - Analytics (fraud-risk, denial-leakage) ✓
   - Operations ✗
   - Financial ✗
   - Reporting ✓
   - Admin (if has any admin access) ?

7. **User clicks Fraud & risk** → navigates to `/fraud-risk`
8. **RoleRoute checks** — `FRAUD_ANALYST` can access `/fraud-risk` ✓
9. **FraudRiskPage loads** — fetches risk scores & indicators
10. **User can** — view, create, delete (per role permissions)

---

## Q18: Explain the error boundary component.

**Answer:**

`ErrorBoundary.tsx` catches render-time errors in any component:

```typescript
export default function ErrorBoundary({ children }: Props) {
  const [hasError, setHasError] = useState(false);

  const handleError = (error: Error, info: ErrorInfo) => {
    console.error('Error boundary caught:', error, info);
    setHasError(true);
  };

  if (hasError) {
    return <ErrorState onReset={() => setHasError(false)} />;
  }

  return (
    <ErrorBoundary.Provider value={{ onError: handleError }}>
      {children}
    </ErrorBoundary.Provider>
  );
}
```

### What it catches:
- Render errors in components
- Lifecycle method errors

### What it does NOT catch:
- Event handler errors (use try/catch)
- Async errors (handled by axios/page catch blocks)
- Server-side errors (already caught by axios interceptor)

### Usage:
```tsx
<ErrorBoundary>
  <AppRouter />
</ErrorBoundary>
```

If any page throws a render error, the boundary catches it and shows an error page without blanking the entire app.

---

## Q19: What API endpoints does the frontend call? Give some examples.

**Answer:**

The frontend calls the API Gateway at `/api` (port 8086).

### Auth endpoints:
- `POST /api/auth/login` — login
- `POST /api/auth/refresh` — refresh token
- `POST /api/auth/register` — register

### Claims KPI endpoints:
- `GET /api/kpis` — all KPIs
- `GET /api/kpis/claim/{claimId}` — KPIs for one claim
- `GET /api/kpis/metric/{metricName}` — KPIs for a metric
- `POST /api/kpis` — create KPI
- `DELETE /api/kpis/{id}` — delete KPI

### Fraud Risk endpoints:
- `GET /api/risk-scores` — all risk scores
- `GET /api/risk-scores/claim/{claimId}` — risk for one claim
- `GET /api/risk-scores/threshold/{threshold}` — high-risk claims
- `POST /api/risk-scores` — create score
- `GET /api/risk-indicators` — all indicators
- `POST /api/risk-indicators` — create indicator

### Reports endpoints:
- `GET /api/reports` — all reports
- `GET /api/reports/scope/{scope}` — reports for scope
- `POST /api/reports` — create report
- `GET /api/reports/{id}/export?format=pdf` — download PDF
- `GET /api/reports/{id}/export?format=csv` — download CSV

### Notifications endpoints:
- `GET /api/notifications/unread-count/{userId}` — unread badge count
- `GET /api/notifications/unread/{userId}` — list unread

### Admin endpoints:
- `GET /api/audit-logs` — audit log entries
- `GET /api/users` — user list
- `PUT /api/users/{userId}` — update user role

---

## Q20: How does the frontend handle loading states and errors?

**Answer:**

### Loading state:
Pages use `state.loading` to show a spinner:

```typescript
{state.loading && <Spin size="large" />}
{!state.loading && state.error && <Alert type="error" message={state.error} />}
{!state.loading && !state.error && <Table dataSource={state.items} />}
```

### Error messages:
The `axiosInstance` normalizes error responses:

```typescript
export function getApiErrorMessage(err: unknown): string {
  const ax = err as AxiosError<any>;
  
  if (ax?.response?.data) {
    const d = ax.response.data;
    if (typeof d.message === 'string') return d.message;
    if (typeof d.error === 'string') return d.error;
  }
  
  if (ax?.code === 'ECONNABORTED') 
    return 'Request timed out — the server is taking too long.';
  
  if (ax?.message === 'Network Error') 
    return 'Cannot reach the server. Check your connection.';
  
  return 'Something went wrong.';
}
```

### Per-page error handling:
```typescript
const load = useCallback(async () => {
  dispatch({ type: 'FETCH_START' });
  try {
    const data = await claimsApi.getAll();
    dispatch({ type: 'FETCH_SUCCESS', payload: data });
  } catch {
    dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load KPIs' });
  }
}, []);
```

### Toast messages:
Pages use Ant Design's message API for quick feedback:

```typescript
const { message } = AntApp.useApp();

try {
  await claimsApi.delete(id);
  message.success('KPI deleted');
} catch {
  message.error('Failed to delete KPI');
}
```

---

## Q21: What is the `useServiceWarmup()` hook?

**Answer:**

`useServiceWarmup.ts` sends lightweight pings to each backend service on app load:

```typescript
export function useServiceWarmup() {
  useEffect(() => {
    const services = [
      '/kpis',
      '/fraud-risk',
      '/reports',
      '/adjusters',
      '/costs',
      '/notifications',
    ];

    services.forEach(path => {
      axiosInstance.get(path).catch(() => {
        // Silently fail — this is just a warmup ping
      });
    });
  }, []);
}
```

### Why?
Spring Boot services are lazily initialized. On first request, they might take 5-10 seconds to spin up beans.

This hook fires small, non-blocking pings so when the user opens a page, the service is already warm and responds quickly.

### Where used?
In `AppLayout.tsx`, which runs after login:

```typescript
export default function AppLayout() {
  useServiceWarmup();
  // ... rest of layout
}
```

---

## Q22: How are pages structured? What is the typical pattern?

**Answer:**

Most pages follow this pattern:

```typescript
import { useEffect, useReducer, useCallback } from 'react';
import { Table, Button, Modal, Form, Alert } from 'antd';
import { claimsApi, type ClaimKpi } from '../../api/claimsApi';

// ── State types ──
interface State {
  items: ClaimKpi[];
  loading: boolean;
  error: string | null;
  modalOpen: boolean;
  filterMetric: string | null;
}

type Action = /* ... */;

// ── Reducer ──
function reducer(state: State, action: Action): State { /* ... */ }

// ── Component ──
export default function ClaimsPage() {
  const [state, dispatch] = useReducer(reducer, initial);
  const [form] = Form.useForm();
  const { message } = AntApp.useApp();

  // ── Data loading ──
  const load = useCallback(async () => {
    dispatch({ type: 'FETCH_START' });
    try {
      const data = await claimsApi.getAll();
      dispatch({ type: 'FETCH_SUCCESS', payload: data });
    } catch {
      dispatch({ type: 'FETCH_ERROR', payload: 'Failed to load' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // ── Handlers ──
  const handleCreate = async (values) => {
    dispatch({ type: 'SUBMIT_START' });
    try {
      const result = await claimsApi.create(values);
      dispatch({ type: 'CREATE_SUCCESS', payload: result });
      form.resetFields();
      message.success('Created');
    } catch (err) {
      dispatch({ type: 'SUBMIT_ERROR' });
      message.error((err as any).userMessage ?? 'Failed');
    }
  };

  // ── Render ──
  return (
    <div>
      <PageHeader title="Claims KPIs" actions={...} />
      
      {state.error && <Alert type="error" message={state.error} />}
      
      <Table
        loading={state.loading}
        dataSource={state.items}
        columns={columns}
      />
      
      <Modal open={state.modalOpen} onOk={() => form.submit()}>
        <Form form={form} onFinish={handleCreate}>
          {/* form items */}
        </Form>
      </Modal>
    </div>
  );
}
```

### Components:
1. **State** — useReducer for page state (items, loading, error)
2. **Data loading** — useCallback + useEffect to fetch on mount
3. **Handlers** — functions for create/update/delete
4. **Render** — table, modal, filters

---

## Q23: How does form validation work in this app? (e.g., LoginPage)

**Answer:**

The app uses **Zod + react-hook-form** for validation:

### Zod schema:
```typescript
const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
  remember: z.boolean().optional(),
});

type LoginFormValues = z.infer<typeof loginSchema>;
```

### react-hook-form setup:
```typescript
const { control, handleSubmit, formState: { errors } } = useForm<LoginFormValues>({
  resolver: zodResolver(loginSchema),
  defaultValues: { username: '', password: '', remember: true },
});
```

### Form rendering:
```typescript
<Form layout="vertical" onFinish={handleSubmit(onSubmit)}>
  <Form.Item label="Username" validateStatus={errors.username ? 'error' : ''}>
    <Controller
      name="username"
      control={control}
      render={({ field }) => <Input {...field} />}
    />
    {errors.username && <Text type="danger">{errors.username.message}</Text>}
  </Form.Item>

  <Form.Item label="Password" validateStatus={errors.password ? 'error' : ''}>
    <Controller
      name="password"
      control={control}
      render={({ field }) => <Input.Password {...field} />}
    />
  </Form.Item>

  <Button type="primary" htmlType="submit" loading={loading}>
    Sign in
  </Button>
</Form>
```

### Submission:
```typescript
const onSubmit = async (values: LoginFormValues) => {
  // Form is already valid here (Zod checked it)
  const response = await authApi.login(values);
  // ...
};

<Form onFinish={handleSubmit(onSubmit)}>
```

---

## Q24: What is ProtectedRoute and how does it work?

**Answer:**

`ProtectedRoute.tsx` ensures only logged-in users can access protected pages:

```typescript
export default function ProtectedRoute() {
  const isAuthenticated = useSelector(selectIsAuthenticated);
  const navigate = useNavigate();

  if (!isAuthenticated) {
    // Store the intended route for redirect-after-login
    try {
      const intended = window.location.pathname + window.location.search + window.location.hash;
      if (intended && intended !== '/') {
        sessionStorage.setItem('ci360.redirectAfterLogin', intended);
      }
    } catch { /* noop */ }

    navigate('/login', { replace: true });
    return null;
  }

  // User is logged in, allow access to child routes
  return <Outlet />;
}
```

### Used in AppRouter:
```typescript
<Route element={<ProtectedRoute />}>
  <Route element={<AppLayout />}>
    <Route path="/dashboard" element={<Dashboard />} />
    <Route path="/claims" element={<ClaimsPage />} />
    {/* all other protected routes */}
  </Route>
</Route>
```

### Flow:
1. User tries to access `/fraud-risk` but not logged in
2. `ProtectedRoute` checks `isAuthenticated`
3. If false, stores `/fraud-risk` in sessionStorage
4. Navigates to `/login`
5. User logs in
6. Redux stores credentials
7. User is redirected back to `/fraud-risk` (from sessionStorage)

---

## Q25: What are the key technologies and libraries used?

**Answer:**

### Core:
- **React 18** — UI library
- **TypeScript** — type safety
- **Vite** — build tool (fast HMR)

### State management:
- **Redux Toolkit** — global auth state
- **useReducer** — page state

### HTTP:
- **axios** — HTTP client
- **interceptors** — JWT attachment, error handling, token refresh

### Form:
- **react-hook-form** — form state
- **Zod** — schema validation
- **@hookform/resolvers** — Zod + react-hook-form integration

### UI:
- **Ant Design** — component library
- **lucide-react** — icons
- **Recharts** — charts (not used in this variant)
- **Bootstrap** — CSS utility (for grid, spacing, etc.)

### Routing:
- **React Router v6** — client-side routing

### Utilities:
- **dayjs** — date handling
- **date-fns** — date manipulation

### Testing:
- **Vitest** — unit test framework
- **@testing-library/react** — component testing

### Dev tools:
- **ESLint** — code linting
- **TypeScript ESLint** — type-aware linting

---

## Summary

The **frontendwihtoutgragh** application is a **role-based claims analytics platform** built with:

- **React + TypeScript** for type-safe UI
- **Redux** for auth state
- **axios + interceptors** for secure API communication
- **Ant Design + custom components** for UI
- **useReducer** for complex page state
- **React Router** for navigation with role guards
- **Zod + react-hook-form** for validation

The key patterns are:
1. API files separate HTTP logic from UI
2. Pages use useReducer for state + useEffect for data loading
3. axiosInstance handles auth + errors centrally
4. RoleRoute + ProtectedRoute guard access
5. useAuth hook provides role/auth helpers throughout the app

---

## Backend — Interview Q&A

This section summarizes the backend microservices architecture and contains common interview questions and answers after scanning the repository.

Q1: What is the overall backend architecture?

Answer:

- The backend is a microservices system built with Spring Boot. Services register with a central `eureka-server` for discovery.
- All external requests from the frontend go to the `api-gateway` (Spring WebFlux). The gateway routes to services by their Eureka name.
- Each business capability is its own service with a separate database/schema (no cross-schema foreign keys). Examples: `data-ingestion-service`, `claims-metrics-service`, `fraud-risk-service`, `denial-leakage-service`, `cost-reserve-service`, `analytics-report-service`, `NotificationService`, `AdjusterAndOperations`.

Q2: How is authentication and authorization handled on the backend?

Answer:

- The API Gateway issues and returns JWT on login (dev secret in gateway config). Downstream services trust gateway-issued JWTs. Services typically read the JWT from the Authorization header and use role claims for access control.
- The frontend stores JWT and sends it in `Authorization` header; `axiosInstance` attaches it automatically.

Q3: How do services communicate with each other?

Answer:

- Backend services use Feign clients to call each other by Eureka service name. Conventions: `@FeignClient(name = "<eureka-service-name>", path = "/api/...")`, `contextId` is used when two Feign beans share the same name.
- Inter-service calls are wrapped in try/catch on the caller side; failures are logged and often non-fatal.

Q4: How are background workflows (like claim ingestion) orchestrated?

Answer:

- `data-ingestion-service` is the entry point for ingesting a claim. Its flow: save raw claim → synchronously trigger KPI calculation (claims-metrics-service) → then in parallel (CompletableFuture) trigger fraud evaluation, cost initialization, denial analysis, SLA violation creation — each to its respective service.
- Many downstream calls are designed to be non-fatal to keep ingestion resilient.

Q5: What are key persistence and performance patterns?

Answer:

- Each service uses Spring Data JPA with `ddl-auto: update` for development.
- JDBC batching is enabled across services (`hibernate.jdbc.batch_size`), and `saveAll()` is used for bulk insert operations.
- All entities include `@Index` annotations on queried columns.
- Caffeine cache is configured in all services (`spring.cache.type: caffeine`) with service-specific TTLs.

Q6: How are alerts/notifications handled?

Answer:

- `NotificationService` polls other services at intervals (or is invoked) and produces user-facing alerts. It exposes endpoints the frontend calls and has a slightly different error envelope (`{ success, message, data }`).

Q7: What are common debugging and startup tips?

Answer:

- Start services in the documented order: `eureka-server` first, then `api-gateway`, then business services, then frontend.
- Check `/actuator/health` to confirm service health.
- If Feign client collisions occur, add `contextId`.
- Check logs under each service's `logs/` folder for startup errors.

---

## Frontend + Backend — Combined Q&A: How they work together

Q1: Describe a full request flow from the frontend to a backend microservice and back.

Answer:

1. The user action in the browser triggers a page component (e.g., `ReportsPage`) to call an API wrapper (e.g., `reportsApi.getAll()`).
2. The API wrapper uses the centralized axios client (`axiosInstance`) which attaches the JWT from Redux and sends the request to `/api/...` (API Gateway).
3. The Gateway authenticates the token (in dev all services trust it) and routes the request to the correct microservice by name using Eureka service discovery.
4. The target microservice receives the HTTP request, performs its business logic (read DB, compute, or call other services via Feign), and returns JSON.
5. The Gateway forwards the response back to the frontend. `axiosInstance` receives it and returns `response.data` to the page layer.
6. The page updates React state and re-renders UI (table, charts) based on the response.

Q2: How is role-based access enforced across the stack?

Answer:

- On the frontend: routes are guarded (`ProtectedRoute`, `RoleRoute`) using the role stored in Redux; UI elements may be hidden or disabled via `useAuth()` checks.
- On the backend: services validate the JWT and may enforce endpoint-level role checks; even if the frontend hides an action, the backend enforces write permissions.

Q3: How are failures handled end-to-end?

Answer:

- Frontend: `axiosInstance` normalizes errors and provides friendly messages. Pages catch and present errors using Ant Design `message` / `Alert`.
- Gateway: can return upstream errors and forward status codes. Frontend shows helpful text from normalized envelopes.
- Services: often treat some downstream failures as non-fatal (especially in ingestion flows), log warnings, and continue processing.

Q4: How to debug an end-to-end issue where the frontend shows "Request timed out"?

Answer:

1. Confirm the frontend received a timeout error (axios message). Check browser network tab for the request and status.
2. Confirm Gateway is running on port 8086 and reachable: `curl http://localhost:8086/actuator/health`.
3. Check the target service health via its actuator endpoint.
4. Inspect gateway logs to see routing errors or timeouts.
5. If a service is down or slow to start, consider `useServiceWarmup` and increase timeouts for long-running endpoints.

Q5: Useful commands for local development

Answer:

PowerShell commands (run from the repo root `C:\Users\2478140\Downloads\cali`):

```powershell
# Start all services (the repo provides scripts that open windows per service)
.\START_ALL_SERVICES.ps1

# Build a single service and run
cd data-ingestion-service; mvn clean install -DskipTests; mvn spring-boot:run

# Check a service health
curl http://localhost:8082/actuator/health

# Frontend dev server
cd claiminsight360-frontend-v3; npm install; npm run dev
```

---

## Next steps

I will now commit the updated `FRONTEND_INTERVIEW_QA.md` and attempt to push it to the repository's `master` branch. If push fails due to authentication, I will show the commands so you can push locally with your credentials.


