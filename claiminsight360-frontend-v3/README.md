# ClaimInsight360 Frontend V3

A comprehensive insurance claims management system frontend built with React, TypeScript, and Vite.

## Features

- **Multi-Role Dashboards**: Specialized views for Claims Analysts, Managers, Fraud Specialists, Actuaries, Operations, and Admins
- **Claims Management**: Browse, filter, and analyze insurance claims
- **Denial Analysis**: Identify and track denial patterns
- **Fraud Detection**: Risk assessment and fraud monitoring
- **Cost Reserve Management**: Track and manage reserves
- **Reporting**: Comprehensive analytics and reporting
- **Adjuster Management**: Manage adjuster workload and performance
- **Notifications**: Real-time alerts and notifications
- **Role-Based Access Control**: Secure, permission-based access

## Project Structure

```
src/
├── components/          # Reusable UI components
├── pages/              # Page components for each role/feature
├── services/           # API integration services
├── store/              # Redux state management
├── types/              # TypeScript type definitions
├── hooks/              # Custom React hooks
├── utils/              # Utility functions
├── middleware/         # Authentication and authorization
├── styles/             # Global styles
└── main.tsx            # Application entry point
```

## Getting Started

### Installation

```bash
npm install
```

### Development

```bash
npm run dev
```

The application will be available at `http://localhost:3000`

### Build

```bash
npm run build
```

### Preview

```bash
npm run preview
```

## Environment Variables

Create a `.env` file in the root directory:

```
VITE_API_GATEWAY_URL=http://localhost:8888
VITE_API_TIMEOUT=30000
VITE_ENVIRONMENT=development
```

## API Integration

The frontend integrates with the following backend services:

- **API Gateway**: http://localhost:8888
- Claims Service
- Denial Leakage Service
- Fraud Risk Service
- Cost Reserve Service
- Analytics Report Service
- Adjusters and Operations Service
- Data Ingestion Service

## Architecture

The application follows a modular architecture with:

- **Components**: Reusable UI building blocks
- **Pages**: Feature-specific page components
- **Services**: Centralized API communication
- **Store**: Redux for global state management
- **Types**: Comprehensive TypeScript interfaces
- **Hooks**: Custom hooks for business logic

## Technologies

- React 18
- TypeScript 5
- Redux Toolkit
- Vite
- Axios
- React Router
- Recharts
- Headless UI
- Lucide React Icons

## Development Guidelines

1. Always use TypeScript with strict mode enabled
2. Follow component composition patterns
3. Use custom hooks for logic
4. Centralize API calls in services
5. Manage state with Redux
6. Write responsive CSS classes
7. Follow naming conventions

## License

Proprietary - Insurance Claims Management System
