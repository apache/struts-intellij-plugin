# Deprecated API Cleanup (Show Diagram / ReadAction)

**Date:** 2026-07-27  
**Status:** Approved for implementation planning  
**Related:** Plugin Verifier report on `262.19039-nightly.2`; prior migrations in CHANGELOG (`ReadAction.nonBlocking().executeSynchronously()`); compact nodes [#120](https://github.com/apache/struts-intellij-plugin/issues/120) / `2026-07-25-show-diagram-compact-nodes-design.md`

## Problem

Plugin Verifier reports that Apache Struts `262.19039-nightly.2` uses deprecated APIs that may be removed later:

| Deprecated API | Count | Location |
|---|---|---|
| `DiagramExtras.createNodeComponent(...)` | 1 | `StrutsDiagramExtras` overrides the `Point`-based overload |
| `ReadAction.compute(ThrowableComputable)` | 1 | `StrutsDiagramVfsResolver` (diagram tests also still call `compute`) |

On IntelliJ Platform 2026.2 (`uml-support.jar`):

- `DiagramExtras.createNodeComponent(DiagramNode, DiagramBuilder, Point, JPanel)` is `@Deprecated`.
- The replacement overload takes `NodeRealizer` instead of `Point` and is not deprecated.
- Non-cancellable `ReadAction.compute` / `run` are deprecated in favor of cancellable APIs (`ReadAction.nonBlocking().submit()` / `.executeSynchronously()` in Java).

`StrutsDiagramExtras` currently overrides **both** overloads and routes them to the same `createLabelNode(...)` helper, so the Point override is redundant. Most of the plugin already migrated off `ReadAction.compute`; these Show Diagram call sites were left behind.

## Goals

1. Clear both Plugin Verifier deprecated-API hits for this report.
2. Keep Show Diagram compact icon+label chrome via the non-deprecated `NodeRealizer` `createNodeComponent` overload.
3. Migrate production **and** diagram test `ReadAction.compute` sites for consistency with the existing codebase pattern.
4. No intentional behavior change to layout, navigation, or snapshot model.
5. Verify with diagram unit tests, `runPluginVerifier`, and a short manual `runIde` Show Diagram smoke check.
6. Document in CHANGELOG under Unreleased.

## Non-Goals

- Migrating other deprecated APIs not listed in this verifier report.
- Moving Show Diagram to `com.intellij.diagram.v2`.
- Changing the leftover Swing Diagram tab (`diagram.fileEditor` / `diagram.ui`).
- Changing layouter, edge rendering, Dom refresh, or node chrome beyond removing the deprecated override.
- Using `ReadAction.computeBlocking()` as the primary replacement (JetBrains documents it as last resort).

## Decisions

| Question | Decision |
|---|---|
| Scope | Production + diagram test call sites (verifier production hit + test consistency) |
| Diagram fix | Remove Point overload override only; keep `NodeRealizer` → `createLabelNode` |
| ReadAction fix | `ReadAction.nonBlocking(...).executeSynchronously()`, matching prior CHANGELOG migration |
| VfsResolver short-circuit | Keep `isReadAccessAllowed()` → direct `build`; else nonBlocking sync |
| Verification | Unit tests + `runPluginVerifier` + manual `runIde` Show Diagram smoke |

### Alternatives considered

| Approach | Verdict |
|---|---|
| Surgical migration (drop Point override; nonBlocking sync for ReadAction) | **Chosen** — smallest diff; matches existing patterns; clears both warnings |
| `ReadAction.computeBlocking()` for sync FQN resolve | Rejected — documented last resort; worse than pattern already used elsewhere |
| Broader deprecation hunt / diagram v2 | Rejected — out of scope for these two verifier hits |

## Architecture

No new components. Two independent, localized API migrations on the existing Show Diagram path.

```
StrutsDiagramExtras
  createNodeComponent(..., NodeRealizer, ...) → createLabelNode(...)   // keep
  createNodeComponent(..., Point, ...)                                 // remove override

StrutsDiagramVfsResolver.resolveElementByFQN
  isReadAccessAllowed? build(xml) : ReadAction.nonBlocking(build).executeSynchronously()

StrutsDiagramProviderTest (3 sites)
  ReadAction.nonBlocking(build).executeSynchronously()
```

**Risk:** If any host still invoked the Point overload and relied on our override, removing it would fall through to the platform default UML `DiagramNodeContainer`. On 262 the platform routes through `NodeRealizer`; the compact-node smoke test already exercises that path.

**Unchanged:** `createLabelNode` chrome, custom LTR layouter, EditNodeHandler / uiDataSnapshot, data model, Swing tab, `plugin.xml`.

## Components

| Unit | Role |
|---|---|
| `StrutsDiagramExtras` | Delete Point `createNodeComponent` override; drop unused `java.awt.Point` import if present. Keep NodeRealizer overload and all other extras behavior. |
| `StrutsDiagramVfsResolver` | Replace the single `ReadAction.compute` with `ReadAction.nonBlocking(...).executeSynchronously()`. Preserve read-access short-circuit. |
| `StrutsDiagramProviderTest` | Migrate three `ReadAction.compute` model-build sites the same way. Compact-node smoke already calls the NodeRealizer overload — no Point test path to update. |
| `CHANGELOG.md` | Unreleased note covering both migrations. |

## Error handling

No new failure modes. `executeSynchronously()` preserves the previous synchronous contract for FQN resolution and test setup. Exceptions from `StrutsConfigDiagramModel.build` continue to propagate as before.

## Testing

**Automated**

- Run diagram unit tests touching the changed files (at least `StrutsDiagramProviderTest`).
- `./gradlew runPluginVerifier` — expect zero hits for `DiagramExtras.createNodeComponent` and `ReadAction.compute(ThrowableComputable)`.

**Manual (`runIde`)**

1. Open a Struts 2 config → Show Diagram.
2. Confirm compact icon+label nodes (not empty UML boxes).
3. Spot-check navigation (double-click / Jump to Source).

## Done criteria

- Both verifier deprecated-API warnings from this report are gone.
- Diagram unit tests green.
- Manual Show Diagram smoke OK.
- CHANGELOG Unreleased updated.
