# Show Diagram: Stale Result Path After Dom Edit / Copy-Paste

**Date:** 2026-07-26  
**Status:** Approved for implementation planning  
**Related:** [#126](https://github.com/apache/struts-intellij-plugin/issues/126); follow-up to [#122](https://github.com/apache/struts-intellij-plugin/issues/122) / [#124](https://github.com/apache/struts-intellij-plugin/pull/124)

## Problem

After adding a new result by copy-pasting an existing `success` result and renaming it to `delete` (and changing the path to `delete.jsp`), Show Diagram still shows the `delete` result pointing at the previous path (e.g. `/WEB-INF/examples/index.jsp`) instead of the updated `delete.jsp` path.

Confirmed reproduction detail: **reopening** Show Diagram shows the correct path. Only **live Dom refresh** is stale. Soft preference / LTR layout from #122 must remain unchanged (refresh must not reset the user’s layout algorithm choice or needlessly reshuffle node positions).

## Goals

1. Editing a result path in XML updates the corresponding Show Diagram result node label/path after Dom refresh.
2. Copy-paste of a result element and changing name + path yields distinct nodes with correct paths.
3. No duplicate/stale identity collision between results that share a previous path.
4. Soft layout preference from #122 remains: Dom refresh does not reset toolbar layout choice; retained nodes keep positions when identity is stable.
5. Automated regression coverage for the copy-paste / path-edit case; short manual `runIde` check.
6. Changelog documents the fix.

## Non-Goals

- Fixing or changing the leftover Swing Diagram tab (`diagram.fileEditor` / `diagram.ui`).
- Changing LTR layouter / `StrutsDiagramExtras.getCustomLayouter` behavior.
- Changing snapshot graph shape semantics (package → action → result, chain/redirect resolution).
- Robot / UI e2e tests.
- Full graph rebuild on every Dom event (rejected — fights soft layout preference).

## Decisions

| Question | Decision |
|---|---|
| Host in scope | Show Diagram (`StrutsDiagramDataModel`) only |
| Root cause layer | Live Dom refresh + identifying-element equality, not snapshot build |
| Soft layout | Required: keep smart mode + avoid layout algorithm reset; prefer in-place presentable updates |
| Identity source of truth | `SmartPsiElementPointer` to the XML element (`navigationPointer`), not `kind@textOffset` and not path text |
| Refresh strategy | Merge by stable identity (update presentables on retained API nodes), then `refreshDataModelInSmartMode` |
| Initial open / `refreshDataModel()` | Full replace (unchanged) |

### Alternatives considered

| Approach | Verdict |
|---|---|
| Stable PSI pointer identity + presentable merge before smart refresh | **Chosen** — fixes stale labels while preserving soft layout / positions |
| Content-aware identity (path/name in `equals` / id) | Rejected — correct labels but edited nodes (and sometimes neighbors) can jump |
| Full rebuild instead of smart mode on Dom refresh | Rejected — simplest correctness but resets layout/positions; fights #122 |

## Architecture

**Why reopen works but live refresh does not**

1. Dom refresh rebuilds a correct `StrutsConfigDiagramModel` snapshot (path already correct in the new `StrutsDiagramNode.name`).
2. `applyLiveUpdate` replaces the data-model node list, then calls `DiagramDataModel.refreshDataModelInSmartMode(builder)`.
3. Smart mode keeps graph nodes whose identifying elements `equals` existing ones.
4. Today identity is effectively `kind@textOffset` via `StrutsDiagramNode` / `StrutsDiagramItem` equality. A path-only edit keeps the same id, so smart mode retains the **old** `StrutsDiagramApiNode` (old path baked into its identifying item) and drops the fresh item’s presentable data.
5. Copy-paste of a result that initially shares a path can also collide/stale when identity is offset- or path-tied rather than element-tied.

**Fix**

```
DomEvent → debounce → buildApiModel (fresh snapshot)
  → mergeByStableIdentity(existing, fresh)  // update presentables in place
  → refreshDataModelInSmartMode(builder)    // add/remove only
```

1. **Stable identity** — treat two nodes as the same when their `navigationPointer`s refer to the same XML element (fallback to existing string id when pointer is null).
2. **Presentable merge** — on live update, match old↔new by that identity; update the retained `StrutsDiagramApiNode`’s identifying `StrutsDiagramItem` so title/tooltip/icon come from the fresh snapshot; add/remove only structural changes; rebuild edges from the fresh model against the post-merge node map.
3. **Keep smart mode** — still call `refreshDataModelInSmartMode` so layout algorithm choice and positions stay. Do not touch `GraphSettings` / layouter on refresh.

**Unchanged:** `StrutsDiagramExtras` LTR layouter, compact node chrome, Swing tab, `plugin.xml`, chain/redirect snapshot semantics.

## Components

| Unit | Role |
|---|---|
| `StrutsDiagramNode` | Equality/hash by `navigationPointer` when present; fallback to existing `id`. Keep `id` for debug/`toString`; stop treating `kind@textOffset` as the identity source of truth for smart mode. |
| `StrutsConfigDiagramModel.buildNodeId` | May keep a debug id or non-path key; must not be what smart mode relies on for sameness after this change. |
| `StrutsDiagramItem` | Equality follows snapshot node identity (pointer-based). Still wraps file + snapshot node. |
| `StrutsDiagramApiNode` | Allow swapping/updating the identifying `StrutsDiagramItem` when merge finds the same element with new presentable data. Same `DiagramNode` instance stays in the graph. |
| `StrutsDiagramDataModel.applyLiveUpdate` | Merge by stable identity → update presentables → then smart mode. Initial `refreshDataModel()` stays full replace. |
| Merge helper | Prefer a small package-private helper on/near `StrutsDiagramDataModel` if merge logic needs unit tests without a full `DiagramBuilder`. |
| `StrutsDiagramExtras` / layouter | Unchanged. |

No new top-level packages.

## Data flow

### Live Dom refresh

1. Debounced `scheduleRefresh` builds a fresh `ApiModel` under a read action (unchanged).
2. On EDT, `applyLiveUpdate`:
   - Index existing API nodes by stable identity.
   - For each fresh node: if match → update retained API node’s item to the fresh presentable snapshot; else → add.
   - Drop nodes whose identity disappeared; rebuild edges from the fresh model (endpoints via post-merge node map).
   - Call `refreshDataModelInSmartMode(builder)` when a builder exists.
3. Never call `GraphSettings.setCurrentLayouter` or replace the custom layouter on refresh.

### Initial open / platform `refreshDataModel()`

Full replace of nodes/edges (today’s behavior). No merge required.

## Error handling

| Scenario | Behavior |
|---|---|
| Path/name edit, same XML element | Identity match → presentable update; node stays; label/path refresh |
| Copy-paste new `<result>` | New pointer → new node; no collision with source result |
| Insert/delete XML above a result (offsets shift) | Smart pointer tracks element → identity stable; no false remove/add |
| Pointer unresolved / null during build | Fallback identity (existing string id); may lose soft-position for that node only |
| Builder null (tests / no UI) | Merge into lists only; skip smart-mode call |
| Invalid / empty snapshot | Clear to empty model (unchanged) |

## Testing

### Automated

1. **Identity** — Two results with the same path get distinct identities; after a simulated path edit on one DOM element, identity stays the same while `name` updates (`StrutsConfigDiagramModel` / node equality tests).
2. **Live merge regression (#126)** — Open data model on a fixture → `refreshDataModel` → edit a copied result’s name+path via document write (same pattern as `testSameFileDomEventRefreshesLiveDataModel`) → wait for Dom refresh → assert edge label `delete` targets a result node whose title/path is `delete.jsp` (not the old shared path), and the original `success` node still shows the old path.
3. **Mapping suite** — Existing `StrutsDiagramDataModelMappingTest` / diagram suite still pass.
4. **No layouter regression** — Existing LTR extras tests unchanged.

### Manual (`runIde`)

- Copy-paste `success` → rename to `delete` + path `delete.jsp` with Show Diagram open → after debounce, both nodes correct; layout algorithm choice unchanged if user picked a non-custom layout earlier.

### Changelog

One line under Unreleased: fix Show Diagram stale result path after Dom edit / copy-paste.

## Success criteria

Matches [#126](https://github.com/apache/struts-intellij-plugin/issues/126) acceptance criteria:

- [ ] Editing a result path in XML updates the corresponding Show Diagram result node label/path after Dom refresh
- [ ] Copy-paste of a result element and changing name + path yields distinct nodes with correct paths
- [ ] No duplicate/stale identity collision between results that share the previous path
- Soft preference / LTR layout from #122 remains unchanged
