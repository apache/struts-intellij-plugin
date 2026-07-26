# Show Diagram: Left-to-Right Hierarchical Layout

**Date:** 2026-07-26  
**Status:** Approved for implementation planning  
**Related:** [#122](https://github.com/apache/struts-intellij-plugin/issues/122); follow-up to [#117](https://github.com/apache/struts-intellij-plugin/issues/117) / [#119](https://github.com/apache/struts-intellij-plugin/pull/119) and [#120](https://github.com/apache/struts-intellij-plugin/issues/120) / [#123](https://github.com/apache/struts-intellij-plugin/pull/123)

## Problem

Show Diagram for a Struts config uses the platform default hierarchic layouter. For a small file-local graph that often places packages toward the bottom and results toward the top, so the package → action → result flow reads bottom-up instead of left-to-right.

Issue [#122](https://github.com/apache/struts-intellij-plugin/issues/122) correctly states the UX goal, but its framing is partly stale:

- It compares against the custom Swing Diagram tab (`Struts2DiagramComponent`) as the layout source of truth. After the Show Diagram migration, that tab is no longer the primary UX reference.
- The cited hooks (`DiagramExtras#getCustomLayouter`, `useDefaultLayouter`) remain valid on IntelliJ Platform 2026.2 and are still used by JetBrains diagram providers (e.g. Maven/Gradle UML extras).

## Goals

1. Initial Show Diagram layout for typical Struts configs reads left-to-right (package → action → result flow).
2. Soft preference: if the user picks a different layout algorithm via diagram chrome, Dom refresh does not reset that choice back to custom LTR.
3. Edge labels (e.g. `success`) remain readable.
4. No changes to `StrutsConfigDiagramModel` semantics.
5. Automated smoke coverage for the extras layouter orientation behavior, plus a short manual `runIde` check.
6. Changelog documents the Show Diagram layout default.

## Non-Goals

- Pixel-perfect three-column grid matching the old Swing host.
- Removing leftover Swing Diagram tab / `PerspectiveFileEditor` code (separate cleanup).
- Locking orientation so toolbar layout/orientation actions are ignored.
- Pixel/layout geometry assertions or Robot UI e2e.
- Changes to compact node chrome (#120), Dom refresh, navigation, or tooltips.

## Decisions

| Question | Decision |
|---|---|
| Layout style | Platform hierarchic group layouter oriented left-to-right by default |
| Soft vs fixed | Soft preference at the **layout algorithm** level — custom layouter is always hierarchic LTR; do not reset a user-chosen non-custom `DiagramLayout` on refresh |
| Swing tab comparison | Dropped as acceptance baseline; Maven/Gradle `getCustomLayouter` pattern is the reference |
| Model changes | None — extras only |
| Verification | Unit tests on extras orientation + manual `runIde` |

### Alternatives considered

| Approach | Verdict |
|---|---|
| `getCustomLayouter` always hierarchic LTR (Maven/Gradle pattern); soft = preserve user-selected alternate `DiagramLayout` | **Chosen** — reliable initial LTR; soft without fighting platform default orientation (`TOP_TO_BOTTOM`) |
| `useDefaultLayouter() = true` only | Rejected — weak/no control of initial orientation; unlikely to fix bottom-up graphs |
| Custom three-column `Layouter` (port Swing columns) | Rejected for this issue — higher maintenance; fights platform layout chrome; overkill for soft LTR preference |
| Always force LTR and ignore toolbar orientation | Rejected — conflicts with soft-preference requirement |

## Architecture

Show Diagram already builds a toolkit-neutral snapshot and maps it to API nodes/edges. This change only supplies a custom layouter from extras.

```
StrutsDiagramProvider
  └── StrutsDiagramExtras  (CommonDiagramExtras)
        └── getCustomLayouter(settings, project)
              ├── GraphManager.createHierarchicGroupLayouter()
              ├── OrientationLayouter = LEFT_TO_RIGHT (always, Maven-like)
              └── keep spacing/layerer tweaks minimal (only if needed for readability)
```

**Why not read `GraphSettings.getCurrentLayoutOrientation()` for the custom layouter?**  
Platform default orientation is top-to-bottom. Honoring settings blindly would keep the broken bottom-up/top-to-bottom graphs. Soft preference is instead: if the user picks another toolbar layout algorithm (e.g. Organic or platform Hierarchic), Dom refresh must leave that choice alone; only our custom layouter path is LTR.

**Unchanged:** `StrutsConfigDiagramModel`, `StrutsDiagramDataModel`, API node/edge adapters, Dom refresh, compact label chrome, Swing `fileEditor` / `ui`, `plugin.xml` registrations.

## Components

| Unit | Role |
|---|---|
| `StrutsDiagramExtras` | Override `getCustomLayouter`. Build a hierarchic group layouter and always set orientation to `LEFT_TO_RIGHT` (Maven/Gradle pattern). Leave `useDefaultLayouter()` at platform default (`false`). Keep `doEdgeLabeling()` default (`true`). Do not mutate `GraphSettings` current layouter on Dom refresh. |
| `StrutsDiagramProvider` | No logic change; still returns the same extras instance. |
| Snapshot / presentation | Untouched. |
| Swing tab (`diagram.fileEditor` / `diagram.ui`) | Untouched. |

No new production classes. No `plugin.xml` changes for this issue.

Implementation should follow the public IntelliJ graph APIs used by Maven/Gradle extras (`GraphManager`, `HierarchicGroupLayouter`, `LayoutOrientation`), not raw yFiles types.

## Data flow

### Initial open

1. User invokes Show Diagram on a Struts 2 config.
2. Platform asks extras for a custom layouter (or resolves `DiagramLayout.CUSTOM` / matching custom class).
3. Extras returns a hierarchic group layouter oriented `LEFT_TO_RIGHT`.
4. Graph lays out with main flow left-to-right; edge labels remain enabled.

### After user picks another layout algorithm

1. User selects a non-custom layout from the diagram toolbar (e.g. Organic, platform Hierarchic).
2. Platform stores that layouter on `GraphSettings`.
3. Dom refresh / data reload must not call `setCurrentLayouter` back to our custom LTR layouter.
4. `getCustomLayouter` may still return LTR hierarchic for the CUSTOM path, but the active toolbar choice remains whatever the user selected.

## Error handling & edge cases

| Scenario | Behavior |
|---|---|
| Default / first open / CUSTOM path | Hierarchic LTR |
| User selects another layout algorithm | That algorithm stays active across Dom refresh |
| User wants top-to-bottom | Choose platform Hierarchic (or equivalent) from toolbar — not our custom LTR path |
| Multi-package / chain / redirect edges | Same model semantics; hierarchic LTR only changes placement |
| Empty / unavailable model | Unchanged — no graph to lay out |
| Edge label `success` / similar | Remains readable (`doEdgeLabeling` stays on) |
| Swing Diagram tab | Unchanged |

## Testing

### Automated

Extend `StrutsDiagramProviderTest` (or a focused extras test):

1. Call `StrutsDiagramExtras.getCustomLayouter(...)` → assert a non-null hierarchic layouter oriented `LEFT_TO_RIGHT` (via the public orientation API available on 262).
2. Optionally assert a second call still returns LTR (custom path is stable; soft preference is “don’t reset GraphSettings,” not “read orientation from settings”).
3. Do not assert pixel coordinates or node bounding boxes.

Gate: `./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"`

If constructing a real `GraphSettings` instance is awkward in light tests, use the smallest platform-supported fixture or stub that still exercises the real extras branch (prefer real graph settings APIs over brittle mocks).

### Manual (`./gradlew runIde` on IU)

1. Show Diagram on a small Struts config → package → action → result reads left-to-right.
2. Confirm edge labels (e.g. `success`) remain readable.
3. Pick a different layout algorithm from the toolbar, edit the XML so Dom refresh runs → the user’s layout choice remains (not forced back to custom LTR).

### Out of scope

- Pixel/layout assertions
- Robot / UI e2e
- Swing tab removal
- Tests for #96–#100

## Issue framing update

When implementing, update [#122](https://github.com/apache/struts-intellij-plugin/issues/122) (comment or PR description) so acceptance criteria match this design:

- Drop Swing tab LTR as the comparison baseline.
- Keep the LTR hierarchical goal and model non-change constraint.
- Add soft-preference acceptance: a user-selected non-custom layout algorithm is not reset on Dom refresh.

## Future work (out of scope)

- Remove Swing Diagram tab once Show Diagram UX is solid
- [#96](https://github.com/apache/struts-intellij-plugin/issues/96) merged multi-file view
- [#98](https://github.com/apache/struts-intellij-plugin/issues/98) selection sync
- [#99](https://github.com/apache/struts-intellij-plugin/issues/99) Structure tool window
- [#100](https://github.com/apache/struts-intellij-plugin/issues/100) zoom/pan beyond platform chrome

## References

- Issue [#122](https://github.com/apache/struts-intellij-plugin/issues/122)
- `com.intellij.diagram.extras.DiagramExtras#getCustomLayouter`
- `com.intellij.diagram.extras.DiagramExtras#useDefaultLayouter`
- `com.intellij.openapi.graph.layout.LayoutOrientation`
- `com.intellij.openapi.graph.GraphManager#createHierarchicGroupLayouter`
- Maven/Gradle UML extras `getCustomLayouter` (platform reference pattern)
- `com.intellij.struts2.diagram.provider.StrutsDiagramExtras`
