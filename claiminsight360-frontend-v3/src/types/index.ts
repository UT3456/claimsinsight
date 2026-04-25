// User and Authentication Types
export interface User {
  userId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  department?: string;
  isActive: boolean;
  createdAt: string;
  lastLogin?: string;
}

export type UserRole = 'ANALYST' | 'MANAGER' | 'FRAUD_SPECIALIST' | 'ACTUARY' | 'OPERATIONS' | 'ADMIN';

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  user: User;
  token: string;
  refreshToken: string;
}

// Claim Types
export interface Claim {
  claimId: string;
  claimNumber: string;
  claimantName: string;
  claimantEmail: string;
  claimAmount: number;
  claimDate: string;
  incidentDate: string;
  claimType: ClaimType;
  claimStatus: ClaimStatus;
  description: string;
  policyNumber: string;
  adjusterId?: string;
  priority: Priority;
  createdAt: string;
  updatedAt: string;
  denialStatus?: DenialStatus;
  fraudRiskScore?: number;
  costReserve?: number;
}

export type ClaimType = 'MEDICAL' | 'PROPERTY' | 'AUTO' | 'LIABILITY' | 'WORKERS_COMP' | 'OTHER';
export type ClaimStatus = 'OPEN' | 'UNDER_REVIEW' | 'APPROVED' | 'DENIED' | 'CLOSED' | 'APPEALED';
export type DenialStatus = 'NOT_DENIED' | 'UNDER_REVIEW' | 'DENIED' | 'APPEALED' | 'UPHELD' | 'REVERSED';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface ClaimFilter {
  status?: ClaimStatus;
  claimType?: ClaimType;
  priority?: Priority;
  dateRange?: DateRange;
  adjusterId?: string;
  searchTerm?: string;
}

export interface DateRange {
  startDate: string;
  endDate: string;
}

// Denial Types
export interface Denial {
  denialId: string;
  claimId: string;
  denialReason: string;
  denialDate: string;
  denialAmount: number;
  appealable: boolean;
  appealDeadline?: string;
  createdBy: string;
  notes?: string;
}

export interface DenialPattern {
  pattern: string;
  frequency: number;
  impactAmount: number;
  claimsAffected: number;
  lastOccurrence: string;
}

// Fraud Types
export interface FraudRisk {
  fraudRiskId: string;
  claimId: string;
  riskScore: number; // 0-100
  riskFactors: string[];
  flaggedDate: string;
  reviewStatus: FraudReviewStatus;
  investigationNotes?: string;
  assignedInvestigator?: string;
}

export type FraudReviewStatus = 'PENDING' | 'UNDER_INVESTIGATION' | 'SUSPICIOUS' | 'CONFIRMED' | 'CLEARED' | 'CLOSED';

export interface FraudMetrics {
  totalClaims: number;
  suspiciousClaims: number;
  confirmedFraud: number;
  suspicionRate: number;
  detectionRate: number;
}

// Cost Reserve Types
export interface CostReserve {
  reserveId: string;
  claimId: string;
  reserveAmount: number;
  reserveType: ReserveType;
  createdDate: string;
  lastUpdated: string;
  status: ReserveStatus;
  notes?: string;
}

export type ReserveType = 'INITIAL' | 'SUPPLEMENTAL' | 'FINAL';
export type ReserveStatus = 'ACTIVE' | 'RELEASED' | 'TRANSFERRED' | 'CLOSED';

export interface ReserveMetrics {
  totalReserves: number;
  activeReserves: number;
  releasedReserves: number;
  totalReserveAmount: number;
}

// Adjuster Types
export interface Adjuster {
  adjusterId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  specialization?: string;
  isActive: boolean;
  assignedClaims: number;
  performanceRating?: number;
  createdAt: string;
}

export interface AdjusterWorkload {
  adjusterId: string;
  adjusterName: string;
  assignedClaims: number;
  openClaims: number;
  closedClaims: number;
  averageClaimResolutionTime: number; // in days
  performanceScore: number;
}

// Notification Types
export interface Notification {
  notificationId: string;
  userId: string;
  type: NotificationType;
  title: string;
  message: string;
  claimId?: string;
  createdAt: string;
  isRead: boolean;
  actionUrl?: string;
}

export type NotificationType = 'CLAIM_ALERT' | 'DENIAL_ALERT' | 'FRAUD_ALERT' | 'TASK_REMINDER' | 'SYSTEM_MESSAGE' | 'PERFORMANCE_ALERT';

// Analytics and Reporting Types
export interface AnalyticsMetrics {
  totalClaims: number;
  approvedClaims: number;
  deniedClaims: number;
  pendingClaims: number;
  averageClaimAmount: number;
  totalClaimAmount: number;
  approvalRate: number;
  denialRate: number;
  averageResolutionTime: number;
}

export interface DashboardMetrics {
  claims: AnalyticsMetrics;
  fraud: FraudMetrics;
  reserves: ReserveMetrics;
  adjusters: AdjusterWorkload[];
}

export interface Report {
  reportId: string;
  reportName: string;
  reportType: ReportType;
  createdAt: string;
  createdBy: string;
  metrics: Record<string, unknown>;
  filters: Record<string, unknown>;
}

export type ReportType = 'CLAIMS_SUMMARY' | 'DENIAL_ANALYSIS' | 'FRAUD_REPORT' | 'RESERVE_ANALYSIS' | 'ADJUSTER_PERFORMANCE' | 'CUSTOM';

// Pagination Types
export interface PaginationParams {
  page: number;
  limit: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

// API Response Types
export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
  timestamp: string;
}

export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}
