# ATHRA visual system

## Visual authority

The user-provided PDFs in `ATHRA UI UX/` are the visual authority. Rebuild the existing native Android screens to their composition; do not substitute default Material layouts or invent a new visual identity. The source is the Figma page **ATHRA – Minimal Redesign**. `ATHRA UI UX/README.md` maps the eight main screens and supporting flows.

The application is a workout logging tool. Prioritize readable workout data, clear selection and a single prominent primary action. Keep Indonesian interface copy and real account data per PRD. The PDF's sample names, statistics, planned routines, goals, notifications and unsupported preferences are not functionality to fabricate.

## Palette and type

| Role | Value |
| --- | --- |
| Background | `#0B0F0D` |
| Card surface | `#191E1B` |
| Primary text | `#F2F6F1` |
| Secondary text | `#A2ADA4` |
| Border | `#303B33` |
| Lime action | `#CAF374` |
| Forest | `#20382A` |
| Soft green | `#293B2E` |
| Muted surface | `#242C26` |

Inter is bundled locally. Regular, Medium, Semibold and Bold use static font instances generated from the repository's original Inter variable font, at optical size 14. Map all fifteen Material typography slots to Inter so controls cannot fall back to the default face. Headings are bold, section titles semibold, metadata subdued, and metric values bold.

## Geometry

- Reference phone frame: 390 × 844, with native Android system insets replacing the drawn iPhone status bar and home indicator.
- Main content horizontal inset: 24 dp.
- Cards: 16 dp radius; prominent workout/current-exercise cards may use 18–20 dp as in the reference.
- Main actions: 52 dp minimum height and 16 dp radius; lime background and near-black text.
- Category and period selectors: separate compact rounded chips.
- Bottom navigation: straight top divider, four outlined icons, labels, and a small lime marker above the selected destination.
- Use the local PDF composition for each screen's card heights and vertical rhythm. Retain scrolling for actual content.

## Screen composition

- **Home:** brand header, date and greeting, forest workout card, separate full-width start/resume action, three weekly metrics, recent sessions, bottom navigation.
- **Pre-workout:** compact back header, heading and description, forest start card, quick-start alternative and actual saved draft if present.
- **Active workout:** compact back header, session name and muscle groups, green elapsed-time/set-count card, current-exercise set table, next-exercise rows, pinned finish action. Additional set editing/reference/removal controls preserve real functionality without replacing the set-table composition.
- **Library:** compact header, title/description, rounded search field, inline category chips, section count and exercise cards with initials/equipment/quick-add action.
- **Summary:** completion mark, heading, session card, three metrics, exercise highlights and pinned save action. PR/reference details remain based on actual calculations.
- **Progress:** brand header, title, compact period chips, two primary metrics, labeled volume chart, and a compact supported-data card in the final section.
- **Profile:** header, profile card, actual weekly activity, grouped account rows and sign-out action.
- **Supporting screens:** use each corresponding Prototype PDF for headers, card grouping, field labels and pinned actions.

## Verification scope for this rebuild

Visual fidelity at the normal font scale is the current priority. Rotation and 200% font QA are explicitly deferred by the user. Keep existing functional behavior and tests, adapting selectors where the reference-driven structure or visible labels change. Screenshot fixtures are review data only, not account history or product defaults.
