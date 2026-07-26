# Show Diagram: Result Edge Label Spacing in LTR Layout

**Date:** 2026-07-26  
**Status:** Approved for implementation planning  
**Related:** [#128](https://github.com/apache/struts-intellij-plugin/issues/128); follow-up to [#122](https://github.com/apache/struts-intellij-plugin/issues/122) / [#124](https://github.com/apache/struts-intellij-plugin/pull/124)

## Problem

After Show Diagram switched to a custom left-to-right hierarchic layouter (#122 / #124), typical package → action → result flow reads correctly. Manual smoke found that when one action has multiple results (e.g. `success` and `delete`), the action → result edge labels sit on top of each other and too close to the action node chrome, so they are hard to read.

Issue [#128](https://github.com/apache/struts-intellij-plugin/issues/128) frames this as edge-label placement / layouter spacing, not model semantics. The LTR plan deferred Maven-style distance tweaks as YAGNI unless readability suffered; this issue is that follow-up.

## Goals

1. Multiple result edge labels from one action are readable without overlapping each other.
2. Labels do not collide with the action node chrome.
3. LTR package → action → result flow from #122 remains intact.
4. Upper-center edge labels and relationship styling (including arrowheads from #125) stay as they are.
5. Automated coverage for the custom layouter distance settings, plus a short manual `runIde` check.
6. Changelog documents the Show Diagram label-spacing fix.

## Non-Goals

- Changing edge label model / placement (upper-center stays).
- Adopting Maven/Gradle `createBFSLayerer()` (layer assignment, not label spacing).
- Tuning spacing for non-custom toolbar layouts (Organic, platform Hierarchic, etc.).
- Generous spacing for dense 4–5+ result actions beyond a minimal 2–3 result fix.
- Model semantics, Dom refresh, compact node chrome, navigation, tooltips.
- Swing Diagram tab pixel parity or Robot / UI e2e.
- Pixel/layout geometry assertions.

## Decisions

| Question | Decision |
|---|---|
| Primary lever | Layouter spacing only (`setMinimalNodeDistance` / `setMinimalLayerDistance`) |
| Platform defaults | Node `20.0`; layer `40.0` on IU 2026.2 |
| Chosen values | Node `40.0`; layer `60.0` — both above platform defaults |
| Label model | Unchanged (keep upper-center labels) |
| Scope of tweak | Custom LTR layouter from `getCustomLayouter` only |
| BFS layerer | Out of scope |
| Soft layout preference (#122) | Unchanged — do not mutate `GraphSettings` on Dom refresh |
| Verification | Unit asserts on layouter distances + manual `runIde` |

### Alternatives considered

| Approach | Verdict |
|---|---|
| `setMinimalNodeDistance(40.0)` + `setMinimalLayerDistance(60.0)` on the custom LTR layouter; no label-model change; skip BFS | **Chosen** — a moderate bump above the IU 2026.2 defaults for typical actions with 2–3 results |
| Maven/Gradle distances (`20` / `20`) | Rejected — their unlabeled graph compaction is the wrong precedent; node `20` is a no-op versus the platform default and layer `20` halves the default `40` |
| Spring labeled-graph distances (`40` / `80`) | Rejected as unnecessarily generous for typical 2–3 result actions; `40` / `60` retains the useful node bump with a moderate layer bump |
| Node distance only | Rejected — may fix label-vs-label overlap but leave labels cramped against the action node |
| Full Maven extras parity including `createBFSLayerer()` | Rejected — more behavioral change than needed for label spacing |
| Label placement / model changes first | Rejected — user chose spacing-only |
| Apply spacing to other toolbar layouts | Rejected — custom LTR path only |

## Architecture

Root cause is insufficient hierarchic spacing on the custom LTR layouter path. Snapshot edges, relationship Builder (solid + `ANGLE` + upper-center label), and `doEdgeLabeling` remain correct. IU 2026.2 defaults to node distance `20.0` and layer distance `40.0`; the custom path raises these to `40.0` and `60.0`.

```
StrutsDiagramProvider
  └── StrutsDiagramExtras  (CommonDiagramExtras)
        └── getCustomLayouter(settings, project)
              ├── GraphManager.createHierarchicGroupLayouter()
              ├── OrientationLayouter = LEFT_TO_RIGHT
              ├── setMinimalNodeDistance(40.0)
              └── setMinimalLayerDistance(60.0)
```

**Why both distances:** In LTR hierarchy, results stack in the same layer (vertical separation → node distance), while action and result sit in adjacent layers (horizontal gap → layer distance). Raising both above their platform defaults gives labels more vertical and horizontal room. Spring labeled graphs use `40` / `80`; `40` / `60` is a moderate bump for typical 2–3 result actions.

**Unchanged:** `StrutsConfigDiagramModel`, `StrutsDiagramDataModel`, `StrutsDiagramApiEdge`, Dom refresh, compact label chrome, Swing `fileEditor` / `ui`, `plugin.xml`. Soft preference from #122 remains: Dom refresh must not call `GraphSettings.setCurrentLayouter`.

## Components

| Unit | Role |
|---|---|
| `StrutsDiagramExtras` | Only production change: after LTR orientation, set minimal node/layer distances to `40.0` / `60.0` |
| `StrutsDiagramProviderTest` | Extend custom-layouter test to assert node/layer distances `40.0` / `60.0` (keep LTR assertion) |
| Snapshot / API edge / Dom refresh / Swing | Untouched |

No new production classes. No `plugin.xml` changes for this issue.

## Data flow

### Initial open / CUSTOM path

1. User invokes Show Diagram on a Struts 2 config.
2. Platform asks extras for the custom layouter.
3. Extras returns a hierarchic group layouter oriented `LEFT_TO_RIGHT` with node/layer distances `40.0` / `60.0`.
4. Graph lays out with LTR flow; edge labeling remains enabled; multi-result labels have more room.

### After user picks another layout algorithm

1. User selects a non-custom layout from the diagram toolbar.
2. Platform stores that layouter on `GraphSettings`.
3. Dom refresh must not force spacing or custom LTR back onto that choice.
4. Spacing tweaks apply only when the custom layouter path is active.

## Error handling & edge cases

| Scenario | Behavior |
|---|---|
| Action with 2–3 results (e.g. `success` + `delete`) | Labels readable; no mutual overlap; clear of action chrome (manual smoke) |
| Action with a single result | Unchanged readability; slightly roomier LTR graph |
| Dense 4–5+ result actions | Best-effort only; not the acceptance bar for this issue |
| User-selected Organic / platform Hierarchic | Platform defaults; our distances do not apply |
| Soft preference / Dom refresh | Non-custom toolbar layout choice preserved |
| Empty / unavailable model | Unchanged — no graph to lay out |
| Swing Diagram tab | Unchanged |

## Testing

### Automated

Extend `StrutsDiagramProviderTest.testCustomLayouterIsHierarchicLeftToRight` (or a focused sibling):

1. Call `StrutsDiagramExtras.getCustomLayouter(...)` → assert hierarchic layouter oriented `LEFT_TO_RIGHT`.
2. Assert `getMinimalNodeDistance() == 40.0` and `getMinimalLayerDistance() == 60.0`.
3. Do not assert pixel coordinates, label bounding boxes, or rendered overlap.

Gate: `./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"`

### Manual (`./gradlew runIde` on IU)

1. Show Diagram on a Struts config whose action has at least two named results (e.g. `success` and `delete`).
2. Confirm result edge labels are readable without overlapping each other.
3. Confirm labels do not collide with the action node chrome.
4. Confirm package → action → result still reads left-to-right.

### Out of scope

- Pixel/layout assertions
- Robot / UI e2e
- Swing tab removal
- Tests for #96–#100

## Future work (out of scope)

- Label-model / placement tweaks if spacing alone proves insufficient after smoke
- BFS layerer or other Maven extras parity
- Spacing for non-custom toolbar layouts
- Remove Swing Diagram tab once Show Diagram UX is solid
- [#96](https://github.com/apache/struts-intellij-plugin/issues/96) merged multi-file view
- [#98](https://github.com/apache/struts-intellij-plugin/issues/98) selection sync
- [#99](https://github.com/apache/struts-intellij-plugin/issues/99) Structure tool window
- [#100](https://github.com/apache/struts-intellij-plugin/issues/100) zoom/pan beyond platform chrome

## References

- Issue [#128](https://github.com/apache/struts-intellij-plugin/issues/128)
- Prior LTR layout design: `docs/superpowers/specs/2026-07-26-show-diagram-ltr-layout-design.md`
- Prior arrowheads design: `docs/superpowers/specs/2026-07-26-show-diagram-result-edge-arrowheads-design.md`
- `com.intellij.struts2.diagram.provider.StrutsDiagramExtras`
- `com.intellij.openapi.graph.layout.hierarchic.HierarchicLayouter#setMinimalNodeDistance`
- `com.intellij.openapi.graph.layout.hierarchic.HierarchicLayouter#setMinimalLayerDistance`
- IU 2026.2 `HierarchicLayouter` defaults: node `20.0`, layer `40.0`
- Maven/Gradle UML extras `getCustomLayouter` (`20` / `20`; rejected unlabeled-compaction precedent)
- Spring labeled-graph extras (`40` / `80`; reference point for a more generous spacing policy)
