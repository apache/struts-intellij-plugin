# Show Diagram: Action → Result Edge Arrowheads

**Date:** 2026-07-26  
**Status:** Approved for implementation planning  
**Related:** [#125](https://github.com/apache/struts-intellij-plugin/issues/125); follow-up to [#122](https://github.com/apache/struts-intellij-plugin/issues/122) / [#124](https://github.com/apache/struts-intellij-plugin/pull/124)

## Problem

After the Show Diagram LTR hierarchic layout fix (#122 / #124), package → action → result reads left-to-right correctly. Package → action edges use `DiagramRelationships.DEPENDENCY` (dashed line + `ANGLE` arrow). Action → result edges (and other labeled edges) are built with the short `DiagramRelationshipInfoAdapter(name, SOLID, label)` constructor, which passes null start/end arrows. Those edges render as plain solid curves without arrowheads, so flow direction is harder to read.

## Goals

1. Action → result edges show a clear directed arrow toward the result node.
2. Edge labels (e.g. `success`, `delete`) remain readable on the upper-center label.
3. Package → action stays visually distinct: dashed `DEPENDENCY` with `ANGLE` arrow, unlabeled.
4. Labeled chain/redirect action → action edges use the same solid + `ANGLE` + label styling (same mapping path).
5. Automated coverage for relationship arrow/label mapping, plus a short manual `runIde` check.
6. Changelog documents the Show Diagram arrowhead fix.

## Non-Goals

- Redesigning edge taxonomy (separate RESULT vs CHAIN presets, colors, etc.).
- Changing layout, compact node chrome, Dom refresh, navigation, or tooltips.
- Unifying package → action with labeled edges (dashed vs solid distinction stays).
- Swing Diagram tab pixel parity or Robot / UI e2e.
- Model semantic changes in `StrutsConfigDiagramModel`.

## Decisions

| Question | Decision |
|---|---|
| Visual distinction | Keep today’s dashed unlabeled package → action vs solid labeled result/chain edges |
| Arrow shape for labeled edges | `DiagramRelationshipInfo.ANGLE` (same chevron as `DEPENDENCY`) |
| Implementation API | `DiagramRelationshipInfoAdapter.Builder` for labeled edges |
| Scope of arrow fix | All non-empty-label edges (action → result and labeled chain/redirect) |
| Model / layout changes | None — relationship presentation only |
| Verification | Unit asserts on mapped relationships + manual `runIde` |

### Alternatives considered

| Approach | Verdict |
|---|---|
| Builder: solid + `ANGLE` + upper-center label for labeled edges; keep `DEPENDENCY` for empty labels | **Chosen** — matches platform `DEPENDENCY` construction; smallest clear fix |
| Full positional `DiagramRelationshipInfoAdapter` constructor with arrow shapes | Rejected — easy to mis-order args; less idiomatic on 262 |
| Per-kind relationship presets (RESULT vs CHAIN vs PACKAGE) | Rejected for #125 — out of scope redesign |
| Unify all edges to one style | Rejected — user chose to keep dashed vs solid distinction |

## Architecture

Root cause is presentation-only in the Show Diagram edge adapter. Snapshot edges already carry direction and labels; only the Diagrams `DiagramRelationshipInfo` lacks a target arrow for labeled edges.

```
StrutsConfigDiagramModel (unchanged)
  └── StrutsDiagramEdge (source, target, label)
        └── StrutsDiagramApiEdge.relationshipFor(edge)
              ├── label empty  → DiagramRelationships.DEPENDENCY
              │                    (DASHED + ANGLE target arrow, no label)
              └── label present → DiagramRelationshipInfoAdapter.Builder
                                   .setName(label)
                                   .setLineType(SOLID)
                                   .setTargetArrow(ANGLE)
                                   .setUpperCenterLabel(label)
                                   .create()
```

**Why the short constructor fails:**  
`new DiagramRelationshipInfoAdapter(label, DiagramLineType.SOLID, label)` delegates to the full ctor with null start/end arrow shapes, so the graph paints a plain curve.

**Unchanged:** `StrutsConfigDiagramModel`, `StrutsDiagramDataModel` structure, nodes, extras/layouter, Dom refresh, compact labels, Swing `fileEditor` / `ui`, `plugin.xml`.

## Components

| Unit | Role |
|---|---|
| `StrutsDiagramApiEdge` | Only production change: Builder-based labeled relationship with `ANGLE` target arrow; empty label still `DEPENDENCY` |
| `StrutsDiagramDataModelMappingTest` | Extend relationship verification: labeled edges keep upper-center label, solid line, target/end arrow = `ANGLE`; unlabeled still `DEPENDENCY` |
| Snapshot / provider / extras / Swing | Untouched |

No new production classes. No `plugin.xml` changes for this issue.

## Data flow

1. User opens Show Diagram on a Struts config.
2. Data model maps snapshot edges to `StrutsDiagramApiEdge`.
3. Package → action (empty label) → dashed dependency arrow.
4. Action → result / labeled chain-redirect (non-empty label) → solid curve with `ANGLE` at the target and the result name as upper-center label.
5. Dom refresh rebuilds the same mapping; styling remains presentation-only.

## Error handling & edge cases

| Scenario | Behavior |
|---|---|
| Result name missing | Model already uses `Result.DEFAULT_NAME` as label → solid + arrow + that label |
| Chain/redirect same-file action → action | Labeled edge → solid + `ANGLE` + label (same path) |
| External/unresolved result node | Still a labeled action → result edge → solid + `ANGLE` + label |
| Package → action | Unchanged `DEPENDENCY` |
| Empty model / unavailable diagram | Unchanged |

## Testing

### Automated

Extend `StrutsDiagramDataModelMappingTest.verifyRelationshipMapping` (or sibling asserts) after mapping a fixture that includes package → action and action → result (preferably also one labeled chain/redirect):

1. Unlabeled edges: `getRelationship() == DiagramRelationships.DEPENDENCY`.
2. Labeled edges: not `DEPENDENCY`; upper-center label equals snapshot label; line type solid; `getTargetArrow()` / `getEndArrow()` is `DiagramRelationshipInfo.ANGLE`.

Gate: `./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"`

Do not assert pixel geometry or rendered Swing strokes.

### Manual (`./gradlew runIde` on IU)

1. Show Diagram on a small Struts config with action → JSP results.
2. Confirm action → result edges show an arrow toward the result node.
3. Confirm labels such as `success` / `delete` remain readable.
4. Confirm package → action remains dashed (visually distinct).

### Out of scope

- Pixel/layout assertions
- Robot / UI e2e
- Swing tab removal or restyling
- Tests for #96–#100

## Future work (out of scope)

- Remove Swing Diagram tab once Show Diagram UX is solid
- Optional richer edge taxonomy (distinct chain vs dispatcher styles)
- [#96](https://github.com/apache/struts-intellij-plugin/issues/96) merged multi-file view
- [#98](https://github.com/apache/struts-intellij-plugin/issues/98) selection sync
- [#99](https://github.com/apache/struts-intellij-plugin/issues/99) Structure tool window
- [#100](https://github.com/apache/struts-intellij-plugin/issues/100) zoom/pan beyond platform chrome

## References

- Issue [#125](https://github.com/apache/struts-intellij-plugin/issues/125)
- `com.intellij.diagram.DiagramRelationshipInfoAdapter` / `Builder`
- `com.intellij.diagram.DiagramRelationships.DEPENDENCY`
- `com.intellij.diagram.DiagramRelationshipInfo.ANGLE`
- `com.intellij.struts2.diagram.provider.StrutsDiagramApiEdge`
- Prior LTR layout design: `docs/superpowers/specs/2026-07-26-show-diagram-ltr-layout-design.md`
)