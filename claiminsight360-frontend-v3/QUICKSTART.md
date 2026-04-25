# ClaimInsight360 Frontend V3 - Quick Start Guide

## 🚀 Getting Started in 5 Minutes

### 1. Install Dependencies
```bash
cd claiminsight360-frontend-v3
npm install
```

### 2. Configure Environment
Create `.env` file:
```
VITE_API_GATEWAY_URL=http://localhost:8888
VITE_API_TIMEOUT=30000
VITE_ENVIRONMENT=development
```

### 3. Start Development Server
```bash
npm run dev
```
Visit: `http://localhost:3000`

### 4. Login
Use credentials from backend setup

## 📁 Project Structure Overview

```
src/
├── components/    → Reusable UI components
├── pages/         → Screen components
├── services/      → API integration
├── store/         → Redux state management
├── types/         → TypeScript interfaces
├── hooks/         → Custom React hooks
├── utils/         → Helper functions
├── middleware/    → Route protection
└── styles/        → Global CSS
```

## 🎨 Core Components

```typescript
// Button
<Button variant="primary" size="lg">Click Me</Button>

// Card
<Card>
  <CardHeader>Title</CardHeader>
  <CardBody>Content</CardBody>
</Card>

// Form
<FormInput label="Email" type="email" />
<FormSelect label="Status" options={...} />

// Table
<Table headers={[...]} rows={[...]} />

// Alert
<Alert type="success">Success message</Alert>

// Badge
<Badge variant="success">Approved</Badge>
```

## 🔐 Authentication

The app uses JWT tokens stored in localStorage. Login automatically:
1. Validates credentials
2. Stores token
3. Redirects to dashboard
4. Auto-injects token in all API calls
5. Auto-logout on 401

## 📊 Available Pages

| Path | Component | Description |
|------|-----------|-------------|
| `/login` | LoginPage | Authentication |
| `/` | DashboardPage | Main dashboard |
| `/claims` | ClaimsPage | Claims list |
| `/claims/:id` | ClaimDetailPage | Claim details |
| `/denials` | DenialsPage | Denial analysis |
| `/fraud` | FraudPage | Fraud detection |
| `/reserves` | ReservesPage | Cost reserves |
| `/reports` | ReportsPage | Reports |
| `/adjusters` | AdjustersPage | Adjusters |
| `/notifications` | NotificationsPage | Notifications |
| `/admin` | AdminPage | Admin panel (ADMIN only) |

## 🔄 API Service Pattern

```typescript
// Using services
import { claimsService } from '@/services/claimsService';

const claims = await claimsService.getClaims({ page: 1, limit: 10 });
const claim = await claimsService.getClaimById(claimId);
```

## 🎯 Redux State Usage

```typescript
import { useAppSelector, useAppDispatch } from '@/hooks';

// Get state
const { user } = useAppSelector(state => state.auth);
const { claims } = useAppSelector(state => state.claims);

// Dispatch actions
const dispatch = useAppDispatch();
dispatch(setUser({ user, token }));
```

## 📝 Form Handling

```typescript
import { useForm } from '@/hooks';

const form = useForm({ username: '', password: '' });

<input 
  name="username"
  value={form.values.username}
  onChange={form.handleChange}
  onBlur={form.handleBlur}
/>
```

## 🛠️ Common Tasks

### Add a New Page
1. Create `src/pages/NewPage.tsx`
2. Add route in `App.tsx`
3. Add nav item in `Layout.tsx`

### Create a Service
1. Create `src/services/newService.ts`
2. Export methods that use `apiClient`
3. Use in components

### Add Redux State
1. Create new slice in `src/store/`
2. Add to store configuration
3. Create hooks in `src/hooks/useAppState.ts`

### Create a Component
1. Create `src/components/MyComponent.tsx`
2. Export in `src/components/index.ts`
3. Import and use in pages

## 🐛 Debugging

### Enable Dev Tools
Install Redux DevTools browser extension

### Console Logging
```typescript
import { useAuth } from '@/hooks';

const { user } = useAuth();
console.log('Current user:', user);
```

### Network Inspection
Open browser DevTools → Network tab to inspect API calls

## 🚢 Building for Production

```bash
npm run build
npm run preview
```

Build output in `dist/` directory

## 📚 Type Definitions

All types in `src/types/index.ts`:
- User, AuthState
- Claim, ClaimFilter, ClaimStatus
- Denial, DenialPattern
- FraudRisk, FraudMetrics
- CostReserve, ReserveMetrics
- Adjuster, AdjusterWorkload
- Notification
- Report

## 🎨 Styling

### CSS Classes
```html
<!-- Buttons -->
<button class="btn btn-primary btn-lg">Click</button>

<!-- Cards -->
<div class="card">Content</div>

<!-- Grid -->
<div class="grid grid-3">Items</div>

<!-- Spacing -->
<div class="mt-4 mb-6 p-4">Content</div>

<!-- Text -->
<span class="text-primary font-bold">Text</span>
```

### CSS Variables
```css
var(--color-primary)
var(--space-4)
var(--font-size-lg)
var(--border-radius-md)
var(--shadow-lg)
```

## 🌐 Environment Variables

```
# API Configuration
VITE_API_GATEWAY_URL=http://localhost:8888
VITE_API_TIMEOUT=30000

# Environment
VITE_ENVIRONMENT=development|production
```

## 📱 Responsive Design

The app uses:
- CSS Grid for layouts
- Flexbox for alignment
- Mobile-first approach
- CSS variables for theming

## 🔍 Performance Tips

- Lazy load pages with React Router
- Use code splitting
- Optimize images
- Minimize bundle size
- Cache API responses

## 🤝 Contributing

1. Follow TypeScript best practices
2. Add meaningful comments
3. Test before committing
4. Keep components small and focused
5. Use meaningful names

## 📞 Support

- Check [INSTALLATION.md](./INSTALLATION.md)
- Review [README.md](./README.md)
- Check browser console for errors
- Verify API Gateway is running
- Check network requests in DevTools

## ✅ Next Steps

1. ✅ Install dependencies
2. ✅ Configure environment
3. ✅ Start development server
4. ✅ Explore the UI
5. → Implement business logic
6. → Add tests
7. → Deploy

---

**Happy Coding! 🎉**
