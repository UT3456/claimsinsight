import { useEffect, useMemo, useReducer, useCallback, useState } from 'react';
import { Alert, Modal, Form, Input, App as AntApp } from 'antd';
import { RefreshCw, Calculator } from 'lucide-react';

import {
  PageHeader, DataCard, Badge, Chip, DarkButton, GhostButton, EmptyState,
} from '../../components/ui';
import { claimsApi, METRIC_NAMES, type ClaimKpi, type MetricName } from '../../api/claimsApi';

/**
 * KpiDefinitionsPage — reference screen #12 "KPI definitions".
 * 2-col card grid. Each card shows name, Active badge, formula in mono font,
 * and chips for unit/threshold. Formulas + chips are static metadata per
 * MetricName; the Active badge + sample count are derived live from the
 * existing /kpis endpoint so admins can see which metrics are being computed.
 */

// ── Static KPI catalogue ─────────────────────────────────────────────────────
// Maps every MetricName in the backend enum to its display metadata.
// Formula strings come from DESIGN_SPEC.md screen #12.

interface KpiDef {
  name: MetricName;
  displayName: string;
  formula: string;
  chips: string[];
}

const KPI_CATALOGUE: KpiDef[] = [
  {
    name: 'TAT',
    displayName: 'Avg TAT',
    formula: 'avg(settled_date − filed_date)',
    chips: ['Unit: days', 'SLA: 17'],
  },
  {
    name: 'CYCLE_TIME',
    displayName: 'Cycle time',
    formula: 'avg(close_date − open_date) per stage',
    chips: ['Unit: days', 'By stage'],
  },
  {
    name: 'SEVERITY',
    displayName: 'Severity',
    formula: 'avg(total_cost) per claim_type',
    chips: ['Unit: ₹', 'By product'],
  },
  {
    name: 'FREQUENCY',
    displayName: 'Frequency',
    formula: 'count(claims) / exposure_years',
    chips: ['Unit: per policy-yr', 'By product'],
  },
  {
    name: 'LOSS_RATIO',
    displayName: 'Loss ratio',
    formula: 'paid_losses / earned_premium',
    chips: ['Unit: %', 'Target: ≤70'],
  },
];

// ── Reducer ──────────────────────────────────────────────────────────────────

interface State {
  samples: Record<string, number>;  // metricName → row count in /kpis
  loading: boolean;
  error: string | null;
}

type Action =
  | { type: 'START' }
  | { type: 'SUCCESS'; payload: Record<string, number> }
  | { type: 'ERROR'; payload: string };

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case 'START':   return { ...s, loading: true, error: null };
    case 'SUCCESS': return { ...s, loading: false, samples: a.payload };
    case 'ERROR':   return { ...s, loading: false, error: a.payload };
    default:        return s;
  }
}

export default function KpiDefinitionsPage() {
  const [state, dispatch] = useReducer(reducer, {
    samples: {}, loading: false, error: null,
  });
  const [recalcOpen, setRecalcOpen]   = useState(false);
  const [recalcBusy, setRecalcBusy]   = useState(false);
  const [form] = Form.useForm();
  const { message } = AntApp.useApp();

  const handleRecalculate = async (values: { claimId: string }) => {
    setRecalcBusy(true);
    try {
      await claimsApi.calculate(values.claimId.trim());
      message.success(`KPIs recalculated for claim ${values.claimId.trim()}`);
      setRecalcOpen(false);
      form.resetFields();
      load();   // refresh sample counts
    } catch (err) {
      const msg = (err as { userMessage?: string }).userMessage ?? 'Failed to recalculate KPIs.';
      message.error(msg);
    } finally {
      setRecalcBusy(false);
    }
  };

  const load = useCallback(async () => {
    dispatch({ type: 'START' });
    try {
      const rows: ClaimKpi[] = await claimsApi.getAll();
      const counts: Record<string, number> = {};
      for (const r of rows) counts[r.metricName] = (counts[r.metricName] ?? 0) + 1;
      dispatch({ type: 'SUCCESS', payload: counts });
    } catch {
      dispatch({ type: 'ERROR', payload: 'Failed to load KPI activity.' });
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const activeCount = useMemo(
    () => Object.values(state.samples).filter(n => n > 0).length,
    [state.samples],
  );

  return (
    <div>
      <PageHeader
        title="KPI definitions"
        subtitle={`${KPI_CATALOGUE.length} metrics defined · ${activeCount} with live data`}
        actions={
          <>
            <GhostButton onClick={load} icon={<RefreshCw size={12} strokeWidth={1.8} />}>
              Refresh
            </GhostButton>
            <DarkButton
              icon={<Calculator size={12} strokeWidth={2} />}
              onClick={() => setRecalcOpen(true)}
            >
              Recalculate
            </DarkButton>
          </>
        }
      />

      {state.error && (
        <Alert type="warning" showIcon message={state.error} style={{ marginBottom: 12, borderRadius: 8 }} closable />
      )}

      {KPI_CATALOGUE.length === 0 ? (
        <EmptyState
          title="No KPIs defined"
          description="Define metrics in the backend’s MetricName enum to populate this page."
        />
      ) : (
        <div style={styles.grid}>
          {KPI_CATALOGUE.map((kpi) => {
            const count = state.samples[kpi.name] ?? 0;
            const live  = count > 0;
            return (
              <DataCard key={kpi.name}>
                <div style={styles.cardHead}>
                  <div>
                    <div style={styles.kpiName}>{kpi.displayName}</div>
                    <div style={styles.kpiEnum}>{kpi.name}</div>
                  </div>
                  <Badge tone={live ? 'green' : 'neutral'}>
                    {live ? 'Active' : 'Idle'}
                  </Badge>
                </div>

                <div style={styles.formula}>{kpi.formula}</div>

                <div style={styles.chipRow}>
                  {kpi.chips.map((label) => (
                    <Chip key={label}>{label}</Chip>
                  ))}
                </div>

                <div style={styles.sampleNote}>
                  {state.loading
                    ? 'Checking live data…'
                    : live
                      ? `${count.toLocaleString()} sample${count === 1 ? '' : 's'} computed`
                      : 'No samples computed yet'}
                </div>
              </DataCard>
            );
          })}
        </div>
      )}

      <div style={styles.footnote}>
        Metric names map to the backend <code style={styles.code}>MetricName</code> enum. Formulas shown here are specs —
        actual computations live in <code style={styles.code}>claims-metrics-service</code>.
        Use <strong>Recalculate</strong> to re-run all 6 KPIs for a specific claim ID.
      </div>

      <Modal
        title="Recalculate KPIs for a claim"
        open={recalcOpen}
        onCancel={() => { setRecalcOpen(false); form.resetFields(); }}
        onOk={() => form.submit()}
        okText="Recalculate"
        confirmLoading={recalcBusy}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={handleRecalculate}>
          <Form.Item
            name="claimId"
            label="Claim ID"
            rules={[{ required: true, min: 3, message: 'Enter a valid claim ID' }]}
            extra="Triggers POST /api/kpis/calculate/{claimId} — overwrites the existing 6 KPI rows for this claim."
          >
            <Input placeholder="CLM-2026-AUTO-001" autoFocus />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

void METRIC_NAMES;   // keep the import live — used as the enum source-of-truth

const styles: Record<string, React.CSSProperties> = {
  grid: {
    display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)',
    gap: 12,
  },
  cardHead: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start',
    marginBottom: 8, gap: 8,
  },
  kpiName: { fontSize: 13, fontWeight: 500, color: 'var(--ci-text-primary)', lineHeight: 1.3 },
  kpiEnum: {
    fontSize: 10, color: 'var(--ci-text-muted)', marginTop: 2,
    fontFamily: 'ui-monospace, SFMono-Regular, monospace', letterSpacing: '0.02em',
  },
  formula: {
    fontFamily: 'ui-monospace, SFMono-Regular, monospace',
    fontSize: 11,
    color: 'var(--ci-text-secondary)',
    background: 'var(--ci-bg-surface-2)',
    padding: '6px 8px',
    borderRadius: 5,
    marginBottom: 8,
  },
  chipRow: { display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 8 },
  sampleNote: { fontSize: 10, color: 'var(--ci-text-muted)' },
  footnote: {
    marginTop: 16, padding: '10px 12px',
    background: 'var(--ci-bg-surface-2)',
    borderRadius: 6, fontSize: 11,
    color: 'var(--ci-text-muted)', lineHeight: 1.5,
  },
  code: {
    fontFamily: 'ui-monospace, SFMono-Regular, monospace',
    background: 'var(--ci-bg-surface)', padding: '1px 5px',
    borderRadius: 3, border: '1px solid var(--ci-border)',
    fontSize: 10,
  },
};
