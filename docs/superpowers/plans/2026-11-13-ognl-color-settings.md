# OGNL Color Settings Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the OGNL color settings page from the deprecated `com.intellij.colorSettingsPage` extension to the declarative `com.intellij.colorSettings` extension, with page id and display name `OGNL`.

**Architecture:** The page class `OgnlColorSettingsPage` stays where it is and gains one override, `getId()`, returning `OgnlLanguage.ID`. `META-INF/ognl.xml` replaces the old registration with a `colorSettings` declaration whose `displayName` and `pageId` match the class. Descriptors, demo text, highlighter, and icon are unchanged, so saved OGNL colors keep working.

**Tech Stack:** Java 25, IntelliJ Platform 2026.3 (`platformVersion = 2026.3`, since-build `263`), IntelliJ Platform Gradle Plugin 2.20.1, JUnit 3-style `LightPlatformTestCase`, Gradle.

**Spec:** `docs/superpowers/specs/2026-11-13-ognl-color-settings-design.md`

## Global Constraints

- Do not change `platformVersion`, `pluginSinceBuild`, `pluginUntilBuild`, or `pluginVersion` in `gradle.properties`.
- Do not change `build.gradle.kts`, `qodana.yml`, or `.github/workflows/build.yml`.
- Do not register `com.intellij.colorSettingsPage` anywhere, not even beside the new registration.
- Do not implement `DisplayPrioritySortable` and do not set `priority` or `groupWeight` in XML.
- Do not change attribute descriptor labels, attribute keys on `OgnlHighlighter`, the demo text, the highlighter, the icon, or `getColorDescriptors()`.
- Do not add a message bundle for the page name.
- Do not re-enable disabled tests.
- Commit message format: `<type>: <description> (#151)`.

---

### Task 1: Page id `OGNL` on `OgnlColorSettingsPage`

**Files:**
- Create: `src/test/java/com/intellij/lang/ognl/highlight/OgnlColorSettingsPageTest.java`
- Modify: `src/main/java/com/intellij/lang/ognl/highlight/OgnlColorSettingsPage.java:51-55` (insert after `getDisplayName()`)
- Test: `src/test/java/com/intellij/lang/ognl/highlight/OgnlColorSettingsPageTest.java`

**Interfaces:**
- Consumes: `OgnlLanguage.ID` (`"OGNL"`) from `com.intellij.lang.ognl.OgnlLanguage`.
- Produces: `OgnlColorSettingsPage#getId()` returning `String` `"OGNL"`. Task 2 must declare `pageId="OGNL"` to match.

- [ ] **Step 1: Write the test class**

Create `src/test/java/com/intellij/lang/ognl/highlight/OgnlColorSettingsPageTest.java`. Copy the Apache license header from lines 1–16 of `src/test/java/com/intellij/lang/ognl/highlight/OgnlHighlightingLexerTest.java` verbatim, then add:

```java
package com.intellij.lang.ognl.highlight;

import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.testFramework.LightPlatformTestCase;

import java.util.Arrays;
import java.util.List;

public class OgnlColorSettingsPageTest extends LightPlatformTestCase {

  private final OgnlColorSettingsPage myPage = new OgnlColorSettingsPage();

  public void testId() {
    assertEquals("OGNL", myPage.getId());
  }

  public void testDisplayName() {
    assertEquals("OGNL", myPage.getDisplayName());
  }

  public void testAttributeDescriptorsInOrder() {
    List<String> names = Arrays.stream(myPage.getAttributeDescriptors())
      .map(AttributesDescriptor::getDisplayName)
      .toList();

    assertEquals(List.of("Background",
                         "Expression bounds",
                         "Keyword",
                         "Operations",
                         "Identifier",
                         "String",
                         "Number",
                         "Comma",
                         "Parentheses",
                         "Brackets",
                         "Braces",
                         "Class reference"), names);
  }

  public void testNoColorDescriptors() {
    assertEquals(0, myPage.getColorDescriptors().length);
  }
}
```

- [ ] **Step 2: Run the test to verify `testId` fails**

Run: `./gradlew test --tests "com.intellij.lang.ognl.highlight.OgnlColorSettingsPageTest" -x rat`
Expected: FAIL in `testId` with `expected:<OGNL> but was:<com.intellij.lang.ognl.highlight.OgnlColorSettingsPage>`. The other three tests PASS: they guard behavior that must not change.

- [ ] **Step 3: Add `getId()` to the page class**

In `src/main/java/com/intellij/lang/ognl/highlight/OgnlColorSettingsPage.java`, insert after the closing brace of `getDisplayName()` (currently line 55), keeping one blank line before the next method:

```java
  @NotNull
  @Override
  public String getId() {
    return OgnlLanguage.ID;
  }
```

`OgnlLanguage` is already imported in this file.

- [ ] **Step 4: Run the test to verify all four pass**

Run: `./gradlew test --tests "com.intellij.lang.ognl.highlight.OgnlColorSettingsPageTest" -x rat`
Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/intellij/lang/ognl/highlight/OgnlColorSettingsPageTest.java src/main/java/com/intellij/lang/ognl/highlight/OgnlColorSettingsPage.java
git commit -m "refactor: give OGNL color settings page id OGNL (#151)"
```

---

### Task 2: Declarative `colorSettings` registration

**Files:**
- Modify: `src/main/resources/META-INF/ognl.xml:30` (replace the `colorSettingsPage` line)

**Interfaces:**
- Consumes: `OgnlColorSettingsPage#getId()` returning `"OGNL"` and `getDisplayName()` returning `"OGNL"` (Task 1).
- Produces: a `com.intellij.colorSettings` declaration with `implementation`, `displayName`, and `pageId`, all matching the class.

- [ ] **Step 1: Confirm the old registration is present**

Run: `grep -n "colorSettingsPage" src/main/resources/META-INF/ognl.xml`
Expected: one match at line 30: `<colorSettingsPage implementation="com.intellij.lang.ognl.highlight.OgnlColorSettingsPage"/>`

- [ ] **Step 2: Replace the registration**

In `src/main/resources/META-INF/ognl.xml`, replace line 30:

```xml
    <colorSettingsPage implementation="com.intellij.lang.ognl.highlight.OgnlColorSettingsPage"/>
```

with:

```xml
    <colorSettings implementation="com.intellij.lang.ognl.highlight.OgnlColorSettingsPage"
                   displayName="OGNL"
                   pageId="OGNL"/>
```

- [ ] **Step 3: Verify the old registration is gone**

Run: `grep -c "colorSettingsPage" src/main/resources/META-INF/ognl.xml`
Expected: `0` (the command prints `0` and exits with status 1, which is the expected result of `grep -c` with no matches).

- [ ] **Step 4: Run the page test and the plugin build check**

Run: `./gradlew test --tests "com.intellij.lang.ognl.highlight.OgnlColorSettingsPageTest" -x rat`
Expected: PASS, 4 tests.

Run: `./gradlew buildPlugin`
Expected: BUILD SUCCESSFUL. The packaged `plugin.xml` in the build output contains the `colorSettings` element and no `colorSettingsPage` element.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/META-INF/ognl.xml
git commit -m "refactor: register OGNL color settings with colorSettings extension (#151)"
```

---

### Task 3: Changelog entry

**Files:**
- Modify: `CHANGELOG.md` (under `## [Unreleased]` → `### Changed`, after the FreeMarker/Velocity bullet at line 18)

**Interfaces:**
- Consumes: nothing.
- Produces: nothing.

- [ ] **Step 1: Add the bullet**

In `CHANGELOG.md`, under `## [Unreleased]` → `### Changed`, add this line immediately after the bullet that starts with `- Dependencies - FreeMarker and Velocity are now Marketplace plugins`:

```markdown
- Migrate the OGNL color settings page to the declarative `com.intellij.colorSettings` extension ([#151](https://github.com/apache/struts-intellij-plugin/issues/151))
```

- [ ] **Step 2: Verify the placement**

Run: `sed -n '10,22p' CHANGELOG.md`
Expected: the new bullet sits inside the `### Changed` list of `## [Unreleased]`, above the `## [262.19039.1]` heading.

- [ ] **Step 3: Commit**

```bash
git add CHANGELOG.md
git commit -m "docs: changelog for OGNL color settings migration (#151)"
```

Note: the 2026.3 compatibility work on another branch also edits this same `Changed` list. When both land, keep both bullets.

---

### Task 4: Full verification (no commit)

**Files:** none modified.

**Interfaces:**
- Consumes: the commits from Tasks 1–3.
- Produces: evidence for the acceptance checks in the spec.

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew test -x rat`
Expected: BUILD SUCCESSFUL. No new failures compared with `main`.

- [ ] **Step 2: Run the plugin verifier**

Run: `./gradlew runPluginVerifier`
Expected: the run completes successfully. Check the output for the deprecated extension point:

Run: `./gradlew runPluginVerifier 2>&1 | grep -i "colorSettingsPage"`
Expected: no output.

- [ ] **Step 3: Manual check in the sandbox IDE**

Run: `./gradlew runIde`

1. Open Settings | Editor | Color Scheme.
2. Select the OGNL entry. Expected: it appears in the tree and its name is `OGNL`.
3. Confirm the twelve descriptors are listed in this order: Background, Expression bounds, Keyword, Operations, Identifier, String, Number, Comma, Parentheses, Brackets, Braces, Class reference.
4. Change the Keyword foreground color, click Apply.
5. Open an OGNL fragment, for example in a `.action` or JSP expression `%{...}`, and confirm keywords use the new color.

Expected: all five checks pass. Record any failure before continuing.

- [ ] **Step 4: Confirm the branch state**

Run: `git log --oneline main..HEAD`
Expected: the spec commit, the plan commit, and the three commits from Tasks 1–3, in that order.

---

## Self-Review Notes

- Spec coverage: registration (Task 2), page id and `getId()` (Task 1), unchanged behavior (Task 1 regression tests and Task 4 manual check), changelog (Task 3), automated tests and verifier (Tasks 1, 2, 4), manual Settings check (Task 4).
- Non-goals: no tasks touch `gradle.properties`, `build.gradle.kts`, priority or weight attributes, a second `colorSettingsPage` registration, or a message bundle.
- Names: `getId()`, `getDisplayName()`, and `pageId="OGNL"` use the same value `"OGNL"` in every task.
