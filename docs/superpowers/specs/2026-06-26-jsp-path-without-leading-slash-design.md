# JSP Result Path Resolution Without Leading Slash

**Date:** 2026-06-26  
**Status:** Approved for implementation planning

## Problem

The Struts plugin reports **"Cannot resolve symbol 'WEB-INF/upload.jsp'"** for valid `<result>` paths that omit the leading `/`, even when the JSP file exists under the Web facet root (`src/main/webapp/WEB-INF/upload.jsp`).

Confirmed by user: changing `WEB-INF/upload.jsp` to `/WEB-INF/upload.jsp` makes the error disappear. Missing classes (e.g. `FailedAction`) and genuinely missing JSPs (`failed.jsp`) are correctly flagged — only namespace-relative path syntax is broken.

## Root Cause

Two cooperating mechanisms fail for paths without a leading `/`:

1. **`Struts2ModelInspection.shouldCheckResolveProblems()`** suppresses the generic DOM **"Cannot resolve symbol"** check only when the path matches `/*/.*\.jsp` (leading `/` required). Paths like `WEB-INF/upload.jsp` bypass suppression, so `PathReference.fromString()` returning `null` triggers a false-positive symbol error.

2. **`DispatchPathResultContributor`** builds a standard `FileReferenceSet` from the raw XML text. IntelliJ resolves paths with a leading `/` against Web facet roots; paths without `/` are not treated as servlet-context-absolute, so `fromString()` fails even when the target file exists.

At **runtime**, Struts 2 resolves result locations without a leading `/` relative to the action's package namespace (`ServletDispatcherResult` semantics). For namespace `/`, `WEB-INF/upload.jsp` is equivalent to `/WEB-INF/upload.jsp`.

## Goals

1. No false **"Cannot resolve symbol"** errors for valid namespace-relative JSP paths (e.g. `WEB-INF/upload.jsp` with namespace `/`).
2. File-level validation still works: missing JSPs report **"Cannot resolve file"** (not symbol errors).
3. Namespace-relative resolution matches Struts: `list.jsp` in package `/admin` resolves to `/admin/list.jsp`.
4. Existing absolute paths (`/WEB-INF/upload.jsp`, `/index.jsp`) continue to work unchanged.

## Non-Goals

- Fixing missing Java action classes — working as intended.
- Changing FreeMarker/Velocity result contributors (separate path logic; follow-up if reported).
- Re-enabling all disabled `StrutsResultResolvingTest` cases unrelated to this fix.

## Decision

**Approach A — Normalize paths at resolution time** (selected over suppression-only or documentation-only).

Introduce a small path-normalization utility and apply it when building `FileReferenceSet` references. Align inspection suppression with normalized paths (or suppress generic DOM checks for all dispatch-type result paths handled by a contributor).

## Architecture

```
<result>WEB-INF/upload.jsp</result>
         │
         ▼
DispatchPathResultContributor.createReferences()
         │
         ├─ namespace = parent <package> namespace (e.g. "/")
         │
         ▼
StrutsResultPathUtil.toAbsoluteWebPath("WEB-INF/upload.jsp", "/")
         → "/WEB-INF/upload.jsp"
         │
         ▼
FileReferenceSet (custom: uses normalized path for resolution)
         + web roots from WebFacet
         │
         ▼
Resolves to src/main/webapp/WEB-INF/upload.jsp ✓

Struts2ModelInspection.shouldCheckResolveProblems()
         │
         ├─ dispatch result + web-resource path → suppress generic symbol check
         └─ FileReferenceSet reports missing files only
```

## Components

### New: `StrutsResultPathUtil`

Location: `src/main/java/com/intellij/struts2/dom/struts/impl/path/StrutsResultPathUtil.java`

```java
public final class StrutsResultPathUtil {
  @Nullable
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

Package-private or public; covered by unit tests.

### Modified: `DispatchPathResultContributor`

Wrap `FileReferenceSet.createSet(...)` in a subclass (or anonymous subclass) that overrides path retrieval to return `toAbsoluteWebPath(rawPath, packageNamespace)` before reference resolution. Keep existing `FileReferenceSetHelper.addWebDirectoryAndCurrentNamespaceAsRoots(...)` call unchanged.

### Modified: `Struts2ModelInspection`

Replace the narrow `/*/.*\.jsp` regex with logic that suppresses generic DOM resolve checks for dispatch-type `<result>` paths where a `StrutsResultContributor` is active — **excluding** paths already handled by existing suppressions (OGNL `${...}`, URLs, wildcards, nested `<param>` bodies).

Alternative (acceptable): normalize the path with `StrutsResultPathUtil` inside `shouldCheckResolveProblems` and apply the existing JSP suffix check on the normalized value.

### Tests

| Test | File | Expectation |
|---|---|---|
| Unit: path normalization | `StrutsResultPathUtilTest.java` | Covers `/`, `/admin`, OGNL, URLs |
| Highlighting: WEB-INF no slash | `struts-path-webinf-relative.xml` | `WEB-INF/upload.jsp` → no error when JSP exists |
| Highlighting: missing JSP | same file | `WEB-INF/missing.jsp` → file error only |
| Highlighting: namespace-relative | `struts-path-namespace-relative.xml` | `list.jsp` in `/admin` package |
| Re-enable dispatcher test | `StrutsResultResolvingTest._testPathDispatcher` | Rename to `testPathDispatcher` once fixtures pass |

## Error Handling

| Input | Behavior |
|---|---|
| `${dynamicPath}` | Pass through unchanged; existing OGNL suppression |
| `http://example.com/page` | Pass through; URL suppression |
| Wildcard `{1}/page.jsp` | Existing wildcard suppression |
| Empty path | No change; existing empty-path handling |
| No Web facet | Existing silent no-op in contributor |

## Verification

```bash
./gradlew test -x rat --tests "StrutsResultPathUtilTest"
./gradlew test -x rat --tests "StrutsResultResolvingTest"
```

Manual: open `struts-examples/file-upload` with `WEB-INF/upload.jsp` (no leading slash) — no symbol error on existing JSP.
