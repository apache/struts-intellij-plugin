# Show Diagram Compact Label Nodes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make IntelliJ Show Diagram Struts nodes render as compact platform icon+label chrome instead of empty UML class boxes.

**Architecture:** Keep the existing Show Diagram provider/data model. Change only `StrutsDiagramExtras` to extend `CommonDiagramExtras` and route both `createNodeComponent` overloads to `createLabelNode`. Leave the Swing Diagram tab untouched.

**Tech Stack:** IntelliJ IDEA Ultimate 2026.2 (262), `com.intellij.diagram` (`CommonDiagramExtras`, `DiagramExtras#createNodeComponent`), JUnit 4 light tests (`BasicLightHighlightingTestCase`), JDK dynamic proxies for a minimal `DiagramBuilder` stub (no Mockito in this project).

**Spec:** `docs/superpowers/specs/2026-07-25-show-diagram-compact-nodes-design.md`

## Global Constraints

- Target platform remains IntelliJ IDEA **2026.2** / build **262** only (`pluginSinceBuild=262`, `pluginUntilBuild=262.*`).
- Hard dependency on plugin id **`com.intellij.diagram`** already present — do not change dependency shape.
- Touch **Show Diagram node chrome only** — do **not** modify or remove the Swing Diagram tab (`diagram.fileEditor` / `diagram.ui`).
- Do **not** change `StrutsConfigDiagramModel` semantics, edge mapping, tooltips, navigation, or Dom refresh.
- Do **not** implement #96–#100 or Swing tab removal in this plan.
- Visual style is platform compact label (`createLabelNode` / `SimpleColoredComponent`), not custom colored chips.
- Tests gate: `./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"`.
- Learn API details from local IU SDK jars under Gradle caches (`plugins/uml/lib/uml-support.jar`) if signatures drift.

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramExtras.java` | Modify | Extend `CommonDiagramExtras`; override both `createNodeComponent` overloads → `createLabelNode` |
| `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java` | Modify | Add smoke test: `createNodeComponent` returns label chrome, not `DiagramNodeContainer` |
| `CHANGELOG.md` | Modify | Unreleased note for compact Show Diagram nodes (#120) |

No new production classes. No `plugin.xml` / Gradle dependency changes.

---

### Task 1: Failing test for compact node chrome

**Files:**
- Modify: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java`
- Test: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java`

**Interfaces:**
- Consumes:
  - `StrutsDiagramProvider.getExtras()`
  - `DiagramExtras.createNodeComponent(DiagramNode, DiagramBuilder, Point, JPanel)`
  - `StrutsDiagramApiNode(DiagramProvider, StrutsDiagramItem)`
  - `StrutsConfigDiagramModel.build(XmlFile)`
- Produces: failing assertion that node component is `SimpleColoredComponent` and not `DiagramNodeContainer`

- [ ] **Step 1: Add imports and stub helpers to `StrutsDiagramProviderTest`**

Add these imports (keep existing ones):

```java
import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.components.DiagramNodeContainer;
import com.intellij.diagram.extras.DiagramExtras;
import com.intellij.diagram.extras.custom.CommonDiagramExtras;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.graph.view.Graph2D;
import com.intellij.ui.SimpleColoredComponent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Point;
import java.lang.reflect.Proxy;
```

Add these private helpers at the bottom of the test class (before or after `getProvider()`):

```java
    /**
     * Minimal DiagramBuilder for createLabelNode / setNodeBorders.
     * No Mockito in this project — use JDK proxies.
     */
    private DiagramBuilder stubBuilderForLabelNodes(@NotNull StrutsDiagramProvider provider,
                                                    @NotNull DiagramDataModel<StrutsDiagramItem> dataModel) {
        var scheme = EditorColorsManager.getInstance().getGlobalScheme();
        Graph2D graph = (Graph2D) Proxy.newProxyInstance(
                Graph2D.class.getClassLoader(),
                new Class<?>[]{Graph2D.class},
                (proxy, method, args) -> {
                    if ("isSelected".equals(method.getName())) {
                        return false;
                    }
                    return proxyDefaultValue(method.getReturnType());
                });
        return (DiagramBuilder) Proxy.newProxyInstance(
                DiagramBuilder.class.getClassLoader(),
                new Class<?>[]{DiagramBuilder.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getColorScheme" -> scheme;
                    case "getProvider" -> provider;
                    case "getDataModel" -> dataModel;
                    case "getGraph" -> graph;
                    case "getNode" -> null;
                    case "toString" -> "StubDiagramBuilder";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> proxyDefaultValue(method.getReturnType());
                });
    }

    private static @Nullable Object proxyDefaultValue(@NotNull Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) return false;
        if (returnType == byte.class) return (byte) 0;
        if (returnType == short.class) return (short) 0;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == float.class) return 0f;
        if (returnType == double.class) return 0d;
        if (returnType == char.class) return '\0';
        return null;
    }
```

- [ ] **Step 2: Write the failing test method**

Add to `StrutsDiagramProviderTest`:

```java
    public void testExtrasCreateCompactLabelNodeComponents() {
        createStrutsFileSet("struts-diagram.xml");
        VirtualFile file = myFixture.findFileInTempDir("struts-diagram.xml");
        assertNotNull(file);
        XmlFile xml = (XmlFile) PsiManager.getInstance(getProject()).findFile(file);
        assertNotNull(xml);

        StrutsConfigDiagramModel model = ReadAction.compute(() -> StrutsConfigDiagramModel.build(xml));
        assertNotNull(model);
        StrutsDiagramNode snapshotNode = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.ACTION)
                .findFirst()
                .orElseThrow();
        assertNotNull(snapshotNode.getIcon());
        assertFalse(snapshotNode.getName().isEmpty());

        StrutsDiagramProvider provider = getProvider();
        DiagramExtras<StrutsDiagramItem> extras = provider.getExtras();
        assertInstanceOf(extras, CommonDiagramExtras.class);

        StrutsDiagramItem item = StrutsDiagramItem.forNode(xml, snapshotNode);
        StrutsDiagramApiNode apiNode = new StrutsDiagramApiNode(provider, item);

        StrutsDiagramDataModel dataModel =
                new StrutsDiagramDataModel(getProject(), provider, StrutsDiagramItem.forFile(xml));
        try {
            DiagramBuilder builder = stubBuilderForLabelNodes(provider, dataModel);
            JPanel wrapper = new JPanel();
            JComponent component = extras.createNodeComponent(apiNode, builder, new Point(0, 0), wrapper);

            assertNotNull(component);
            assertFalse(
                    "Show Diagram nodes must not use UML DiagramNodeContainer chrome",
                    component instanceof DiagramNodeContainer);
            assertInstanceOf(component, SimpleColoredComponent.class);

            SimpleColoredComponent label = (SimpleColoredComponent) component;
            assertNotNull("Package/action/result icon must remain visible", label.getIcon());
            assertTrue(
                    "Node name must remain visible on the label",
                    label.getCharSequence(true).toString().contains(snapshotNode.getName()));
        } finally {
            com.intellij.openapi.util.Disposer.dispose(dataModel);
        }
    }
```

Also add:

```java
import com.intellij.struts2.diagram.provider.StrutsDiagramDataModel;
```

If `getCharSequence(true)` is unavailable on 262, fall back to asserting `label.toString()` contains the name, or that `provider.getElementManager().getElementTitle(item)` equals `snapshotNode.getName()` **and** the component is still a `SimpleColoredComponent` with a non-null icon.

- [ ] **Step 3: Run test to verify it fails**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramProviderTest.testExtrasCreateCompactLabelNodeComponents"
```

Expected: FAIL because current `createNodeComponent` returns `DiagramNodeContainer` (or assertion `assertInstanceOf(..., SimpleColoredComponent.class)` fails). Also `assertInstanceOf(extras, CommonDiagramExtras.class)` may fail first — that is an acceptable first failure.

- [ ] **Step 4: Commit the failing test**

```bash
git add src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java
git commit -m "$(cat <<'EOF'
test(diagram): assert Show Diagram uses compact label nodes (#120)

EOF
)"
```

---

### Task 2: Implement `CommonDiagramExtras` label chrome

**Files:**
- Modify: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramExtras.java`
- Test: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java`

**Interfaces:**
- Consumes:
  - `com.intellij.diagram.extras.custom.CommonDiagramExtras#createLabelNode(DiagramNode, DiagramBuilder, JPanel)`
  - Existing `EditNodeHandler` / `uiDataSnapshot` behavior
- Produces:
  - `JComponent createNodeComponent(DiagramNode<StrutsDiagramItem>, DiagramBuilder, NodeRealizer, JPanel)` → label node
  - `JComponent createNodeComponent(DiagramNode<StrutsDiagramItem>, DiagramBuilder, Point, JPanel)` → label node

- [ ] **Step 1: Change superclass and imports**

In `StrutsDiagramExtras.java`, replace:

```java
import com.intellij.diagram.extras.DiagramExtras;
```

with:

```java
import com.intellij.diagram.extras.custom.CommonDiagramExtras;
import com.intellij.openapi.graph.view.NodeRealizer;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Point;
```

Change the class declaration from:

```java
public final class StrutsDiagramExtras extends DiagramExtras<StrutsDiagramItem> {
```

to:

```java
public final class StrutsDiagramExtras extends CommonDiagramExtras<StrutsDiagramItem> {
```

Update the class Javadoc to mention compact label chrome via `createLabelNode`, while keeping the existing note about `EditNodeHandler`.

- [ ] **Step 2: Override both `createNodeComponent` overloads**

Add these methods to `StrutsDiagramExtras` (keep existing `getEditNodeHandler`, `uiDataSnapshot`, `navigateNode`, `resolvePsiElement` unchanged):

```java
    @Override
    public @NotNull JComponent createNodeComponent(@NotNull DiagramNode<StrutsDiagramItem> node,
                                                   @NotNull DiagramBuilder builder,
                                                   @NotNull NodeRealizer nodeRealizer,
                                                   @NotNull JPanel wrapper) {
        return createLabelNode(node, builder, wrapper);
    }

    @Override
    public @NotNull JComponent createNodeComponent(@NotNull DiagramNode<StrutsDiagramItem> node,
                                                   @NotNull DiagramBuilder builder,
                                                   @NotNull Point basePoint,
                                                   @NotNull JPanel wrapper) {
        return createLabelNode(node, builder, wrapper);
    }
```

Do **not** customize `createLabel` / `setNodeBorders` unless the test or `runIde` shows a hard failure; accept platform label defaults.

If the 262 SDK uses a different `NodeRealizer` package or `createNodeComponent` parameter names, match the exact signatures from:

```bash
javap -classpath "$UML_SUPPORT_JAR" -public com.intellij.diagram.extras.DiagramExtras
```

- [ ] **Step 3: Run the new test to verify it passes**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramProviderTest.testExtrasCreateCompactLabelNodeComponents"
```

Expected: BUILD SUCCESSFUL / test PASS.

If `createLabelNode` NPEs on the proxy stub (e.g. deferred icon evaluator / content manager), fix the stub first — do not weaken the production override. Only if the platform API requires a real builder, narrow assertions to the strongest feasible check that still proves non-UML chrome while keeping the override.

- [ ] **Step 4: Run full diagram suite**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"
```

Expected: BUILD SUCCESSFUL; all diagram tests green.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramExtras.java \
        src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java
git commit -m "$(cat <<'EOF'
feat(diagram): use compact icon+label nodes in Show Diagram (#120)

EOF
)"
```

---

### Task 3: Changelog + manual verification notes

**Files:**
- Modify: `CHANGELOG.md`
- Test: full diagram suite (regression)

**Interfaces:**
- Consumes: Task 2 behavior
- Produces: user-facing Unreleased changelog entry for #120

- [ ] **Step 1: Update `CHANGELOG.md`**

Under `## [Unreleased]` → `### Changed`, add:

```markdown
- Show Diagram Struts config nodes use compact icon+label chrome instead of empty UML class boxes ([#120](https://github.com/apache/struts-intellij-plugin/issues/120))
```

Keep the existing #117 Show Diagram bullet as-is (hosts still coexist).

- [ ] **Step 2: Re-run diagram tests**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual smoke checklist** (`./gradlew runIde` on IU)

Verify:

1. Show Diagram on a Struts config → nodes are compact icon+label (no empty UML body).
2. Package / action / result icons and names remain visible.
3. Double-click navigates to XML; hover shows tooltips.
4. Swing Diagram tab still opens and behaves as before.

- [ ] **Step 4: Commit**

```bash
git add CHANGELOG.md
git commit -m "$(cat <<'EOF'
docs: note compact Show Diagram nodes in changelog (#120)

EOF
)"
```

---

## Spec coverage check

| Spec requirement | Task |
|---|---|
| Compact icon+label via `createLabelNode` | Task 2 |
| Package/action/result icons and names visible | Task 1 assertions + Task 3 manual |
| Swing tab unchanged | Global constraint; no Swing files in File Structure |
| Navigation/tooltips/Dom refresh unchanged | Task 2 keeps extras handlers; no data-model edits |
| Automated component-type smoke | Task 1 |
| Manual `runIde` | Task 3 |
| No custom colored chips / no Swing removal / no #96–#100 | Global constraints |

## Manual fallback notes

- If `NodeRealizer` import path differs on the resolved IU SDK, use the type from `DiagramExtras.createNodeComponent` via `javap`.
- If `SimpleColoredComponent.getCharSequence(boolean)` is missing, keep the `SimpleColoredComponent` + non-null icon assertions and title check via `getElementManager().getElementTitle(item)`.
- Do not add Mockito for this plan; prefer JDK `Proxy` stubs as shown.
