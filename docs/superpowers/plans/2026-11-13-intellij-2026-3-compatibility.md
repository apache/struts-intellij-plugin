# IntelliJ IDEA 2026.3 (263) Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the plugin installable only on IntelliJ IDEA 2026.3 (build branch `263`), and declare the XML plugin dependency only if a local 2026.3 build proves it is missing.

**Architecture:** Two commits on branch `build/intellij-2026-3`. Commit 1 retargets versions, the Gradle plugin, the changelog, and the platform table, then captures the first XML failure. Commit 2 adds `com.intellij.modules.xml` and `bundledPlugin("com.intellij.xml")`, then re-runs tests and the plugin verifier. The branch is pushed and the pull request opened only when the tip is green.

**Tech Stack:** Gradle 9.0.0, IntelliJ Platform Gradle Plugin 2.20.1, Java 25 toolchain, JUnit 4 (`test -x rat`), IntelliJ Plugin Verifier, GitHub CLI (`gh`).

**Spec:** `docs/superpowers/specs/2026-11-13-intellij-2026-3-compatibility-design.md`

## Global Constraints

- Target platform `2026.3` only; `pluginSinceBuild = 263`, `pluginUntilBuild = 263.*`, `pluginVersion = 263.19043.1`.
- `org.jetbrains.intellij.platform` Gradle plugin version `2.20.1`.
- Java stays at `25` (`jvmToolchain(25)`, CI `java-version: 25`); Gradle stays at `9.0.0`.
- Do not change `qodana.yml` or the `qodana-action` pin in `.github/workflows/build.yml`.
- Do not change `src/main/resources/META-INF/ognl.xml` (`colorSettingsPage` stays; #151 is out of scope).
- Do not re-enable disabled tests.
- Do not run `prepare_release.yml`, `release.yml`, or `nightly.yml`.
- Commits follow `.github` conventional style with a type prefix (no JIRA ticket): `build:`, `docs:`, `fix:`.
- Never commit to `main`. Work only on `build/intellij-2026-3`.

---

### Task 1: Retarget the plugin to IntelliJ Platform 2026.3 (commit 1)

**Files:**
- Modify: `gradle.properties` (lines for `platformVersion`, `pluginVersion`, `pluginSinceBuild`, `pluginUntilBuild`)
- Modify: `build.gradle.kts` (the `org.jetbrains.intellij.platform` plugin line)
- Modify: `CHANGELOG.md` (insert a `### Changed` block under `## [Unreleased]`)
- Modify: `CLAUDE.md` (platform table, after the `262.x` row)

**Interfaces:**
- Consumes: the existing `2026.2` / `262` configuration, which is the baseline in `gradle.properties`.
- Produces: a branch tip in which `./gradlew compileJava` runs against IntelliJ IDEA 2026.3. Task 2 reads that build output.

- [ ] **Step 1: Confirm the baseline**

Run: `grep -n "platformVersion\|pluginVersion\|pluginSinceBuild\|pluginUntilBuild" gradle.properties`
Expected:
```
pluginVersion = 262.19043.1
pluginSinceBuild = 262
pluginUntilBuild = 262.*
platformVersion = 2026.2
```

- [ ] **Step 2: Update `gradle.properties`**

Replace these four lines, changing nothing else in the file:

```properties
pluginVersion = 262.19043.1
```
→
```properties
pluginVersion = 263.19043.1
```

```properties
pluginSinceBuild = 262
```
→
```properties
pluginSinceBuild = 263
```

```properties
pluginUntilBuild = 262.*
```
→
```properties
pluginUntilBuild = 263.*
```

```properties
platformVersion = 2026.2
```
→
```properties
platformVersion = 2026.3
```

- [ ] **Step 3: Update the Gradle plugin in `build.gradle.kts`**

Replace:

```kotlin
    id("org.jetbrains.intellij.platform") version "2.18.1"
```
with:
```kotlin
    id("org.jetbrains.intellij.platform") version "2.20.1"
```

Do not touch the other plugin versions, the `bundledPlugin` / `bundledModule` lists, or the `testBundledPlugins` list in this commit.

- [ ] **Step 4: Add the changelog entry**

In `CHANGELOG.md`, the `## [Unreleased]` section currently ends with its `### Added` bullet, followed by a blank line and then `## [262.19039.1] - 2026-08-01`. Insert this block between the Added bullet and `## [262.19039.1]`:

```markdown
### Changed

- Update `platformVersion` to `2026.3`
- Change since/until build to `263-263.*` (2026.3 only)
- Dependencies - upgrade `org.jetbrains.intellij.platform` to `2.20.1`

```

Keep one blank line before `## [262.19039.1] - 2026-08-01`.

- [ ] **Step 5: Add the platform table row in `CLAUDE.md`**

In the platform table, after the row:

```markdown
| 262.x  | 2026.2           | 262.*       |
```

add:

```markdown
| 263.x  | 2026.3           | 263.*       |
```

Keep the `262.x` row; the table lists all supported branches.

- [ ] **Step 6: Verify the edits**

Run: `grep -n "263" gradle.properties build.gradle.kts CHANGELOG.md CLAUDE.md`
Expected: `pluginVersion = 263.19043.1`, `pluginSinceBuild = 263`, `pluginUntilBuild = 263.*`, `platformVersion = 2026.3`, `version "2.20.1"` in `build.gradle.kts`, the `### Changed` block in `CHANGELOG.md`, and the `263.x` row in `CLAUDE.md`.

- [ ] **Step 7: Commit**

```bash
git add gradle.properties build.gradle.kts CHANGELOG.md CLAUDE.md
git commit -m "build: target IntelliJ Platform 2026.3" -m "Retarget platformVersion to 2026.3 with since/until build 263-263.*, upgrade the IntelliJ Platform Gradle Plugin to 2.20.1, and record the change in CHANGELOG and CLAUDE.md. The XML plugin dependency is added in a follow-up commit once a 2026.3 build confirms it is required."
```

---

### Task 2: Capture the first 2026.3 XML failure (gate)

**Files:** none modified. Output is recorded for the pull request body.

**Interfaces:**
- Consumes: the commit 1 tree from Task 1.
- Produces: the first diagnostic that names a missing XML type. Task 3 starts only if this exists.

- [ ] **Step 1: Compile against 2026.3**

Run: `./gradlew compileJava 2>&1 | tee /tmp/commit1-compile.log`
Expected: either `BUILD FAILED` with an XML-related error, or `BUILD SUCCESSFUL`.

- [ ] **Step 2: Search the log for XML diagnostics**

Run: `grep -nE "com\.intellij\.psi\.xml|com\.intellij\.xml|intellij\.xml\.structureView|Could not resolve|cannot find symbol" /tmp/commit1-compile.log | head -n 20`

- If a missing XML type appears (for example `package com.intellij.psi.xml does not exist`, or `intellij.xml.structureView` not resolved), copy the first matching line. Go to Task 3.
- If compilation succeeds, run `./gradlew test -x rat 2>&1 | tee /tmp/commit1-test.log` and search `/tmp/commit1-test.log` with the same `grep` pattern. If a `NoClassDefFoundError` for an XML class appears, copy the first line and go to Task 3.
- If the test run also has no XML failure, run `./gradlew runPluginVerifier 2>&1 | tee /tmp/commit1-verify.log` and search it with the same pattern.
- If none of the three runs shows a missing XML type, **stop**. Do not add the XML dependency. Report the three log summaries to the user and leave #150 open. Do not push the branch or open a pull request for commit 1 alone.

- [ ] **Step 3: Record the diagnostic**

Keep the first matching line in a local note for the pull request body (Task 5). Do not commit the log files.

---

### Task 2a: Resolve 2026.3 bundled and Marketplace plugin IDs (commit 1b)

**Why this task exists:** Task 2 found that `./gradlew compileJava` on 2026.3 stops at dependency resolution. An audit of `printBundledPlugins` and `printBundledModules` for 2026.3 found:

- `com.intellij.jsp` is no longer bundled with IntelliJ IDEA 2026.3. It is the JetBrains Marketplace plugin with the same ID (Jakarta EE: Server Pages, Ultimate subscription). Pin it to `263.7307.101` (since `263.7307`, until `263.*`). That plugin provides the `intellij.javaee.jsp.shared` module, so the bundled `intellij.javaee.jsp.shared` declaration is removed. The dependency is required, as approved by the user. Users without an Ultimate subscription cannot load this plugin.
- `com.intellij.freemarker` and `com.intellij.velocity` are not bundled in 2026.3. They are Marketplace plugins with the same IDs. Pin FreeMarker to `263.7307.112` and Velocity to `263.7307.101` (since `263.7307`, until `263.*`). Their optional `<depends>` entries in plugin.xml are unchanged.
- `com.intellij.structureView`, `com.intellij.structuralSearch`, and `com.intellij.todo` are the 2026.3 IDs. The build used the old `intellij.*.plugin` IDs.

**Files:**
- Modify: `build.gradle.kts`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Interfaces:**
- Consumes: the commit 1 tree from Task 1 (`014bd2f`).
- Produces: a build that resolves every declared ID on 2026.3 past dependency resolution, so Task 2's XML gate can be re-run.

- [ ] **Step 1: Update `build.gradle.kts`**

Replace these lines exactly. The working tree may already contain the FreeMarker, Velocity, and Structure View replacements from an earlier attempt. Keep those, and apply only what is missing:

```kotlin
        bundledModule("intellij.jsp")
        bundledModule("intellij.javaee.jsp.shared")
```
→
```kotlin
        plugin("com.intellij.jsp", "263.7307.101")
```

The second line is removed, not replaced. Keep the blank line structure around them.

If the working tree still has `bundledPlugin("com.intellij.jsp")` instead of `bundledModule("intellij.jsp")`, replace that line with the `plugin("com.intellij.jsp", "263.7307.101")` line above, and remove `bundledModule("intellij.javaee.jsp.shared")` in the same way.

These replacements must be present exactly once when you finish:

```kotlin
        plugin("com.intellij.freemarker", "263.7307.112")
        plugin("com.intellij.velocity", "263.7307.101")
        bundledPlugin("com.intellij.structureView")
```

In the `testBundledPlugins(...)` block, these entries must be present:

```kotlin
            "com.intellij.structureView",
            "com.intellij.todo",
            "com.intellij.structuralSearch",
```

Keep every other line unchanged.

- [ ] **Step 2: Update `plugin.xml`**

In `src/main/resources/META-INF/plugin.xml`, replace:

```xml
        <module name="intellij.jsp"/>
```
→
```xml
        <plugin id="com.intellij.jsp"/>
```

Keep `<module name="intellij.javaee.jsp.shared"/>` on the next line. If the verifier in Step 4 reports it as unresolved, remove it there and record that in the report.

Keep `<plugin id="com.intellij.structureView"/>`. Leave `<depends optional="true" ...>com.intellij.freemarker</depends>` and `...com.intellij.velocity</depends>` unchanged.

- [ ] **Step 3: Re-run compilation (XML gate re-run)**

Run: `./gradlew compileJava 2>&1 | tee .superpowers/sdd/2026-3-compile-2a.log`
Expected: `BUILD SUCCESSFUL`, or a failure that names an XML type (record it). A dependency-resolution error on any other ID is a new break: stop and report it. Do not fix it in this task.

- [ ] **Step 4: Verify with the plugin verifier**

Run: `./gradlew runPluginVerifier 2>&1 | tee .superpowers/sdd/2026-3-verify-2a.log`
Record the result. If the verifier reports `intellij.javaee.jsp.shared` as unresolved, remove that `<module>` line from plugin.xml and re-run the verifier, then record the change in the commit body.

- [ ] **Step 5: Commit**

```bash
git add build.gradle.kts src/main/resources/META-INF/plugin.xml
git commit -m "build: resolve IntelliJ 2026.3 plugin IDs" -m "JSP, FreeMarker, and Velocity are Marketplace plugins on 2026.3, so depend on com.intellij.jsp 263.7307.101 (required), com.intellij.freemarker 263.7307.112, and com.intellij.velocity 263.7307.101. Structure View, Structural Search, and TODO use their 2026.3 IDs."
```

Do not change `CHANGELOG.md` in this task. Task 3's changelog bullet covers the platform dependency changes.

---

### Task 3: Declare the XML plugin dependency (commit 2)

Run this task only when Task 2 recorded a missing XML type. If Task 2a's re-run shows a missing XML type, Task 2's gate result is satisfied and this task runs.

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml` (inside `<dependencies>`)
- Modify: `build.gradle.kts` (the `intellijPlatform` dependency block)
- Modify: `CHANGELOG.md` (one bullet under `### Changed` in `[Unreleased]`)

**Interfaces:**
- Consumes: the commit 1 tree and the diagnostic recorded in Task 2.
- Produces: a build where `com.intellij.xml` is resolved as a bundled plugin and `com.intellij.modules.xml` is a declared dependency.

- [ ] **Step 1: Add the plugin dependency in `plugin.xml`**

In `src/main/resources/META-INF/plugin.xml`, the `<dependencies>` block starts with:

```xml
    <dependencies>
        <plugin id="com.intellij.java"/>
```

Insert one line before `<plugin id="com.intellij.java"/>`:

```xml
        <plugin id="com.intellij.modules.xml"/>
```

The `<plugin>` element accepts a plugin ID or alias, and `com.intellij.modules.xml` is the alias covering the moved XML modules. Leave `<plugin id="intellij.structureView.plugin"/>` unchanged.

- [ ] **Step 2: Add the bundled plugin in `build.gradle.kts`**

In the `intellijPlatform { ... }` dependency block, the existing lines are:

```kotlin
        bundledPlugin("com.intellij.jsp")
        bundledModule("intellij.javaee.jsp.shared")
```

Insert after the `bundledPlugin("com.intellij.jsp")` line:

```kotlin
        bundledPlugin("com.intellij.xml")
```

- [ ] **Step 3: Re-check the structure-view modules**

Run: `./gradlew compileJava 2>&1 | tee /tmp/commit2-compile.log`

- If the log shows `intellij.xml.structureView` or `intellij.xml.structureView.impl` as unresolved, delete these two lines from `build.gradle.kts` and re-run the command:

```kotlin
        bundledModule("intellij.xml.structureView")
        bundledModule("intellij.xml.structureView.impl")
```

- If they resolve, keep both lines.

- [ ] **Step 4: Confirm the XML error is gone**

Run: `grep -nE "com\.intellij\.psi\.xml|com\.intellij\.xml|intellij\.xml\.structureView" /tmp/commit2-compile.log`
Expected: no matches. Then run `./gradlew compileJava` and expect `BUILD SUCCESSFUL`.

- [ ] **Step 5: Test fallback for `NoClassDefFoundError`**

Run: `./gradlew test -x rat 2>&1 | tee /tmp/commit2-test.log`

If a test fails with `NoClassDefFoundError` for an XML class, add this line inside `testBundledPlugins(...)` in `build.gradle.kts`, next to the existing `"com.intellij.diagram"` entry:

```kotlin
            "com.intellij.xml",
```

Re-run `./gradlew test -x rat` and expect `BUILD SUCCESSFUL`. If there is no such error, leave `testBundledPlugins` unchanged.

- [ ] **Step 6: Add the changelog bullet**

In `CHANGELOG.md`, under `## [Unreleased]` → `### Changed`, after the `Dependencies - upgrade org.jetbrains.intellij.platform to 2.20.1` bullet, add:

```markdown
- Dependencies - declare the IntelliJ Platform 2026.3 XML plugin (`com.intellij.xml` / `com.intellij.modules.xml`) ([#150](https://github.com/apache/struts-intellij-plugin/issues/150))
```

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/META-INF/plugin.xml build.gradle.kts CHANGELOG.md
git commit -m "build: declare IntelliJ 2026.3 XML plugin dependency" -m "IntelliJ Platform 2026.3 moved XML and HTML into the bundled com.intellij.xml plugin. Declare com.intellij.modules.xml in plugin.xml and bundledPlugin(\"com.intellij.xml\") in Gradle. Refs #150."
```

---

### Task 4: Fix any further 2026.3 breaks, one commit each

Run this task only when `./gradlew test -x rat` or `./gradlew runPluginVerifier` fails after Task 3 for a reason other than the XML dependency.

**Files:** only the production or test file that the failure names.

- [ ] **Step 1: Identify the failure**

Run `./gradlew test -x rat 2>&1 | tee /tmp/fix-test.log` and `./gradlew runPluginVerifier 2>&1 | tee /tmp/fix-verify.log`. For each failure, record the test name or verifier problem and the file it names.

- [ ] **Step 2: Fix only enabled tests and Marketplace-blocking problems**

Change only what the failing enabled test or verifier problem needs. Do not re-enable a disabled test (an underscore-prefixed method or a placeholder). If the failure is in a disabled test, leave it disabled and note it in the pull request.

- [ ] **Step 3: Re-run the failing command**

Run the same command from Step 1 and expect `BUILD SUCCESSFUL` (tests) or no blocking problems (verifier).

- [ ] **Step 4: Commit each fix separately**

Use `fix:` for a behavior change and `build:` for a build configuration change, with a body that names the error:

```bash
git add <changed-files>
git commit -m "fix: <what the 2026.3 break was>" -m "<the first error line>"
```

---

### Task 5: Verify, push, open the pull request, and file the Qodana follow-up

**Files:** none modified.

- [ ] **Step 1: Final verification on the branch tip**

Run: `./gradlew test -x rat`
Expected: `BUILD SUCCESSFUL`.

Run: `./gradlew runPluginVerifier`
Expected: no Marketplace-blocking incompatibilities for `263.*`.

If either fails, return to Task 4. Do not push until both pass.

- [ ] **Step 2: Confirm the branch and history**

Run: `git status && git log --oneline -5`
Expected: a clean tree on `build/intellij-2026-3`, with the spec commit (`docs: add IntelliJ 2026.3 compatibility design`), the plan commit, then the commit 1 and commit 2 changes.

- [ ] **Step 3: Push the branch**

Run: `git push -u origin build/intellij-2026-3`

- [ ] **Step 4: Open the pull request**

Write the body to a file so the quoted error stays exact:

```bash
cat > /tmp/pr-body-2026-3.md <<'EOF'
## Summary

Targets IntelliJ IDEA 2026.3 (build `263`, range `263`–`263.*`) and declares the XML plugin dependency that 2026.3 requires.

Closes #149. Closes #150.

## Why the XML dependency is needed

Commit 1 (`build: target IntelliJ Platform 2026.3`) was built with the XML dependency absent. The first 2026.3 failure was:

```
<paste the first line recorded in Task 2>
```

## Verification

- `./gradlew test -x rat`: BUILD SUCCESSFUL
- `./gradlew runPluginVerifier`: no Marketplace-blocking incompatibilities for `263.*`

## Not in this PR

- #151 (OGNL `colorSettingsPage` migration)
- Qodana linter and `qodana-action` pins (tracked in a follow-up issue)
EOF
gh pr create --base main --head build/intellij-2026-3 --title "build: target IntelliJ Platform 2026.3" --body-file /tmp/pr-body-2026-3.md
```

Replace the `<paste ...>` placeholder with the Task 2 line before running the command.

- [ ] **Step 5: File the Qodana follow-up issue**

```bash
gh issue create --label dependencies --title "Point Qodana at the 2026.3 JVM linter when published" --body "$(cat <<'EOF'
## Context

`qodana.yml` still uses `jetbrains/qodana-jvm:2026.2-eap`, and `.github/workflows/build.yml` pins `JetBrains/qodana-action` to `v2026.2.1`. No `jetbrains/qodana-jvm:2026.3` image or `qodana-action` `v2026.3` is published yet.

## Proposed change

When both exist, set `linter: jetbrains/qodana-jvm:2026.3` in `qodana.yml` and update the `qodana-action` pin in `.github/workflows/build.yml`. `projectJDK: 25` is already correct.

Tracked with #149. Spec: `docs/superpowers/specs/2026-11-13-intellij-2026-3-compatibility-design.md`.
EOF
)"
```

- [ ] **Step 6: Link the pull request from #149**

Run `gh issue comment 149 --body "Implementation PR: <pull request URL from Step 4>"`, using the URL that `gh pr create` printed.

---

## Self-Review

**Spec coverage**

- Commit 1, retarget versions, Gradle plugin, changelog, `CLAUDE.md` → Task 1.
- Confirm the XML break before adding the dependency, with the stop rule → Task 2.
- Commit 2, `com.intellij.modules.xml` and `bundledPlugin("com.intellij.xml")`, structure-view modules resolved again, test fallback → Task 3.
- Further 2026.3 breaks as separate commits, enabled tests only → Task 4.
- Verification (`test -x rat`, `runPluginVerifier`), push only when green, PR body quoting the commit 1 error → Task 5 Steps 1–4.
- Qodana follow-up issue, pins unchanged → Task 5 Step 5 and Global Constraints.
- `ognl.xml` unchanged, #151 out of scope → Global Constraints.
- Success criteria are covered by Task 1 Step 6, Task 3 Step 4, and Task 5 Step 1.

**Placeholder scan**

Task 4 describes a procedure rather than code because the failures are not known until Task 2 and Task 3 run. Task 5 Step 4 has one `<paste ...>` marker that is filled from Task 2's recorded output; it is not a code gap.

**Type consistency**

Identifiers match across tasks: `com.intellij.modules.xml`, `com.intellij.xml`, `build/intellij-2026-3`, `263.19043.1`, and the `/tmp` log names used in Task 2 and Task 3.
