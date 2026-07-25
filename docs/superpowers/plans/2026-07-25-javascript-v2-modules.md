# JavaScript v2 Content Modules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Declare IntelliJ Platform 2026.2 JavaScript v2 content modules so Struts JSP/FreeMarker JS injectors load without classloader errors.

**Architecture:** Minimal declarative fix on top of the existing `JavaScript` plugin dependency. Add `intellij.javascript.parser` and `intellij.javascript.backend` in `plugin.xml` and matching `bundledModule(...)` entries in Gradle. Escalate to `intellij.javascript.common` (then broader modules) only if build, plugin verifier, or `runIde` shows a real classloader gap. No injector logic changes.

**Tech Stack:** IntelliJ Platform Gradle Plugin 2.x, IntelliJ IDEA 2026.2 (`262.*`), Java 25, JetBrains Plugin Verifier.

**Spec:** `docs/superpowers/specs/2026-07-25-javascript-v2-modules-design.md`  
**Issue:** [#103](https://github.com/apache/struts-intellij-plugin/issues/103)

## Global Constraints

- Keep `<plugin id="JavaScript"/>` and `bundledPlugin("JavaScript")` — v2 `<module>` entries are **additional**.
- Start with exactly `intellij.javascript.parser` and `intellij.javascript.backend`.
- Add `intellij.javascript.common` only after a real `NoClassDefFoundError` / `ClassNotFoundException` for `com.intellij.lang.javascript.*`.
- Broader modules (`psi.impl`, `analysis.impl`) only after `common` still fails.
- Do **not** change `TaglibJavaScriptInjector` or `FreeMarkerJavaScriptInjector`.
- Do **not** make JavaScript optional.
- Do **not** add new automated JS injector tests.
- Do **not** run prepare/publish release workflows.
- Platform target is already 2026.2 / Java 25 (from #115); do not re-bump platform versions unless verification forces it.
- Prefer public APIs; fix Marketplace-blocking verifier issues before merging.

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `src/main/resources/META-INF/plugin.xml` | Modify | Declare JS v2 content modules next to existing `JavaScript` plugin dependency |
| `build.gradle.kts` | Modify | Declare matching `bundledModule(...)` entries for compile/runtime classpath |
| `CHANGELOG.md` | Modify | Record JavaScript v2 content module dependencies under Unreleased |
| `docs/superpowers/specs/2026-07-25-javascript-v2-modules-design.md` | Reference only | Approved design decisions |
| Injector sources under `src/main/java/.../jsp/` and `.../freemarker/` | Do not modify | Already use the classes hosted by the modules above |

No new modules, extension points, or feature code.

---

### Task 1: Declare JavaScript v2 content modules

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml` (around the `<dependencies>` block containing `JavaScript`)
- Modify: `build.gradle.kts` (around `bundledPlugin("JavaScript")`)

**Interfaces:**
- Consumes: existing required dependency on plugin id `JavaScript`
- Produces: runtime/classpath access to `JavaScriptSupportLoader` (`intellij.javascript.parser`) and `JSInXmlLanguagesInjector` (`intellij.javascript.backend`)

- [ ] **Step 1: Confirm current dependency wiring**

Run:

```bash
rg -n 'JavaScript|javascript\.(parser|backend|common)|bundledModule' \
  src/main/resources/META-INF/plugin.xml build.gradle.kts
```

Expected: `plugin.xml` has `<plugin id="JavaScript"/>` and no JS module entries yet; `build.gradle.kts` has `bundledPlugin("JavaScript")` and existing `bundledModule("intellij.xml.structureView*")` only.

- [ ] **Step 2: Add modules to `plugin.xml`**

In `src/main/resources/META-INF/plugin.xml`, change the JavaScript dependency lines from:

```xml
        <plugin id="JavaScript"/>
        <plugin id="com.intellij.css"/>
```

to:

```xml
        <plugin id="JavaScript"/>
        <module name="intellij.javascript.parser"/>
        <module name="intellij.javascript.backend"/>
        <plugin id="com.intellij.css"/>
```

Keep every other dependency unchanged.

- [ ] **Step 3: Add matching Gradle `bundledModule` entries**

In `build.gradle.kts`, inside `intellijPlatform { ... }`, change:

```kotlin
        bundledPlugin("JavaScript")
        bundledPlugin("com.intellij.css")
        bundledPlugin("intellij.structureView.plugin")
        bundledPlugin("com.intellij.modules.json")
        bundledModule("intellij.xml.structureView")
        bundledModule("intellij.xml.structureView.impl")
```

to:

```kotlin
        bundledPlugin("JavaScript")
        bundledModule("intellij.javascript.parser")
        bundledModule("intellij.javascript.backend")
        bundledPlugin("com.intellij.css")
        bundledPlugin("intellij.structureView.plugin")
        bundledPlugin("com.intellij.modules.json")
        bundledModule("intellij.xml.structureView")
        bundledModule("intellij.xml.structureView.impl")
```

- [ ] **Step 4: Resolve and compile to confirm modules exist on 2026.2**

Run:

```bash
./gradlew compileJava --no-daemon
```

Expected: BUILD SUCCESSFUL. If Gradle fails with an unknown/`bundledModule` resolution error for either JS module name, stop and re-check JetBrains docs / the 2026.2 distribution module list before inventing alternate names.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/META-INF/plugin.xml build.gradle.kts
git commit -m "$(cat <<'EOF'
build: declare JavaScript v2 content modules for 2026.2

Add intellij.javascript.parser and intellij.javascript.backend so
Struts taglib JS injectors can load under the fully v2 JavaScript plugin.
EOF
)"
```

---

### Task 2: Verify build, plugin verifier, tests, and runIde smoke

**Files:**
- Modify only if escalation is required:
  - `src/main/resources/META-INF/plugin.xml`
  - `build.gradle.kts`

**Interfaces:**
- Consumes: Task 1 module declarations
- Produces: evidence that the declared set is sufficient (or an escalated set that is)

- [ ] **Step 1: Full build**

Run:

```bash
./gradlew build --no-daemon
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Plugin verifier against 2026.2**

Run:

```bash
./gradlew runPluginVerifier --no-daemon
```

Expected: completes without Marketplace-blocking incompatibilities for the 262 target. Warnings about unrelated internal APIs may already exist; do not expand scope to clean those unless they block merge.

- [ ] **Step 3: Enabled unit tests**

Run:

```bash
./gradlew test -x rat --no-daemon
```

Expected: BUILD SUCCESSFUL for the currently enabled suite. Do not re-enable underscore-prefixed / historically disabled tests.

- [ ] **Step 4: Manual `runIde` smoke for JSP JS injection**

Run:

```bash
./gradlew runIde --no-daemon
```

In the launched IDE:

1. Open or create a JSP that uses Struts UI / jQuery taglib attributes such as `onclick` / `onComplete` (any `on*` attribute covered by `TaglibJavaScriptInjector`).
2. Confirm the attribute value gets JavaScript language injection (highlighting / language host present).
3. Check Help → Show Log in Finder/Explorer (or the idea.log) for `NoClassDefFoundError` / `ClassNotFoundException` mentioning `com.intellij.lang.javascript`.

Expected: injection works; no JS classloader errors.

- [ ] **Step 5: Manual FreeMarker smoke (if FreeMarker plugin is available in the sandbox)**

In the same `runIde` session, open a FreeMarker file using Struts taglib macros with `on*` / `doubleOn*` attributes (not ending in `Topics`). Confirm injection via `FreeMarkerJavaScriptInjector` and re-check the log.

If FreeMarker is unavailable in the sandbox, note that in the PR and rely on the JSP smoke plus compile-time module resolution (injector still compiles against `JavaScriptSupportLoader`).

- [ ] **Step 6: Escalate only if classloader errors remain**

If Steps 4–5 (or earlier steps) show residual `com.intellij.lang.javascript.*` classloader failures, add `intellij.javascript.common` in both places:

`plugin.xml` (after the two existing JS modules):

```xml
        <module name="intellij.javascript.common"/>
```

`build.gradle.kts` (after the two existing JS `bundledModule` lines):

```kotlin
        bundledModule("intellij.javascript.common")
```

Then re-run Steps 1–5. Only if `common` is still insufficient, add broader modules named in the failure (`psi.impl` / `analysis.impl` equivalents as documented by JetBrains for the missing class), never a speculative full JS module dump.

If escalation changes files, commit before continuing:

```bash
git add src/main/resources/META-INF/plugin.xml build.gradle.kts
git commit -m "$(cat <<'EOF'
build: add intellij.javascript.common for residual JS classloader gaps

Required after runIde/verifier showed missing com.intellij.lang.javascript classes
beyond parser and backend modules.
EOF
)"
```

Adjust the commit message to name whatever module(s) were actually added.

- [ ] **Step 7: No-op commit checkpoint if no escalation**

If no escalation was needed, do not create an empty commit. Proceed to Task 3.

---

### Task 3: Changelog and handoff

**Files:**
- Modify: `CHANGELOG.md`

**Interfaces:**
- Consumes: final module set from Tasks 1–2
- Produces: Unreleased changelog entry matching what was actually declared

- [ ] **Step 1: Update `CHANGELOG.md`**

Under `## [Unreleased]` → `### Changed`, add a bullet after the existing 2026.2 / dependency entries. If only the two planned modules were declared:

```markdown
- Dependencies - declare JavaScript v2 content modules `intellij.javascript.parser` and `intellij.javascript.backend` for IntelliJ Platform 2026.2 ([#103](https://github.com/apache/struts-intellij-plugin/issues/103))
```

If escalation added more modules, list every module that landed, for example:

```markdown
- Dependencies - declare JavaScript v2 content modules `intellij.javascript.parser`, `intellij.javascript.backend`, and `intellij.javascript.common` for IntelliJ Platform 2026.2 ([#103](https://github.com/apache/struts-intellij-plugin/issues/103))
```

Do not invent a Marketplace release section; keep under Unreleased.

- [ ] **Step 2: Confirm descriptor/Gradle still match**

Run:

```bash
rg -n 'intellij\.javascript\.(parser|backend|common)' \
  src/main/resources/META-INF/plugin.xml build.gradle.kts CHANGELOG.md
```

Expected: every JS module named in `plugin.xml` also appears in `build.gradle.kts` and is mentioned in `CHANGELOG.md`.

- [ ] **Step 3: Commit**

```bash
git add CHANGELOG.md
git commit -m "$(cat <<'EOF'
docs: record JavaScript v2 content modules in changelog

Document the 2026.2 JavaScript modular dependency fix for #103.
EOF
)"
```

- [ ] **Step 4: Final status check**

Run:

```bash
git status -sb
git log --oneline main..HEAD
```

Expected: clean working tree; commits covering module wiring, optional escalation, and changelog. Ready for PR that closes #103.

---

## Spec coverage checklist

| Spec requirement | Task |
|---|---|
| Declare `intellij.javascript.parser` + `intellij.javascript.backend` in `plugin.xml` | Task 1 |
| Matching Gradle `bundledModule(...)` | Task 1 |
| Keep existing `JavaScript` plugin dependency | Task 1 (explicit non-removal) |
| Escalate to `common` / broader only with evidence | Task 2 Step 6 |
| `./gradlew build` | Task 2 Step 1 |
| `./gradlew runPluginVerifier` | Task 2 Step 2 |
| `./gradlew test -x rat` | Task 2 Step 3 |
| Manual `runIde` JSP + FreeMarker smoke | Task 2 Steps 4–5 |
| No injector code changes | Global Constraints + File Structure |
| No new automated JS injector tests | Global Constraints |
| `CHANGELOG.md` updated | Task 3 |
| No Marketplace release in this work | Global Constraints |
