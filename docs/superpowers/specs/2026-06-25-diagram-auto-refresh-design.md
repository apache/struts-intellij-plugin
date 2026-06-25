# Diagram Tab Auto-Refresh on DOM Changes

**Issue:** [#97](https://github.com/apache/struts-intellij-plugin/issues/97)  
**Date:** 2026-06-25  
**Status:** Approved for implementation planning

## Problem

The Diagram tab (`Struts2DiagramFileEditor`) rebuilds its model only on editor creation and `reset()`. Edits in the Text/XML perspective are not reflected until the tab is reopened or reset.

The removed Graph tab solved this with a project-wide `DomEventListener` that called `queueUpdate()` when the component was visible (`isShowing()`).

## Goals

1. Diagram updates when the user edits `struts.xml` in the same file while the Diagram tab is the **active** editor tab.
2. Diagram refreshes immediately when the user **switches to** the Diagram tab after editing on the Text tab.
3. Rebuild is debounced and does not block the EDT.
4. Model build stays on a background read action (existing `scheduleModelBuild()` pattern).
5. No noticeable UI freezes on large configs.

## Non-Goals

- Refresh when Diagram tab is open but not selected (user chose option A).
- Refresh on DOM changes in other files in the file set.
- Loading indicator or incremental/diff-based diagram updates.
- UI robot / end-to-end IDE tests in v1.

## Decisions

| Question | Decision |
|---|---|
| When to rebuild on live edit? | Only while Diagram is the active tab (`selectNotify` / `deselectNotify`) |
| Catch-up after Text tab edits? | Yes — immediate rebuild on `selectNotify` |
| Change detection mechanism | `DomEventListener` (Approach 1; matches legacy Graph tab, aligned with DOM-based model build) |
| Debounce delay | 300 ms via `Alarm` (SWING_THREAD) |
| Scope of file changes | Same `VirtualFile` as the editor only |

## Architecture

```
Editor created
  → register DomEventListener (disposed with editor)
  → scheduleModelBuild()                    // initial load

selectNotify (Diagram tab activated)
  → myDiagramSelected = true
  → cancel pending alarm
  → scheduleModelBuild()                    // immediate, no debounce

deselectNotify (Diagram tab deactivated)
  → myDiagramSelected = false
  → cancel pending alarm

DomEvent (project-wide)
  → if !myDiagramSelected → ignore
  → if !isEventForMyFile(event) → ignore
  → queueDebouncedModelBuild()              // 300 ms Alarm

queueDebouncedModelBuild()
  → cancel/reschedule alarm
  → on fire: scheduleModelBuild()

scheduleModelBuild()                        // unchanged pipeline
  → ReadAction.nonBlocking(() -> StrutsConfigDiagramModel.build(myXmlFile))
      .expireWith(this)
      .finishOnUiThread(..., model -> {
          if (myDiagramSelected) myComponent.rebuild(model);
      })
      .submit(AppExecutorUtil.getAppExecutorService())
```

## Components

All changes are confined to `Struts2DiagramFileEditor`. No new top-level classes.

### New fields

- `boolean myDiagramSelected` — set in `selectNotify` / `deselectNotify`.
- `Alarm myUpdateAlarm` — `new Alarm(Alarm.ThreadToUse.SWING_THREAD, this)`.

### New / modified methods

| Method | Visibility | Description |
|---|---|---|
| `registerDomChangeListener()` | private | Registers `DomEventListener` via `DomManager.addDomEventListener(..., this)` |
| `isEventForMyFile(DomEvent)` | package-private static | Returns true when event context resolves to editor's `VirtualFile` |
| `queueDebouncedModelBuild()` | private | Schedules alarm-fired call to `scheduleModelBuild()` |
| `selectNotify()` | public override | Visibility on + immediate rebuild |
| `deselectNotify()` | public override | Visibility off + cancel alarm |
| `scheduleModelBuild()` | private | Add visibility guard in UI-thread callback |

`Struts2DiagramComponent` and `StrutsConfigDiagramModel` are unchanged.

## Error Handling & Edge Cases

| Scenario | Behavior |
|---|---|
| Rapid typing while on Diagram tab | Debounced — one build after 300 ms pause |
| Switch to Diagram during debounce | Alarm cancelled; immediate rebuild on `selectNotify` |
| In-flight build completes after tab switch | UI callback skips `rebuild()` when `!myDiagramSelected` |
| Editor disposed | `expireWith(this)` cancels read action; alarm disposed with editor |
| DOM event from another file | Ignored by `isEventForMyFile` |
| Build returns null / empty | Existing `UNAVAILABLE` / `EMPTY` component states |

## File Filtering

`isEventForMyFile` resolves the event's context element (via `DomEvent.getContextElement()`) to its containing `VirtualFile` and compares with `myXmlFile.getVirtualFile()`. Returns false for null context or mismatched files.

Extracted as package-private static for unit testing.

## Testing

### Automated

1. **`isEventForMyFile` unit tests** — same-file event accepted; other-file event rejected; null context rejected.
2. **`selectNotify` / `deselectNotify` lifecycle test** — editor survives tab activation cycle without exception (extends `Struts2DiagramFileEditorProviderTest` pattern).
3. **Regression** — existing `testResetDoesNotThrow` continues to pass.

### Manual smoke test

1. Open `struts.xml`, switch to Diagram tab.
2. Add/remove an action in Text tab, switch back to Diagram — diagram reflects change.
3. Stay on Diagram tab, edit XML in split or via structure — diagram updates after brief pause.
4. Open a large config — no EDT freeze during typing.

## References

- `Struts2DiagramFileEditor.scheduleModelBuild()` — current rebuild pipeline
- Removed `Struts2GraphComponent` (commit `9394888^`) — legacy `DomEventListener` + visibility gating pattern
