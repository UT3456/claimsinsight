np# ClaimInsight360 Frontend V3 - Documentation Index

Welcome to the ClaimInsight360 Frontend V3! This file helps you navigate all resources.

## 📖 Documentation

### Start Here 👇
- **[QUICKSTART.md](./QUICKSTART.md)** - 5-minute setup guide
- **[PROJECT_SUMMARY.md](./PROJECT_SUMMARY.md)** - Complete project overview

### Detailed Guides
- **[INSTALLATION.md](./INSTALLATION.md)** - Complete installation & setup
- **[README.md](./README.md)** - Features & architecture

---

## 🗂️ Source Code Structure

### Entry Points
- `src/main.tsx` - Application entry point
- `src/App.tsx` - Root component & routes

### User Interface
```
src/components/
├── Button.tsx           - Button component
├── Card.tsx            - Card layout
├── Badge.tsx           - Badge indicators
├── Alert.tsx           - Alert messages
├── LoadingSpinner.tsx  - Loading state
├── Table.tsx           - Data tables
├── Form.tsx            - Form components
├── Layout.tsx          - Main layout
└── index.ts            - Component exports
```

### Pages/Screens
```
src/pages/
├── LoginPage.tsx              - 🔐 Authentication
├── DashboardPage.tsx          - 📊 Dashboard
├── ClaimsPage.tsx             - 📋 Claims list
├── ClaimDetailPage.tsx        - 📄 Claim details
├── DenialsPage.tsx            - ❌ Denial analysis
├── FraudPage.tsx              - ⚠️ Fraud detection
├── ReservesPage.tsx           - 💰 Cost reserves
├── ReportsPage.tsx            - 📈 Reports
├── AdjustersPage.tsx          - 👥 Adjusters
├── NotificationsPage.tsx      - 🔔 Notifications
├── AdminPage.tsx              - ⚙️ Administration
└── NotFoundPage.tsx           - 404 error page
```

### API Integration
```
src/services/
├── apiClient.ts           - HTTP client
├── authService.ts         - 🔐 Authentication
├── claimsService.ts       - 📋 Claims
├── denialService.ts       - ❌ Denials
├── fraudService.ts        - ⚠️ Fraud
├── reserveService.ts      - 💰 Reserves
├── adjusterService.ts     - 👥 Adjusters
├── notificationService.ts - 🔔 Notifications
└── analyticsService.ts    - 📈 Analytics
```

### State Management
```
src/store/
├── index.ts           - Store configuration
├── authSlice.ts       - 🔐 Auth state
└── claimsSlice.ts     - 📋 Claims state
```

### Code Organization
```
src/
├── types/              - 🏷️  TypeScript interfaces
├── hooks/              - 🪝 Custom React hooks
├── middleware/         - 🛡️ Route protection
├── utils/              - 🧰 Helper functions
└── styles/             - 🎨 CSS styling
```

---

## 🚀 Quick Commands

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run linter
npm run lint
```

---

## 🏃 Getting Started

### Step 1: Install
```bash
cd claiminsight360-frontend-v3
npm install
```

### Step 2: Configure
Create `.env`:
```
VITE_API_GATEWAY_URL=http://localhost:8888
VITE_API_TIMEOUT=30000
VITE_ENVIRONMENT=development
```

### Step 3: Run
```bash
npm run dev
```

### Step 4: Open
Visit `http://localhost:3000`

---

## 📚 Type Definitions

Located in `src/types/index.ts`:

```typescript
// User & Auth
User, AuthState, LoginRequest, LoginResponse

// Claims
Claim, ClaimFilter, ClaimType, ClaimStatus

// Denials
Denial, DenialPattern, DenialStatus

// Fraud
FraudRisk, FraudMetrics, FraudReviewStatus

// Reserves
CostReserve, ReserveMetrics

// Adjusters
Adjuster, AdjusterWorkload

// Notifications
Notification, NotificationType

// Analytics
AnalyticsMetrics, Report, ReportType

// Utilities
PaginationParams, PaginatedResponse, ApiResponse, ApiError
```

---

## 🪝 Custom Hooks

```typescript
// From src/hooks/

// Redux hooks
useAppDispatch()      - Get Redux dispatch
useAppSelector()      - Get Redux state
useAuth()            - Get auth state
useClaims()          - Get claims state

// Data fetching
useFetch()           - Fetch data with loading state
useAsync()           - Async data fetching

// Form handling
useForm()            - Form state management
```

---

## 🔌 API Services

```typescript
// Authentication
authService.login()           - Login user
authService.logout()          - Logout user
authService.getCurrentUser()  - Get current user
authService.refreshToken()    - Refresh JWT token

// Claims
claimsService.getClaims()           - Get claims list
claimsService.getClaimById()        - Get claim details
claimsService.createClaim()         - Create claim
claimsService.updateClaim()         - Update claim
claimsService.assignClaimToAdjuster() - Assign to adjuster

// Denials
denialService.getDenials()        - Get denials
denialService.createDenial()      - Create denial
denialService.appealDenial()      - Appeal denial
denialService.getDenialPatterns() - Get patterns

// Fraud
fraudService.getFraudRisks()           - Get fraud risks
fraudService.calculateClaimFraudScore() - Calculate score
fraudService.updateFraudReviewStatus()  - Update status

// Reserves
reserveService.getReserves()      - Get reserves
reserveService.createReserve()    - Create reserve
reserveService.releaseReserve()   - Release reserve

// Adjusters
adjusterService.getAdjusters()      - Get adjusters
adjusterService.getAllWorkloads()   - Get workloads
adjusterService.getAdjusterWorkload() - Get workload

// Notifications
notificationService.getNotifications() - Get notifications
notificationService.markAsRead()      - Mark as read

// Analytics
analyticsService.getDashboardMetrics() - Get metrics
analyticsService.generateReport()      - Generate report
analyticsService.exportReport()        - Export report
```

---

## 🎨 Components Usage

```typescript
// Button
<Button variant="primary" size="lg">Click</Button>

// Card
<Card>
  <CardHeader>Title</CardHeader>
  <CardBody>Content</CardBody>
  <CardFooter>Footer</CardFooter>
</Card>

// Form
<FormInput label="Email" type="email" />
<FormSelect label="Status" options={options} />
<FormTextarea label="Notes" />

// Alert
<Alert type="success">Success!</Alert>

// Badge
<Badge variant="primary">Active</Badge>

// Table
<Table headers={[...]} rows={[...]} />

// Loading
<LoadingSpinner size="md" message="Loading..." />
```

---

## 📊 Page Routes

| Route | Component | Purpose |
|-------|-----------|---------|
| `/login` | LoginPage | User login |
| `/` | DashboardPage | Main dashboard |
| `/claims` | ClaimsPage | Claims management |
| `/claims/:id` | ClaimDetailPage | Claim details |
| `/denials` | DenialsPage | Denial analysis |
| `/fraud` | FraudPage | Fraud detection |
| `/reserves` | ReservesPage | Cost reserves |
| `/reports` | ReportsPage | Reports |
| `/adjusters` | AdjustersPage | Adjusters |
| `/notifications` | NotificationsPage | Notifications |
| `/admin` | AdminPage | Admin (ADMIN only) |
| `*` | NotFoundPage | 404 page |

---

## 🔐 Authentication Flow

1. User enters credentials at `/login`
2. `authService.login()` validates credentials
3. Server returns JWT token
4. Token stored in localStorage
5. User redirected to dashboard
6. Token auto-injected in API requests
7. Protected routes check authentication
8. Auto-logout on 401 Unauthorized

---

## 🛡️ Protected Routes

```typescript
// Routes protected by ProtectedRoute wrapper
<ProtectedRoute>
  <Component />
</ProtectedRoute>

// Routes with role requirement
<ProtectedRoute requiredRoles={['ADMIN']}>
  <AdminComponent />
</ProtectedRoute>
```

---

## 🧪 Testing

The project structure supports:
- Unit testing with Jest
- Component testing with React Testing Library
- E2E testing with Cypress/Playwright

Create test files alongside components:
```
src/components/Button.tsx
src/components/Button.test.tsx
```

---

## 📦 Build & Deploy

### Development Build
```bash
npm run dev
```

### Production Build
```bash
npm run build
# Output in dist/ directory
```

### Preview Build
```bash
npm run preview
```

### Environment Variables
- `VITE_API_GATEWAY_URL` - Backend API URL
- `VITE_API_TIMEOUT` - Request timeout
- `VITE_ENVIRONMENT` - Environment name

---

## 🎓 Learning Resources

### Component Development
See: `src/components/` for examples

### Page Development
See: `src/pages/` for examples

### Service Development
See: `src/services/` for examples

### State Management
See: `src/store/` for examples

### Type Safety
See: `src/types/index.ts` for definitions

### Custom Hooks
See: `src/hooks/` for examples

---

## ❓ Troubleshooting

### Dependencies not installing
```bash
npm cache clean --force
npm install
```

### Port 3000 in use
```bash
npm run dev -- --port 3001
```

### API connection errors
- Check `VITE_API_GATEWAY_URL` in `.env`
- Verify backend services running
- Check network connectivity

### TypeScript errors
- Ensure all types are defined
- Use strict mode
- Check type annotations

---

## 📞 Support

1. Read [QUICKSTART.md](./QUICKSTART.md)
2. Check [INSTALLATION.md](./INSTALLATION.md)
3. Review [README.md](./README.md)
4. Check browser console for errors
5. Inspect network requests in DevTools

---

## 🎯 Development Workflow

1. **Create component** in `src/components/`
2. **Create page** in `src/pages/`
3. **Add API service** in `src/services/`
4. **Create Redux slice** in `src/store/`
5. **Add route** in `src/App.tsx`
6. **Add navigation** in `src/components/Layout.tsx`
7. **Add types** in `src/types/index.ts`
8. **Test** in development server
9. **Build** for production
10. **Deploy** to server

---

## 📋 File Checklist

- [x] Configuration files
- [x] HTML template
- [x] TypeScript setup
- [x] React setup
- [x] Redux setup
- [x] Router setup
- [x] UI components
- [x] Page components
- [x] API services
- [x] Custom hooks
- [x] Type definitions
- [x] Global styles
- [x] Documentation

---

## 🎉 You're Ready!

Start with [QUICKSTART.md](./QUICKSTART.md) and begin development!

---

**Last Updated**: April 22, 2026  
**Status**: ✅ Production Ready  
**Version**: 3.0.0
