# Prompt to paste into Claude Code

Copy everything below the line into Claude Code, after placing both `claiminsight360-screens.html` and `DESIGN_SPEC.md` in your project root.

---

I have a working frontend for ClaimInsight360, an insurance claims analytics platform. I need to polish it to match a new design reference. Two files are in the project root:

1. `claiminsight360-screens.html` — open this in a browser. It contains all 13 polished screens (login, register, claims overview, fraud & risk, denial & leakage, adjuster performance, reserves & cost, claim drill-down, reports, notifications, data feeds, KPI definitions, users & roles).

2. `DESIGN_SPEC.md` — design tokens (colors, typography, spacing), component patterns, per-screen polish notes, and universal rules.

Here's what I want you to do, in order:

**Step 1 — Audit.** Read `DESIGN_SPEC.md` fully. Open `claiminsight360-screens.html` and inspect the computed CSS for the components I'll reference (KPI cards, data cards, tables, badges, chips, sidebar nav, page headers). Then walk my codebase and list every screen I currently have, mapping it to the reference screen numbers. Tell me which screens already exist and which are missing. Do not make any changes yet.

**Step 2 — Design tokens.** Once I approve the audit, update my Tailwind config (or CSS variables) to match the tokens in the spec. Replace existing color names that conflict. Run the app and show me that existing screens still render.

**Step 3 — Shared components.** Extract or update these shared components to match the reference: sidebar nav, page header, KPI card, data card, badge, chip, button, avatar, empty state, status dot. Put them in `src/components/ui/`. Update existing screens to use them.

**Step 4 — Screen-by-screen polish.** Go through the 13 screens in the reference order. For each one:
- Compare my existing screen to the reference
- Apply the polish listed in the per-screen table in `DESIGN_SPEC.md`
- Fix the specific issues I've flagged (contradictory empty states, wrong weights, inconsistent colors)
- Pause and show me the result before moving to the next screen

**Step 5 — Responsive pass.** After all 13 are polished, verify responsive behavior at 1280px, 1024px, 768px, and 375px widths.

Rules for this work:

- Keep my existing routing, data layer, API calls, and state management untouched. Only change presentation.
- If my codebase already has a component that does the job (just with different styling), modify it — don't create a new one alongside.
- Match the reference visually — gradients, spacing, typography scale, color semantics. Don't substitute your own design choices.
- Ask before making destructive changes (deleting files, renaming routes, changing package dependencies).
- If the spec is ambiguous on any point, ask rather than guess.

Start with Step 1.
