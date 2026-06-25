# Diagram Tab Auto-Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Struts Diagram tab automatically when the underlying `struts.xml` DOM changes (same file, active tab) and when the user switches to the Diagram tab after editing elsewhere.

**Architecture:** Extend `Struts2DiagramFileEditor` with a project-wide `DomEventListener` (disposed with the editor), a 300 ms debounce `Alarm`, and `selectNotify`/`deselectNotify` visibility gating. Reuse existing `ReadAction.nonBlocking` → `StrutsConfigDiagramModel.build()` → `myComponent.rebuild()` pipeline.

**Tech Stack:** IntelliJ Platform (`DomEventListener`, `Alarm`, `ReadAction.nonBlocking`, `PerspectiveFileEditor`), Apache Struts 2 DOM model, JUnit 4 light tests.

**Spec:** `docs/superpowers/specs/2026-06-25-diagram-auto-refresh-design.md`

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `src/main/java/com/intellij/struts2/diagram/fileEditor/Struts2DiagramFileEditor.java` | Modify | DOM listener, debounce, tab visibility, guarded rebuild |
| `src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorDomFilterTest.java` | Create | Unit tests for `isDomElementInFile` / `isEventForMyFile` |
| `src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorProviderTest.java` | Modify | Add `selectNotify`/`deselectNotify` lifecycle test |
| `CHANGELOG.md` | Modify | Unreleased entry for auto-refresh feature |

No other files change.

---

### Task 1: File-filter helper tests

**Files:**
- Create: `src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorDomFilterTest.java`
- Modify: (none yet — tests reference methods that do not exist)

- [ ] **Step 1: Write the failing test class**

Create `Struts2DiagramFileEditorDomFilterTest.java`:

```java
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.struts2.diagram;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.diagram.fileEditor.Struts2DiagramFileEditor;
import com.intellij.struts2.dom.struts.StrutsRoot;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NotNull;

public class Struts2DiagramFileEditorDomFilterTest extends BasicLightHighlightingTestCase {

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
            assertTrue(Struts2DiagramFileEditor.isDomElementInFile(pkg, vfA));
            assertFalse(Struts2DiagramFileEditor.isDomElementInFile(pkg, vfB));
        });
    }

    public void testIsDomElementInFileRejectsNullElement() {
        createStrutsFileSet("struts-local-a.xml");
        VirtualFile vfA = myFixture.findFileInTempDir("struts-local-a.xml");
        assertNotNull(vfA);
        assertFalse(Struts2DiagramFileEditor.isDomElementInFile(null, vfA));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.Struts2DiagramFileEditorDomFilterTest"
```

Expected: FAIL — `isDomElementInFile` method not found on `Struts2DiagramFileEditor`.

- [ ] **Step 3: Add package-private static helpers to `Struts2DiagramFileEditor`**

Add these methods (keep existing code intact for now):

```java
import com.intellij.util.xml.events.DomEvent;

static boolean isEventForMyFile(@NotNull DomEvent event, @NotNull VirtualFile file) {
    return isDomElementInFile(event.getContextElement(), file);
}

static boolean isDomElementInFile(@Nullable DomElement element, @NotNull VirtualFile file) {
    if (element == null) {
        return false;
    }
    VirtualFile elementFile = element.getOriginalFile().getVirtualFile();
    return file.equals(elementFile);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.Struts2DiagramFileEditorDomFilterTest"
```

Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorDomFilterTest.java \
        src/main/java/com/intellij/struts2/diagram/fileEditor/Struts2DiagramFileEditor.java
git commit -m "$(cat <<'EOF'
test: add DOM file-filter helpers for diagram auto-refresh

Extract isDomElementInFile for unit testing before wiring the listener.
EOF
)"
```

---

### Task 2: Tab lifecycle test

**Files:**
- Modify: `src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorProviderTest.java`

- [ ] **Step 1: Add lifecycle test**

Append to `Struts2DiagramFileEditorProviderTest`:

```java
public void testSelectAndDeselectNotifyDoNotThrow() {
    createStrutsFileSet("struts-diagram.xml");
    VirtualFile file = myFixture.findFileInTempDir("struts-diagram.xml");
    assertNotNull(file);

    Struts2DiagramFileEditor editor =
            (Struts2DiagramFileEditor) myProvider.createEditor(getProject(), file);
    try {
        editor.selectNotify();
        editor.deselectNotify();
        editor.selectNotify();
    } finally {
        Disposer.dispose(editor);
    }
}
```

- [ ] **Step 2: Run test — expect compile error or no-op pass**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.Struts2DiagramFileEditorProviderTest.testSelectAndDeselectNotifyDoNotThrow"
```

Expected before Task 3: PASS (default `FileEditor` no-op methods) or compile error if overrides missing — either way, test is in place.

- [ ] **Step 3: Commit test only**

```bash
git add src/test/java/com/intellij/struts2/diagram/Struts2DiagramFileEditorProviderTest.java
git commit -m "$(cat <<'EOF'
test: cover diagram editor select/deselect notify lifecycle
EOF
)"
```

---

### Task 3: Wire DOM listener, debounce, and visibility gating

**Files:**
- Modify: `src/main/java/com/intellij/struts2/diagram/fileEditor/Struts2DiagramFileEditor.java`

- [ ] **Step 1: Add fields and constants**

Inside `Struts2DiagramFileEditor`, add:

```java
private static final int DOM_UPDATE_DELAY_MS = 300;

private final VirtualFile myVirtualFile;
private final Alarm myUpdateAlarm;
private boolean myDiagramSelected;
```

In constructor, after `myXmlFile = (XmlFile) psiFile;`:

```java
myVirtualFile = file;
myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
registerDomChangeListener();
```

Add imports:

```java
import com.intellij.util.Alarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.xml.DomEventListener;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.events.DomEvent;
```

- [ ] **Step 2: Add listener, debounce, and tab overrides**

Add methods:

```java
@Override
public void selectNotify() {
    myDiagramSelected = true;
    myUpdateAlarm.cancelAllRequests();
    scheduleModelBuild();
}

@Override
public void deselectNotify() {
    myDiagramSelected = false;
    myUpdateAlarm.cancelAllRequests();
}

private void registerDomChangeListener() {
    DomManager.getDomManager(getProject()).addDomEventListener(new DomEventListener() {
        @Override
        public void eventOccured(@NotNull DomEvent event) {
            if (!myDiagramSelected) {
                return;
            }
            if (!isEventForMyFile(event, myVirtualFile)) {
                return;
            }
            queueDebouncedModelBuild();
        }
    }, this);
}

private void queueDebouncedModelBuild() {
    myUpdateAlarm.cancelAllRequests();
    myUpdateAlarm.addRequest(this::scheduleModelBuild, DOM_UPDATE_DELAY_MS);
}
```

- [ ] **Step 3: Guard UI-thread rebuild callback**

Replace `scheduleModelBuild()` with:

```java
private void scheduleModelBuild() {
    ReadAction.nonBlocking(() -> StrutsConfigDiagramModel.build(myXmlFile))
            .expireWith(this)
            .finishOnUiThread(com.intellij.openapi.application.ModalityState.defaultModalityState(),
                    model -> {
                        if (myDiagramSelected) {
                            myComponent.rebuild(model);
                        }
                    })
            .submit(AppExecutorUtil.getAppExecutorService());
}
```

Remove the old `.submit(com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService())` duplicate import if present — use `AppExecutorUtil` consistently.

- [ ] **Step 4: Update class Javadoc**

Extend the class Javadoc to mention DOM-driven auto-refresh while the tab is selected.

- [ ] **Step 5: Run all diagram tests**

Run:

```bash
./gradlew test -x rat --tests "com.intellij.struts2.diagram.*"
```

Expected: BUILD SUCCESSFUL — all diagram tests pass including new lifecycle and filter tests.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/intellij/struts2/diagram/fileEditor/Struts2DiagramFileEditor.java
git commit -m "$(cat <<'EOF'
feat: auto-refresh Diagram tab on struts.xml DOM changes

Register a debounced DomEventListener while the Diagram tab is active
and rebuild immediately on tab selection. Closes #97.
EOF
)"
```

---

### Task 4: CHANGELOG and full regression

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Add Unreleased entry**

Under `## [Unreleased]` → `### Added`:

```markdown
- Diagram tab auto-refreshes when `struts.xml` is edited (same file, active tab) and on tab activation after Text edits ([#97](https://github.com/apache/struts-intellij-plugin/issues/97))
```

- [ ] **Step 2: Run full test suite (excluding RAT)**

Run:

```bash
./gradlew test -x rat
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add CHANGELOG.md
git commit -m "$(cat <<'EOF'
docs: note Diagram tab auto-refresh in CHANGELOG
EOF
)"
```

---

## Manual Smoke Test (post-implementation)

1. `./gradlew runIde`
2. Open a project with a registered `struts.xml`; open the file.
3. Switch to **Diagram** tab — packages/actions/results render.
4. Switch to **Text** tab; add or rename an `<action>`; switch back to **Diagram** — change appears immediately.
5. Stay on **Diagram** tab; edit XML in a split editor — diagram updates after ~300 ms pause.
6. Open a large config; type continuously — no EDT freeze.

---

## Plan Self-Review

| Spec requirement | Task |
|---|---|
| Live update while Diagram tab active | Task 3 — `DomEventListener` + `myDiagramSelected` |
| Refresh on tab activation | Task 3 — `selectNotify()` |
| Debounced, non-blocking EDT | Task 3 — `Alarm` 300 ms + `ReadAction.nonBlocking` |
| Background read action | Task 3 — unchanged `scheduleModelBuild()` pipeline |
| Same-file filtering | Task 1 helpers + Task 3 listener guard |
| Skip rebuild when tab inactive | Task 3 — `deselectNotify` + UI callback guard |
| Unit tests for filtering | Task 1 |
| Lifecycle tests | Task 2 |
| No cross-file refresh | Task 1 + Task 3 filtering |

No placeholders. All code shown is complete for each step.
