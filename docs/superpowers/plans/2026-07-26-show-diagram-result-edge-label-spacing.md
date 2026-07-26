# Show Diagram Result Edge Label Spacing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the custom LTR Show Diagram layouter node/layer distances above the platform defaults so multi-result edge labels stay readable without overlapping each other or the action node.

**Architecture:** Keep the existing Show Diagram provider/data model and LTR custom layouter. IU 2026.2 defaults to node distance `20.0` and layer distance `40.0`; in `StrutsDiagramExtras.getCustomLayouter`, after setting `LEFT_TO_RIGHT`, raise them to `setMinimalNodeDistance(40.0)` and `setMinimalLayerDistance(60.0)`. Maven/Gradle's `20` / `20` is rejected because it is unlabeled-graph compaction, while Spring's labeled graphs use `40` / `80`; `40` / `60` is a moderate bump for typical 2–3 result actions. Do not change label model, relationship Builder, BFS layerer, snapshot semantics, or Dom refresh. Soft preference from #122 remains: never call `GraphSettings.setCurrentLayouter`.

**Tech Stack:** IntelliJ IDEA Ultimate 2026.2 (262), `com.intellij.diagram` (`DiagramExtras#getCustomLayouter`), `com.intellij.openapi.graph` (`HierarchicGroupLayouter` / `HierarchicLayouter` distance setters), JUnit 4 light tests (`BasicLightHighlightingTestCase`).

**Spec:** `docs/superpowers/specs/2026-07-26-show-diagram-result-edge-label-spacing-design.md`

## Global Constraints

- Target platform remains IntelliJ IDEA **2026.2** / build **262** only (`pluginSinceBuild=262`, `pluginUntilBuild=262.*`).
- Hard dependency on plugin id **`com.intellij.diagram`** already present — do not change dependency shape.
- Touch **Show Diagram custom layouter extras only** — do **not** modify or remove the Swing Diagram tab (`diagram.fileEditor` / `diagram.ui`).
- Do **not** change `StrutsConfigDiagramModel` semantics, `StrutsDiagramApiEdge` relationship Builder, Dom refresh, compact node chrome, navigation, or tooltips.
- Keep orientation **`LEFT_TO_RIGHT`**. Soft preference = do not reset a user-selected non-custom toolbar layout via `GraphSettings.setCurrentLayouter`.
- Platform defaults are node **`20.0`** and layer **`40.0`** on IU 2026.2.
- Production spacing values are exactly node **`40.0`** and layer **`60.0`**, raising both above their defaults.
- Maven/Gradle **`20` / `20`** is not the precedent: it compacts unlabeled graphs and would leave node spacing unchanged while halving layer spacing.
- Do **not** call `createBFSLayerer()` / `setLayerer(...)`.
- Do **not** change edge label model / upper-center placement.
- Do **not** apply spacing to non-custom toolbar layouts.
- Do **not** implement #96–#100 or Swing tab removal in this plan.
- Tests gate: `./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"`.
- Learn API details from local IU SDK jars under Gradle caches (`lib/intellij.platform.graph.jar`, `plugins/uml/lib/uml-support.jar`) if signatures drift.

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramExtras.java` | Modify | After LTR, set minimal node/layer distances to `40.0` / `60.0` |
| `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java` | Modify | Assert custom layouter distances are `40.0` / `60.0` |
| `CHANGELOG.md` | Modify | Unreleased note for Show Diagram label spacing (#128) |

No new production classes. No `plugin.xml` / Gradle dependency changes. No Dom refresh changes (verify no `setCurrentLayouter`).

---

### Task 1: Failing test for above-default layouter distances

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
  - `com.intellij.openapi.graph.layout.hierarchic.HierarchicLayouter#getMinimalNodeDistance()`
  - `com.intellij.openapi.graph.layout.hierarchic.HierarchicLayouter#getMinimalLayerDistance()`
- Produces: failing assertions that custom layouter node/layer distances equal `40.0` / `60.0` (current implementation uses `20.0` / `20.0`)

- [ ] **Step 1: Extend `testCustomLayouterIsHierarchicLeftToRight` with distance asserts**

In `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java`, replace the existing `testCustomLayouterIsHierarchicLeftToRight` method with:

```java
    public void testCustomLayouterIsHierarchicLeftToRight() {
        StrutsDiagramProvider provider = getProvider();
        DiagramExtras<StrutsDiagramItem> extras = provider.getExtras();
        assertFalse("Custom layouter is only consulted when useDefaultLayouter() is false",
                extras.useDefaultLayouter());
        GraphSettings settings = new GraphSettings();

        Layouter layouter = extras.getCustomLayouter(settings, getProject());
        assertNotNull("Show Diagram must provide a custom layouter for LTR hierarchy", layouter);
        assertInstanceOf(layouter, HierarchicGroupLayouter.class);

        HierarchicGroupLayouter hierarchic = (HierarchicGroupLayouter) layouter;
        CanonicMultiStageLayouter multiStage = (CanonicMultiStageLayouter) layouter;
        assertEquals("Custom layouter must be left-to-right (Maven/Gradle pattern)",
                LayoutOrientation.LEFT_TO_RIGHT,
                multiStage.getLayoutOrientation());
        assertEquals("Custom layouter must leave more vertical room for result labels",
                40.0,
                hierarchic.getMinimalNodeDistance(),
                0.0);
        assertEquals("Custom layouter must leave more horizontal room for result labels",
                60.0,
                hierarchic.getMinimalLayerDistance(),
                0.0);

        // Stable custom path — soft preference is "don't reset GraphSettings", not reading orientation
        Layouter again = extras.getCustomLayouter(settings, getProject());
        assertNotNull(again);
        assertEquals(LayoutOrientation.LEFT_TO_RIGHT,
                ((CanonicMultiStageLayouter) again).getLayoutOrientation());
        HierarchicGroupLayouter againHierarchic = (HierarchicGroupLayouter) again;
        assertEquals("Repeated custom layouter must preserve minimal node distance",
                40.0, againHierarchic.getMinimalNodeDistance(), 0.0);
        assertEquals("Repeated custom layouter must preserve minimal layer distance",
                60.0, againHierarchic.getMinimalLayerDistance(), 0.0);
    }
```

Notes:

- Keep existing imports (`CanonicMultiStageLayouter`, `Layouter`, `LayoutOrientation`, `HierarchicGroupLayouter`, `GraphSettings`). No new imports required if those are already present.
- Do **not** assert pixel coordinates, label bounding boxes, or rendered overlap.
- Use the three-argument double overload with a message: `assertEquals(message, expected, actual, 0.0)`.

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramProviderTest.testCustomLayouterIsHierarchicLeftToRight"
```

Expected: FAIL because current `getCustomLayouter` still sets node/layer distances to `20.0` / `20.0`.

- [ ] **Step 3: Commit the failing test**

```bash
git add src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java
git commit -m "$(cat <<'EOF'
test(diagram): require above-default Show Diagram layouter distances (#128)

EOF
)"
```

---

### Task 2: Set above-default distances on custom LTR layouter

**Files:**
- Modify: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramExtras.java`
- Test: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java`

**Interfaces:**
- Consumes:
  - `GraphManager.getGraphManager()`
  - `GraphManager.createHierarchicGroupLayouter()`
  - `GraphManager.createOrientationLayouter(byte)`
  - `HierarchicGroupLayouter.setOrientationLayouter(LayoutStage)`
  - `HierarchicLayouter.setMinimalNodeDistance(double)`
  - `HierarchicLayouter.setMinimalLayerDistance(double)`
  - `LayoutOrientation.LEFT_TO_RIGHT`
- Produces: `StrutsDiagramExtras.getCustomLayouter(GraphSettings, Project)` returning hierarchic LTR with node/layer distances `40.0` / `60.0`

- [ ] **Step 1: Update `getCustomLayouter` in `StrutsDiagramExtras`**

Replace the existing `getCustomLayouter` method body so the method looks like this (keep the existing javadoc intent; expand slightly if useful):

```java
    /**
     * Prefer package → action → result left-to-right on Show Diagram.
     * {@code settings} is deliberately not consulted: the platform default orientation is
     * top-to-bottom, and the soft preference on Dom refresh is not resetting the user's
     * toolbar layout choice rather than reading orientation from {@link GraphSettings}.
     * <p>
     * Distances above the platform defaults give multi-result edge labels more vertical
     * and horizontal room under LTR hierarchy.
     */
    @Override
    public @NotNull Layouter getCustomLayouter(GraphSettings settings,
                                               Project project) {
        GraphManager graphManager = GraphManager.getGraphManager();
        HierarchicGroupLayouter layouter = graphManager.createHierarchicGroupLayouter();
        layouter.setOrientationLayouter(
                graphManager.createOrientationLayouter(LayoutOrientation.LEFT_TO_RIGHT));
        layouter.setMinimalNodeDistance(40.0);
        layouter.setMinimalLayerDistance(60.0);
        return layouter;
    }
```

Notes for the implementer:

- Use `40.0` / `60.0`, above the IU 2026.2 defaults (`20.0` / `40.0`).
- Do not copy Maven/Gradle's `20` / `20` unlabeled-compaction values. Spring's `40` / `80` is only a reference point; this plan deliberately uses the moderate `40` / `60` bump.
- Do **not** call `graphManager.createBFSLayerer()` or `layouter.setLayerer(...)`.
- Do **not** call `settings.setCurrentLayouter(...)`.
- Do **not** read `settings.getCurrentLayoutOrientation()`.
- Do **not** override `doEdgeLabeling()` — leave platform default (`true`).
- Keep parameter nullability matching the current override signature in this file (drop `@NotNull` on parameters/return only if the platform signature requires it).
- Confirm setters exist on `HierarchicGroupLayouter` / `HierarchicLayouter` if the IDE complains:

```bash
GRAPH_JAR=$(find ~/.gradle/caches -path '*idea-2026.2*/lib/intellij.platform.graph.jar' | head -1)
javap -classpath "$GRAPH_JAR" -public com.intellij.openapi.graph.layout.hierarchic.HierarchicLayouter | rg Minimal
```

- [ ] **Step 2: Run the focused test to verify it passes**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramProviderTest.testCustomLayouterIsHierarchicLeftToRight"
```

Expected: PASS.

- [ ] **Step 3: Run the diagram test suite**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"
```

Expected: BUILD SUCCESSFUL; all `com.intellij.struts2.diagram.*` tests green.

- [ ] **Step 4: Confirm Dom refresh still does not reset layouter**

Run:

```bash
rg -n "setCurrentLayouter" src/main/java/com/intellij/struts2/diagram
```

Expected: no matches (soft preference preserved by omission).

- [ ] **Step 5: Confirm BFS layerer was not introduced**

Run:

```bash
rg -n "createBFSLayerer|setLayerer" src/main/java/com/intellij/struts2/diagram
```

Expected: no matches.

- [ ] **Step 6: Commit the implementation**

```bash
git add src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramExtras.java \
        src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java
git commit -m "$(cat <<'EOF'
fix(diagram): space Show Diagram result edge labels in LTR (#128)

EOF
)"
```

---

### Task 3: Changelog + manual smoke checklist

**Files:**
- Modify: `CHANGELOG.md`

**Interfaces:**
- Consumes: Unreleased changelog sections already used by prior Show Diagram entries
- Produces: Unreleased Fixed (or Changed) bullet linking #128

- [ ] **Step 1: Add Unreleased changelog entry**

Under `## [Unreleased]` → `### Fixed` in `CHANGELOG.md`, add (keep existing bullets; place near other Show Diagram fixes):

```markdown
- Fix Show Diagram cramped multi-result edge labels under left-to-right layout ([#128](https://github.com/apache/struts-intellij-plugin/issues/128))
```

If `### Fixed` is missing under Unreleased, create that subsection rather than putting the bullet under `### Changed`.

- [ ] **Step 2: Manual smoke (`./gradlew runIde` on IU)**

1. Open a Struts config whose action has at least two named results (e.g. `success` and `delete`).
2. Show Diagram → confirm package → action → result still reads left-to-right.
3. Confirm result edge labels are readable without overlapping each other.
4. Confirm labels do not collide with the action node chrome.
5. Optionally pick a non-custom toolbar layout, edit XML / refresh → confirm the user’s layout choice is not forced back to custom LTR (soft preference from #122).

- [ ] **Step 3: Commit changelog**

```bash
git add CHANGELOG.md
git commit -m "$(cat <<'EOF'
docs: note Show Diagram result edge label spacing (#128)

EOF
)"
```

---

## Spec coverage checklist

| Spec requirement | Task |
|---|---|
| `setMinimalNodeDistance(40.0)` + `setMinimalLayerDistance(60.0)` on custom LTR layouter | Task 2 |
| Keep LTR orientation / #122 flow | Task 2 (preserve existing orientation) + Task 3 manual |
| Upper-center labels / relationship Builder unchanged | Global constraint + no ApiEdge edits |
| No BFS layerer | Task 2 notes + Step 5 grep |
| Custom LTR path only; other toolbar layouts untouched | Global constraint + soft-preference grep |
| Soft preference / no `setCurrentLayouter` | Task 2 Step 4 |
| Automated distance asserts | Task 1 |
| Manual `runIde` multi-result smoke | Task 3 |
| Changelog #128 | Task 3 |
| No model / Dom refresh / Swing / pixel asserts | Global constraints |

## Plan self-review

1. **Spec coverage:** All goals/decisions from the design map to Task 1–3; future work (label model, BFS, non-custom layouts) stays out.
2. **Placeholder scan:** No TBD/TODO; concrete code, commands, and expected results included.
3. **Type consistency:** `HierarchicGroupLayouter` + `getMinimalNodeDistance` / `getMinimalLayerDistance` / setters use `40.0` / `60.0` consistently; method name remains `getCustomLayouter(GraphSettings, Project)`.
