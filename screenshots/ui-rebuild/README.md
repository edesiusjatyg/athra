# Figma UI rebuild review

Visual authority: the local PDFs in [`ATHRA UI UX`](../../ATHRA%20UI%20UX/README.md).

The review images are native Compose **Layoutlib renders**, using production screen composables and deterministic fixtures at 390 dp, normal font scale. They are not emulator screenshots and do not establish system-bar, keyboard, gesture, or runtime-navigation behavior. Layoutlib omits system chrome; the Figma PDFs include an iPhone status bar. Compare content after the reference's top 44 points rather than treating the drawn iPhone chrome as Android UI.

## Reproduce the visual captures

```sh
./gradlew :app:updateDebugScreenshotTest
./gradlew :app:validateDebugScreenshotTest
```

The first command refreshes the review baselines from the current implementation. It does not compare against Figma automatically: inspect the local PDF beside the generated PNG before approving a baseline. The second command checks subsequent render changes against those approved PNGs.

Preview fixtures: [`FigmaPreviewCapture.kt`](../../app/src/screenshotTest/kotlin/id/ac/ub/athra/ui/FigmaPreviewCapture.kt).

Generated full-resolution images: [`reference/id/ac/ub/athra/ui/FigmaPreviewCapture`](../../app/src/screenshotTestDebug/reference/id/ac/ub/athra/ui/FigmaPreviewCapture/).

For actual device captures, run `id.ac.ub.athra.ui.FigmaScreenCaptureTest` via instrumentation. It saves PNGs in the app's external files directory, `ui-rebuild/`. This harness uses fixture data without requiring Firebase or Room.

## Scope

The eight main screens and six supported Prototype screens follow the PDFs' hierarchy, typography, palette, card grouping, selectors, and actions. Session Detail uses the same visual system; its content is distinct from the Exercise Detail PDF.

Indonesian labels and real account data remain authoritative per PRD. Prototype-only routines, planned-set denominators, streaks, goals, notification settings, rest timers, and unsupported preferences are not implemented as fictional working features. Equivalent card positions use supported session/account information where useful.

Rotation and font scale 200% QA are deferred by explicit user request. Existing runtime screenshots in `screenshots/runtime/` predate this rebuild and must not be presented as evidence of the rebuilt UI.

## Final visual review — 26 November 2026

- [Eight main screens](main-screens.png)
- [Seven supporting/detail screens](supporting-screens.png)

An independent GPT-6.1 Sol reviewer at high reasoning inspected all 15 final native Compose renders against the local references. Verdict: **ship for the agreed normal-font visual scope**. The reviewer marked the named findings resolved: Profile number wrapping/equal metric heights; supporting row typography and density; Login/Progress/Profile top placement; Summary metric sizes; Home hero/CTA grouping; Active Workout table and controls; Progress chart proportions; compact Edit Profile fields; and shared outline navigation icons.

This verdict establishes composition-level fidelity in the reviewed fixtures, not pixel-perfect identity or runtime verification. Indonesian text and supported-data substitutions remain intentional.

Final checks: debug and release APK builds, 14 unit tests, Android UI test compilation, lint, capture generation, and screenshot baseline validation all passed. Device instrumentation execution remains unverified after the rebuild for the environment reason above.
