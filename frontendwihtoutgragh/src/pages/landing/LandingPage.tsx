import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Shield, BarChart3, AlertTriangle, FileSearch, Users, Bell,
  ArrowRight, CheckCircle2, Activity, Zap, Lock, GitBranch,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import './landing.css';

/**
 * Public marketing page at "/". Authenticated visitors are bounced to
 * the dashboard so they don't have to click through the marketing copy
 * every time they hit the root URL.
 */
export default function LandingPage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    if (isAuthenticated) navigate('/dashboard', { replace: true });
  }, [isAuthenticated, navigate]);

  return (
    <div className="landing">
      {/* ── Top nav ─────────────────────────────────────────────────────── */}
      <header className="landing-nav">
        <div className="landing-nav-inner">
          <div className="landing-brand">
            <div className="landing-brand-mark"><Shield size={16} strokeWidth={2.2} color="#ffffff" /></div>
            <span className="landing-brand-text">ClaimInsight360</span>
          </div>
          <nav className="landing-nav-links">
            <a href="#features">Features</a>
            <a href="#roles">For your team</a>
            <a href="#flow">How it works</a>
          </nav>
          <div className="landing-nav-actions">
            <button className="landing-link" onClick={() => navigate('/login')}>Sign in</button>
            <button className="landing-cta-sm" onClick={() => navigate('/register')}>
              Request access
            </button>
          </div>
        </div>
      </header>

      {/* ── Hero ────────────────────────────────────────────────────────── */}
      <section className="landing-hero">
        <div className="landing-hero-inner">
          <span className="landing-pill">
            <span className="landing-pill-dot" /> Insurance Claims Analytics & Intelligence
          </span>
          <h1 className="landing-h1">
            See every claim.<br />
            <span className="landing-h1-accent">Catch every signal.</span>
          </h1>
          <p className="landing-subhead">
            ClaimInsight360 unifies claims, policy, and financial data into a single
            real-time intelligence layer — so analysts catch fraud earlier, managers
            hit SLAs more often, and executives see the portfolio without waiting
            for a report.
          </p>
          <div className="landing-hero-cta">
            <button className="landing-cta-lg" onClick={() => navigate('/login')}>
              Sign in to your workspace <ArrowRight size={14} strokeWidth={2} />
            </button>
            <button className="landing-cta-ghost" onClick={() => navigate('/register')}>
              Request access
            </button>
          </div>
          <div className="landing-hero-trust">
            <span><CheckCircle2 size={13} strokeWidth={2} /> 16 audited entities</span>
            <span><CheckCircle2 size={13} strokeWidth={2} /> Role-based access</span>
            <span><CheckCircle2 size={13} strokeWidth={2} /> Real-time alerts</span>
          </div>
        </div>

        {/* Mock dashboard preview */}
        <div className="landing-hero-art">
          <div className="landing-mock">
            <div className="landing-mock-bar">
              <span className="landing-mock-dot" style={{ background: '#ff5f57' }} />
              <span className="landing-mock-dot" style={{ background: '#febc2e' }} />
              <span className="landing-mock-dot" style={{ background: '#28c840' }} />
              <span className="landing-mock-url">claiminsight360.acme/dashboard</span>
            </div>
            <div className="landing-mock-body">
              <div className="landing-mock-kpis">
                <div className="landing-mock-kpi">
                  <div className="landing-mock-kpi-label">Open claims</div>
                  <div className="landing-mock-kpi-value">2,847</div>
                  <div className="landing-mock-kpi-delta good">+4.2%</div>
                </div>
                <div className="landing-mock-kpi">
                  <div className="landing-mock-kpi-label">Avg TAT</div>
                  <div className="landing-mock-kpi-value">17.3d</div>
                  <div className="landing-mock-kpi-delta bad">+2.3d</div>
                </div>
                <div className="landing-mock-kpi">
                  <div className="landing-mock-kpi-label">Loss ratio</div>
                  <div className="landing-mock-kpi-value">71.4%</div>
                </div>
                <div className="landing-mock-kpi">
                  <div className="landing-mock-kpi-label">High risk</div>
                  <div className="landing-mock-kpi-value">28</div>
                  <div className="landing-mock-kpi-delta bad">flagged</div>
                </div>
              </div>
              <div className="landing-mock-chart">
                <div className="landing-mock-bars">
                  {[42, 67, 55, 80, 73, 90, 78, 85, 95].map((h, i) => (
                    <div key={i} className="landing-mock-bar-col" style={{ height: `${h}%` }} />
                  ))}
                </div>
                <div className="landing-mock-chart-legend">
                  <span className="landing-mock-dot-blue" /> Filed
                  <span className="landing-mock-dot-teal" /> Settled
                </div>
              </div>
              <div className="landing-mock-list">
                <div className="landing-mock-row">
                  <span className="landing-mock-tag red">94</span>
                  <span>CLM-2026-PROP-007 — mill scheme suspect</span>
                </div>
                <div className="landing-mock-row">
                  <span className="landing-mock-tag amber">87</span>
                  <span>CLM-2026-HLTH-005 — denial pattern CO-4</span>
                </div>
                <div className="landing-mock-row">
                  <span className="landing-mock-tag amber">76</span>
                  <span>CLM-2026-WC-002 — overpayment flag</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── Features grid ──────────────────────────────────────────────── */}
      <section id="features" className="landing-section">
        <div className="landing-section-head">
          <h2 className="landing-h2">One platform. Every claim signal.</h2>
          <p className="landing-section-sub">
            Nine focused modules built around the claims lifecycle —
            connected by a real-time event bus, gated by role-based access.
          </p>
        </div>
        <div className="landing-features">
          <FeatureCard
            icon={<BarChart3 size={18} strokeWidth={2} />}
            title="Claims metrics engine"
            text="TAT, cycle time, severity, frequency, loss ratio, settlement time — computed per claim, refreshed in real time."
          />
          <FeatureCard
            icon={<AlertTriangle size={18} strokeWidth={2} />}
            title="Fraud & risk scoring"
            text="Rule-based indicators (HighCost, UnusualTiming, Pattern) + composite risk score per claim. SIU escalation in one click."
          />
          <FeatureCard
            icon={<FileSearch size={18} strokeWidth={2} />}
            title="Denial & leakage analysis"
            text="Denial codes parsed and clustered. Leakage flags for overpayment, processing errors, and adjudication delays."
          />
          <FeatureCard
            icon={<Users size={18} strokeWidth={2} />}
            title="Adjuster productivity"
            text="Workload, quality score, SLA compliance, error rate — all rolled up by adjuster, by quarter, by team."
          />
          <FeatureCard
            icon={<Activity size={18} strokeWidth={2} />}
            title="Cost, reserve & aging"
            text="Cost categorized by type. Reserves tracked at 120% of expected payout. Aging bucketed 0-30 / 31-60 / 61-90 / 90+."
          />
          <FeatureCard
            icon={<Bell size={18} strokeWidth={2} />}
            title="Real-time notifications"
            text="Server-Sent Events push alerts to the bell instantly. Threshold-based dispatch by role and category."
          />
        </div>
      </section>

      {/* ── Role-based use cases ────────────────────────────────────────── */}
      <section id="roles" className="landing-section landing-section-alt">
        <div className="landing-section-head">
          <h2 className="landing-h2">Built for every seat at the table</h2>
          <p className="landing-section-sub">
            Every role gets the slice of data that matters to them — gated at
            the API gateway, not just hidden in the UI.
          </p>
        </div>
        <div className="landing-roles">
          <RoleCard
            tag="Claims Analyst"
            color="#185FA5"
            text="Investigates trends, denials, and cost drivers. Drills from a denial spike to the underlying CO-codes in two clicks."
          />
          <RoleCard
            tag="Claims Manager"
            color="#0F766E"
            text="Watches team performance, cycle time, SLA compliance. Reassigns workload from overloaded adjusters in real time."
          />
          <RoleCard
            tag="Fraud Analyst"
            color="#B22C2B"
            text="Reviews high-risk claims and red-flag indicators. One click escalates to SIU and notifies the fraud manager."
          />
          <RoleCard
            tag="Actuary"
            color="#7E5BEF"
            text="Severity, frequency, reserves. Spots reserve adequacy gaps before they hit the loss-development triangle."
          />
          <RoleCard
            tag="Operations Executive"
            color="#C77800"
            text="Portfolio-level KPIs. Loss ratio, combined ratio, settlement rate, SLA compliance — all without individual claim noise."
          />
          <RoleCard
            tag="Admin"
            color="#5B6B7B"
            text="Configures users, roles, data feeds, KPI definitions. Audits every action for compliance."
          />
        </div>
      </section>

      {/* ── How it works ───────────────────────────────────────────────── */}
      <section id="flow" className="landing-section">
        <div className="landing-section-head">
          <h2 className="landing-h2">From raw claim to actionable signal</h2>
          <p className="landing-section-sub">
            One ingested claim triggers six engines in parallel. The result
            shows up on the right dashboard, in the right inbox, under a second.
          </p>
        </div>
        <div className="landing-flow">
          <FlowStep n="1" title="Ingest"
            text="Claim arrives via batch or API. Raw payload stored in ClaimRaw with full JSON, indexed by claim ID." />
          <FlowStep n="2" title="Compute"
            text="Six engines fire in parallel: KPIs, fraud rules, denial parsing, cost categorization, reserve seeding, SLA tracking." />
          <FlowStep n="3" title="Surface"
            text="Results land on role-specific dashboards. Anomalies trigger notifications dispatched by role and category." />
          <FlowStep n="4" title="Act"
            text="Analyst investigates, escalates to SIU, reassigns adjuster, or flags for actuarial review. Every action audited." />
        </div>
      </section>

      {/* ── Tech bar ───────────────────────────────────────────────────── */}
      <section className="landing-tech">
        <div className="landing-tech-inner">
          <div className="landing-tech-cell">
            <Zap size={16} strokeWidth={1.8} />
            <span><strong>Microservices</strong> · 9 services, Eureka discovery</span>
          </div>
          <div className="landing-tech-cell">
            <Lock size={16} strokeWidth={1.8} />
            <span><strong>JWT + Refresh</strong> · Rate-limit + lockout</span>
          </div>
          <div className="landing-tech-cell">
            <GitBranch size={16} strokeWidth={1.8} />
            <span><strong>Event-driven</strong> · Real-time SSE bell</span>
          </div>
          <div className="landing-tech-cell">
            <Activity size={16} strokeWidth={1.8} />
            <span><strong>Observability</strong> · Zipkin tracing, audit log</span>
          </div>
        </div>
      </section>

      {/* ── CTA ────────────────────────────────────────────────────────── */}
      <section className="landing-cta-band">
        <div className="landing-cta-inner">
          <h2 className="landing-h2-cta">Ready to see your claims clearly?</h2>
          <p className="landing-cta-sub">
            Sign in with your workspace credentials, or request access for your team.
          </p>
          <div className="landing-cta-row">
            <button className="landing-cta-lg light" onClick={() => navigate('/login')}>
              Sign in
            </button>
            <button className="landing-cta-ghost light" onClick={() => navigate('/register')}>
              Request access
            </button>
          </div>
        </div>
      </section>

      {/* ── Footer ─────────────────────────────────────────────────────── */}
      <footer className="landing-footer">
        <div className="landing-footer-inner">
          <div className="landing-brand">
            <div className="landing-brand-mark"><Shield size={14} strokeWidth={2.2} color="#ffffff" /></div>
            <span className="landing-brand-text">ClaimInsight360</span>
          </div>
          <span className="landing-footer-meta">© 2026 ClaimInsight360 · Built for insurance carriers</span>
        </div>
      </footer>
    </div>
  );
}

// ── Sub-components ──────────────────────────────────────────────────────

function FeatureCard({ icon, title, text }: { icon: React.ReactNode; title: string; text: string }) {
  return (
    <div className="landing-feature-card">
      <div className="landing-feature-icon">{icon}</div>
      <div className="landing-feature-title">{title}</div>
      <div className="landing-feature-text">{text}</div>
    </div>
  );
}

function RoleCard({ tag, color, text }: { tag: string; color: string; text: string }) {
  return (
    <div className="landing-role-card">
      <div className="landing-role-tag" style={{ background: color }}>{tag}</div>
      <div className="landing-role-text">{text}</div>
    </div>
  );
}

function FlowStep({ n, title, text }: { n: string; title: string; text: string }) {
  return (
    <div className="landing-flow-step">
      <div className="landing-flow-num">{n}</div>
      <div className="landing-flow-content">
        <div className="landing-flow-title">{title}</div>
        <div className="landing-flow-text">{text}</div>
      </div>
    </div>
  );
}
