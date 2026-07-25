# Fix Blank Diagram Tab on IntelliJ 2026.2 (262)

**Date:** 2026-07-25  
**Status:** Approved for implementation planning  
**Related:** [#97](https://github.com/apache/struts-intellij-plugin/issues/97) / [#101](https://github.com/apache/struts-intellij-plugin/pull/101) auto-refresh (prior); platform bump [#115](https://github.com/apache/struts-intellij-plugin/pull/115)

## Problem

After targeting IntelliJ IDEA **2026.2** (build branch **262**), the read-only **Diagram** tab for `struts.xml` shows a completely empty canvas: editor background only — no package/action/result nodes and no centered placeholder message.

The 262 upgrade PR did not change diagram sources. The blank symptom matches a lifecycle/layout failure in the existing lightweight Swing host rather than a broken `StrutsConfigDiagramModel` snapshot.

### Working hypothesis

1. `Struts2DiagramComponent` starts with a `null` model (`UNAVAILABLE`) and does not set a preferred/minimum size large enough to fill the editor area, so placeholder text (if painted) is not visible against the parent chrome.
2. `scheduleModelBuild()`’s UI callback applies the model only when `myDiagramSelected` is true. That flag is set solely in `selectNotify()`.
3. `selectNotify()` / `deselectNotify()` overrides do not call `super`, which can skip `PerspectiveFileEditor` wiring that 262 relies on more strictly.
4. Combined, the child panel can remain zero-sized / never rebuilt → full blank tab.

## Goals

1. Diagram tab on 262 shows packages, actions, and results for file-set `struts.xml` files (not a blank pane).
2. Null/empty models show the existing centered placeholder text in a filled editor area (never blank).
3. Live DomEvent debounce and Text→Diagram catch-up refresh keep working.
4. Regression tests catch “editor created but never reaches `LOADED` / never sized for placeholders”.
5. Harden lifecycle so rendering does not depend only on an ungated `selectNotify` apply path.

## Non-Goals

- Migrating to `com.intellij.diagram.Provider` / Diagrams API (documented future work; see below).
- Loading indicators, incremental/diff updates, or robot/UI e2e tests.
- Changing model/DOM traversal or presentation tooltip/navigation logic.
- Re-enabling unrelated disabled platform tests.

## Decision

**Approach 1 — Harden the existing `PerspectiveFileEditor` + Swing panel.**

Keep the toolkit-neutral model. Fix editor lifecycle and component sizing so the canvas always receives a model (or a visible fallback) and fills the viewport.

### Alternatives considered

| Approach | Verdict |
|---|---|
| Minimal ungating only (drop `myDiagramSelected` in UI callback) | Rejected as sole fix — does not address zero-size placeholder / missing `super` |
| Migrate to Diagrams API now | Rejected for this bug — larger undocumented Ultimate API rewrite; blank fix would be coupled to migration risk |
| Two-phase (harden then migrate) in one delivery | Rejected for this ticket — migration remains a separate design |

## Architecture & data flow

```
Editor created
  → create Struts2DiagramComponent(null)
  → scheduleModelBuild()                 // initial load

ReadAction completes → finishOnUiThread
  → ALWAYS myComponent.rebuild(model)    // no myDiagramSelected gate

selectNotify
  → super.selectNotify()
  → myDiagramSelected = true
  → cancel alarm → scheduleModelBuild()  // catch-up after Text edits

deselectNotify
  → myDiagramSelected = false
  → cancel alarm
  → super.deselectNotify()

DomEvent (live edit)
  → only if myDiagramSelected && same file
  → debounced scheduleModelBuild()       // unchanged intent
```

`myDiagramSelected` remains a **DOM live-refresh gate only**, not a gate on whether the canvas may show a model.

This revises the auto-refresh design (`2026-06-25-diagram-auto-refresh-design.md`): that spec gated `rebuild()` on selection to skip stale applies after tab switch. Skipping rebuild after deselect is an optimization; applying a completed snapshot is cheap and avoids the blank-tab failure mode when selection timing differs across platform versions. Live **scheduling** of builds from DomEvents stays selection-gated.

## Components

| Unit | Change |
|---|---|
| `Struts2DiagramFileEditor` | Call `super` in select/deselect; remove `myDiagramSelected` check from the UI apply callback; keep the flag for DomEvent filtering only |
| `Struts2DiagramComponent` | Ensure `EMPTY` / `UNAVAILABLE` fill the editor area (preferred and/or minimum size, or equivalent layout expansion) so placeholders are visible |
| `StrutsConfigDiagramModel` / presentation | Unchanged |
| Provider / `plugin.xml` | Unchanged |

No new modules or extension points.

## Error handling & edge cases

| Scenario | Behavior |
|---|---|
| Model build returns `null` | `UNAVAILABLE` + centered placeholder, filling the editor area |
| Model has no nodes | `EMPTY` + placeholder, same fill behavior |
| In-flight build finishes after tab switch away | Still apply `rebuild()`; DomEvents stay gated so extra builds are not scheduled while deselected |
| Rapid Text edits while on Diagram | Unchanged 300 ms debounce |
| `selectNotify` / `deselectNotify` | Always invoke `super` so platform perspective wiring stays intact |
| Exceptions during build | Existing `ReadAction.nonBlocking` + `expireWith(this)`; blank must not be the failure mode for null/empty |
| Editor disposed | Alarm disposed with editor; in-flight read actions expire |

## Testing

### Automated

1. **Model applies without DomEvent selection semantics blocking first paint** — create editor (and pump non-blocking read actions / UI as needed); component reaches `LOADED` with expected nodes from `struts-diagram.xml`. Use a package-visible test accessor or `getPreferredFocusedComponent()` cast if that stays clean.
2. **Placeholder not zero-sized** — `rebuild(null)` / empty model → state `UNAVAILABLE`/`EMPTY` and preferred (or minimum) size is large enough to paint the message in a normal editor area.
3. Keep existing select/deselect and Dom filter tests; adjust only if lifecycle changes require it.

### Manual (`./gradlew runIde`)

1. Open a file-set `struts.xml` → Diagram shows packages/actions/results (not blank).
2. Text → Diagram after an edit → catch-up refresh still works.
3. Stay on Diagram, edit XML → debounced refresh still works.
4. Unavailable/empty cases show placeholder text, not a blank pane.

### Gate

`./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"` green; `CHANGELOG.md` notes the fix.

## Future work (out of scope)

Migrate rendering/editor to `com.intellij.diagram.Provider` per `com.intellij.struts2.diagram.model` package-info: keep `StrutsConfigDiagramModel` + `StrutsDiagramPresentation`, replace only `diagram.ui` and `diagram.fileEditor`. Requires optional/bundled dependency on `com.intellij.diagram` (Ultimate Diagrams). Track as a separate design when product UX (zoom, IDE diagram chrome) justifies the undocumented API cost.

## References

- `Struts2DiagramFileEditor` — lifecycle and `scheduleModelBuild()`
- `Struts2DiagramComponent` — paint / placeholder / preferred size
- `docs/superpowers/specs/2026-06-25-diagram-auto-refresh-design.md` — prior selection-gating decision (partially revised here)
- `docs/superpowers/specs/2026-07-24-intellij-2026-2-compatibility-design.md` — 262 upgrade (no diagram code changes)
)
