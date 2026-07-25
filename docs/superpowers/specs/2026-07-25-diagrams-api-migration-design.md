# Migrate Struts Diagram to IntelliJ Diagrams API

**Date:** 2026-07-25  
**Status:** Approved for implementation planning  
**Related:** [#117](https://github.com/apache/struts-intellij-plugin/issues/117); prior blank-tab host fix [#118](https://github.com/apache/struts-intellij-plugin/pull/118) / `2026-07-25-diagram-blank-262-design.md`; auto-refresh `2026-06-25-diagram-auto-refresh-design.md`; model boundary in `com.intellij.struts2.diagram.model` package-info

## Problem

The struts.xml **Diagram** tab is a custom Swing panel (`Struts2DiagramComponent`) hosted by `PerspectiveFileEditor`. That host owns layout, zoom, and lifecycle, which has already caused platform-version regressions (blank tab on 262). The model layer (`StrutsConfigDiagramModel`, nodes/edges) and presentation helpers were intentionally kept toolkit-neutral so rendering can move to the platform Diagrams API.

Related UX follow-ups (#96 multi-file, #98 selection sync, #99 Structure tool window, #100 zoom/pan) would largely come for free—or be simpler—on the IDE diagram chrome, but are **not** in scope for this migration.

## Goals

1. Provide a Struts config diagram via the IntelliJ **Show Diagram** entry (Diagrams API), not a custom editor tab.
2. Packages, actions, results, and edges match current model semantics (including chain/redirect action→action edges).
3. Tooltips and double-click / navigate-to-XML still work via `StrutsDiagramPresentation`.
4. Auto-refresh on DOM changes (or equivalent Diagrams refresh) while the diagram is open/visible.
5. Plugin declares a **hard** dependency on `com.intellij.diagram`; builds and tests against IU.
6. Custom Swing diagram UI and `PerspectiveFileEditor` path are removed.
7. Changelog documents the migration and UX change (tab removed → Show Diagram).

## Non-Goals

- Fixing blank Diagram tab lifecycle of the current Swing host (already tracked/fixed separately).
- Rewriting model building or Struts DOM converters.
- Merged multi-file view (#96), selection sync (#98), Structure tool window (#99), or custom zoom/pan beyond platform chrome (#100).
- Optional dependency / Community Edition fallback / keeping a Swing tab as backup.
- Robot / UI e2e tests or layout pixel assertions.
- Experimental Diagrams “v2” Builder API.

## Decisions

| Question | Decision |
|---|---|
| Entry point | **Show Diagram only** — remove the Diagram editor tab |
| `com.intellij.diagram` dependency | **Hard** (plugin already targets IU features such as JavaEE/JSP) |
| When Show Diagram is available | Any file recognized as a Struts 2 config (`StrutsManager.isStruts2ConfigFile`), **not** limited to file-set membership |
| Scope | Pure host swap; #96–#100 remain separate issues |
| Implementation approach | Classic `DiagramProvider` + snapshot `DiagramDataModel` consuming `StrutsConfigDiagramModel.build()` |

### Alternatives considered

| Approach | Verdict |
|---|---|
| Keep Diagram editor tab backed by Diagrams API | Rejected — retains custom editor lifecycle |
| Show Diagram only (this design) | **Chosen** — matches Ultimate diagram UX; escapes `PerspectiveFileEditor` |
| Both tab + Show Diagram | Rejected for v1 — two hosts to maintain |
| Optional `com.intellij.diagram` + Swing fallback | Rejected — unnecessary given IU-only surface |
| Experimental Diagrams v2 Builder API | Rejected — stability risk for Apache plugin on 262 |

## Architecture

```
struts.xml (Psi/XmlFile)
        │
        ▼
StrutsConfigDiagramModel.build()     ← unchanged (toolkit-neutral snapshot)
        │
        ▼
StrutsDiagramDataModel                 ← NEW: DiagramDataModel adapter
  maps StrutsDiagramNode/Edge
  → Diagrams API nodes/edges
        │
        ▼
StrutsDiagramProvider                  ← NEW: DiagramProvider + EP
  Show Diagram action / dedicated diagram editor
        │
        ├── tooltips / navigate  → StrutsDiagramPresentation (unchanged)
        └── live refresh         → DomEvent (while diagram open) → rebuild snapshot → refreshDataModel
```

**Kept:** `diagram.model`, `diagram.presentation`  
**Removed:** `diagram.ui`, `diagram.fileEditor`, `PerspectiveFileEditorProvider` registration  
**Added:** adapter package (e.g. `diagram.provider`) + hard `com.intellij.diagram` dependency  
**Semantics:** Still file-local packages/actions/results + chain/redirect action→action edges — no multi-file merge

## Components

| Unit | Role |
|---|---|
| `StrutsConfigDiagramModel` / `StrutsDiagramNode` / `StrutsDiagramEdge` | Unchanged snapshot builder and graph types |
| `StrutsDiagramPresentation` | Unchanged tooltips + navigate-to-XML |
| `StrutsDiagramProvider` | `DiagramProvider` for Struts config; accept ≈ `isStruts2ConfigFile`; wires element manager, VFS resolver, data model factory |
| `StrutsDiagramDataModel` | Holds current snapshot; `refreshDataModel()` rebuilds via `StrutsConfigDiagramModel.build(xmlFile)` and republishes nodes/edges |
| Node/edge adapters | Thin wrappers: id, kind, name, icon, tooltip from snapshot; double-click → `navigateToElement` |
| Dom refresh bridge | While the data model for file F is alive, DomEvents for F debounce (~300 ms) and call `refreshDataModel()` |
| `plugin.xml` / Gradle | Hard depend on `com.intellij.diagram`; register `com.intellij.diagram.Provider`; remove `Struts2DiagramFileEditorProvider` |
| Deleted | `Struts2DiagramComponent`, `Struts2DiagramFileEditor`, `Struts2DiagramFileEditorProvider` |

No new public plugin extension points. Follow-ups #96–#100 stay out of this package set.

Exact Diagrams API type names (`DiagramProvider` vs historical `Provider`, element manager interfaces, etc.) are resolved against the 262 IU SDK during implementation; the responsibilities above are fixed.

## Data flow

### Open

1. User invokes Show Diagram on a Struts 2 config `XmlFile`.
2. `StrutsDiagramProvider` accepts the element.
3. Platform creates `StrutsDiagramDataModel` for that file.
4. Initial `refreshDataModel()` runs under a read action → `StrutsConfigDiagramModel.build(xmlFile)` → adapters publish nodes/edges.
5. Diagrams UI renders with platform layout/chrome (zoom/pan/toolbar).

### Interact

- Hover → precomputed tooltip HTML from the snapshot (via presentation helpers).
- Double-click / navigate → `StrutsDiagramPresentation.navigateToElement`.

### Live refresh

Replaces tab `selectNotify` / `deselectNotify` gating from the auto-refresh design:

- While the Struts diagram data model for file F is alive (diagram editor/popup still open): DomEvents for F debounce 300 ms → rebuild snapshot → `refreshDataModel()`.
- No Diagram tab → no `selectNotify` catch-up; an already-open diagram refreshes on DomEvents; after dispose, nothing until Show Diagram again.
- Other files’ DomEvents ignored; multi-file set merge remains out of scope.

### Close / dispose

- Dom listener and alarms dispose with the diagram/data-model lifecycle so closed diagrams do not refresh.

## Error handling & edge cases

| Scenario | Behavior |
|---|---|
| File not a Struts 2 config | Provider rejects; Show Diagram not offered |
| `build()` returns `null` | Empty diagram + platform empty state (or provider message); no crash |
| Model with no nodes | Empty diagram; same intent as today’s empty placeholder |
| Unresolved / external chain-redirect targets | Keep current model semantics (labeled RESULT fallback) |
| DomEvent after diagram closed | No-op — listener disposed / gated on open model |
| Rapid edits while diagram open | 300 ms debounce; coalesce rebuilds |
| Invalid / deleted file while diagram open | Refresh yields empty/unavailable; no PSI access on EDT outside read actions |
| Navigation pointer stale | `navigateToElement` no-ops safely (existing behavior) |
| Exceptions during build | Caught/logged by read-action path; diagram stays on last good snapshot or empty |

## Testing

### Automated

- Keep `StrutsConfigDiagramModelTest` as the semantic source of truth (nodes/edges, chain/redirect).
- Replace editor-provider / `PerspectiveFileEditor` lifecycle tests with provider-focused tests:
  - Accept: Struts config yes; non-Struts XML no; **file-set membership not required**
  - Adapter mapping: snapshot nodes/edges → Diagrams nodes/edges (ids, kinds, edge labels) without full UI paint
  - Dom-refresh helper: same-file vs other-file filtering (pure static helpers, as today)

### Out of scope for v1

- Robot / UI e2e for Show Diagram chrome
- Layout pixel assertions
- Tests for #96–#100

### Manual (`./gradlew runIde` on IU)

1. Show Diagram on a Struts config → packages/actions/results + chain/redirect edges
2. Edit XML while diagram open → debounced refresh
3. Double-click node → navigates to XML
4. Hover → tooltips
5. Confirm Diagram **tab** is gone
6. Zoom/pan via platform chrome works at a smoke level

### Gate

`./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"` green; `CHANGELOG.md` notes migration, hard `com.intellij.diagram` dependency, and removal of the Diagram tab.

## UX / changelog note

This is a **breaking UX change** for users who relied on the Diagram editor tab: the tab is removed; diagrams are opened via the IDE **Show Diagram** action on a Struts 2 config file. Document that clearly in `CHANGELOG.md`.

## Future work (out of scope)

- [#96](https://github.com/apache/struts-intellij-plugin/issues/96) merged multi-file view across Struts file set
- [#98](https://github.com/apache/struts-intellij-plugin/issues/98) sync selection with XML perspective
- [#99](https://github.com/apache/struts-intellij-plugin/issues/99) Structure tool window integration
- [#100](https://github.com/apache/struts-intellij-plugin/issues/100) zoom/pan/fit beyond what platform chrome already provides

## References

- Issue [#117](https://github.com/apache/struts-intellij-plugin/issues/117)
- `com.intellij.struts2.diagram.model` package-info — migration boundary
- `docs/superpowers/specs/2026-07-25-diagram-blank-262-design.md` — Swing host fix; migration deferred here
- `docs/superpowers/specs/2026-06-25-diagram-auto-refresh-design.md` — DomEvent debounce semantics (adapted to open diagram, not tab selection)
- Platform Explorer / OSS examples of `com.intellij.diagram.Provider` (e.g. JHipster) — learn-by-examples for undocumented Ultimate API
