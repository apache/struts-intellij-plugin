# JSP Result Path Without Leading Slash — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix false "Cannot resolve symbol" errors for namespace-relative JSP result paths (e.g. `WEB-INF/upload.jsp`) by normalizing them to servlet-context-absolute paths before reference resolution.

**Architecture:** Add `StrutsResultPathUtil.toAbsoluteWebPath()` mirroring Struts `ServletDispatcherResult` semantics. Use a custom `FileReferenceSet` in `DispatchPathResultContributor` that resolves against the normalized path. Broaden `Struts2ModelInspection` suppression so generic DOM checks defer to file references for dispatch-type results.

**Tech Stack:** IntelliJ Platform (`FileReferenceSet`, Web facet), Apache Struts 2 DOM model, JUnit 4 light tests.

**Spec:** `docs/superpowers/specs/2026-06-26-jsp-path-without-leading-slash-design.md`

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `src/main/java/com/intellij/struts2/dom/struts/impl/path/StrutsResultPathUtil.java` | Create | Namespace-relative → absolute web path normalization |
| `src/main/java/com/intellij/struts2/dom/struts/impl/path/DispatchPathResultContributor.java` | Modify | Custom `FileReferenceSet` using normalized path |
| `src/main/java/com/intellij/struts2/dom/inspection/Struts2ModelInspection.java` | Modify | Suppress generic symbol check for dispatch web-resource paths |
| `src/test/java/com/intellij/struts2/dom/struts/impl/path/StrutsResultPathUtilTest.java` | Create | Unit tests for normalization |
| `src/test/testData/strutsXml/result/struts-path-webinf-relative.xml` | Create | Highlighting fixture for WEB-INF paths without `/` |
| `src/test/testData/strutsXml/result/WEB-INF/upload.jsp` | Create | Existing JSP fixture |
| `src/test/java/com/intellij/struts2/dom/struts/StrutsResultResolvingTest.java` | Modify | Add `testPathWebInfRelative()` |
| `src/test/testData/strutsXml/result/struts-path-dispatcher.xml` | Modify | Update `valid1` — `index.jsp` should no longer error |
| `CHANGELOG.md` | Modify | Unreleased bugfix entry |

---

### Task 1: Path normalization utility

**Files:**
- Create: `src/main/java/com/intellij/struts2/dom/struts/impl/path/StrutsResultPathUtil.java`
- Create: `src/test/java/com/intellij/struts2/dom/struts/impl/path/StrutsResultPathUtilTest.java`

- [ ] **Step 1: Write the failing test class**

Create `StrutsResultPathUtilTest.java`:

```java
package com.intellij.struts2.dom.struts.impl.path;

import com.intellij.struts2.dom.struts.strutspackage.StrutsPackage;
import junit.framework.TestCase;

public class StrutsResultPathUtilTest extends TestCase {

  public void testRootNamespacePrependsSlash() {
    assertEquals("/WEB-INF/upload.jsp",
                 StrutsResultPathUtil.toAbsoluteWebPath("WEB-INF/upload.jsp", StrutsPackage.DEFAULT_NAMESPACE));
  }

  public void testAbsolutePathUnchanged() {
    assertEquals("/WEB-INF/upload.jsp",
                 StrutsResultPathUtil.toAbsoluteWebPath("/WEB-INF/upload.jsp", StrutsPackage.DEFAULT_NAMESPACE));
  }

  public void testNonRootNamespacePrependsNamespace() {
    assertEquals("/admin/list.jsp",
                 StrutsResultPathUtil.toAbsoluteWebPath("list.jsp", "/admin"));
  }

  public void testNonRootNamespaceWithTrailingSlash() {
    assertEquals("/admin/list.jsp",
                 StrutsResultPathUtil.toAbsoluteWebPath("list.jsp", "/admin/"));
  }

  public void testOgnlExpressionUnchanged() {
    assertEquals("${someProperty}",
                 StrutsResultPathUtil.toAbsoluteWebPath("${someProperty}", StrutsPackage.DEFAULT_NAMESPACE));
  }

  public void testUrlUnchanged() {
    assertEquals("http://example.com/page",
                 StrutsResultPathUtil.toAbsoluteWebPath("http://example.com/page", StrutsPackage.DEFAULT_NAMESPACE));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test -x rat --tests "StrutsResultPathUtilTest"`
Expected: FAIL — class `StrutsResultPathUtil` not found

- [ ] **Step 3: Write minimal implementation**

Create `StrutsResultPathUtil.java`:

```java
package com.intellij.struts2.dom.struts.impl.path;

import com.intellij.struts2.dom.struts.strutspackage.StrutsPackage;
import com.intellij.util.io.URLUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class StrutsResultPathUtil {

  private StrutsResultPathUtil() {
  }

  @NotNull
  public static String toAbsoluteWebPath(@NotNull String path, @NotNull String namespace) {
    if (path.startsWith("/") || path.startsWith("${") || URLUtil.containsScheme(path)) {
      return path;
    }
    if (Objects.equals(namespace, StrutsPackage.DEFAULT_NAMESPACE)) {
      return "/" + path;
    }
    final String prefix = namespace.endsWith("/") ? namespace : namespace + "/";
    return prefix + path;
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test -x rat --tests "StrutsResultPathUtilTest"`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/intellij/struts2/dom/struts/impl/path/StrutsResultPathUtil.java \
        src/test/java/com/intellij/struts2/dom/struts/impl/path/StrutsResultPathUtilTest.java
git commit -m "$(cat <<'EOF'
Add StrutsResultPathUtil for namespace-relative result paths.

EOF
)"
```

---

### Task 2: Apply normalization in DispatchPathResultContributor

**Files:**
- Modify: `src/main/java/com/intellij/struts2/dom/struts/impl/path/DispatchPathResultContributor.java`

- [ ] **Step 1: Replace FileReferenceSet.createSet with normalized subclass**

In `createReferences()`, replace:

```java
final FileReferenceSet fileReferenceSet = FileReferenceSet.createSet(psiElement, soft, false, true);
```

with:

```java
final String normalizedNamespace = packageNamespace;
final FileReferenceSet fileReferenceSet = new FileReferenceSet(psiElement, soft, false, true) {
  @Override
  public @NotNull String getPathString() {
    return StrutsResultPathUtil.toAbsoluteWebPath(super.getPathString(), normalizedNamespace);
  }
};
```

Add import for `StrutsResultPathUtil`.

- [ ] **Step 2: Compile**

Run: `./gradlew compileJava compileTestJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/intellij/struts2/dom/struts/impl/path/DispatchPathResultContributor.java
git commit -m "$(cat <<'EOF'
Normalize namespace-relative paths in dispatch result references.

EOF
)"
```

---

### Task 3: Fix inspection suppression

**Files:**
- Modify: `src/main/java/com/intellij/struts2/dom/inspection/Struts2ModelInspection.java`

- [ ] **Step 1: Replace narrow JSP regex with normalized-path check**

Replace the block:

```java
      // WEB-INF/**/*.jsp
      if (stringValue.matches("/*/.*\\.jsp")) {
        LOG.info("Inspecting jsp file: " + stringValue);
        return false;
      }
```

with:

```java
      // Let FileReferenceSet report file-level errors for web-resource paths
      final StrutsPackage strutsPackage = DomUtil.getParentOfType(value, StrutsPackage.class, true);
      if (strutsPackage != null) {
        final String namespace = strutsPackage.searchNamespace();
        if (namespace != null) {
          final String absolutePath = StrutsResultPathUtil.toAbsoluteWebPath(stringValue, namespace);
          if (absolutePath.startsWith("/") && absolutePath.contains(".")) {
            return false;
          }
        }
      }
```

Add imports for `StrutsPackage`, `StrutsResultPathUtil`, and `DomUtil` (if not already present).

- [ ] **Step 2: Compile**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/intellij/struts2/dom/inspection/Struts2ModelInspection.java
git commit -m "$(cat <<'EOF'
Suppress symbol errors for namespace-relative dispatch result paths.

EOF
)"
```

---

### Task 4: Highlighting tests

**Files:**
- Create: `src/test/testData/strutsXml/result/WEB-INF/upload.jsp`
- Create: `src/test/testData/strutsXml/result/struts-path-webinf-relative.xml`
- Modify: `src/test/testData/strutsXml/result/struts-path-dispatcher.xml`
- Modify: `src/test/java/com/intellij/struts2/dom/struts/StrutsResultResolvingTest.java`

- [ ] **Step 1: Create JSP fixture**

Create `src/test/testData/strutsXml/result/WEB-INF/upload.jsp`:

```jsp
<%@ page contentType="text/html;charset=UTF-8" %>
<html><body>upload</body></html>
```

- [ ] **Step 2: Create highlighting fixture**

Create `src/test/testData/strutsXml/result/struts-path-webinf-relative.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE struts PUBLIC
    "-//Apache Software Foundation//DTD Struts Configuration 2.0//EN"
    "http://struts.apache.org/dtds/struts-2.0.dtd">
<struts>
  <package name="default" namespace="/" extends="struts-default">
    <action name="upload">
      <result name="input">WEB-INF/upload.jsp</result>
      <result name="missing"><error descr="Cannot resolve file 'missing.jsp'">WEB-INF/missing.jsp</error></result>
      <result name="absolute">/WEB-INF/upload.jsp</result>
    </action>
  </package>
</struts>
```

- [ ] **Step 3: Update struts-path-dispatcher.xml valid1**

Change line 38 from:

```xml
      <result name="valid1"><error descr="Cannot resolve file 'index.jsp'">index.jsp</error></result>
```

to:

```xml
      <result name="valid1">index.jsp</result>
```

(`index.jsp` normalizes to `/index.jsp` which exists in test `jsp/` web root.)

- [ ] **Step 4: Add test method**

In `StrutsResultResolvingTest.java`, add:

```java
  public void testPathWebInfRelative() {
    performHighlightingTest("struts-path-webinf-relative.xml");
  }
```

- [ ] **Step 5: Run tests**

Run: `./gradlew test -x rat --tests "StrutsResultResolvingTest.testPathWebInfRelative" --tests "StrutsResultPathUtilTest"`
Expected: BUILD SUCCESSFUL

If `testPathWebInfRelative` fails on error message text or offsets, adjust the `<error descr="...">` annotations in the fixture to match actual IDE output (same pattern as other tests in this class).

- [ ] **Step 6: Commit**

```bash
git add src/test/testData/strutsXml/result/WEB-INF/upload.jsp \
        src/test/testData/strutsXml/result/struts-path-webinf-relative.xml \
        src/test/testData/strutsXml/result/struts-path-dispatcher.xml \
        src/test/java/com/intellij/struts2/dom/struts/StrutsResultResolvingTest.java
git commit -m "$(cat <<'EOF'
Add tests for namespace-relative JSP result path resolution.

EOF
)"
```

---

### Task 5: Changelog and full test run

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Add changelog entry**

Under `[Unreleased]` / `### Fixed`:

```markdown
- Fix false "Cannot resolve symbol" errors for namespace-relative JSP result paths (e.g. `WEB-INF/upload.jsp` without leading slash)
```

- [ ] **Step 2: Run full test suite**

Run: `./gradlew test -x rat`
Expected: BUILD SUCCESSFUL (pre-existing disabled tests remain disabled)

- [ ] **Step 3: Commit**

```bash
git add CHANGELOG.md
git commit -m "$(cat <<'EOF'
Document fix for namespace-relative JSP result paths.

EOF
)"
```

---

## Spec Coverage Checklist

| Spec requirement | Task |
|---|---|
| No false symbol errors for `WEB-INF/upload.jsp` | Task 2 + 3 + 4 |
| Missing JSPs still report file errors | Task 4 fixture `missing` result |
| Namespace-relative `/admin/list.jsp` | Task 1 unit tests; optional follow-up fixture |
| Absolute paths unchanged | Task 1 + Task 4 `absolute` result |
| Manual verification on file-upload example | Post-merge manual check |

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-26-jsp-path-without-leading-slash.md`.

**Two execution options:**

1. **Subagent-Driven (recommended)** — dispatch a fresh subagent per task, review between tasks
2. **Inline Execution** — implement all tasks in this session with checkpoints

Which approach do you prefer?
