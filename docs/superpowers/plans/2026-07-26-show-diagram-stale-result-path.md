# Show Diagram Stale Result Path Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix Show Diagram live Dom refresh so edited/copy-pasted result paths update node labels without resetting soft layout preference from #122.

**Architecture:** Make snapshot/API node identity use `SmartPsiElementPointer` (`navigationPointer`) instead of `kind@textOffset`. On Dom-triggered live updates, merge fresh presentable data onto retained `StrutsDiagramApiNode` instances by that identity, then call `refreshDataModelInSmartMode` so layout algorithm/positions stay. Initial `refreshDataModel()` remains a full replace.

**Tech Stack:** IntelliJ IDEA Ultimate 2026.2 (262), `com.intellij.diagram` (`DiagramDataModel.refreshDataModelInSmartMode`), IntelliJ `SmartPsiElementPointer`, JUnit 4 light tests (`BasicLightHighlightingTestCase`).

**Spec:** `docs/superpowers/specs/2026-07-26-show-diagram-stale-result-path-design.md`

## Global Constraints

- Target platform remains IntelliJ IDEA **2026.2** / build **262** only (`pluginSinceBuild=262`, `pluginUntilBuild=262.*`).
- Host in scope is **Show Diagram** (`StrutsDiagramDataModel`) only — do **not** modify or remove the Swing Diagram tab (`diagram.fileEditor` / `diagram.ui`).
- Soft layout preference from #122 must remain: keep `refreshDataModelInSmartMode`; do **not** call `GraphSettings.setCurrentLayouter` or change `StrutsDiagramExtras.getCustomLayouter`.
- Identity source of truth is **`SmartPsiElementPointer`** on the XML element (`navigationPointer`), not path text and not `kind@textOffset`.
- Snapshot graph shape semantics (package → action → result, chain/redirect) stay unchanged.
- Tests gate: `./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"`.
- Prefer editing existing files; only add a tiny package-private helper if merge logic needs isolation.

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `src/test/java/com/intellij/struts2/diagram/StrutsConfigDiagramModelTest.java` | Modify | Identity tests: pointer equality survives offset shift; same-path results remain unequal |
| `src/main/java/com/intellij/struts2/diagram/model/StrutsDiagramNode.java` | Modify | `equals`/`hashCode` by `navigationPointer` when present; fallback to `id` |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramApiNode.java` | Modify | Allow updating identifying `StrutsDiagramItem` in place |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramApiEdge.java` | Modify | Retain `StrutsDiagramEdge` for edge remap during merge |
| `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramDataModel.java` | Modify | Live merge-by-identity before smart mode |
| `src/test/java/com/intellij/struts2/diagram/StrutsDiagramDataModelMappingTest.java` | Modify | Live Dom refresh regression: path edit updates title; retained `DiagramNode` instance; copy-paste distinct paths |
| `CHANGELOG.md` | Modify | Unreleased Fixed note for #126 |

No `plugin.xml` / Gradle dependency changes. No layouter / extras changes. Fixture XML can stay as-is (`struts-diagram.xml`, `struts-duplicate-names.xml`); document edits happen in tests.

---

### Task 1: Pointer-based node identity

**Files:**
- Modify: `src/test/java/com/intellij/struts2/diagram/StrutsConfigDiagramModelTest.java`
- Modify: `src/main/java/com/intellij/struts2/diagram/model/StrutsDiagramNode.java`
- Test: `src/test/java/com/intellij/struts2/diagram/StrutsConfigDiagramModelTest.java`

**Interfaces:**
- Consumes: `StrutsConfigDiagramModel.build(XmlFile)`, `StrutsDiagramNode.getNavigationPointer()`, `StrutsDiagramNode.getName()`, `StrutsDiagramNode.getId()`
- Produces: `StrutsDiagramNode.equals`/`hashCode` treat same `navigationPointer` as same node even when text offsets change; different elements remain unequal even when display names match

- [ ] **Step 1: Write the failing identity tests**

In `StrutsConfigDiagramModelTest.java`, in the "Duplicate name and identity tests" section, add:

```java
    public void testResultNodeIdentitySurvivesOffsetShift() {
        createStrutsFileSet("struts-diagram.xml");
        VirtualFile vf = myFixture.findFileInTempDir("struts-diagram.xml");
        assertNotNull(vf);
        XmlFile xml = (XmlFile) PsiManager.getInstance(getProject()).findFile(vf);
        assertNotNull(xml);

        StrutsConfigDiagramModel before = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build(xml)).executeSynchronously();
        assertNotNull(before);
        StrutsDiagramNode resultBefore = before.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                .findFirst()
                .orElseThrow();
        assertNotNull(resultBefore.getNavigationPointer());

        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(xml);
        assertNotNull(document);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            // Insert ahead of the only result so its textOffset changes.
            String updated = document.getText().replace(
                    "<action name=\"testAction\"",
                    "<!-- pad -->\n    <action name=\"testAction\"");
            document.setText(updated);
            PsiDocumentManager.getInstance(getProject()).commitDocument(document);
        });

        StrutsConfigDiagramModel after = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build(xml)).executeSynchronously();
        assertNotNull(after);
        StrutsDiagramNode resultAfter = after.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                .findFirst()
                .orElseThrow();

        assertFalse("Debug ids may still differ after offset shift",
                resultBefore.getId().equals(resultAfter.getId()));
        assertTrue("Pointer-based identity must survive offset shift",
                resultBefore.equals(resultAfter));
        assertEquals(resultBefore.hashCode(), resultAfter.hashCode());
        assertEquals(resultBefore.getName(), resultAfter.getName());
    }

    public void testSamePathResultsRemainUnequalAcrossActions() {
        createStrutsFileSet("struts-duplicate-names.xml");
        VirtualFile vf = myFixture.findFileInTempDir("struts-duplicate-names.xml");
        assertNotNull(vf);
        XmlFile xml = (XmlFile) PsiManager.getInstance(getProject()).findFile(vf);
        assertNotNull(xml);

        // Two default results with different paths already exist; force a shared path on both.
        Document document = PsiDocumentManager.getInstance(getProject()).getDocument(xml);
        assertNotNull(document);
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            String text = document.getText()
                    .replace("/admin/index.jsp", "/shared/index.jsp")
                    .replace("/public/index.jsp", "/shared/index.jsp");
            document.setText(text);
            PsiDocumentManager.getInstance(getProject()).commitDocument(document);
        });

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build(xml)).executeSynchronously();
        assertNotNull(model);
        List<StrutsDiagramNode> shared = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                .filter(n -> n.getName().contains("/shared/index.jsp"))
                .collect(Collectors.toList());
        assertEquals(2, shared.size());
        assertFalse(shared.get(0).equals(shared.get(1)));
        assertFalse(shared.get(0).getId().equals(shared.get(1).getId()));
    }
```

Add imports if missing:

```java
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsConfigDiagramModelTest.testResultNodeIdentitySurvivesOffsetShift" --tests "com.intellij.struts2.diagram.StrutsConfigDiagramModelTest.testSamePathResultsRemainUnequalAcrossActions"
```

Expected: `testResultNodeIdentitySurvivesOffsetShift` FAIL — current `equals` uses offset-based `id`, so after the pad insert the nodes are unequal (or the "ids may differ / equals must hold" pair fails on `assertTrue(...equals...)`). `testSamePathResultsRemainUnequalAcrossActions` may already PASS (different offsets); keep it as a regression guard.

- [ ] **Step 3: Implement pointer-based `equals`/`hashCode`**

In `StrutsDiagramNode.java`, replace `equals`/`hashCode` with:

```java
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StrutsDiagramNode that)) return false;
        if (navigationPointer != null && that.navigationPointer != null) {
            return navigationPointer.equals(that.navigationPointer);
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        if (navigationPointer != null) {
            return navigationPointer.hashCode();
        }
        return id.hashCode();
    }
```

Leave `buildNodeId` / `id` field as-is for debug/`toString` and for the fallback when `navigationPointer` is null. Update the class javadoc sentence that says the stable id uniquely identifies the node so it states pointer-based equality is the identity source of truth when a navigation pointer exists.

`StrutsDiagramItem.equals` already delegates to `snapshotNode.equals` — no change required there once node equality is pointer-based.

- [ ] **Step 4: Run identity tests to verify they pass**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsConfigDiagramModelTest.testResultNodeIdentitySurvivesOffsetShift" --tests "com.intellij.struts2.diagram.StrutsConfigDiagramModelTest.testSamePathResultsRemainUnequalAcrossActions" --tests "com.intellij.struts2.diagram.StrutsConfigDiagramModelTest.testDuplicateActionNamesAcrossPackagesProduceDistinctNodes" --tests "com.intellij.struts2.diagram.StrutsConfigDiagramModelTest.testDuplicateResultPathsProduceDistinctNodes"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add \
  src/main/java/com/intellij/struts2/diagram/model/StrutsDiagramNode.java \
  src/test/java/com/intellij/struts2/diagram/StrutsConfigDiagramModelTest.java
git commit -m "$(cat <<'EOF'
fix(diagram): identify Show Diagram nodes by PSI pointer (#126)

EOF
)"
```

---

### Task 2: Live merge updates presentables on retained API nodes

**Files:**
- Modify: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramApiNode.java`
- Modify: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramApiEdge.java`
- Modify: `src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramDataModel.java`
- Modify: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramDataModelMappingTest.java`
- Test: `src/test/java/com/intellij/struts2/diagram/StrutsDiagramDataModelMappingTest.java`

**Interfaces:**
- Consumes: Task 1 pointer equality; `StrutsDiagramDataModel` Dom debounce refresh; `DiagramProvider.findByID(StrutsDiagramProvider.ID)`
- Produces:
  - `StrutsDiagramApiNode.updateIdentifyingElement(StrutsDiagramItem)`
  - `StrutsDiagramApiEdge.getSnapshotEdge()` (package-private)
  - `StrutsDiagramDataModel.applyLiveUpdate` merges by identity before smart mode
  - Live Dom path edit keeps the same `DiagramNode` instance and updates presentable title

- [ ] **Step 1: Write the failing live-refresh regression tests**

In `StrutsDiagramDataModelMappingTest.java`, add:

```java
    public void testDomPathEditUpdatesResultTitleOnRetainedApiNode() throws InterruptedException {
        createStrutsFileSet("struts-diagram.xml");
        VirtualFile vf = myFixture.findFileInTempDir("struts-diagram.xml");
        assertNotNull(vf);
        XmlFile xml = (XmlFile) PsiManager.getInstance(getProject()).findFile(vf);
        assertNotNull(xml);

        DiagramProvider<?> diagramProvider = DiagramProvider.findByID(StrutsDiagramProvider.ID);
        assertInstanceOf(diagramProvider, StrutsDiagramProvider.class);
        StrutsDiagramDataModel dataModel = new StrutsDiagramDataModel(
                getProject(), (StrutsDiagramProvider) diagramProvider, StrutsDiagramItem.forFile(xml));
        try {
            ReadAction.run(dataModel::refreshDataModel);
            DiagramNode<StrutsDiagramItem> resultNode = dataModel.getNodes().stream()
                    .filter(n -> {
                        StrutsDiagramNode snap = n.getIdentifyingElement().getSnapshotNode();
                        return snap != null && snap.getKind() == StrutsDiagramNode.Kind.RESULT;
                    })
                    .findFirst()
                    .orElseThrow();
            String oldPath = resultNode.getIdentifyingElement().getSnapshotNode().getName();
            assertTrue(oldPath.contains("test.jsp"));

            Document document = PsiDocumentManager.getInstance(getProject()).getDocument(xml);
            assertNotNull(document);
            WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                document.setText(document.getText().replace("/pages/test.jsp", "/pages/delete.jsp"));
                PsiDocumentManager.getInstance(getProject()).commitDocument(document);
            });

            long deadline = System.currentTimeMillis() + 10_000;
            boolean updated = false;
            while (System.currentTimeMillis() < deadline) {
                UIUtil.dispatchAllInvocationEvents();
                StrutsDiagramNode snap = resultNode.getIdentifyingElement().getSnapshotNode();
                if (snap != null && snap.getName().contains("delete.jsp")) {
                    updated = true;
                    break;
                }
                Thread.sleep(50);
            }
            assertTrue("Dom refresh must update retained API node presentable path", updated);

            boolean sameInstance = dataModel.getNodes().stream().anyMatch(n -> n == resultNode);
            assertTrue("Live merge must retain DiagramNode instance for soft layout", sameInstance);

            // Original path must be gone from result titles.
            boolean stale = dataModel.getNodes().stream()
                    .map(DiagramNode::getIdentifyingElement)
                    .map(StrutsDiagramItem::getSnapshotNode)
                    .filter(Objects::nonNull)
                    .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                    .anyMatch(n -> n.getName().contains("test.jsp"));
            assertFalse(stale);
        } finally {
            Disposer.dispose(dataModel);
        }
    }

    public void testCopyPasteResultGetsDistinctPathAfterDomRefresh() throws InterruptedException {
        createStrutsFileSet("struts-diagram.xml");
        VirtualFile vf = myFixture.findFileInTempDir("struts-diagram.xml");
        assertNotNull(vf);
        XmlFile xml = (XmlFile) PsiManager.getInstance(getProject()).findFile(vf);
        assertNotNull(xml);

        DiagramProvider<?> diagramProvider = DiagramProvider.findByID(StrutsDiagramProvider.ID);
        assertInstanceOf(diagramProvider, StrutsDiagramProvider.class);
        StrutsDiagramDataModel dataModel = new StrutsDiagramDataModel(
                getProject(), (StrutsDiagramProvider) diagramProvider, StrutsDiagramItem.forFile(xml));
        try {
            ReadAction.run(dataModel::refreshDataModel);
            int initialResults = (int) dataModel.getNodes().stream()
                    .map(DiagramNode::getIdentifyingElement)
                    .map(StrutsDiagramItem::getSnapshotNode)
                    .filter(Objects::nonNull)
                    .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                    .count();
            assertEquals(1, initialResults);

            Document document = PsiDocumentManager.getInstance(getProject()).getDocument(xml);
            assertNotNull(document);
            WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                String updated = document.getText().replace(
                        "<result>/pages/test.jsp</result>",
                        "<result name=\"success\">/pages/test.jsp</result>\n" +
                                "      <result name=\"delete\">/pages/delete.jsp</result>");
                document.setText(updated);
                PsiDocumentManager.getInstance(getProject()).commitDocument(document);
            });

            long deadline = System.currentTimeMillis() + 10_000;
            Set<String> resultNames = Set.of();
            while (System.currentTimeMillis() < deadline) {
                UIUtil.dispatchAllInvocationEvents();
                resultNames = dataModel.getNodes().stream()
                        .map(DiagramNode::getIdentifyingElement)
                        .map(StrutsDiagramItem::getSnapshotNode)
                        .filter(Objects::nonNull)
                        .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                        .map(StrutsDiagramNode::getName)
                        .collect(Collectors.toSet());
                if (resultNames.size() >= 2) {
                    break;
                }
                Thread.sleep(50);
            }

            assertTrue("Expected success path present, got: " + resultNames,
                    resultNames.stream().anyMatch(n -> n.contains("test.jsp")));
            assertTrue("Expected delete path present, got: " + resultNames,
                    resultNames.stream().anyMatch(n -> n.contains("delete.jsp")));
            assertFalse("Delete must not reuse success path label",
                    resultNames.size() == 1 && resultNames.iterator().next().contains("test.jsp"));

            Set<String> edgeLabels = dataModel.getEdges().stream()
                    .map(StrutsDiagramDataModelMappingTest::apiEdgeLabel)
                    .collect(Collectors.toSet());
            assertTrue(edgeLabels.contains("success"));
            assertTrue(edgeLabels.contains("delete"));
        } finally {
            Disposer.dispose(dataModel);
        }
    }
```

Add imports if missing:

```java
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
```

(`WriteCommandAction`, `Document`, `PsiDocumentManager`, `UIUtil`, `Disposer` should already be present from existing tests.)

- [ ] **Step 2: Run tests to verify the retention assert fails**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramDataModelMappingTest.testDomPathEditUpdatesResultTitleOnRetainedApiNode" --tests "com.intellij.struts2.diagram.StrutsDiagramDataModelMappingTest.testCopyPasteResultGetsDistinctPathAfterDomRefresh"
```

Expected: `testDomPathEditUpdatesResultTitleOnRetainedApiNode` FAIL on `sameInstance` (today `applyApiModel` replaces node objects). Path text may already update when `builder == null`; the retention assert is the intentional red. Copy-paste test may already PASS on path sets; keep it as acceptance coverage for #126.

- [ ] **Step 3: Make `StrutsDiagramApiNode` updatable**

Replace the `item` field handling in `StrutsDiagramApiNode.java` with:

```java
    private @NotNull StrutsDiagramItem item;

    public StrutsDiagramApiNode(@NotNull DiagramProvider<StrutsDiagramItem> provider,
                                @NotNull StrutsDiagramItem item) {
        super(provider);
        this.item = item;
    }

    /**
     * Replaces presentable identifying data while keeping this API node instance for smart refresh.
     */
    void updateIdentifyingElement(@NotNull StrutsDiagramItem newItem) {
        this.item = newItem;
    }

    @Override
    public @NotNull StrutsDiagramItem getIdentifyingElement() {
        return item;
    }
```

Leave `getTooltip` / `getIcon` / `navigate` / `canNavigate` unchanged (they already read through `item`).

- [ ] **Step 4: Retain snapshot edge on `StrutsDiagramApiEdge`**

In `StrutsDiagramApiEdge.java`, store the snapshot edge for merge remapping:

```java
public final class StrutsDiagramApiEdge extends DiagramEdgeBase<StrutsDiagramItem> {

    private final @NotNull StrutsDiagramEdge snapshotEdge;

    public StrutsDiagramApiEdge(@NotNull DiagramNode<StrutsDiagramItem> source,
                                @NotNull DiagramNode<StrutsDiagramItem> target,
                                @NotNull StrutsDiagramEdge snapshotEdge) {
        super(source, target, relationshipFor(snapshotEdge));
        this.snapshotEdge = snapshotEdge;
    }

    @NotNull StrutsDiagramEdge getSnapshotEdge() {
        return snapshotEdge;
    }

    // relationshipFor unchanged...
}
```

- [ ] **Step 5: Implement merge-by-identity in `StrutsDiagramDataModel`**

Replace `applyLiveUpdate` and add helpers in `StrutsDiagramDataModel.java`:

```java
    private void applyLiveUpdate(@NotNull ApiModel fresh) {
        mergeApiModel(fresh);
        DiagramBuilder builder = getUserData(DiagramDataKeys.GRAPH_BUILDER);
        if (builder != null) {
            DiagramDataModel.refreshDataModelInSmartMode(builder);
        }
    }

    private void mergeApiModel(@NotNull ApiModel fresh) {
        if (nodes.isEmpty()) {
            applyApiModel(fresh);
            return;
        }

        Map<Object, DiagramNode<StrutsDiagramItem>> existingByKey = new HashMap<>();
        for (DiagramNode<StrutsDiagramItem> existing : nodes) {
            existingByKey.put(identityKey(existing.getIdentifyingElement()), existing);
        }

        List<DiagramNode<StrutsDiagramItem>> mergedNodes = new ArrayList<>();
        Map<DiagramNode<StrutsDiagramItem>, DiagramNode<StrutsDiagramItem>> freshToMerged =
                new IdentityHashMap<>();

        for (DiagramNode<StrutsDiagramItem> freshNode : fresh.nodes()) {
            Object key = identityKey(freshNode.getIdentifyingElement());
            DiagramNode<StrutsDiagramItem> existing = existingByKey.get(key);
            if (existing instanceof StrutsDiagramApiNode apiNode) {
                apiNode.updateIdentifyingElement(freshNode.getIdentifyingElement());
                mergedNodes.add(apiNode);
                freshToMerged.put(freshNode, apiNode);
            }
            else {
                mergedNodes.add(freshNode);
                freshToMerged.put(freshNode, freshNode);
            }
        }

        List<DiagramEdge<StrutsDiagramItem>> mergedEdges = new ArrayList<>();
        for (DiagramEdge<StrutsDiagramItem> freshEdge : fresh.edges()) {
            DiagramNode<StrutsDiagramItem> source = freshToMerged.get(freshEdge.getSource());
            DiagramNode<StrutsDiagramItem> target = freshToMerged.get(freshEdge.getTarget());
            if (source == null || target == null) {
                continue;
            }
            if (freshEdge instanceof StrutsDiagramApiEdge apiEdge) {
                mergedEdges.add(new StrutsDiagramApiEdge(source, target, apiEdge.getSnapshotEdge()));
            }
        }

        nodes.clear();
        nodes.addAll(mergedNodes);
        edges.clear();
        edges.addAll(mergedEdges);
    }

    private static @NotNull Object identityKey(@NotNull StrutsDiagramItem item) {
        StrutsDiagramNode snapshotNode = item.getSnapshotNode();
        if (snapshotNode == null) {
            XmlFile file = item.getXmlFile();
            return file != null && file.getVirtualFile() != null
                    ? file.getVirtualFile().getUrl()
                    : item;
        }
        if (snapshotNode.getNavigationPointer() != null) {
            return snapshotNode.getNavigationPointer();
        }
        return snapshotNode.getId();
    }
```

Add imports:

```java
import com.intellij.psi.xml.XmlFile;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Set; // only if needed; may omit
```

Keep `applyApiModel` for `refreshDataModel()` full replace. Do **not** change Dom debounce delay, listener registration, or layouter/settings.

- [ ] **Step 6: Run live-refresh tests to verify they pass**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.StrutsDiagramDataModelMappingTest.testDomPathEditUpdatesResultTitleOnRetainedApiNode" --tests "com.intellij.struts2.diagram.StrutsDiagramDataModelMappingTest.testCopyPasteResultGetsDistinctPathAfterDomRefresh" --tests "com.intellij.struts2.diagram.StrutsDiagramDataModelMappingTest.testSameFileDomEventRefreshesLiveDataModel" --tests "com.intellij.struts2.diagram.StrutsDiagramDataModelMappingTest.testRefreshMapsSnapshotNodesAndEdges"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add \
  src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramApiNode.java \
  src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramApiEdge.java \
  src/main/java/com/intellij/struts2/diagram/provider/StrutsDiagramDataModel.java \
  src/test/java/com/intellij/struts2/diagram/StrutsDiagramDataModelMappingTest.java
git commit -m "$(cat <<'EOF'
fix(diagram): merge Show Diagram presentables on Dom refresh (#126)

EOF
)"
```

---

### Task 3: Changelog + full diagram suite

**Files:**
- Modify: `CHANGELOG.md`
- Test: `src/test/java/com/intellij/struts2/diagram/*`

**Interfaces:**
- Consumes: Tasks 1–2 behavior
- Produces: Unreleased changelog entry for #126; green diagram test suite

- [ ] **Step 1: Add changelog entry**

Under `## [Unreleased]`, ensure a `### Fixed` subsection exists in the Unreleased block (add it if missing). Add:

```markdown
- Fix Show Diagram stale result path labels after Dom edit / copy-paste refresh ([#126](https://github.com/apache/struts-intellij-plugin/issues/126))
```

Do not invent a release version section.

- [ ] **Step 2: Run full diagram suite**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"
```

Expected: BUILD SUCCESSFUL; all diagram tests PASS.

- [ ] **Step 3: Manual smoke check (recommended)**

Run:

```bash
./gradlew runIde
```

Open a Struts config → Show Diagram → copy-paste a `success` result → rename to `delete` and change path → after ~300 ms both nodes show correct paths. If a non-custom toolbar layout was selected earlier, Dom refresh must not reset it to custom LTR.

- [ ] **Step 4: Commit**

```bash
git add CHANGELOG.md
git commit -m "$(cat <<'EOF'
docs: note Show Diagram stale result path fix (#126)

EOF
)"
```

---

## Spec coverage checklist

| Spec requirement | Task |
|---|---|
| Pointer-based stable identity (not offset/path) | Task 1 |
| Presentable merge on retained API nodes before smart mode | Task 2 |
| Keep `refreshDataModelInSmartMode` / soft layout | Task 2 (no GraphSettings/layouter changes) |
| Initial `refreshDataModel()` full replace | Task 2 (`applyApiModel` unchanged for that path) |
| Path edit updates label after Dom refresh | Task 2 test + impl |
| Copy-paste name+path → distinct correct nodes | Task 2 test |
| No same-path identity collision | Task 1 + Task 2 tests |
| Swing tab / LTR extras untouched | Global constraints; no tasks touch those files |
| Changelog | Task 3 |
| Manual `runIde` check | Task 3 |

## Self-review notes

- No TBD/placeholder steps; concrete test code and production snippets included.
- `identityKey` uses the same pointer object that `StrutsDiagramNode.equals` uses, so merge matching stays consistent with smart-mode identifying-element equality.
- Unit tests intentionally assert retained `DiagramNode` instance identity because `DiagramBuilder` is null in light tests; that is the merge contract smart mode needs. Path correctness alone is insufficient to prove the fix.
