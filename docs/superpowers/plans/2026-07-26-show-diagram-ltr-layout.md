# Show Diagram Left-to-Right Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make IntelliJ Show Diagram for Struts configs use a hierarchic left-to-right custom layouter by default so package → action → result reads LTR.

**Architecture:** Keep the existing Show Diagram provider/data model. Override only `StrutsDiagramExtras.getCustomLayouter` to return a Maven/Gradle-style hierarchic group layouter oriented `LEFT_TO_RIGHT`. Soft preference is already satisfied because Dom refresh does not call `GraphSettings.setCurrentLayouter` — do not add that. Leave Swing Diagram tab and snapshot model untouched.

**Tech Stack:** IntelliJ IDEA Ultimate 2026.2 (262), `com.intellij.diagram` (`DiagramExtras#getCustomLayouter`), `com.intellij.openapi.graph` (`GraphManager`, `HierarchicGroupLayouter`, `LayoutOrientation`), JUnit 4 light tests (`BasicLightHighlightingTestCase`).

**Spec:** `docs/superpowers/specs/2026-07-26-show-diagram-ltr-layout-design.md`

## Global Constraints

- Target platform remains IntelliJ IDEA **2026.2** / build **262** only (`pluginSinceBuild=262`, `pluginUntilBuild=262.*`).
- Hard dependency on plugin id **`com.intellij.diagram`** already present — do not change dependency shape.
- Touch **Show Diagram layouter extras only** — do **not** modify or remove the Swing Diagram tab (`diagram.fileEditor` / `diagram.ui`).
- Do **not** change `StrutsConfigDiagramModel` semantics, edge mapping, tooltips, navigation, compact node chrome, or Dom refresh logic.
- Custom layouter orientation is always **`LEFT_TO_RIGHT`** (Maven pattern). Soft preference = do not reset a user-selected non-custom toolbar layout via `GraphSettings.setCurrentLayouter`.
- Do **not** read `GraphSettings.getCurrentLayoutOrientation()` for the custom layouter (platform default is top-to-bottom and would defeat LTR).
- Do **not** implement #96–#100 or Swing tab removal in this plan.
- Tests gate: `./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"`.
- Learn API details from local IU SDK jars under Gradle caches (`lib/intellij.platform.graph.jar`, `plugins/uml/lib/uml-support.jar`) if signatures drift.

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramExtras.java` | Modify | Override `getCustomLayouter` → hierarchic LTR |
| `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java` | Modify | Assert custom layouter is hierarchic and `LEFT_TO_RIGHT` |
| `CHANGELOG.md` | Modify | Unreleased note for Show Diagram LTR layout (#122) |

No new production classes. No `plugin.xml` / Gradle dependency changes. No Dom refresh changes (verify no `setCurrentLayouter`).

---

### Task 1: Failing test for LTR custom layouter

**Files:**
- Modify: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java`
- Test: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java`

**Interfaces:**
- Consumes:
  - `StrutsDiagramProvider.getExtras()`
  - `DiagramExtras.getCustomLayouter(GraphSettings, Project)`
  - `com.intellij.openapi.graph.settings.GraphSettings`
  - `com.intellij.openapi.graph.layout.CanonicMultiStageLayouter#getLayoutOrientation()`
  - `com.intellij.openapi.graph.layout.LayoutOrientation#LEFT_TO_RIGHT`
  - `com.intellij.openapi.graph.layout.hierarchic.HierarchicGroupLayouter`
- Produces: failing assertion that `getCustomLayouter` returns hierarchic LTR (currently returns `null` from `DiagramExtras` default)

- [ ] **Step 1: Add imports to `StrutsDiagramProviderTest`**

Add these imports (keep existing ones):

```java
import com.intellij.openapi.graph.layout.CanonicMultiStageLayouter;
import com.intellij.openapi.graph.layout.Layouter;
import com.intellij.openapi.graph.layout.LayoutOrientation;
import com.intellij.openapi.graph.layout.hierarchic.HierarchicGroupLayouter;
import com.intellij.openapi.graph.settings.GraphSettings;
```

- [ ] **Step 2: Add failing test method**

Add this test method to `StrutsDiagramProviderTest` (near the other extras tests):

```java
    public void testCustomLayouterIsHierarchicLeftToRight() {
        StrutsDiagramProvider provider = getProvider();
        DiagramExtras<StrutsDiagramItem> extras = provider.getExtras();
        GraphSettings settings = new GraphSettings();

        Layouter layouter = extras.getCustomLayouter(settings, getProject());
        assertNotNull("Show Diagram must provide a custom layouter for LTR hierarchy", layouter);
        assertInstanceOf(layouter, HierarchicGroupLayouter.class);

        CanonicMultiStageLayouter multiStage = (CanonicMultiStageLayouter) layouter;
        assertEquals("Custom layouter must be left-to-right (Maven/Gradle pattern)",
                LayoutOrientation.LEFT_TO_RIGHT,
                multiStage.getLayoutOrientation());

        // Stable custom path — soft preference is "don't reset GraphSettings", not reading orientation
        Layouter again = extras.getCustomLayouter(settings, getProject());
        assertNotNull(again);
        assertEquals(LayoutOrientation.LEFT_TO_RIGHT,
                ((CanonicMultiStageLayouter) again).getLayoutOrientation());
    }
```

- [ ] **Step 3: Run test to verify it fails**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramProviderTest.testCustomLayouterIsHierarchicLeftToRight"
```

Expected: FAIL because current `DiagramExtras.getCustomLayouter` returns `null` (assertion `assertNotNull(...)` fails).

- [ ] **Step 4: Commit the failing test**

```bash
git add src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java
git commit -m "$(cat <<'EOF'
test(diagram): require Show Diagram LTR custom layouter (#122)

EOF
)"
```

---

### Task 2: Implement Maven-style LTR `getCustomLayouter`

**Files:**
- Modify: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramExtras.java`
- Test: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java`

**Interfaces:**
- Consumes:
  - `GraphManager.getGraphManager()`
  - `GraphManager.createHierarchicGroupLayouter()`
  - `GraphManager.createOrientationLayouter(byte)`
  - `HierarchicGroupLayouter.setOrientationLayouter(LayoutStage)`
  - `LayoutOrientation.LEFT_TO_RIGHT`
- Produces: `StrutsDiagramExtras.getCustomLayouter(GraphSettings, Project)` returning hierarchic LTR

- [ ] **Step 1: Add imports to `StrutsDiagramExtras`**

Add these imports (keep existing ones):

```java
import com.intellij.openapi.graph.GraphManager;
import com.intellij.openapi.graph.layout.Layouter;
import com.intellij.openapi.graph.layout.LayoutOrientation;
import com.intellij.openapi.graph.layout.hierarchic.HierarchicGroupLayouter;
import com.intellij.openapi.graph.settings.GraphSettings;
import com.intellij.openapi.project.Project;
```

- [ ] **Step 2: Override `getCustomLayouter`**

Add this method to `StrutsDiagramExtras` (after `isZoomAnimationsEnabled()` is a good place):

```java
    /**
     * Prefer package → action → result left-to-right on Show Diagram.
     * Soft preference for user-selected non-custom toolbar layouts is handled by
     * not mutating {@link GraphSettings#setCurrentLayouter} on Dom refresh.
     */
    @Override
    public @NotNull Layouter getCustomLayouter(@NotNull GraphSettings settings,
                                               @NotNull Project project) {
        GraphManager graphManager = GraphManager.getGraphManager();
        HierarchicGroupLayouter layouter = graphManager.createHierarchicGroupLayouter();
        layouter.setOrientationLayouter(
                graphManager.createOrientationLayouter(LayoutOrientation.LEFT_TO_RIGHT));
        return layouter;
    }
```

Notes for the implementer:

- Match the Maven/Gradle UML extras pattern (`createHierarchicGroupLayouter` + `createOrientationLayouter(LEFT_TO_RIGHT)`).
- Do **not** call `settings.setCurrentLayouter(...)`.
- Do **not** read `settings.getCurrentLayoutOrientation()`.
- Optional Maven tweaks (`setMinimalNodeDistance`, `setLayerer(createBFSLayerer())`) are **out of scope** unless the minimal override fails manual LTR smoke — YAGNI.
- If `@NotNull` on the override conflicts with the platform signature (`Layouter` nullable), drop `@NotNull` on the method return and keep `return layouter;` non-null in practice.
- Confirm parameter nullability against `javap` on `DiagramExtras` if the IDE complains:

```bash
UML_JAR=$(find ~/.gradle/caches -path '*idea-2026.2*/plugins/uml/lib/uml-support.jar' | head -1)
javap -classpath "$UML_JAR" -public com.intellij.diagram.extras.DiagramExtras | rg getCustomLayouter
```

- [ ] **Step 3: Run the focused test to verify it passes**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramProviderTest.testCustomLayouterIsHierarchicLeftToRight"
```

Expected: PASS.

- [ ] **Step 4: Run the diagram test suite**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"
```

Expected: BUILD SUCCESSFUL; all `com.intellij.struts2.diagram.*` tests green.

- [ ] **Step 5: Confirm Dom refresh still does not reset layouter**

Run:

```bash
rg -n "setCurrentLayouter" src/main/java/com/intellij/struts2/diagram
```

Expected: no matches (soft preference preserved by omission).

- [ ] **Step 6: Commit the implementation**

```bash
git add src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramExtras.java \
        src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java
git commit -m "$(cat <<'EOF'
feat(diagram): use LTR hierarchic Show Diagram layouter (#122)

EOF
)"
```

---

### Task 3: Changelog + issue framing note

**Files:**
- Modify: `CHANGELOG.md`
- Related issue: [#122](https://github.com/apache/struts-intellij-plugin/issues/122) (PR description / optional issue comment)

**Interfaces:**
- Consumes: Keep a Changelog `[Unreleased]` → `### Changed` section style already used for #117/#120
- Produces: Unreleased changelog bullet for #122

- [ ] **Step 1: Add Unreleased changelog entry**

Under `## [Unreleased]` → `### Changed`, add (near the other Show Diagram bullets):

```markdown
- Show Diagram uses a left-to-right hierarchic layout by default for Struts config graphs ([#122](https://github.com/apache/struts-intellij-plugin/issues/122))
```

- [ ] **Step 2: Commit changelog**

```bash
git add CHANGELOG.md
git commit -m "$(cat <<'EOF'
docs: changelog Show Diagram LTR layout (#122)

EOF
)"
```

- [ ] **Step 3: Manual `runIde` smoke (implementer / reviewer)**

Run:

```bash
./gradlew runIde
```

Check on IU:

1. Open a small Struts config → **Show Diagram** → package → action → result reads left-to-right.
2. Edge label (e.g. `success`) remains readable.
3. Pick a different layout algorithm from the diagram toolbar, edit the XML so Dom refresh runs → the user’s layout choice remains (not forced back to custom LTR).

- [ ] **Step 4: When opening the PR, refresh #122 framing**

In the PR body (and optionally an issue comment), note:

- Swing Diagram tab LTR is no longer the acceptance baseline.
- Implementation follows Maven/Gradle `getCustomLayouter` + `LEFT_TO_RIGHT`.
- Soft preference: Dom refresh does not reset a user-selected non-custom layout algorithm.

Suggested PR acceptance checklist:

```markdown
## Summary
- Show Diagram Struts configs use hierarchic left-to-right custom layouter via `StrutsDiagramExtras.getCustomLayouter`
- Soft preference: Dom refresh does not reset user-selected non-custom toolbar layouts
- Closes #122

## Test plan
- [ ] `./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"`
- [ ] Manual `runIde`: small config reads package → action → result LTR; `success` label readable
- [ ] Manual: change layout algorithm, edit XML → choice preserved after Dom refresh
```

---

## Spec coverage checklist

| Spec requirement | Task |
|---|---|
| Initial LTR hierarchic layout via `getCustomLayouter` | Task 2 |
| Soft preference (no `setCurrentLayouter` on Dom refresh) | Task 2 Step 5 (verify omission) |
| Edge labeling left at platform default (`true`) | Task 2 (no `doEdgeLabeling` override) |
| No `StrutsConfigDiagramModel` changes | All tasks (extras/tests/changelog only) |
| Automated orientation smoke test | Task 1 |
| Manual `runIde` checks | Task 3 Step 3 |
| Changelog | Task 3 |
| Update #122 framing | Task 3 Step 4 |
| No Swing tab / pixel grid / #96–#100 | Global constraints |

## Plan self-review

1. **Spec coverage:** All goals mapped to tasks above; no gaps.
2. **Placeholder scan:** No TBD/TODO; concrete code and commands included.
3. **Type consistency:** `getCustomLayouter(GraphSettings, Project) -> Layouter` / `HierarchicGroupLayouter` / `LayoutOrientation.LEFT_TO_RIGHT` used consistently across tasks.
