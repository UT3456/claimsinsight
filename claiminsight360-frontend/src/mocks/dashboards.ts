// Mock data for the six role-specific dashboards.
// Numbers mirror the reference design (claiminsight360-simple.html).

/* ── Claims Analyst ──────────────────────────────────────────────────────── */
export const analystMock = {
  assignedToMe:     { value: 87,    sub: '12 opened today' },
  dueThisWeek:      { value: 23,    sub: '5 overdue',        tone: 'down' as const },
  myAvgTat:         { value: '14.2d', sub: 'under team avg', tone: 'up'   as const },
  denialsWeek:      { value: 9,     sub: 'review needed' },

  denialCodes: [
    { code: 'D-101 Missing docs',    count: 12, pct: 85 },
    { code: 'D-204 Policy lapsed',   count: 7,  pct: 50 },
    { code: 'D-311 Out of coverage', count: 4,  pct: 28 },
    { code: 'Other codes',           count: 3,  pct: 22 },
  ],

  priorityQueue: [
    { claim: 'CL-48821', age: '39d', priority: 'High' as const },
    { claim: 'CL-48109', age: '28d', priority: 'Med'  as const },
    { claim: 'CL-47998', age: '21d', priority: 'Med'  as const },
  ],
};

/* ── Claims Manager ──────────────────────────────────────────────────────── */
export const managerMock = {
  openClaims:    { value: '14,287', sub: '▲ 3.2%',       tone: 'up'   as const },
  avgTeamTat:    { value: '18.4d',  sub: '1.1 over SLA', tone: 'down' as const },
  slaBreaches:   { value: 47,       sub: '▲ 14',          tone: 'down' as const },
  teamQuality:   { value: 86,       sub: '▲ 2 pts',      tone: 'up'   as const },

  alertText: '47 SLA breaches this period — 14 from Meera Thomas',

  workload: [
    { name: 'Ravi',   count: 187, color: '#16a34a' },
    { name: 'Aisha',  count: 164, color: '#16a34a' },
    { name: 'Daniel', count: 152, color: '#f59e0b' },
    { name: 'Jay',    count: 141, color: '#f59e0b' },
    { name: 'Meera',  count: 132, color: '#dc2626' },
  ],

  aging: [
    { label: '0–30 days',  count: 6842, pct: 78, color: '#16a34a' },
    { label: '31–60 days', count: 4128, pct: 52, color: '#2563eb' },
    { label: '61–90 days', count: 2194, pct: 30, color: '#f59e0b' },
    { label: '90+ days',   count: 1123, pct: 15, color: '#dc2626' },
  ],
};

/* ── Fraud Analyst ───────────────────────────────────────────────────────── */
export const fraudMock = {
  highRisk:    { value: 342,   sub: '▲ 27 (7d)',   tone: 'danger' as const },
  newFlags:    { value: 89,    sub: '▲ 12% WoW',   tone: 'warn'   as const },
  exposure:    { value: '₹4.2Cr', sub: '▲ ₹38L',   tone: 'down'   as const },
  avgScore:    { value: 68,    sub: 'of 100' },

  alertText: '27 new high-risk flags in last 24 hours',

  scoreDistribution: [
    { bucket: '0-20',   count: 25, color: '#16a34a' },
    { bucket: '21-40',  count: 45, color: '#2563eb' },
    { bucket: '41-70',  count: 65, color: '#2563eb' },
    { bucket: '71-85',  count: 43, color: '#f59e0b' },
    { bucket: '86-100', count: 23, color: '#dc2626' },
  ],

  flagged: [
    { claim: 'CL-48821', indicator: 'High cost + unusual timing', score: 94, tone: 'red'    as const },
    { claim: 'CL-48109', indicator: 'Repeat pattern',             score: 78, tone: 'yellow' as const },
    { claim: 'CL-47998', indicator: 'Late filing',                score: 71, tone: 'yellow' as const },
  ],
};

/* ── Actuary ─────────────────────────────────────────────────────────────── */
export const actuaryMock = {
  lossRatio:   { value: '67.2%', sub: '▼ 2.4 YoY', tone: 'up'   as const },
  avgSeverity: { value: '₹1.52L', sub: '▲ 6.8%',   tone: 'down' as const },
  reserves:    { value: '₹218Cr', sub: '▼ 4.1%',   tone: 'up'   as const },
  frequency:   { value: '3.8%',  sub: 'per policy' },

  severityTrend: [
    { m: 'Apr', v: 20 }, { m: 'May', v: 25 }, { m: 'Jun', v: 35 },
    { m: 'Jul', v: 45 }, { m: 'Aug', v: 50 }, { m: 'Sep', v: 60 },
    { m: 'Oct', v: 65 },
  ],

  lossRatioByProduct: [
    { product: 'Auto',   pct: 72, value: '72.4%', color: '#dc2626' },
    { product: 'Health', pct: 68, value: '68.1%', color: '#f59e0b' },
    { product: 'Home',   pct: 62, value: '62.3%', color: '#16a34a' },
    { product: 'Life',   pct: 59, value: '58.9%', color: '#16a34a' },
  ],
};

/* ── Operations Executive ────────────────────────────────────────────────── */
export const opsExecMock = {
  exposure:      { value: '₹218Cr', sub: '▼ 4.1% YoY', tone: 'up' as const },
  lossRatio:     { value: '67.2%',  sub: '▼ 2.4 pts',  tone: 'up' as const },
  slaCompliance: { value: '92.1%',  sub: 'above target', tone: 'up' as const },
  nps:           { value: '+42',    sub: '▲ 6 pts',    tone: 'up' as const },

  alertText: 'SLA compliance 92.1% — above target (90%)',

  monthlyCost: [
    { m: 'Aug', v: 45 }, { m: 'Sep', v: 53 }, { m: 'Oct', v: 49 },
    { m: 'Nov', v: 59 }, { m: 'Dec', v: 57 }, { m: 'Jan', v: 65 },
    { m: 'Feb', v: 69 }, { m: 'Mar', v: 75 },
  ],
};

/* ── Admin ───────────────────────────────────────────────────────────────── */
export const adminMock = {
  activeUsers:    { value: 48,     sub: 'of 52 invited' },
  feedsHealthy:   { value: '3/4',  sub: '1 error',   tone: 'down' as const },
  pendingInvites: { value: 4,      sub: 'sent this week' },
  uptime:         { value: '99.9%', sub: 'within SLA', tone: 'up' as const },

  alertText: '1 data feed in error state — feed-reserve-01',

  feedStatus: [
    { feed: 'feed-claim-01',   sync: '12m',    status: 'OK',    tone: 'green'  as const },
    { feed: 'feed-policy-01',  sync: '1h',     status: 'OK',    tone: 'green'  as const },
    { feed: 'feed-payment-01', sync: '4h',     status: 'Slow',  tone: 'yellow' as const },
    { feed: 'feed-reserve-01', sync: 'Failed', status: 'Error', tone: 'red'    as const },
  ],

  auditEvents: [
    { time: '14:22', text: 'p.menon viewed CL-48821' },
    { time: '14:08', text: 'a.shah escalated CL-48109' },
    { time: '12:51', text: 'admin updated KPI loss_ratio' },
    { time: '09:14', text: 'l.zhou exported Q1 report' },
  ],
};
