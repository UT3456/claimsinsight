# ClaimInsight360 — Design System & Polish Spec

Companion to `claiminsight360-screens.html`. Open that file in a browser for the visual reference.

## How to use this with your existing frontend

You already have a working frontend. This is a **polish pass**, not a rewrite. Match the tokens, components, and screen patterns below — keep your existing routes, state management, and data layer intact.

## Design tokens

### Typography
- Font: system stack (`-apple-system, BlinkMacSystemFont, "Inter", "Segoe UI", sans-serif`)
- Weights: only 400 (regular) and 500 (medium). Never 600 or 700.
- Scale: page title 16px, section title 13px, body 12px, labels 11px, micro 10px

### Colors
```
Primary blue:   #185FA5   hover #0C447C
Text primary:   #2C2C2A
Text secondary: #5F5E5A
Text tertiary:  #888780
Borders:        #E8E6DF (default)  #D3D1C7 (emphasis)
Surface tint:   #F8F7F3
Page bg:        #F1EFE8
```

### Semantic colors (use for meaning, not decoration)
```
Success  bg #EAF3DE  text #27500A
Warning  bg #FAEEDA  text #633806
Danger   bg #FCEBEB  text #791F1F
Info     bg #E6F1FB  text #0C447C
```

### Chart palette
```
Blue    #378ADD   Teal   #5DCAA5
Amber   #EF9F27   Red    #E24B4A
Purple  #7F77DD   Coral  #D85A30
```

### Spacing
- Card padding: 14px 16px
- Card radius: 10px
- Button/chip radius: 6px
- Pill radius: 10-20px
- Input height: 38px (auth), 32px (inline controls)
- Gap between cards: 12px

## Universal polish rules

Apply these across every screen:

1. **No contradictory empty states.** If the API errors, show one error. If there's no data, show one empty state. Never both at once.

2. **One bold weight only.** Use 500 for emphasis. Never 600/700. If something looks too heavy, that's why.

3. **Sentence case everywhere.** "Claims overview" not "Claims Overview". Applies to buttons, headers, labels, chips.

4. **Color for meaning, not decoration.** Red = risk/error. Amber = warning/lagging. Green = healthy/good. Blue = info/active. Purple = actuarial/KPI config. Teal = financial/reserves. Don't reach for other colors.

5. **KPI cards need context.** Every KPI value gets a small delta line below it (▲ 3.2% vs last period). If there's no delta, write "No change" in muted gray.

6. **Tables use tinted avatars.** 24px circle, initials, background-color and text-color from the same ramp (amber-100 bg with amber-800 text). Each user gets a consistent color across screens.

7. **Status is a dot-prefix, not a box.** `● Healthy` in green text, `● Lagging` in amber, `● Error` in red. Full bordered chips are reserved for filter controls.

8. **Filter rows align right with the page title.** Title + subtitle on the left of the page header, filter chips + primary button on the right of the same row.

## Component patterns

### Page header (every inner screen)
```
[Title 16/500]              [chip] [chip] [primary btn]
[Subtitle 11/400 muted]
```

### KPI card
```
[Label 11px muted]
[Value 22px 500]
[Delta 11px green/red with ▲▼]
```
Background `#F8F7F3`, no border, radius 8px, padding 12/14.

### Data card (for charts, lists, tables)
Background `#fff`, `1px solid #E8E6DF`, radius 10px, padding 14/16.
Head row: title left, subtitle or link right.

### Badge vs chip vs button
- **Badge** (11px, radius 10px): shows state/category inline in tables. No border, tinted bg + matching text.
- **Chip** (11px, radius 6px, 1px border): interactive filter. Dropdown arrow if it opens a picker.
- **Button**: primary is `#2C2C2A` bg + white text OR `#185FA5` for main CTA. Ghost button is `#fff` bg + `1px solid #D3D1C7`.

### Empty state
Centered. 48px green/gray circle icon, 14px title, 12px muted subtitle (max 340px wide), two ghost buttons for actions (e.g. "Clear filters" + "Notification settings"). Never show an empty table and an error banner at the same time.

## Screen-specific notes

Refer to `claiminsight360-screens.html` for exact layouts. Key behaviors to implement:

| # | Screen | Key polish points |
|---|--------|-------------------|
| 01 | Login | SSO row above email form. Focused input gets blue ring. Show/hide password toggle inside input. |
| 02 | Register | 3-step progress bar at top. Inline email domain verification (✓ icon + helper text). Live password strength bar. |
| 03 | Claims overview | KPI delta colors matter: green for improvement, red for deterioration even when number went up. |
| 04 | Fraud & risk | KPI cards get red/amber tints when value is high-risk. Score badges use same color scale as bars. |
| 05 | Denials | Line chart in purple (denial = actuarial). Leakage bars in coral (financial harm). Spike badge for trend alerts. |
| 06 | Adjusters | Avatar colors stable per person. Quality badge: green ≥90, amber 75-89, red <75. Breach count red+bold when ≥10. |
| 07 | Reserves | Severity in teal, reserve comparison in red dashed. Donut uses chart palette, not semantic colors. |
| 08 | Claim drill-down | Timeline dots colored by event type (blue=created, green=assigned, amber=flagged, red=escalated). |
| 09 | Reports | Filter chips in the left rail use `bg #E6F1FB, text #0C447C`. Unselected metrics in muted gray with hollow checkbox. |
| 10 | Notifications | Each item has a 32px square icon (not circle). Critical items get tinted row bg (#FEF6F6 for red, #FFFBF2 for amber). Resolved items at 65% opacity. |
| 11 | Data feeds | Status dots, not pills. Failed row shows "Retry" as a blue link in the action column. |
| 12 | KPI definitions | Formula displayed in mono font on a `#F8F7F3` block. |
| 13 | Users & roles | Role badge color matches persona's semantic meaning (fraud=red, actuary=purple, etc.). |

## Responsive rules

Break at 768px. Below that:
- Sidebar becomes a top drawer (hamburger menu)
- KPI grids go 2-col (instead of 3 or 4)
- Auth pages stack vertically (brand panel becomes a compact header with just logo + tagline)
- Tables get horizontal scroll, not squished columns

## What NOT to do

- Don't add shadows. This design is flat — box-shadow only for focus rings (`0 0 0 3px rgba(24, 95, 165, 0.1)`).
- Don't add gradients except on the auth brand panels.
- Don't add icons to every label. Icons only when they carry information (status dots, severity, avatars).
- Don't create new colors. If you can't express it with the palette above, the hierarchy is wrong.
- Don't use emoji as UI icons in production. The HTML reference uses glyphs like ✉ ⚿ for speed — replace with proper SVG icons (Lucide, Heroicons) in code.
