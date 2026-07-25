# Fix Blank Diagram Tab on 262 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore the struts.xml Diagram tab on IntelliJ 2026.2 so it shows packages/actions/results (or a visible placeholder), not a blank editor pane.

**Architecture:** Keep the toolkit-neutral `StrutsConfigDiagramModel` and Swing `Struts2DiagramComponent`. Harden `Struts2DiagramFileEditor` so model apply is not gated on `myDiagramSelected`, call `super` on select/deselect, and size the component so EMPTY/UNAVAILABLE fill the editor area. DomEvent live-refresh stays selection-gated.

**Tech Stack:** IntelliJ Platform (`PerspectiveFileEditor`, `ReadAction.nonBlocking`, `DomEventListener`), Swing, JUnit 4 light tests (`BasicLightHighlightingTestCase`).

**Spec:** `docs/superpowers/specs/2026-07-25-diagram-blank-262-design.md`

## Global Constraints

- Target platform remains IntelliJ IDEA **2026.2** / build **262** only (`pluginSinceBuild=262`, `pluginUntilBuild=262.*`).
- Do **not** migrate to `com.intellij.diagram.Provider` (tracked in [#117](https://github.com/apache/struts-intellij-plugin/issues/117)).
- Do not change `StrutsConfigDiagramModel` build semantics or `StrutsDiagramPresentation`.
- Prefer editing existing diagram files; no new modules or extension points.
- Tests: `./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"`.

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `src/main/java/com/intellij/struts2/diagram/ui/Struts2DiagramComponent.java` | Modify | Preferred/minimum size for EMPTY/UNAVAILABLE so placeholders fill the editor |
| `src/main/java/com/intellij/struts2/diagram/fileEditor/Struts2DiagramFileEditor.java` | Modify | Always apply model on UI thread; call `super` in select/deselect; keep DomEvent selection gate |
| `src/test/java/com/intellij/struts2/diagram/StrutsConfigDiagramModelTest.java` | Modify | Assert placeholder preferred/minimum size after `rebuild(null)` / empty model |
| `src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorProviderTest.java` | Modify | Assert editor reaches `LOADED` after create + async pump without requiring DomEvent selection |
| `CHANGELOG.md` | Modify | Unreleased Fixed entry |
| `docs/superpowers/specs/2026-07-25-diagram-blank-262-design.md` | Modify | Link [#117](https://github.com/apache/struts-intellij-plugin/issues/117) under Future work; remove stray trailing `)` if still present |

No other production files change.

---

### Task 1: Placeholder sizing on `Struts2DiagramComponent`

**Files:**
- Modify: `src/test/java/com/intellij/struts2/diagram/StrutsConfigDiagramModelTest.java`
- Modify: `src/main/java/com/intellij/struts2/diagram/ui/Struts2DiagramComponent.java`
- Test: `src/test/java/com/intellij/struts2/diagram/StrutsConfigDiagramModelTest.java`

**Interfaces:**
- Consumes: `Struts2DiagramComponent.rebuild(@Nullable StrutsConfigDiagramModel)`, `getState()`, `getPreferredSize()`, `getMinimumSize()`
- Produces: EMPTY/UNAVAILABLE states set preferred and minimum size to at least `400×300` (logical pixels via `JBUI.size` if already used elsewhere in the file; otherwise plain `Dimension`)

- [ ] **Step 1: Write the failing size assertions**

In `StrutsConfigDiagramModelTest.java`, add:

```java
public void testPlaceholderStatesHaveNonZeroPreferredSize() {
    Struts2DiagramComponent unavailable = new Struts2DiagramComponent(null);
    assertEquals(Struts2DiagramComponent.State.UNAVAILABLE, unavailable.getState());
    assertTrue("UNAVAILABLE preferred width must fill a normal editor area, got "
                    + unavailable.getPreferredSize(),
            unavailable.getPreferredSize().width >= 400);
    assertTrue("UNAVAILABLE preferred height must fill a normal editor area, got "
                    + unavailable.getPreferredSize(),
            unavailable.getPreferredSize().height >= 300);
    assertTrue("UNAVAILABLE minimum width must be non-trivial, got "
                    + unavailable.getMinimumSize(),
            unavailable.getMinimumSize().width >= 400);
    assertTrue("UNAVAILABLE minimum height must be non-trivial, got "
                    + unavailable.getMinimumSize(),
            unavailable.getMinimumSize().height >= 300);

    createStrutsFileSet("struts-empty.xml");
    VirtualFile vf = myFixture.findFileInTempDir("struts-empty.xml");
    assertNotNull(vf);
    PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
    assertInstanceOf(psi, XmlFile.class);
    StrutsConfigDiagramModel emptyModel = ReadAction.nonBlocking(
            () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();
    assertNotNull(emptyModel);

    Struts2DiagramComponent empty = new Struts2DiagramComponent(emptyModel);
    assertEquals(Struts2DiagramComponent.State.EMPTY, empty.getState());
    assertTrue("EMPTY preferred width must fill a normal editor area, got "
                    + empty.getPreferredSize(),
            empty.getPreferredSize().width >= 400);
    assertTrue("EMPTY preferred height must fill a normal editor area, got "
                    + empty.getPreferredSize(),
            empty.getPreferredSize().height >= 300);

    empty.rebuild(null);
    assertEquals(Struts2DiagramComponent.State.UNAVAILABLE, empty.getState());
    assertTrue("rebuild(null) must restore non-zero preferred size, got "
                    + empty.getPreferredSize(),
            empty.getPreferredSize().width >= 400
                    && empty.getPreferredSize().height >= 300);
}
```

Add imports only if missing (`VirtualFile`, `PsiFile`, `PsiManager`, `XmlFile`, `ReadAction`, `StrutsConfigDiagramModel` are already used in this class).

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsConfigDiagramModelTest.testPlaceholderStatesHaveNonZeroPreferredSize"
```

Expected: FAIL — preferred/minimum size width or height below 400/300 (default `JPanel` size).

- [ ] **Step 3: Implement placeholder sizing**

In `Struts2DiagramComponent.java`, add a constant near the other layout constants:

```java
private static final Dimension PLACEHOLDER_SIZE = new Dimension(400, 300);
```

Update `applyModel` so null/empty paths size the panel, and LOADED keeps using `layoutModel` (which already calls `setPreferredSize`):

```java
private void applyModel(@Nullable StrutsConfigDiagramModel model) {
    if (model == null) {
        state = State.UNAVAILABLE;
        setPreferredSize(PLACEHOLDER_SIZE);
        setMinimumSize(PLACEHOLDER_SIZE);
        return;
    }
    if (model.getNodes().isEmpty()) {
        state = State.EMPTY;
        setPreferredSize(PLACEHOLDER_SIZE);
        setMinimumSize(PLACEHOLDER_SIZE);
        return;
    }
    state = State.LOADED;
    setMinimumSize(null);
    layoutModel(model);
}
```

Do not change paint/placeholder message strings.

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsConfigDiagramModelTest.testPlaceholderStatesHaveNonZeroPreferredSize"
```

Expected: BUILD SUCCESSFUL / test PASS.

Also run existing component state tests:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsConfigDiagramModelTest.testComponentState*"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/intellij/struts2/diagram/ui/Struts2DiagramComponent.java \
        src/test/java/com/intellij/struts2/diagram/StrutsConfigDiagramModelTest.java
git commit -m "$(cat <<'EOF'
fix(diagram): size EMPTY/UNAVAILABLE panels so placeholders are visible

EOF
)"
```

---

### Task 2: Ungate model apply and harden editor lifecycle

**Files:**
- Modify: `src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorProviderTest.java`
- Modify: `src/main/java/com/intellij/struts2/diagram/fileEditor/Struts2DiagramFileEditor.java`
- Test: `src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorProviderTest.java`

**Interfaces:**
- Consumes: `Struts2DiagramFileEditorProvider.createEditor`, `FileEditor.getPreferredFocusedComponent()`, `Struts2DiagramComponent.getState()`
- Produces: `scheduleModelBuild()` UI callback always calls `myComponent.rebuild(model)`; `selectNotify`/`deselectNotify` call `super`; `myDiagramSelected` still gates DomEvent scheduling only

- [ ] **Step 1: Write the failing editor load test**

In `Struts2DiagramFileEditorProviderTest.java`, add imports:

```java
import com.intellij.struts2.diagram.ui.Struts2DiagramComponent;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ui.UIUtil;
```

Add test method (do **not** call `selectNotify` before asserting load — that is the regression):

```java
public void testEditorAppliesModelWithoutSelectNotify() {
    createStrutsFileSet("struts-diagram.xml");
    VirtualFile file = myFixture.findFileInTempDir("struts-diagram.xml");
    assertNotNull(file);

    Struts2DiagramFileEditor editor =
            (Struts2DiagramFileEditor) myProvider.createEditor(getProject(), file);
    try {
        PlatformTestUtil.waitForCondition(10_000, () -> {
            UIUtil.dispatchAllInvocationEvents();
            Struts2DiagramComponent component =
                    (Struts2DiagramComponent) editor.getPreferredFocusedComponent();
            return component != null
                    && component.getState() == Struts2DiagramComponent.State.LOADED;
        });

        Struts2DiagramComponent component =
                (Struts2DiagramComponent) editor.getPreferredFocusedComponent();
        assertNotNull(component);
        assertEquals("Constructor scheduleModelBuild must apply model without selectNotify",
                Struts2DiagramComponent.State.LOADED, component.getState());
    } finally {
        Disposer.dispose(editor);
    }
}
```

If `PlatformTestUtil.waitForCondition(long, BooleanSupplier)` is unavailable or has a different signature on 262, use this equivalent wait loop instead (same semantics):

```java
long deadline = System.currentTimeMillis() + 10_000;
Struts2DiagramComponent component = null;
while (System.currentTimeMillis() < deadline) {
    UIUtil.dispatchAllInvocationEvents();
    component = (Struts2DiagramComponent) editor.getPreferredFocusedComponent();
    if (component != null && component.getState() == Struts2DiagramComponent.State.LOADED) {
        break;
    }
    Thread.sleep(50);
}
assertNotNull(component);
assertEquals(Struts2DiagramComponent.State.LOADED, component.getState());
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.Struts2DiagramFileEditorProviderTest.testEditorAppliesModelWithoutSelectNotify"
```

Expected: FAIL or timeout — `myDiagramSelected` is false, so `finishOnUiThread` skips `rebuild()` and state stays `UNAVAILABLE`.

- [ ] **Step 3: Implement editor lifecycle fixes**

In `Struts2DiagramFileEditor.java`, replace `selectNotify` / `deselectNotify` / `scheduleModelBuild` as follows:

```java
@Override
public void selectNotify() {
    super.selectNotify();
    myDiagramSelected = true;
    myUpdateAlarm.cancelAllRequests();
    scheduleModelBuild();
}

@Override
public void deselectNotify() {
    myDiagramSelected = false;
    myUpdateAlarm.cancelAllRequests();
    super.deselectNotify();
}

private void scheduleModelBuild() {
    ReadAction.nonBlocking(() -> StrutsConfigDiagramModel.build(myXmlFile))
            .expireWith(this)
            .finishOnUiThread(com.intellij.openapi.application.ModalityState.defaultModalityState(),
                    model -> myComponent.rebuild(model))
            .submit(AppExecutorUtil.getAppExecutorService());
}
```

Leave `registerDomChangeListener()` unchanged — it must still return early when `!myDiagramSelected`.

Update the class Javadoc bullet that says the UI callback is visibility-gated for rebuild; state that DomEvents remain gated, but completed builds always call `rebuild`.

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.Struts2DiagramFileEditorProviderTest"
```

Expected: BUILD SUCCESSFUL — including `testEditorAppliesModelWithoutSelectNotify` and existing select/deselect / reset tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/intellij/struts2/diagram/fileEditor/Struts2DiagramFileEditor.java \
        src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorProviderTest.java
git commit -m "$(cat <<'EOF'
fix(diagram): always apply built model and call super on tab select

EOF
)"
```

---

### Task 3: Changelog, spec cross-link, full diagram suite

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `docs/superpowers/specs/2026-07-25-diagram-blank-262-design.md`
- Test: full `com.intellij.struts2.diagram.*` suite

**Interfaces:**
- Consumes: Tasks 1–2 behavior
- Produces: Documented fix under Unreleased Fixed; Future work links [#117](https://github.com/apache/struts-intellij-plugin/issues/117)

- [ ] **Step 1: Update CHANGELOG**

Under `## [Unreleased]`, add a `### Fixed` section if missing, with:

```markdown
### Fixed

- Fix blank Diagram tab on IntelliJ 2026.2: always apply the built model, invoke `PerspectiveFileEditor` select/deselect hooks, and size EMPTY/UNAVAILABLE panels so placeholders are visible
```

Keep existing `### Changed` entries intact.

- [ ] **Step 2: Cross-link Diagrams API issue in the design spec**

In `docs/superpowers/specs/2026-07-25-diagram-blank-262-design.md`:

1. In **Future work (out of scope)**, mention tracking issue [#117](https://github.com/apache/struts-intellij-plugin/issues/117).
2. If the file still ends with a stray `)` after the last line, delete that character.

Example Future work opener:

```markdown
## Future work (out of scope)

Migrate rendering/editor to `com.intellij.diagram.Provider` (tracked in [#117](https://github.com/apache/struts-intellij-plugin/issues/117)) per `com.intellij.struts2.diagram.model` package-info: ...
```

- [ ] **Step 3: Run full diagram test suite**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"
```

Expected: BUILD SUCCESSFUL — all diagram tests green.

- [ ] **Step 4: Manual smoke (optional but recommended before PR)**

```bash
./gradlew runIde
```

1. Open a file-set `struts.xml` → Diagram shows packages/actions/results (not blank).
2. Edit on Text, switch to Diagram → catch-up refresh.
3. Stay on Diagram, edit XML → debounced refresh.
4. Empty/unavailable case shows placeholder text, not a blank pane.

- [ ] **Step 5: Commit**

```bash
git add CHANGELOG.md docs/superpowers/specs/2026-07-25-diagram-blank-262-design.md
git commit -m "$(cat <<'EOF'
docs: changelog and #117 link for Diagram blank-tab fix

EOF
)"
```

---

## Spec coverage checklist

| Spec requirement | Task |
|---|---|
| Always apply model in `finishOnUiThread` | Task 2 |
| `super.selectNotify` / `super.deselectNotify` | Task 2 |
| DomEvent scheduling still gated by `myDiagramSelected` | Task 2 (leave listener unchanged) |
| EMPTY/UNAVAILABLE fill editor / non-zero size | Task 1 |
| Regression: editor reaches `LOADED` without selection gate blocking first paint | Task 2 |
| Placeholder size test | Task 1 |
| Keep existing select/deselect + Dom filter tests | Task 2 step 4 / Task 3 step 3 |
| CHANGELOG | Task 3 |
| No Diagrams API migration | Global constraint; Future work → #117 in Task 3 |

## Manual verification (PR description)

Copy the four `runIde` checks from Task 3 Step 4 into the PR test plan.
)
