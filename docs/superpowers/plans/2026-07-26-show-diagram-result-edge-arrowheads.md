# Show Diagram Result Edge Arrowheads Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Show Diagram action → result (and other labeled) edges render with a clear `ANGLE` target arrow while keeping package → action as dashed `DEPENDENCY`.

**Architecture:** Presentation-only fix in `StrutsDiagramApiEdge.relationshipFor`. Empty-label edges stay `DiagramRelationships.DEPENDENCY`. Labeled edges switch from the short no-arrow `DiagramRelationshipInfoAdapter` constructor to `DiagramRelationshipInfoAdapter.Builder` with solid line, `ANGLE` target arrow, and upper-center label. Extend existing mapping-test relationship verification; leave model, layout, Dom refresh, and Swing tab untouched.

**Tech Stack:** IntelliJ IDEA Ultimate 2026.2 (262), `com.intellij.diagram` (`DiagramRelationshipInfoAdapter.Builder`, `DiagramRelationships`, `DiagramLineType`), JUnit 4 light tests (`BasicLightHighlightingTestCase`).

**Spec:** `docs/superpowers/specs/2026-07-26-show-diagram-result-edge-arrowheads-design.md`

## Global Constraints

- Target platform remains IntelliJ IDEA **2026.2** / build **262** only (`pluginSinceBuild=262`, `pluginUntilBuild=262.*`).
- Hard dependency on plugin id **`com.intellij.diagram`** already present — do not change dependency shape.
- Touch **Show Diagram edge relationship presentation only** — do **not** modify or remove the Swing Diagram tab (`diagram.fileEditor` / `diagram.ui`).
- Do **not** change `StrutsConfigDiagramModel` semantics, layouter extras, compact node chrome, Dom refresh, tooltips, or navigation.
- Keep visual distinction: empty label → `DEPENDENCY` (dashed + `ANGLE`); non-empty label → solid + `ANGLE` + upper-center label.
- Arrow shape for labeled edges is **`DiagramRelationshipInfo.ANGLE`** (same chevron as `DEPENDENCY`).
- Do **not** implement per-kind edge presets, unify all edge styles, #96–#100, or Swing tab removal.
- Tests gate: `./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"`.
- If Builder / arrow accessor signatures drift, confirm against local IU SDK jar `plugins/uml/lib/uml-support.jar`.

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `src/test/java/com/intellij/struts2/diagram/StrutsDiagramDataModelMappingTest.java` | Modify | Assert labeled edges have solid line + `ANGLE` end/target arrow |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramApiEdge.java` | Modify | Builder-based labeled relationship with `ANGLE` target arrow |
| `CHANGELOG.md` | Modify | Unreleased note for Show Diagram arrowheads (#125) |

No new production classes. No `plugin.xml` / Gradle dependency changes. No fixture XML changes (`struts-diagram.xml` already has package → action and a result edge).

---

### Task 1: Failing test for labeled edge arrowheads

**Files:**
- Modify: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramDataModelMappingTest.java`
- Test: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramDataModelMappingTest.java`

**Interfaces:**
- Consumes:
  - `DiagramEdge.getRelationship()`
  - `DiagramRelationshipInfo.getLineType()`
  - `DiagramRelationshipInfo.getEndArrow()` (Adapter stores Builder `setTargetArrow` here)
  - `DiagramRelationshipInfo.ANGLE`
  - `DiagramLineType.SOLID`
  - Existing helpers `apiEdgeLabel`, `centerLabelText`, `verifyRelationshipMapping`
- Produces: failing assertion that labeled relationships use solid line and `ANGLE` end arrow (currently null arrows from the short adapter ctor)

- [ ] **Step 1: Add import for `DiagramLineType`**

In `StrutsDiagramDataModelMappingTest.java`, add:

```java
import com.intellij.diagram.presentation.DiagramLineType;
```

Keep existing `DiagramRelationshipInfo` / `DiagramRelationships` imports.

- [ ] **Step 2: Strengthen `verifyRelationshipMapping`**

Replace the existing `verifyRelationshipMapping` method with:

```java
    private static void verifyRelationshipMapping(@NotNull DiagramEdge<StrutsDiagramItem> edge) {
        String label = apiEdgeLabel(edge);
        DiagramRelationshipInfo relationship = edge.getRelationship();
        if (label.isEmpty()) {
            assertSame("Unlabeled edges must use DEPENDENCY", DiagramRelationships.DEPENDENCY, relationship);
        }
        else {
            assertNotSame("Labeled edges must not use DEPENDENCY", DiagramRelationships.DEPENDENCY, relationship);
            assertEquals(label, centerLabelText(relationship.getUpperCenterLabel()));
            assertEquals("Labeled edges must be solid", DiagramLineType.SOLID, relationship.getLineType());
            assertSame("Labeled edges must have ANGLE target arrow",
                    DiagramRelationshipInfo.ANGLE, relationship.getEndArrow());
        }
    }
```

`testRefreshMapsSnapshotNodesAndEdges` already calls `verifyRelationshipMapping` for every API edge on `struts-diagram.xml` (package → action unlabeled + action → result labeled as default result name). No new test method required unless that fixture somehow lacks a labeled edge — in that case fail fast with an explicit count assert; it should not.

- [ ] **Step 3: Run test to verify it fails**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramDataModelMappingTest.testRefreshMapsSnapshotNodesAndEdges"
```

Expected: FAIL on the new `assertSame(... ANGLE ...)` (or solid/line assert) because labeled edges currently use the short adapter constructor with null arrows.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/intellij/struts2/diagram/StrutsDiagramDataModelMappingTest.java
git commit -m "$(cat <<'EOF'
test(diagram): require ANGLE arrowheads on labeled Show Diagram edges (#125)

EOF
)"
```

---

### Task 2: Builder-based labeled relationships + changelog

**Files:**
- Modify: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramApiEdge.java`
- Modify: `CHANGELOG.md`
- Test: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramDataModelMappingTest.java`

**Interfaces:**
- Consumes:
  - `StrutsDiagramEdge.getLabel()`
  - `DiagramRelationships.DEPENDENCY`
  - `DiagramRelationshipInfoAdapter.Builder`
  - `DiagramRelationshipInfo.ANGLE`
  - `DiagramLineType.SOLID`
- Produces: labeled edges with solid line, `ANGLE` target arrow, upper-center label; unlabeled edges unchanged `DEPENDENCY`

- [ ] **Step 1: Replace labeled-edge construction in `relationshipFor`**

In `StrutsDiagramApiEdge.java`, replace `relationshipFor` with:

```java
    private static @NotNull DiagramRelationshipInfo relationshipFor(@NotNull StrutsDiagramEdge edge) {
        String label = edge.getLabel();
        if (label.isEmpty()) {
            return DiagramRelationships.DEPENDENCY;
        }
        return new DiagramRelationshipInfoAdapter.Builder()
                .setName(label)
                .setLineType(DiagramLineType.SOLID)
                .setTargetArrow(DiagramRelationshipInfo.ANGLE)
                .setUpperCenterLabel(label)
                .create();
    }
```

Keep existing imports; `DiagramRelationshipInfo` is already imported (needed for `ANGLE`). No other methods in this class change.

- [ ] **Step 2: Run mapping test to verify it passes**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramDataModelMappingTest.testRefreshMapsSnapshotNodesAndEdges"
```

Expected: PASS.

- [ ] **Step 3: Run full diagram test suite**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"
```

Expected: all diagram tests PASS (same count as before this change; currently 44 if unchanged since #124 — accept whatever the suite reports as long as zero failures).

- [ ] **Step 4: Add changelog entry**

Under `## [Unreleased]` → `### Changed` in `CHANGELOG.md`, add (near the other Show Diagram bullets):

```markdown
- Show Diagram action → result edges use directed arrowheads while keeping package → action as dashed dependencies ([#125](https://github.com/apache/struts-intellij-plugin/issues/125))
```

- [ ] **Step 5: Manual smoke (recommended before PR)**

Run:

```bash
./gradlew runIde
```

Then: open a small Struts config → Show Diagram → confirm action → result edges show an arrow toward the result, labels like `success` remain readable, package → action stays dashed.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramApiEdge.java CHANGELOG.md
git commit -m "$(cat <<'EOF'
fix(diagram): add ANGLE arrowheads on labeled Show Diagram edges (#125)

EOF
)"
```

---

## Spec coverage checklist

| Spec requirement | Task |
|---|---|
| Action → result clear directed arrow | Task 2 |
| Labels remain readable (upper-center) | Task 2 (Builder `setUpperCenterLabel`) + Task 1 asserts |
| Package → action stays `DEPENDENCY` / distinct | Task 1 + Task 2 empty-label branch |
| Labeled chain/redirect same path | Task 2 (all non-empty labels) |
| Automated relationship coverage | Task 1 |
| Manual `runIde` check | Task 2 Step 5 |
| Changelog | Task 2 Step 4 |
| No model/layout/Swing/Dom changes | Global constraints + File Structure |

## Out of scope (do not implement)

- Per-kind RESULT vs CHAIN relationship presets
- Unifying dashed package edges with solid result edges
- Layout / compact nodes / Dom refresh changes
- Swing tab removal or restyling
- Robot / pixel assertions
- #96–#100
