# Show Diagram: Compact Icon+Label Nodes

**Date:** 2026-07-25  
**Status:** Approved for implementation planning  
**Related:** [#120](https://github.com/apache/struts-intellij-plugin/issues/120); follow-up to [#117](https://github.com/apache/struts-intellij-plugin/issues/117) / [#119](https://github.com/apache/struts-intellij-plugin/pull/119); prior migration design `2026-07-25-diagrams-api-migration-design.md`

## Problem

After migrating to the IntelliJ **Show Diagram** host, Struts package/action/result nodes render with the default UML-style chrome (`DiagramNodeContainer`: header + empty body). Nodes look oversized and harder to read than the custom Swing Diagram tab, which paints compact icon + name chips.

Root cause: `StrutsDiagramExtras` extends plain `DiagramExtras` and does not override `createNodeComponent`. The default implementation builds `DiagramNodeContainer`. `StrutsDiagramElementManager.getNodeItems` returns `EMPTY_ARRAY`, so the UML body is empty.

Icons and titles already exist on the API path (`StrutsDiagramApiNode.getIcon()`, `getElementTitle`); only node chrome is wrong.

## Goals

1. Show Diagram nodes use compact platform icon+label chrome (no empty UML body).
2. Package / action / result icons and names remain visible.
3. Swing Diagram tab behavior stays unchanged while both hosts coexist.
4. Navigation, tooltips, and Dom refresh stay unchanged.
5. Automated smoke asserts label-style components (not `DiagramNodeContainer`), plus a short manual `runIde` check.

## Non-Goals

- Custom Swing-matching colored rounded chips for Show Diagram.
- Removing the Swing Diagram tab / `PerspectiveFileEditor` path (separate from #120).
- Merged multi-file view (#96), selection sync (#98), Structure tool window (#99), zoom/pan beyond platform chrome (#100).
- Pixel/layout assertions or Robot UI e2e.
- Changing snapshot model semantics or edge rendering.

## Decisions

| Question | Decision |
|---|---|
| Visual style | Platform compact label (`CommonDiagramExtras.createLabelNode` / `SimpleColoredComponent`) |
| Swing tab | Untouched for this issue |
| Verification | Automated component-type smoke + manual `runIde` |
| Implementation approach | Extend `CommonDiagramExtras`; override both `createNodeComponent` overloads to return `createLabelNode(...)` |

### Alternatives considered

| Approach | Verdict |
|---|---|
| Extend `CommonDiagramExtras`, `createNodeComponent` → `createLabelNode` | **Chosen** — matches issue proposal; least custom UI; same direction as Spring Integration diagrams |
| Stay on `DiagramExtras`, hand-build `SimpleColoredComponent` | Rejected — duplicates platform label logic; higher break risk |
| Keep UML box, hide empty body | Rejected — still UML chrome; fails compact icon+label goal |
| Custom colored rounded chips matching Swing | Rejected for this issue — more maintenance; user chose platform label style |

## Architecture

Show Diagram already builds a toolkit-neutral snapshot and maps it to API nodes/edges. This change only swaps node chrome at the extras hook.

```
StrutsDiagramProvider
  └── StrutsDiagramExtras  (extends CommonDiagramExtras)
        createNodeComponent(...) → createLabelNode(...)
          └── SimpleColoredComponent (icon + title)
```

**Unchanged:** `StrutsConfigDiagramModel`, `StrutsDiagramDataModel`, element manager title/tooltip APIs, API node/edge adapters, Dom refresh, Swing `fileEditor` / `ui`, `plugin.xml` registrations.

## Components

| Unit | Role |
|---|---|
| `StrutsDiagramExtras` | Extend `CommonDiagramExtras<StrutsDiagramItem>` instead of `DiagramExtras`. Override both `createNodeComponent` overloads (`NodeRealizer` and `Point`) to return `createLabelNode(node, builder, wrapper)`. Keep existing `EditNodeHandler` and `uiDataSnapshot`. |
| `StrutsDiagramProvider` | No logic change; still returns the same extras instance. |
| `StrutsDiagramElementManager` / `StrutsDiagramApiNode` | Unchanged — title via `getElementTitle`, icon via `DiagramNode.getIcon()`, tooltips unchanged. |
| Swing tab (`diagram.fileEditor` / `diagram.ui`) | Untouched. |

No new production classes. No `plugin.xml` changes for this issue.

`CommonDiagramExtras` may bring platform helpers/categories (e.g. borders/selection). Accept platform defaults for label nodes; do not add custom category UI unless required for `createLabelNode` to work.

## Data flow

### Render (changed chrome only)

1. User invokes Show Diagram on a Struts 2 config.
2. Data model publishes snapshot-backed API nodes/edges (unchanged).
3. Platform asks extras for a node component.
4. `createNodeComponent` returns `createLabelNode(...)`.
5. Platform paints a `SimpleColoredComponent` with:
   - title from element manager presentable title / `getElementTitle`
   - icon from the diagram node (`StrutsDiagramApiNode.getIcon()` via deferred evaluator)

### Interact / refresh (unchanged)

- Hover → precomputed tooltip HTML from the snapshot.
- Double-click / navigate → `EditNodeHandler` / `StrutsDiagramPresentation.navigateToElement`.
- Same-file DomEvents → debounced `refreshDataModel()`.

## Error handling & edge cases

| Scenario | Behavior |
|---|---|
| Package / action / result node | Compact icon + name; no empty UML body |
| Missing icon | Label still shows name |
| Missing / empty title | Platform label with empty/minimal text; no crash |
| Root file item (not a graph node) | Unchanged; not rendered as a graph node (`isAcceptableAsNode` false) |
| Navigation / tooltips / Dom refresh | Unchanged |
| Swing Diagram tab | Unchanged |

## Testing

### Automated

Extend `StrutsDiagramProviderTest` or add a focused extras test:

1. Build a small Struts snapshot node (package/action/result) → `StrutsDiagramApiNode`.
2. Call `StrutsDiagramExtras.createNodeComponent(...)` with the lightest `DiagramBuilder` / wrapper the 262 Diagrams test APIs allow.
3. Assert the result is a label-style component (`SimpleColoredComponent` or equivalent), **not** `DiagramNodeContainer`.
4. Assert icon and title are present for that node kind.

If a full `DiagramBuilder` is impractical in light tests, use the smallest fixture that still exercises the real override (not a pure mock that never calls platform label code). Keep existing provider/mapping/dom-refresh tests green.

Gate: `./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"`

### Manual (`./gradlew runIde` on IU)

1. Show Diagram on a Struts config → nodes are compact icon+label.
2. Package / action / result icons and names remain visible.
3. Double-click → navigates to XML; hover → tooltips.
4. Confirm Swing Diagram tab still behaves as before.

### Out of scope

- Pixel/layout assertions
- Robot / UI e2e
- Swing tab removal
- Tests for #96–#100

## Future work (out of scope)

- Remove Swing Diagram tab once Show Diagram UX is solid (deferred from migration design; not part of #120)
- [#96](https://github.com/apache/struts-intellij-plugin/issues/96) merged multi-file view
- [#98](https://github.com/apache/struts-intellij-plugin/issues/98) selection sync
- [#99](https://github.com/apache/struts-intellij-plugin/issues/99) Structure tool window
- [#100](https://github.com/apache/struts-intellij-plugin/issues/100) zoom/pan beyond platform chrome

## References

- Issue [#120](https://github.com/apache/struts-intellij-plugin/issues/120)
- `com.intellij.diagram.extras.DiagramExtras#createNodeComponent`
- `com.intellij.diagram.extras.custom.CommonDiagramExtras#createLabelNode`
- `com.intellij.struts2.diagram.provider.StrutsDiagramExtras`
- `com.intellij.struts2.diagram.provider.StrutsDiagramProvider`
