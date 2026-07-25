# Migrate Struts Diagram to Diagrams API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the custom Swing Diagram editor tab with an IntelliJ **Show Diagram** provider that consumes the existing toolkit-neutral Struts config snapshot.

**Architecture:** Keep `StrutsConfigDiagramModel` + `StrutsDiagramPresentation`. Add a `diagram.provider` package implementing `BaseDiagramProvider` / `DiagramDataModel` that maps snapshot nodes/edges into Diagrams API types, registers `com.intellij.diagram.Provider`, hard-depends on `com.intellij.diagram`, and removes `diagram.ui` + `diagram.fileEditor`.

**Tech Stack:** IntelliJ IDEA Ultimate 2026.2 (262), `com.intellij.diagram` (`BaseDiagramProvider`, `DiagramDataModel`, `DiagramNodeBase`, `DiagramEdgeBase`), DomEventListener, JUnit 4 light tests (`BasicLightHighlightingTestCase`).

**Spec:** `docs/superpowers/specs/2026-07-25-diagrams-api-migration-design.md`

## Global Constraints

- Target platform remains IntelliJ IDEA **2026.2** / build **262** only (`pluginSinceBuild=262`, `pluginUntilBuild=262.*`).
- Hard dependency on plugin id **`com.intellij.diagram`** (bundled UML/Diagrams).
- Entry point is **Show Diagram only** — remove the Diagram `PerspectiveFileEditor` tab.
- Show Diagram accepts any Struts 2 config (`StrutsManager.isStruts2ConfigFile`), not only file-set members.
- Do **not** change `StrutsConfigDiagramModel` build semantics or `StrutsDiagramPresentation` tooltip/navigation logic.
- Do **not** implement #96–#100 in this plan.
- Dom live refresh: while the data model is alive, same-file DomEvents debounce **300 ms**, then `refreshDataModel()`.
- Tests gate: `./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"`.
- Learn API details from local IU SDK jars under Gradle caches (`plugins/uml/lib/uml-support.jar`) and Spring Integration’s `SpringIntegrationDiagramProvider` pattern when signatures drift.

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `build.gradle.kts` | Modify | `bundledPlugin("com.intellij.diagram")` (+ test bundled if needed) |
| `src/main/resources/META-INF/plugin.xml` | Modify | Hard `<plugin id="com.intellij.diagram"/>`; register `diagram.Provider`; remove `fileEditorProvider` |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramItem.java` | Create | Identifying element `T` for Diagrams API (file root + snapshot node) |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramDomRefresh.java` | Create | Pure DomEvent same-file helpers (migrated from `Struts2DiagramFileEditor`) |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramElementManager.java` | Create | `findInDataContext` / `isAcceptableAsNode` / titles / tooltips |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramVfsResolver.java` | Create | Persist/resolve root `XmlFile` by URL |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramApiNode.java` | Create | `DiagramNodeBase` wrapping `StrutsDiagramNode` |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramApiEdge.java` | Create | `DiagramEdgeBase` wrapping snapshot edges |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramDataModel.java` | Create | Snapshot → nodes/edges; DomEvent debounce refresh |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramProvider.java` | Create | `BaseDiagramProvider` + EP implementation |
| `src/main/java/com/intellij/struts2/diagram/model/package-info.java` | Modify | Document Show Diagram host; drop Swing references |
| `src/main/java/com/intellij/struts2/diagram/ui/*` | Delete | Custom Swing renderer |
| `src/main/java/com/intellij/struts2/diagram/fileEditor/*` | Delete | `PerspectiveFileEditor` host |
| `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java` | Create | Accept / reject / EP presence (replaces editor provider tests) |
| `src/test/java/com/intellij/struts2/diagram/StrutsDiagramDomRefreshTest.java` | Create | Renamed/moved Dom filter tests |
| `src/test/java/com/intellij/struts2/diagram/StrutsDiagramDataModelMappingTest.java` | Create | Snapshot → API node/edge mapping |
| `src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorProviderTest.java` | Delete | Tab host tests |
| `src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorDomFilterTest.java` | Delete | Replaced by Dom refresh test |
| `src/test/java/com/intellij/struts2/diagram/StrutsConfigDiagramModelTest.java` | Keep | Semantic model tests (remove any Swing-only assertions if present) |
| `CHANGELOG.md` | Modify | Unreleased Changed/Removed for Show Diagram migration |

No new public plugin extension points.

---

### Task 1: Hard-depend on `com.intellij.diagram`

**Files:**
- Modify: `build.gradle.kts`
- Modify: `src/main/resources/META-INF/plugin.xml`
- Test: compile/plugin classpath (no unit test yet)

**Interfaces:**
- Consumes: IntelliJ Platform Gradle `bundledPlugin`
- Produces: compile-time access to `com.intellij.diagram.*`; runtime hard dependency on plugin id `com.intellij.diagram`

- [ ] **Step 1: Add Gradle bundled plugin**

In `build.gradle.kts` inside `dependencies { intellijPlatform { ... } }`, after the existing bundled plugins (near `com.intellij.java-i18n`), add:

```kotlin
bundledPlugin("com.intellij.diagram")
```

Also add `"com.intellij.diagram"` to `testBundledPlugins(...)` so light tests can load the EP.

- [ ] **Step 2: Declare hard plugin dependency in `plugin.xml`**

Inside the existing `<dependencies>` block, add:

```xml
<plugin id="com.intellij.diagram"/>
```

Do **not** register `diagram.Provider` yet (next tasks).

- [ ] **Step 3: Verify the dependency resolves**

Run:

```bash
./gradlew compileJava -q
```

Expected: BUILD SUCCESSFUL (no missing `com.intellij.diagram` types yet because no new sources).

- [ ] **Step 4: Commit**

```bash
git add build.gradle.kts src/main/resources/META-INF/plugin.xml
git commit -m "$(cat <<'EOF'
build: hard-depend on IntelliJ Diagrams plugin for #117

EOF
)"
```

---

### Task 2: Dom refresh helpers (pure, testable)

**Files:**
- Create: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramDomRefresh.java`
- Create: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramDomRefreshTest.java`
- Delete (later Task 6, not now): old Dom filter test still references file editor — leave it until migration completes, **or** rewrite now and leave a temporary compile dependency. Prefer: rewrite test now against the new helper; keep old file editor until Task 6.

**Interfaces:**
- Consumes: `DomEvent`, `DomElement`, `DomUtil`, `VirtualFile`
- Produces:
  - `static boolean isEventForMyFile(@NotNull DomEvent event, @NotNull VirtualFile file)`
  - `static boolean isDomElementInFile(@Nullable DomElement element, @NotNull VirtualFile file)`

- [ ] **Step 1: Write failing Dom filter tests**

Create `StrutsDiagramDomRefreshTest.java` (copy logic from `Struts2DiagramFileEditorDomFilterTest`, change call sites to `StrutsDiagramDomRefresh`):

```java
package com.intellij.struts2.diagram;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.diagram.provider.StrutsDiagramDomRefresh;
import com.intellij.struts2.dom.struts.StrutsRoot;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NotNull;

public class StrutsDiagramDomRefreshTest extends BasicLightHighlightingTestCase {

    @Override
    @NotNull
    protected String getTestDataLocation() {
        return "diagram";
    }

    public void testIsDomElementInFileMatchesSameVirtualFile() {
        createStrutsFileSet("struts-local-a.xml", "struts-local-b.xml");
        VirtualFile vfA = myFixture.findFileInTempDir("struts-local-a.xml");
        VirtualFile vfB = myFixture.findFileInTempDir("struts-local-b.xml");
        assertNotNull(vfA);
        assertNotNull(vfB);

        ReadAction.run(() -> {
            XmlFile fileA = (XmlFile) PsiManager.getInstance(getProject()).findFile(vfA);
            assertNotNull(fileA);
            DomFileElement<StrutsRoot> root =
                    DomManager.getDomManager(getProject()).getFileElement(fileA, StrutsRoot.class);
            assertNotNull(root);
            DomElement pkg = root.getRootElement().getPackages().getFirst();
            assertTrue(StrutsDiagramDomRefresh.isDomElementInFile(pkg, vfA));
            assertFalse(StrutsDiagramDomRefresh.isDomElementInFile(pkg, vfB));
        });
    }

    public void testIsDomElementInFileRejectsNullElement() {
        createStrutsFileSet("struts-local-a.xml");
        VirtualFile vfA = myFixture.findFileInTempDir("struts-local-a.xml");
        assertNotNull(vfA);
        assertFalse(StrutsDiagramDomRefresh.isDomElementInFile(null, vfA));
    }
}
```

Use the same Apache license header as sibling test files.

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramDomRefreshTest" 
```

Expected: FAIL (class `StrutsDiagramDomRefresh` not found).

- [ ] **Step 3: Implement helper**

Create `StrutsDiagramDomRefresh.java`:

```java
package com.intellij.struts2.diagram.provider;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.events.DomEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pure helpers for same-file DomEvent filtering used by {@link StrutsDiagramDataModel}.
 */
public final class StrutsDiagramDomRefresh {

    private StrutsDiagramDomRefresh() {}

    public static boolean isEventForMyFile(@NotNull DomEvent event, @NotNull VirtualFile file) {
        return isDomElementInFile(event.getElement(), file);
    }

    public static boolean isDomElementInFile(@Nullable DomElement element, @NotNull VirtualFile file) {
        if (element == null) {
            return false;
        }
        VirtualFile elementFile = DomUtil.getFile(element).getOriginalFile().getVirtualFile();
        return file.equals(elementFile);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramDomRefreshTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramDomRefresh.java \
        src/test/java/com/intellij/struts2/diagram/StrutsDiagramDomRefreshTest.java
git commit -m "$(cat <<'EOF'
feat(diagram): extract DomEvent same-file helpers for Diagrams host

EOF
)"
```

---

### Task 3: Identifying item + Provider accept path

**Files:**
- Create: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramItem.java`
- Create: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramElementManager.java`
- Create: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramVfsResolver.java`
- Create: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramProvider.java` (skeleton; data model stub OK temporarily)
- Create: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramDataModel.java` (minimal stub compiling)
- Modify: `src/main/resources/META-INF/plugin.xml` — register provider
- Create: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java`

**Interfaces:**
- Consumes: `StrutsManager.isStruts2ConfigFile(XmlFile)`, `BaseDiagramProvider`, `AbstractDiagramElementManager`
- Produces:
  - `StrutsDiagramItem.forFile(XmlFile)` / `forNode(XmlFile, StrutsDiagramNode)` / `getXmlFile()` / `getSnapshotNode()`
  - Provider id constant `StrutsDiagramProvider.ID = "ApacheStrutsConfig"`
  - `StrutsDiagramElementManager.findInDataContext` → root item when PSI file is Struts config
  - `canBeBuiltFrom(Object)` / accept semantics: Struts config **without** requiring file-set membership

- [ ] **Step 1: Write failing accept tests**

Create `StrutsDiagramProviderTest.java`:

```java
package com.intellij.struts2.diagram;

import com.intellij.diagram.DiagramProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.diagram.provider.StrutsDiagramItem;
import com.intellij.struts2.diagram.provider.StrutsDiagramProvider;
import org.jetbrains.annotations.NotNull;

public class StrutsDiagramProviderTest extends BasicLightHighlightingTestCase {

    @Override
    @NotNull
    protected String getTestDataLocation() {
        return "diagram";
    }

    public void testProviderExtensionIsRegistered() {
        DiagramProvider<?> provider = DiagramProvider.findByID(StrutsDiagramProvider.ID);
        assertNotNull("Struts DiagramProvider must be registered via com.intellij.diagram.Provider",
                provider);
        assertInstanceOf(provider, StrutsDiagramProvider.class);
    }

    public void testAcceptsStrutsConfigWithoutFileSet() {
        VirtualFile file = myFixture.copyFileToProject("struts-diagram.xml");
        XmlFile xml = (XmlFile) PsiManager.getInstance(getProject()).findFile(file);
        assertNotNull(xml);
        StrutsDiagramProvider provider = (StrutsDiagramProvider) DiagramProvider.findByID(StrutsDiagramProvider.ID);
        assertNotNull(provider);
        DataContext ctx = SimpleDataContext.builder()
                .add(CommonDataKeys.PSI_FILE, xml)
                .add(CommonDataKeys.PROJECT, getProject())
                .build();
        StrutsDiagramItem item = provider.getElementManager().findInDataContext(ctx);
        assertNotNull("Show Diagram must accept Struts config outside a file set", item);
        assertEquals(xml, item.getXmlFile());
        assertNull(item.getSnapshotNode());
    }

    public void testRejectsPlainXml() {
        XmlFile xml = (XmlFile) myFixture.configureByText("plain.xml", "<root/>");
        StrutsDiagramProvider provider = (StrutsDiagramProvider) DiagramProvider.findByID(StrutsDiagramProvider.ID);
        assertNotNull(provider);
        DataContext ctx = SimpleDataContext.builder()
                .add(CommonDataKeys.PSI_FILE, xml)
                .add(CommonDataKeys.PROJECT, getProject())
                .build();
        assertNull(provider.getElementManager().findInDataContext(ctx));
        assertFalse(provider.getElementManager().canBeBuiltFrom(xml));
    }
}
```

If `SimpleDataContext.builder()` API differs on 262, use the project’s existing DataContext construction pattern or `MapDataContext` — resolve against the IU SDK; keep the same assertions.

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramProviderTest"
```

Expected: FAIL (provider not registered / classes missing).

- [ ] **Step 3: Implement item + managers + stub provider/data model**

`StrutsDiagramItem.java`:

```java
package com.intellij.struts2.diagram.provider;

import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Identifying element for Struts Diagrams API nodes.
 * Root items (snapshotNode == null) seed Show Diagram from an XmlFile.
 */
public final class StrutsDiagramItem {

    private final @NotNull SmartPsiElementPointer<XmlFile> filePointer;
    private final @Nullable StrutsDiagramNode snapshotNode;

    private StrutsDiagramItem(@NotNull SmartPsiElementPointer<XmlFile> filePointer,
                              @Nullable StrutsDiagramNode snapshotNode) {
        this.filePointer = filePointer;
        this.snapshotNode = snapshotNode;
    }

    public static @NotNull StrutsDiagramItem forFile(@NotNull XmlFile file) {
        return new StrutsDiagramItem(
                SmartPointerManager.getInstance(file.getProject()).createSmartPsiElementPointer(file),
                null);
    }

    public static @NotNull StrutsDiagramItem forNode(@NotNull XmlFile file, @NotNull StrutsDiagramNode node) {
        return new StrutsDiagramItem(
                SmartPointerManager.getInstance(file.getProject()).createSmartPsiElementPointer(file),
                node);
    }

    public @Nullable XmlFile getXmlFile() {
        return filePointer.getElement();
    }

    public @Nullable StrutsDiagramNode getSnapshotNode() {
        return snapshotNode;
    }

    public boolean isRoot() {
        return snapshotNode == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StrutsDiagramItem that)) return false;
        if (snapshotNode == null || that.snapshotNode == null) {
            return snapshotNode == that.snapshotNode
                    && Objects.equals(fileUrl(), that.fileUrl());
        }
        return snapshotNode.equals(that.snapshotNode);
    }

    @Override
    public int hashCode() {
        return snapshotNode != null ? snapshotNode.hashCode() : Objects.hash(fileUrl());
    }

    private @Nullable String fileUrl() {
        PsiFile file = filePointer.getElement();
        return file != null && file.getVirtualFile() != null ? file.getVirtualFile().getUrl() : null;
    }
}
```

`StrutsDiagramVfsResolver.java`:

```java
package com.intellij.struts2.diagram.provider;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.diagram.DiagramVfsResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StrutsDiagramVfsResolver implements DiagramVfsResolver<StrutsDiagramItem> {

    @Override
    public @Nullable String getQualifiedName(StrutsDiagramItem item) {
        if (item == null) return null;
        XmlFile file = item.getXmlFile();
        if (file == null || file.getVirtualFile() == null) return null;
        if (item.getSnapshotNode() != null) {
            return file.getVirtualFile().getUrl() + "#" + item.getSnapshotNode().getId();
        }
        return file.getVirtualFile().getUrl();
    }

    @Override
    public @Nullable StrutsDiagramItem resolveElementByFQN(@NotNull String fqn, @NotNull Project project) {
        String url = fqn.contains("#") ? fqn.substring(0, fqn.indexOf('#')) : fqn;
        VirtualFile vf = VirtualFileManager.getInstance().findFileByUrl(url);
        if (vf == null) return null;
        PsiFile psi = PsiManager.getInstance(project).findFile(vf);
        if (!(psi instanceof XmlFile xmlFile)) return null;
        return StrutsDiagramItem.forFile(xmlFile);
    }
}
```

`StrutsDiagramElementManager.java` (extend `AbstractDiagramElementManager`):

```java
package com.intellij.struts2.diagram.provider;

import com.intellij.diagram.AbstractDiagramElementManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.struts2.dom.struts.model.StrutsManager;
import com.intellij.ui.SimpleColoredText;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class StrutsDiagramElementManager extends AbstractDiagramElementManager<StrutsDiagramItem> {

    @Override
    public @Nullable StrutsDiagramItem findInDataContext(@NotNull DataContext context) {
        PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(context);
        if (!(psiFile instanceof XmlFile xmlFile)) {
            return null;
        }
        if (!StrutsManager.getInstance(xmlFile.getProject()).isStruts2ConfigFile(xmlFile)) {
            return null;
        }
        return StrutsDiagramItem.forFile(xmlFile);
    }

    @Override
    public boolean canBeBuiltFrom(Object element) {
        if (element instanceof StrutsDiagramItem item) {
            XmlFile file = item.getXmlFile();
            return file != null
                    && StrutsManager.getInstance(file.getProject()).isStruts2ConfigFile(file);
        }
        if (element instanceof XmlFile xmlFile) {
            return StrutsManager.getInstance(xmlFile.getProject()).isStruts2ConfigFile(xmlFile);
        }
        return false;
    }

    @Override
    public boolean isAcceptableAsNode(Object element) {
        return element instanceof StrutsDiagramItem item && !item.isRoot();
    }

    @Override
    public @Nullable @Nls String getElementTitle(StrutsDiagramItem item) {
        if (item == null) return null;
        StrutsDiagramNode node = item.getSnapshotNode();
        if (node != null) return node.getName();
        XmlFile file = item.getXmlFile();
        return file != null ? file.getName() : null;
    }

    @Override
    public @Nullable String getNodeTooltip(StrutsDiagramItem item) {
        if (item == null || item.getSnapshotNode() == null) return null;
        return item.getSnapshotNode().getTooltipHtml();
    }

    @Override
    public Object[] getNodeItems(StrutsDiagramItem element) {
        return EMPTY_ARRAY;
    }

    @Override
    public boolean canCollapse(StrutsDiagramItem element) {
        return false;
    }

    @Override
    public boolean isContainerFor(StrutsDiagramItem parent, StrutsDiagramItem child) {
        return false;
    }
}
```

Remove unused imports (`SimpleColoredText`, `SimpleTextAttributes`, `Icon`) if the compiler flags them. After adding the class, compile once and fix any remaining 262 abstract-method signatures / `@NotNull` annotations; keep the accept/tooltip/title behavior above.

Minimal compiling `StrutsDiagramDataModel` stub (full implementation in Task 4):

```java
package com.intellij.struts2.diagram.provider;

import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public final class StrutsDiagramDataModel extends DiagramDataModel<StrutsDiagramItem> {

    public StrutsDiagramDataModel(@NotNull Project project,
                                  @NotNull StrutsDiagramProvider provider,
                                  @Nullable StrutsDiagramItem seed) {
        super(project, provider);
        if (seed != null) {
            setOriginalElement(seed);
        }
    }

    @Override
    public @NotNull ModificationTracker getModificationTracker() {
        return PsiManager.getInstance(getProject()).getModificationTracker();
    }

    @Override
    public @NotNull Collection<? extends DiagramNode<StrutsDiagramItem>> getNodes() {
        return Collections.emptyList();
    }

    @Override
    public @NotNull Collection<? extends DiagramEdge<StrutsDiagramItem>> getEdges() {
        return Collections.emptyList();
    }

    @Override
    public @NotNull String getNodeName(@NotNull DiagramNode<StrutsDiagramItem> node) {
        StrutsDiagramItem item = node.getIdentifyingElement();
        String title = getProvider().getElementManager().getElementTitle(item);
        return title != null ? title : "";
    }

    @Override
    public @Nullable DiagramNode<StrutsDiagramItem> addElement(@Nullable StrutsDiagramItem element) {
        return null;
    }

    @Override
    public void refreshDataModel() {
        // Task 4
    }
}
```

`StrutsDiagramProvider.java`:

```java
package com.intellij.struts2.diagram.provider;

import com.intellij.diagram.BaseDiagramProvider;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramElementManager;
import com.intellij.diagram.DiagramPresentationModel;
import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.struts2.Struts2Icons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class StrutsDiagramProvider extends BaseDiagramProvider<StrutsDiagramItem> {

    public static final String ID = "ApacheStrutsConfig";

    private final DiagramElementManager<StrutsDiagramItem> elementManager = new StrutsDiagramElementManager();
    private final DiagramVfsResolver<StrutsDiagramItem> vfsResolver = new StrutsDiagramVfsResolver();

    @Override
    public @NotNull String getID() {
        return ID;
    }

    @Override
    public @NotNull @Nls String getPresentableName() {
        return "Struts Configuration";
    }

    @Override
    public @Nullable Icon getActionIcon(boolean isPopup) {
        return Struts2Icons.Action;
    }

    @Override
    public @NotNull DiagramElementManager<StrutsDiagramItem> getElementManager() {
        return elementManager;
    }

    @Override
    public @NotNull DiagramVfsResolver<StrutsDiagramItem> getVfsResolver() {
        return vfsResolver;
    }

    @Override
    public @NotNull DiagramDataModel<StrutsDiagramItem> createDataModel(@NotNull Project project,
                                                                       @Nullable StrutsDiagramItem element,
                                                                       @Nullable VirtualFile file,
                                                                       @NotNull DiagramPresentationModel presentationModel) {
        return new StrutsDiagramDataModel(project, this, element);
    }
}
```

Register in `plugin.xml` under `<extensions defaultExtensionNs="com.intellij">`:

```xml
<diagram.Provider implementation="com.intellij.struts2.diagram.provider.StrutsDiagramProvider"/>
```

Keep the old `fileEditorProvider` until Task 6 so the IDE still has a diagram tab during incremental work.

- [ ] **Step 4: Compile and fix API mismatches**

```bash
./gradlew compileJava compileTestJava
```

Fix any 262 signature mismatches (imports, `@NotNull`, removed abstract methods). Re-run until green compile.

- [ ] **Step 5: Run provider tests**

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramProviderTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/intellij/struts2/diagram/provider \
        src/main/resources/META-INF/plugin.xml \
        src/test/java/com/intellij/struts2/diagram/StrutsDiagramProviderTest.java
git commit -m "$(cat <<'EOF'
feat(diagram): register Show Diagram provider for Struts configs

EOF
)"
```

---

### Task 4: Snapshot → Diagrams nodes/edges mapping

**Files:**
- Create: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramApiNode.java`
- Create: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramApiEdge.java`
- Modify: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramDataModel.java` (full refresh)
- Create: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramDataModelMappingTest.java`

**Interfaces:**
- Consumes: `StrutsConfigDiagramModel.build(XmlFile)`, `StrutsDiagramNode`, `StrutsDiagramEdge`, `DiagramRelationships.DEPENDENCY`, `DiagramRelationshipInfoAdapter` for labeled edges
- Produces: After `refreshDataModel()`, `getNodes()`/`getEdges()` mirror the snapshot (same counts, ids, edge endpoints/labels)

- [ ] **Step 1: Write failing mapping test**

```java
package com.intellij.struts2.diagram;

import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramProvider;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.diagram.model.StrutsConfigDiagramModel;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.struts2.diagram.provider.StrutsDiagramDataModel;
import com.intellij.struts2.diagram.provider.StrutsDiagramItem;
import com.intellij.struts2.diagram.provider.StrutsDiagramProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

public class StrutsDiagramDataModelMappingTest extends BasicLightHighlightingTestCase {

    @Override
    @NotNull
    protected String getTestDataLocation() {
        return "diagram";
    }

    public void testRefreshMapsSnapshotNodesAndEdges() {
        createStrutsFileSet("struts-diagram.xml");
        VirtualFile vf = myFixture.findFileInTempDir("struts-diagram.xml");
        assertNotNull(vf);
        XmlFile xml = (XmlFile) PsiManager.getInstance(getProject()).findFile(vf);
        assertNotNull(xml);

        StrutsConfigDiagramModel snapshot = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build(xml)).executeSynchronously();
        assertNotNull(snapshot);
        assertFalse(snapshot.getNodes().isEmpty());

        StrutsDiagramProvider provider =
                (StrutsDiagramProvider) DiagramProvider.findByID(StrutsDiagramProvider.ID);
        assertNotNull(provider);
        StrutsDiagramDataModel dataModel =
                new StrutsDiagramDataModel(getProject(), provider, StrutsDiagramItem.forFile(xml));
        try {
            ReadAction.run(dataModel::refreshDataModel);

            assertEquals(snapshot.getNodes().size(), dataModel.getNodes().size());
            assertEquals(snapshot.getEdges().size(), dataModel.getEdges().size());

            Set<String> snapshotIds = snapshot.getNodes().stream()
                    .map(StrutsDiagramNode::getId).collect(Collectors.toSet());
            Set<String> apiIds = dataModel.getNodes().stream()
                    .map(DiagramNode::getIdentifyingElement)
                    .map(StrutsDiagramItem::getSnapshotNode)
                    .map(StrutsDiagramNode::getId)
                    .collect(Collectors.toSet());
            assertEquals(snapshotIds, apiIds);

            for (DiagramEdge<StrutsDiagramItem> edge : dataModel.getEdges()) {
                assertNotNull(edge.getSource().getIdentifyingElement().getSnapshotNode());
                assertNotNull(edge.getTarget().getIdentifyingElement().getSnapshotNode());
            }
        } finally {
            dataModel.dispose();
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramDataModelMappingTest"
```

Expected: FAIL (empty nodes from stub refresh).

- [ ] **Step 3: Implement API node/edge + full `refreshDataModel`**

`StrutsDiagramApiNode.java`:

```java
package com.intellij.struts2.diagram.provider;

import com.intellij.diagram.DiagramNodeBase;
import com.intellij.diagram.DiagramProvider;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.struts2.diagram.presentation.StrutsDiagramPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class StrutsDiagramApiNode extends DiagramNodeBase<StrutsDiagramItem> {

    private final @NotNull StrutsDiagramItem item;

    public StrutsDiagramApiNode(@NotNull DiagramProvider<StrutsDiagramItem> provider,
                                @NotNull StrutsDiagramItem item) {
        super(provider);
        this.item = item;
    }

    @Override
    public @NotNull StrutsDiagramItem getIdentifyingElement() {
        return item;
    }

    @Override
    public @Nullable String getTooltip() {
        StrutsDiagramNode node = item.getSnapshotNode();
        return node != null ? node.getTooltipHtml() : null;
    }

    @Override
    public @Nullable Icon getIcon() {
        StrutsDiagramNode node = item.getSnapshotNode();
        return node != null ? node.getIcon() : null;
    }

    @Override
    public void navigate(boolean requestFocus) {
        StrutsDiagramNode node = item.getSnapshotNode();
        if (node != null) {
            StrutsDiagramPresentation.navigateToElement(node);
        }
    }

    @Override
    public boolean canNavigate() {
        StrutsDiagramNode node = item.getSnapshotNode();
        return node != null && node.getNavigationPointer() != null;
    }
}
```

`StrutsDiagramApiEdge.java`:

```java
package com.intellij.struts2.diagram.provider;

import com.intellij.diagram.DiagramEdgeBase;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramRelationshipInfo;
import com.intellij.diagram.DiagramRelationshipInfoAdapter;
import com.intellij.diagram.DiagramRelationships;
import com.intellij.diagram.presentation.DiagramLineType;
import com.intellij.struts2.diagram.model.StrutsDiagramEdge;
import org.jetbrains.annotations.NotNull;

public final class StrutsDiagramApiEdge extends DiagramEdgeBase<StrutsDiagramItem> {

    public StrutsDiagramApiEdge(@NotNull DiagramNode<StrutsDiagramItem> source,
                                @NotNull DiagramNode<StrutsDiagramItem> target,
                                @NotNull StrutsDiagramEdge snapshotEdge) {
        super(source, target, relationshipFor(snapshotEdge));
    }

    private static @NotNull DiagramRelationshipInfo relationshipFor(@NotNull StrutsDiagramEdge edge) {
        String label = edge.getLabel();
        if (label == null || label.isEmpty()) {
            return DiagramRelationships.DEPENDENCY;
        }
        return new DiagramRelationshipInfoAdapter(label, DiagramLineType.SOLID, label);
    }
}
```

If `DiagramRelationshipInfoAdapter` / `DiagramLineType` constructors differ on 262, adjust to the closest public constructor that accepts a center/name label; unlabeled edges must still use `DiagramRelationships.DEPENDENCY`.

Replace `refreshDataModel` / collections in `StrutsDiagramDataModel`:

```java
private final List<DiagramNode<StrutsDiagramItem>> nodes = new ArrayList<>();
private final List<DiagramEdge<StrutsDiagramItem>> edges = new ArrayList<>();

@Override
public void refreshDataModel() {
    nodes.clear();
    edges.clear();
    StrutsDiagramItem seed = getOriginalElement();
    XmlFile xmlFile = seed != null ? seed.getXmlFile() : null;
    if (xmlFile == null) {
        return;
    }
    StrutsConfigDiagramModel snapshot = StrutsConfigDiagramModel.build(xmlFile);
    if (snapshot == null) {
        return;
    }
    Map<StrutsDiagramNode, DiagramNode<StrutsDiagramItem>> map = new IdentityHashMap<>();
    for (StrutsDiagramNode snapshotNode : snapshot.getNodes()) {
        StrutsDiagramItem item = StrutsDiagramItem.forNode(xmlFile, snapshotNode);
        StrutsDiagramApiNode apiNode = new StrutsDiagramApiNode(getProvider(), item);
        nodes.add(apiNode);
        map.put(snapshotNode, apiNode);
    }
    for (StrutsDiagramEdge snapshotEdge : snapshot.getEdges()) {
        DiagramNode<StrutsDiagramItem> source = map.get(snapshotEdge.getSource());
        DiagramNode<StrutsDiagramItem> target = map.get(snapshotEdge.getTarget());
        if (source != null && target != null) {
            edges.add(new StrutsDiagramApiEdge(source, target, snapshotEdge));
        }
    }
}
```

Return `nodes` / `edges` from `getNodes()` / `getEdges()`. Document that `refreshDataModel()` must run under a read action (callers: initial Show Diagram path and Dom refresh bridge).

- [ ] **Step 4: Run mapping test**

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramDataModelMappingTest"
```

Expected: PASS. Also run model tests:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsConfigDiagramModelTest"
```

Expected: PASS (unchanged semantics).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/intellij/struts2/diagram/provider \
        src/test/java/com/intellij/struts2/diagram/StrutsDiagramDataModelMappingTest.java
git commit -m "$(cat <<'EOF'
feat(diagram): map Struts snapshot model into Diagrams API nodes

EOF
)"
```

---

### Task 5: DomEvent live refresh while data model is alive

**Files:**
- Modify: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramDataModel.java`
- Test: extend `StrutsDiagramDomRefreshTest` or mapping test with a small unit assertion that the listener uses `StrutsDiagramDomRefresh` (full DomEvent UI pump optional)

**Interfaces:**
- Consumes: `DomManager.addDomEventListener`, `Alarm` 300 ms, `StrutsDiagramDomRefresh`, `ReadAction.nonBlocking`
- Produces: While model not disposed, same-file DomEvents debounce then rebuild snapshot on background read + apply on EDT via Diagrams refresh (`refreshDataModel` + builder update if required by 262)

- [ ] **Step 1: Implement listener in data model constructor**

Fields:

```java
private static final int DOM_UPDATE_DELAY_MS = 300;
private final Alarm updateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
private final VirtualFile virtualFile; // from seed XmlFile; may be null
```

In constructor, after `setOriginalElement`:

```java
XmlFile xml = seed != null ? seed.getXmlFile() : null;
this.virtualFile = xml != null ? xml.getVirtualFile() : null;
if (virtualFile != null) {
    DomManager.getDomManager(project).addDomEventListener(event -> {
        if (!StrutsDiagramDomRefresh.isEventForMyFile(event, virtualFile)) {
            return;
        }
        queueDebouncedRefresh();
    }, this);
}
```

```java
private void queueDebouncedRefresh() {
    updateAlarm.cancelAllRequests();
    updateAlarm.addRequest(this::scheduleRefresh, DOM_UPDATE_DELAY_MS);
}

private void scheduleRefresh() {
    ReadAction.nonBlocking(() -> {
                refreshDataModel();
                return null;
            })
            .expireWith(this)
            .coalesceBy(this)
            .finishOnUiThread(ModalityState.defaultModalityState(), ignored -> {
                // If 262 requires an explicit diagram builder update after mutating
                // getNodes()/getEdges(), call DiagramDataModel.refreshDataModelInSmartMode(getBuilder())
                // or getBuilder().queryUpdate()... per SDK — only when getBuilder() != null.
            })
            .submit(AppExecutorUtil.getAppExecutorService());
}
```

**Threading note:** Prefer building the snapshot inside `ReadAction.nonBlocking` and assigning node lists on the EDT. If `refreshDataModel()` both reads PSI and mutates lists, split into `buildSnapshot()` (read) + `applySnapshot()` (EDT) to match the old editor’s pattern and avoid PSI from EDT.

Recommended split:

```java
private @Nullable StrutsConfigDiagramModel buildSnapshot() { /* StrutsConfigDiagramModel.build */ }
private void applySnapshot(@Nullable StrutsConfigDiagramModel snapshot) { /* clear/fill nodes+edges */ }

public void refreshDataModel() {
    // Called under read action by platform OR by scheduleRefresh apply path:
    applySnapshot(buildSnapshot());
}
```

Use the split that compiles cleanly and keeps PSI off the EDT for Dom-triggered updates.

- [ ] **Step 2: Smoke-test Dom helpers still pass; run diagram suite**

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"
```

Expected: all current diagram tests PASS (old editor tests still present until Task 6).

- [ ] **Step 3: Manual checkpoint (optional but recommended)**

```bash
./gradlew runIde
```

Open a Struts config → Show Diagram → edit XML → diagram updates after ~300 ms. If refresh does not repaint, fix the builder update call from Step 1 before committing.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramDataModel.java
git commit -m "$(cat <<'EOF'
feat(diagram): refresh Show Diagram model on same-file DomEvents

EOF
)"
```

---

### Task 6: Remove Swing Diagram tab host

**Files:**
- Delete: `src/main/java/com/intellij/struts2/diagram/ui/Struts2DiagramComponent.java`
- Delete: `src/main/java/com/intellij/struts2/diagram/fileEditor/Struts2DiagramFileEditor.java`
- Delete: `src/main/java/com/intellij/struts2/diagram/fileEditor/Struts2DiagramFileEditorProvider.java`
- Delete: `src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorProviderTest.java`
- Delete: `src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorDomFilterTest.java`
- Modify: `src/main/resources/META-INF/plugin.xml` — remove `fileEditorProvider` line
- Modify: `src/test/java/com/intellij/struts2/diagram/StrutsConfigDiagramModelTest.java` — remove any imports/assertions against `Struts2DiagramComponent` if present after #118

**Interfaces:**
- Consumes: provider path from Tasks 3–5
- Produces: no Diagram editor tab; Show Diagram is the only host

- [ ] **Step 1: Remove `fileEditorProvider` registration**

Delete this line from `plugin.xml`:

```xml
<fileEditorProvider implementation="com.intellij.struts2.diagram.fileEditor.Struts2DiagramFileEditorProvider"/>
```

- [ ] **Step 2: Delete UI/fileEditor sources and obsolete tests**

Delete the five files listed above. Grep for leftover references:

```bash
rg -n "Struts2DiagramComponent|Struts2DiagramFileEditor|diagram\\.ui|diagram\\.fileEditor" src || true
```

Expected: no matches (except historical docs/specs).

- [ ] **Step 3: Fix `StrutsConfigDiagramModelTest` if it still touches Swing**

Remove placeholder-size / component tests that import `Struts2DiagramComponent`. Keep pure model assertions.

- [ ] **Step 4: Run full diagram test suite**

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"
```

Expected: PASS. No references to deleted classes.

- [ ] **Step 5: Commit**

```bash
git add -A src/main/java/com/intellij/struts2/diagram \
        src/test/java/com/intellij/struts2/diagram \
        src/main/resources/META-INF/plugin.xml
git commit -m "$(cat <<'EOF'
refactor(diagram): remove Swing Diagram tab in favor of Show Diagram

EOF
)"
```

---

### Task 7: Docs, package-info, changelog, final gate

**Files:**
- Modify: `src/main/java/com/intellij/struts2/diagram/model/package-info.java`
- Modify: `CHANGELOG.md`
- Modify (optional clarity): `docs/superpowers/specs/2026-07-25-diagrams-api-migration-design.md` status line only if needed

**Interfaces:**
- Produces: accurate migration boundary docs + user-facing changelog

- [ ] **Step 1: Update `package-info.java`**

Replace the “future migration” wording so it states the Diagrams API host lives in `com.intellij.struts2.diagram.provider`, model/presentation remain toolkit-neutral, and the Swing/`PerspectiveFileEditor` host has been removed.

- [ ] **Step 2: Update `CHANGELOG.md` under `[Unreleased]`**

Add under **Changed** (and **Removed** as appropriate):

```markdown
### Changed

- Migrate struts.xml diagram visualization from the custom Diagram editor tab to the IntelliJ **Show Diagram** action (`com.intellij.diagram`) ([#117](https://github.com/apache/struts-intellij-plugin/issues/117))
- Dependencies - hard-depend on `com.intellij.diagram` (Ultimate Diagrams)

### Removed

- Remove custom Swing Diagram editor tab (`Struts2DiagramFileEditor` / `Struts2DiagramComponent`); use **Show Diagram** on a Struts 2 config file instead
```

Adjust section placement to match existing Unreleased structure.

- [ ] **Step 3: Final automated gate**

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"
```

Expected: BUILD SUCCESSFUL, all diagram tests green.

- [ ] **Step 4: Manual verification checklist** (`./gradlew runIde`)

1. Open a Struts 2 config **not** in a file set → Show Diagram available → packages/actions/results render  
2. Chain/redirect action→action edges still appear (`struts-redirect-*.xml` fixtures)  
3. Edit XML while diagram open → debounced refresh  
4. Double-click node → navigates to XML  
5. Hover → tooltip HTML  
6. Confirm **Diagram** editor tab is gone  
7. Zoom/pan via platform chrome works at smoke level  

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/intellij/struts2/diagram/model/package-info.java CHANGELOG.md
git commit -m "$(cat <<'EOF'
docs: changelog and package-info for Diagrams API migration

EOF
)"
```

---

## Spec coverage (self-review)

| Spec requirement | Task |
|---|---|
| Show Diagram entry (no tab) | 3, 6 |
| Hard `com.intellij.diagram` dependency | 1 |
| Any Struts config (not only file set) | 3 |
| Keep model + presentation | 4 (consume only) |
| Tooltips + navigate | 4 (`StrutsDiagramApiNode`) |
| Dom debounce refresh while model alive | 5 |
| Remove Swing / PerspectiveFileEditor | 6 |
| Changelog | 7 |
| Automated diagram tests + manual checklist | 3–7 |
| Out of scope #96–#100 | Not scheduled |

## Placeholder / consistency notes

- Exact 262 method signatures for `AbstractDiagramElementManager` / relationship adapters may need compile-driven tweaks; behavior in this plan is normative.
- Provider id is fixed: `ApacheStrutsConfig`.
- Dom helpers live only in `StrutsDiagramDomRefresh` after Task 6.
