# IntelliJ IDEA 2026.2 (262) Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Apache Struts IntelliJ plugin build, verify, and test cleanly against IntelliJ IDEA 2026.2 (build branch 262) with Java 25, without cutting a Marketplace release.

**Architecture:** Sequential checklist upgrade: bump platform/compatibility properties and Java 25 tooling first, then iterate compile → plugin verifier → enabled unit tests, fixing only what 262 breaks. Document the bump in CHANGELOG and CLAUDE; leave Marketplace publish for a separate release.

**Tech Stack:** IntelliJ Platform Gradle Plugin 2.x, Gradle 9, Java 25, JetBrains Plugin Verifier, GitHub Actions (Zulu JDK), Qodana JVM linter.

**Spec:** `docs/superpowers/specs/2026-07-24-intellij-2026-2-compatibility-design.md`

## Global Constraints

- Target IDEA **2026.2 only** (`pluginSinceBuild = 262`, `pluginUntilBuild = 262.*`, `platformVersion = 2026.2`).
- Do **not** keep 2026.1 in the compatibility range.
- Use **Java 25** for `jvmToolchain`, all workflow `java-version` values, and `qodana.yml` `projectJDK`.
- Do **not** run prepare/publish release workflows as part of this work.
- Do **not** re-enable historically disabled / underscore-prefixed tests unless they block the upgrade.
- Bump IntelliJ Platform Gradle Plugin / Gradle / other tooling **only if** compile, tests, or verifier require it; record any forced bump in CHANGELOG.
- If JetBrains’ official build-number table or verifier disagrees with branch `262`, use the official source of truth (do not invent branch numbers).
- Prefer public APIs; fix Marketplace-blocking verifier issues before merging.

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `gradle.properties` | Modify | `pluginVersion` prefix `262`, since/until `262`/`262.*`, `platformVersion = 2026.2` |
| `build.gradle.kts` | Modify | `jvmToolchain(25)`; optional tooling version bump only if required |
| `.github/workflows/build.yml` | Modify | All `java-version: 21` → `25` |
| `.github/workflows/nightly.yml` | Modify | `java-version: 21` → `25` |
| `.github/workflows/prepare_release.yml` | Modify | `java-version: 21` → `25` |
| `.github/workflows/release.yml` | Modify | `java-version: 21` → `25` |
| `qodana.yml` | Modify | `projectJDK: 25`; linter image → 2026.2 when available |
| Production/test sources under `src/` | Modify only if needed | Compile, verifier, or enabled-test fixes for 262 |
| `CHANGELOG.md` | Modify | Unreleased Changed entries for platform + Java |
| `CLAUDE.md` | Modify | Platform version mapping rows for 261/262 |

No new modules, extension points, or feature code.

---

### Task 1: Platform version and Java 25 configuration

**Files:**
- Modify: `gradle.properties`
- Modify: `build.gradle.kts` (toolchain line only unless a dependency bump is forced later)
- Modify: `.github/workflows/build.yml`
- Modify: `.github/workflows/nightly.yml`
- Modify: `.github/workflows/prepare_release.yml`
- Modify: `.github/workflows/release.yml`
- Modify: `qodana.yml`

**Interfaces:**
- Consumes: current `pluginVersion = 261.19039.1`, `platformVersion = 2026.1`, Java 21 settings
- Produces: project configured for `platformVersion = 2026.2`, compatibility `262`–`262.*`, `pluginVersion` prefix `262`, Java 25 everywhere tooling runs

- [ ] **Step 1: Confirm local JDK 25 is available**

Run:

```bash
java -version 2>&1 | head -5
/usr/libexec/java_home -V 2>&1 | grep -E '25|21' || true
```

Expected: a JDK 25 install usable by Gradle toolchains (Temurin/Zulu/Oracle all fine). If missing, install JDK 25 before continuing.

- [ ] **Step 2: Update `gradle.properties`**

Replace the version block so it reads:

```properties
# SemVer format -> https://semver.org
pluginVersion = 262.19039.1

# Supported build number ranges and IntelliJ Platform versions -> https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html
pluginSinceBuild = 262
pluginUntilBuild = 262.*

# IntelliJ Platform Properties -> https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#configuration-intellij-extension
platformVersion = 2026.2
```

Keep `gradleVersion` and other properties unchanged. Keep BUILD (`19039`) and FIX (`1`) unless `main` has moved further — then preserve whatever BUILD/FIX is current and only change the branch prefix from `261` to `262`.

- [ ] **Step 3: Switch JVM toolchain to 25**

In `build.gradle.kts`, change:

```kotlin
kotlin {
    jvmToolchain(21)
}
```

to:

```kotlin
kotlin {
    jvmToolchain(25)
}
```

Do not bump `org.jetbrains.intellij.platform` or Gradle in this step unless Step 6 proves it is required.

- [ ] **Step 4: Update GitHub Actions Java versions**

In each of these files, replace every `java-version: 21` with `java-version: 25` (leave `distribution: zulu` as-is):

- `.github/workflows/build.yml` (4 occurrences)
- `.github/workflows/nightly.yml` (1 occurrence)
- `.github/workflows/prepare_release.yml` (1 occurrence)
- `.github/workflows/release.yml` (1 occurrence)

Verify with:

```bash
rg -n "java-version:" .github/workflows
```

Expected: every listed workflow shows `java-version: 25`; no remaining `java-version: 21`.

- [ ] **Step 5: Update Qodana config**

In `qodana.yml`, set:

```yaml
linter: jetbrains/qodana-jvm:2026.2
projectJDK: 25
```

If the stable `2026.2` image tag is not pullable, use `jetbrains/qodana-jvm:2026.2-eap` instead. Only keep `jetbrains/qodana-jvm:2026.1` if neither 2026.2 tag works; if so, still set `projectJDK: 25` and note the linter lag in the Task 4 CHANGELOG entry.

- [ ] **Step 6: Resolve the IntelliJ Platform and compile**

Run:

```bash
./gradlew --stop
./gradlew build -x test -x rat --no-configuration-cache
```

Expected: BUILD SUCCESSFUL after downloading IDEA 2026.2 artifacts.

If resolution fails on `platformVersion = 2026.2`, check https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html and JetBrains artifact repositories; adjust only if the published platform coordinate differs (still targeting 2026.2 / branch 262).

If compile fails with API errors:

1. Open https://jb.gg/intellij-api-changes (2026.2 section).
2. Fix call sites with the smallest public-API replacement.
3. Do **not** reintroduce 2026.1 compatibility shims.
4. Re-run the same `build -x test -x rat` command until green.

If the IntelliJ Platform Gradle Plugin itself cannot resolve/run against 2026.2, bump `id("org.jetbrains.intellij.platform")` in `build.gradle.kts` to the minimum working 2.x version and record that version for the CHANGELOG task.

- [ ] **Step 7: Confirm patched plugin XML range**

Run:

```bash
./gradlew patchPluginXml --no-configuration-cache
rg -n "since-build|until-build|<version>" build/patchedPluginXmlFiles/plugin.xml
```

Expected: `since-build="262"`, `until-build="262.*"`, and version starting with `262.`.

- [ ] **Step 8: Commit configuration**

```bash
git add gradle.properties build.gradle.kts \
  .github/workflows/build.yml \
  .github/workflows/nightly.yml \
  .github/workflows/prepare_release.yml \
  .github/workflows/release.yml \
  qodana.yml
# include any compile-fix source files from Step 6 if present
git add -u src || true
git commit -m "$(cat <<'EOF'
build: target IntelliJ 2026.2 (262) with Java 25

EOF
)"
```

---

### Task 2: Plugin verifier against 2026.2

**Files:**
- Modify: production sources under `src/main/` only if verifier reports blocking incompatibilities
- Modify: `build.gradle.kts` only if verifier tooling/config must change

**Interfaces:**
- Consumes: Task 1 platform/Java configuration and successful compile
- Produces: `verifyPlugin` clean for recommended IDE(s) covering 262 / 2026.2 (no blocking incompatibilities)

- [ ] **Step 1: Run plugin verification**

Run (matches CI task name):

```bash
./gradlew verifyPlugin --no-configuration-cache
```

Expected: task succeeds; report under `build/reports/pluginVerifier/` has no Compatibility Problems that block Marketplace for IDEA 2026.2 / build 262.

Note: CLAUDE.md also mentions `runPluginVerifier`; if `verifyPlugin` is unavailable, use `./gradlew runPluginVerifier --no-configuration-cache` instead. Prefer the task that CI uses (`verifyPlugin`).

- [ ] **Step 2: Triage failures**

If verification fails:

1. Open the generated report HTML/text under `build/reports/pluginVerifier/`.
2. Fix **Compatibility Problems** and other Marketplace-blocking findings first.
3. Prefer public API replacements over `@Suppress` / muting new problem types.
4. Do not mute `ForbiddenPluginIdPrefix` / `TemplateWordInPluginId` beyond the existing `freeArgs` in `build.gradle.kts`.
5. Re-run Step 1 until green.

If the only failures are pre-existing muted ID-prefix warnings, leave the existing mutes; do not expand mute scope without documenting why in the commit message.

- [ ] **Step 3: Commit verifier fixes (skip if none)**

If source or build config changed:

```bash
git add -u
git commit -m "$(cat <<'EOF'
fix: resolve Plugin Verifier issues for IDEA 2026.2

EOF
)"
```

If nothing changed, skip the commit.

---

### Task 3: Enabled unit tests on 2026.2

**Files:**
- Modify: `src/test/**` and/or `src/main/**` only for failures in currently enabled tests
- Do not re-enable underscore-prefixed / placeholder disabled tests

**Interfaces:**
- Consumes: Task 1–2 green build and verifier
- Produces: `./gradlew test -x rat` green for the currently enabled suite

- [ ] **Step 1: Run the unit test suite**

Run:

```bash
./gradlew test -x rat --no-configuration-cache
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Fix only enabled-test failures**

If tests fail:

1. Identify whether the failure is a real product regression vs test-infra (paths, fixtures, deprecated test APIs).
2. For path / fixture issues, prefer project-relative overrides already documented in `CLAUDE.md` (`getBasePath()`, `getTestDataPath()`, `"src/test/testData/..."`).
3. For API/behavior changes, apply the smallest production or test fix.
4. Leave historically disabled suites alone (examples: `OgnlLexerTest`, `StrutsStructureViewTest`, `StrutsHighlightingSpringTest`, `ActionLinkReferenceProviderTest`, `ResultActionPropertyTest` placeholder/`_` patterns).
5. Re-run:

```bash
./gradlew test -x rat --no-configuration-cache
```

until green. For a single failing class while iterating:

```bash
./gradlew test -x rat --tests "FullyQualifiedTestClassName" --no-configuration-cache
```

- [ ] **Step 3: Commit test fixes (skip if none)**

```bash
git add -u
git commit -m "$(cat <<'EOF'
fix: adapt tests for IntelliJ 2026.2

EOF
)"
```

Skip if Task 1 configuration alone left tests green.

---

### Task 4: Documentation

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: final versions/tooling choices from Tasks 1–3 (including any forced dependency bump or Qodana image tag)
- Produces: Unreleased changelog notes and updated platform mapping table

- [ ] **Step 1: Update `CHANGELOG.md` Unreleased section**

Under `## [Unreleased]`, ensure a `### Changed` section exists and includes (adjust versions to what was actually landed):

```markdown
### Changed

- Update `platformVersion` to `2026.2`
- Change since/until build to `262-262.*` (2026.2 only)
- Upgrade JVM toolchain and CI Java version to `25`
```

If Task 1 bumped the IntelliJ Platform Gradle Plugin or Qodana image, add matching lines, for example:

```markdown
- Dependencies - upgrade `org.jetbrains.intellij.platform` to `<version>`
- Dependencies - upgrade Qodana linter image to `jetbrains/qodana-jvm:2026.2` (or `2026.2-eap`)
```

Do not invent feature bullets. Do not create a dated release section (no Marketplace release in this work).

- [ ] **Step 2: Update `CLAUDE.md` platform mapping**

In the “IntelliJ Platform Version Mapping” table, ensure rows exist for recent branches. Replace the stale end of the table so it includes at least:

```markdown
| Branch | Platform Version | Build Range |
|--------|------------------|-------------|
| 242.x  | 2024.2           | 242.*       |
| 243.x  | 2024.3           | 243.*       |
| 251.x  | 2025.1           | 251.*       |
| 252.x  | 2025.2           | 252.*       |
| 253.x  | 2025.3           | 253.*       |
| 261.x  | 2026.1           | 261.*       |
| 262.x  | 2026.2           | 262.*       |
```

In “Platform Upgrade Checklist” / tooling notes, if Java guidance still says older JDK requirements for the current target, align the Java bullet with **Java 25 for 2026.2+**.

- [ ] **Step 3: Commit docs**

```bash
git add CHANGELOG.md CLAUDE.md
git commit -m "$(cat <<'EOF'
docs: record IntelliJ 2026.2 and Java 25 upgrade

EOF
)"
```

---

### Task 5: Final verification gate

**Files:**
- None expected (read-only verification)

**Interfaces:**
- Consumes: Tasks 1–4 complete on the feature branch
- Produces: evidence that success criteria from the spec are met before merge/PR

- [ ] **Step 1: Re-run the full local gate**

```bash
./gradlew build -x rat --no-configuration-cache
./gradlew verifyPlugin --no-configuration-cache
```

Expected: both succeed. (`build` here includes tests; `-x rat` keeps Apache RAT out of the local gate as in normal contributor workflow.)

- [ ] **Step 2: Sanity-check key properties**

```bash
rg -n "^(pluginVersion|pluginSinceBuild|pluginUntilBuild|platformVersion)\s*=" gradle.properties
rg -n "jvmToolchain" build.gradle.kts
rg -n "java-version:" .github/workflows
rg -n "^(linter|projectJDK):" qodana.yml
rg -n "262\.x|2026\.2" CLAUDE.md CHANGELOG.md
```

Expected:

- `pluginVersion` starts with `262.`
- `pluginSinceBuild = 262`, `pluginUntilBuild = 262.*`
- `platformVersion = 2026.2`
- `jvmToolchain(25)`
- all workflow `java-version: 25`
- `projectJDK: 25`
- CHANGELOG/CLAUDE mention 2026.2 / 262

- [ ] **Step 3: Stop — do not release**

Do **not** run `prepare_release` / `release` workflows and do **not** publish to Marketplace. Hand off a merge-ready branch/PR only.

---

## Spec Coverage Checklist

| Spec requirement | Task |
|---|---|
| 2026.2-only compatibility (`262` / `262.*`) | Task 1 |
| `platformVersion = 2026.2` | Task 1 |
| Java 25 toolchain + CI + Qodana JDK | Task 1 |
| Compile / API fixes as needed | Task 1 |
| Plugin verifier green | Task 2 |
| Enabled unit tests green (`test -x rat`) | Task 3 |
| CHANGELOG + CLAUDE updates | Task 4 |
| No Marketplace release in this work | Task 5 |
| No re-enable of disabled historical tests | Task 3 |
| Official branch numbers if docs disagree | Task 1 Step 6 |
