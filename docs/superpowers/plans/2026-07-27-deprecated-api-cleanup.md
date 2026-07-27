# Deprecated API Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clear Plugin Verifier deprecated-API hits for `DiagramExtras.createNodeComponent(Point)` and `ReadAction.compute` on the Show Diagram path, with no intentional behavior change.

**Architecture:** Two independent surgical migrations. Keep the non-deprecated `NodeRealizer` `createNodeComponent` override that returns `createLabelNode`. Remove the redundant deprecated `Point` overload override. Replace leftover `ReadAction.compute` call sites with `ReadAction.nonBlocking(...).executeSynchronously()`, matching the pattern already used elsewhere in this plugin.

**Tech Stack:** IntelliJ IDEA Ultimate 2026.2 (262), `com.intellij.diagram`, `ReadAction.nonBlocking`, JUnit 4 light tests (`BasicLightHighlightingTestCase`).

**Spec:** `docs/superpowers/specs/2026-07-27-deprecated-api-cleanup-design.md`

## Global Constraints

- Target platform remains IntelliJ IDEA **2026.2** / build **262** only (`pluginSinceBuild=262`, `pluginUntilBuild=262.*`).
- Touch only the listed Show Diagram / test / CHANGELOG files — do **not** modify Swing Diagram tab (`diagram.fileEditor` / `diagram.ui`).
- Do **not** migrate to `com.intellij.diagram.v2`.
- Do **not** use `ReadAction.computeBlocking()` as the primary replacement.
- Keep `isReadAccessAllowed()` short-circuit in `StrutsDiagramVfsResolver`.
- Keep compact label chrome via `NodeRealizer` overload → `createLabelNode`.
- Tests gate: `./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramProviderTest"`.
- Full diagram suite after both code tasks: `./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"`.
- Verifier gate: `./gradlew runPluginVerifier` with zero hits for the two deprecated APIs in this report.
- Manual `runIde` Show Diagram smoke required before calling the work done.

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java` | Modify | Migrate 3× `ReadAction.compute`; add regression that Point overload is not declared on `StrutsDiagramExtras` |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramVfsResolver.java` | Modify | Replace production `ReadAction.compute` with nonBlocking sync |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramExtras.java` | Modify | Remove deprecated Point `createNodeComponent` override; drop unused `Point` import |
| `CHANGELOG.md` | Modify | Unreleased note for both migrations |

No new production classes. No `plugin.xml` / Gradle dependency changes.

---

### Task 1: Migrate `ReadAction.compute` (tests + VfsResolver)

**Files:**
- Modify: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java:107`
- Modify: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java:136`
- Modify: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java:160`
- Modify: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramVfsResolver.java:56-58`
- Test: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java`

**Interfaces:**
- Consumes: `ReadAction.nonBlocking(ThrowableComputable).executeSynchronously()`, `StrutsConfigDiagramModel.build(XmlFile)`, existing `isReadAccessAllowed()` short-circuit
- Produces: no new public API; FQN resolve and provider tests keep the same synchronous contract

- [ ] **Step 1: Update the three test `ReadAction.compute` call sites**

In `StrutsDiagramProviderTest`, replace each:

```java
StrutsConfigDiagramModel model = ReadAction.compute(() -> StrutsConfigDiagramModel.build(xml));
```

with:

```java
StrutsConfigDiagramModel model = ReadAction.nonBlocking(
        () -> StrutsConfigDiagramModel.build(xml)).executeSynchronously();
```

Exact sites:
1. `testVfsResolverRoundTripsSnapshotNodeFqn` (~line 107)
2. `testResolvePsiElementFromSnapshotNode` (~line 136)
3. `testExtrasCreateCompactLabelNodeComponents` (~line 160)

Do not change other test logic. Pattern reference: `StrutsConfigDiagramModelTest` / `Struts2ProblemFileHighlightFilter`.

- [ ] **Step 2: Run provider tests (should still pass — tests no longer call deprecated compute)**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramProviderTest"
```

Expected: BUILD SUCCESSFUL; all tests in the class PASS. Production `StrutsDiagramVfsResolver` still uses `compute` until Step 3 — that is OK for this step; `testVfsResolverRoundTripsSnapshotNodeFqn` still exercises resolve.

- [ ] **Step 3: Migrate production `StrutsDiagramVfsResolver`**

Replace the model-build branch in `resolveElementByFQN` so it becomes:

```java
if (nodeSeparator >= 0) {
    String nodeId = fqn.substring(nodeSeparator + 1);
    StrutsConfigDiagramModel model = ApplicationManager.getApplication().isReadAccessAllowed()
            ? StrutsConfigDiagramModel.build(xmlFile)
            : ReadAction.nonBlocking(() -> StrutsConfigDiagramModel.build(xmlFile))
                    .executeSynchronously();
    if (model != null) {
        StrutsDiagramNode node = model.getNodes().stream()
                .filter(candidate -> candidate.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
        if (node != null) {
            return StrutsDiagramItem.forNode(xmlFile, node);
        }
    }
}
```

Keep the `isReadAccessAllowed()` short-circuit. Keep the rest of the class unchanged.

- [ ] **Step 4: Re-run provider tests**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramProviderTest"
```

Expected: BUILD SUCCESSFUL; `testVfsResolverRoundTripsSnapshotNodeFqn` PASS.

- [ ] **Step 5: Confirm no remaining `ReadAction.compute` in these files**

Run:

```bash
rg "ReadAction\\.compute" src/main/java/com/intellij/struts2/diagram src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java
```

Expected: no matches.

- [ ] **Step 6: Commit**

```bash
git add \
  src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java \
  src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramVfsResolver.java
git commit -m "$(cat <<'EOF'
fix(diagram): migrate leftover ReadAction.compute call sites

Use ReadAction.nonBlocking().executeSynchronously() in VfsResolver and
StrutsDiagramProviderTest to match the 2026.2 cancellable read-action API.
EOF
)"
```

---

### Task 2: Drop deprecated Point `createNodeComponent` override

**Files:**
- Modify: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java`
- Modify: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramExtras.java:46-47` (Point import) and `:98-104` (Point overload)
- Test: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java`

**Interfaces:**
- Consumes: existing `createNodeComponent(DiagramNode, DiagramBuilder, NodeRealizer, JPanel)` → `createLabelNode`
- Produces: `StrutsDiagramExtras` no longer declares the deprecated Point overload; platform default remains unused for Struts compact labels

- [ ] **Step 1: Write failing regression that Point overload is not declared**

Add imports if missing:

```java
import java.awt.Point;
import java.lang.reflect.Method;
```

Add test method to `StrutsDiagramProviderTest`:

```java
public void testExtrasDoNotOverrideDeprecatedPointCreateNodeComponent() {
    Method pointOverload = null;
    try {
        pointOverload = StrutsDiagramExtras.class.getDeclaredMethod(
                "createNodeComponent",
                DiagramNode.class,
                DiagramBuilder.class,
                Point.class,
                JPanel.class);
    } catch (NoSuchMethodException ignored) {
        // expected once the deprecated override is removed
    }
    assertNull(
            "Must not override deprecated DiagramExtras.createNodeComponent(..., Point, ...)",
            pointOverload);
}
```

Keep `testExtrasCreateCompactLabelNodeComponents` unchanged — it already calls the `NodeRealizer` overload.

- [ ] **Step 2: Run the new test to verify it fails**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramProviderTest.testExtrasDoNotOverrideDeprecatedPointCreateNodeComponent"
```

Expected: FAIL — assertion error because `pointOverload` is non-null (method still declared on `StrutsDiagramExtras`).

- [ ] **Step 3: Remove the Point overload from `StrutsDiagramExtras`**

Delete this entire method:

```java
@Override
public @NotNull JComponent createNodeComponent(@NotNull DiagramNode<StrutsDiagramItem> node,
                                               @NotNull DiagramBuilder builder,
                                               @NotNull Point basePoint,
                                               @NotNull JPanel wrapper) {
    return createLabelNode(node, builder, wrapper);
}
```

Remove the unused import:

```java
import java.awt.Point;
```

Keep the `NodeRealizer` overload exactly as-is:

```java
@Override
public @NotNull JComponent createNodeComponent(@NotNull DiagramNode<StrutsDiagramItem> node,
                                               @NotNull DiagramBuilder builder,
                                               @NotNull NodeRealizer nodeRealizer,
                                               @NotNull JPanel wrapper) {
    return createLabelNode(node, builder, wrapper);
}
```

- [ ] **Step 4: Run provider tests**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramProviderTest"
```

Expected: BUILD SUCCESSFUL; `testExtrasDoNotOverrideDeprecatedPointCreateNodeComponent` PASS; `testExtrasCreateCompactLabelNodeComponents` PASS.

- [ ] **Step 5: Commit**

```bash
git add \
  src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java \
  src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramExtras.java
git commit -m "$(cat <<'EOF'
fix(diagram): drop deprecated Point createNodeComponent override

Keep the NodeRealizer overload for compact label nodes; remove the
deprecated Point overload flagged by Plugin Verifier on 2026.2.
EOF
)"
```

---

### Task 3: CHANGELOG + verifier + manual smoke

**Files:**
- Modify: `CHANGELOG.md` (Unreleased / Changed)
- Test: verifier + manual `runIde` (no new automated test file)

**Interfaces:**
- Consumes: Tasks 1–2 completed on the working branch
- Produces: documented Unreleased notes; green verifier for the two targeted APIs; manual smoke confirmation

- [ ] **Step 1: Update CHANGELOG Unreleased**

Under `## [Unreleased]` → `### Changed`, add:

```markdown
- Replace remaining deprecated `ReadAction.compute(ThrowableComputable)` Show Diagram call sites with `ReadAction.nonBlocking().executeSynchronously()`
- Drop deprecated `DiagramExtras.createNodeComponent(..., Point, ...)` override; keep the `NodeRealizer` overload for compact Show Diagram nodes
```

Place near the other Show Diagram Unreleased bullets. Do not invent issue numbers unless a tracking issue already exists.

- [ ] **Step 2: Run full diagram unit suite**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run Plugin Verifier**

Run:

```bash
./gradlew runPluginVerifier
```

Expected: report no longer lists:
- `DiagramExtras.createNodeComponent(...)`
- `ReadAction.compute(ThrowableComputable)`

If the report path differs, search the verifier output / `build/reports/pluginVerifier` for those two strings — both must be absent.

- [ ] **Step 4: Manual `runIde` smoke**

Run:

```bash
./gradlew runIde
```

Checklist:
1. Open a Struts 2 `struts.xml` (or test fixture config) that has package/action/result.
2. Invoke **Show Diagram**.
3. Confirm nodes are compact icon+label (not empty UML class boxes).
4. Double-click a node or use Jump to Source — navigation still works.

- [ ] **Step 5: Commit CHANGELOG**

```bash
git add CHANGELOG.md
git commit -m "$(cat <<'EOF'
docs: changelog for Show Diagram deprecated API cleanup

EOF
)"
```

---

## Self-Review Checklist (plan author)

1. **Spec coverage:** Point override removal → Task 2; ReadAction production + tests → Task 1; CHANGELOG → Task 3; unit tests + verifier + manual smoke → Task 3 (plus per-task test steps). Non-goals (v2, Swing tab, computeBlocking) not scheduled.
2. **Placeholders:** None — exact snippets, commands, and expected results included.
3. **Type consistency:** `NodeRealizer` overload kept; Point overload removed; `ReadAction.nonBlocking(...).executeSynchronously()` matches existing call sites.
