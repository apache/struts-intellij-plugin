# IntelliJ IDEA 2026.3 (263) Compatibility

**Date:** 2026-11-13
**Status:** Draft for review
**Issues:** [#149](https://github.com/apache/struts-intellij-plugin/issues/149), [#150](https://github.com/apache/struts-intellij-plugin/issues/150)

## Problem

The plugin targets IntelliJ IDEA **2026.2** only (`platformVersion = 2026.2`, builds `262`–`262.*`). The current platform release is **2026.3**, build **263.7307.200**.

In 2026.3, XML and HTML left the core plugin and became the bundled plugin `com.intellij.xml`. This plugin's `struts.xml` DOM, taglibs, and XML structure view use those classes and do not declare that dependency. JetBrains documents `com.intellij.modules.xml` as the alias that covers the moved modules, including `intellij.xml.psi`, `intellij.xml.dom`, and `intellij.xml.structureView`.

The upgrade must show that break on a 2026.3 build before the dependency is added.

## Goals

1. Make the plugin installable only on IntelliJ IDEA **2026.3** (build branch **263**).
2. Build against `platformVersion = 2026.3` with IntelliJ Platform Gradle Plugin **2.20.1**.
3. Declare the XML plugin dependency only after a local 2026.3 build reports a missing XML type.
4. Leave `./gradlew test -x rat` and `./gradlew runPluginVerifier` green for the enabled test suite on `263.*`.
5. Record the platform bump and the XML dependency in `CHANGELOG.md`, and add the `263` row to the platform table in `CLAUDE.md`.

## Non-Goals

- [#151](https://github.com/apache/struts-intellij-plugin/issues/151): migrating the OGNL color settings page off `colorSettingsPage`.
- Changing `qodana.yml` or the `qodana-action` pin in `.github/workflows/build.yml`. No `jetbrains/qodana-jvm:2026.3` image and no `qodana-action` `v2026.3` are published. A follow-up issue tracks that move.
- Supporting 2026.2 and 2026.3 in one plugin build.
- Cutting a Marketplace release.
- Re-enabling disabled tests.
- Changing the Java toolchain or CI Java version (already 25) or the Gradle version (already 9.0.0).
- Replacing `intellij.structureView.plugin`. That id remains an alias of `com.intellij.structureView`.

## Decision

Use two commits on one branch. The pull request is opened only when the branch tip is green.

1. **Commit 1 — platform target.** Retarget versions and the Gradle plugin. Do not add the XML dependency. Run a local build and keep the first XML error.
2. **Commit 2 — XML dependency.** Add the dependency that the captured error justifies. Re-run tests and the plugin verifier.

Further 2026.3 compile or test breaks are additional commits on the same branch. They are limited to what `263` breaks in enabled tests. The branch is not pushed, and the pull request is not opened, until `./gradlew test -x rat` and `./gradlew runPluginVerifier` pass.

If `./gradlew compileJava`, `./gradlew test -x rat`, and `./gradlew runPluginVerifier` all complete without a missing XML type, stop. Do not add the XML dependency on speculation.

## Commit 1 — Platform Target

### `gradle.properties`

| Property | From | To |
|---|---|---|
| `platformVersion` | `2026.2` | `2026.3` |
| `pluginSinceBuild` | `262` | `263` |
| `pluginUntilBuild` | `262.*` | `263.*` |
| `pluginVersion` | `262.19043.1` | `263.19043.1` |

Change the branch prefix only. Build number `19043` and fix number `1` stay. Nightly and release workflows own later build bumps.

If the plugin verifier disagrees with branch `263`, use JetBrains' build-number table and adjust `pluginSinceBuild`, `pluginUntilBuild`, and the version prefix. Do not invent a branch number.

### `build.gradle.kts`

Upgrade `org.jetbrains.intellij.platform` from `2.18.1` to `2.20.1`.

Leave these lines unchanged in this commit:

```kotlin
bundledModule("intellij.xml.structureView")
bundledModule("intellij.xml.structureView.impl")
```

Do not add `bundledPlugin("com.intellij.xml")` in this commit.

### Docs in this commit

Under `CHANGELOG.md` `[Unreleased]` → `Changed`:

- Update `platformVersion` to `2026.3`
- Change since/until build to `263-263.*` (2026.3 only)
- Dependencies - upgrade `org.jetbrains.intellij.platform` to `2.20.1`

In `CLAUDE.md`, add a row to the platform version table:

| Branch | Platform Version | Build Range |
|---|---|---|
| `263.x` | `2026.3` | `263.*` |

### Confirm the XML break

Run `./gradlew compileJava` on this commit.

- On failure, keep the first diagnostic that names `com.intellij.psi.xml`, `com.intellij.xml`, `intellij.xml.structureView`, or `intellij.xml.structureView.impl`. That text goes in the pull request body.
- If compilation succeeds, run `./gradlew test -x rat`, then `./gradlew runPluginVerifier`, and keep the first failure from those runs the same way.
- If none of the three commands fail on a missing XML type, stop. Commit 2 does not happen.

Do not push this commit by itself.

## Commit 2 — XML Dependency

Start this commit only when the captured error names a missing XML type. If the failure is a different 2026.3 break, fix that break in its own commit and add the XML dependency only when an error names a missing XML type.

### `plugin.xml`

Inside `<dependencies>`:

```xml
<plugin id="com.intellij.modules.xml"/>
```

`com.intellij.modules.xml` is the alias. It covers the modules moved into the XML plugin.

### `build.gradle.kts`

```kotlin
bundledPlugin("com.intellij.xml")
```

`com.intellij.xml` is the plugin id Gradle resolves.

Resolve `bundledModule("intellij.xml.structureView")` and `bundledModule("intellij.xml.structureView.impl")` again after the plugin dependency is declared.

- Remove both lines when Gradle cannot resolve them as top-level modules.
- Keep both lines when they still resolve.

Do not add `com.intellij.xml` to `testBundledPlugins` in the initial edit. Gradle plugin 2.20 puts bundled plugins declared in `product-info.json` on the test classpath for platform `262` and later. If a test then throws `NoClassDefFoundError` for an XML class, add `"com.intellij.xml"` to `testBundledPlugins`.

### Changelog

Under `CHANGELOG.md` `[Unreleased]` → `Changed`, add:

- Dependencies - declare the IntelliJ Platform 2026.3 XML plugin (`com.intellij.xml` / `com.intellij.modules.xml`) ([#150](https://github.com/apache/struts-intellij-plugin/issues/150))

## Verification

After commit 2, and after any later fix commits:

1. `./gradlew test -x rat` passes.
2. `./gradlew runPluginVerifier` reports no Marketplace-blocking incompatibilities for `263.*`.
3. The pull request body quotes the commit 1 error that justified the XML dependency.

Enabled tests that fail because of a 2026.3 API change are fixed on this branch. A failure that is the existing disabled-test debt stays disabled.

## Qodana Follow-up

Open one GitHub issue. Do not change Qodana pins in this work.

**Title:** Point Qodana at the 2026.3 JVM linter when it is published

**Body:** `qodana.yml` stays on `jetbrains/qodana-jvm:2026.2-eap`. `.github/workflows/build.yml` stays on `JetBrains/qodana-action` `v2026.2.1`. Move both when `jetbrains/qodana-jvm:2026.3` and `qodana-action` `v2026.3` exist. `projectJDK: 25` is already correct.

## Components Touched

| Unit | Commit | Change |
|---|---|---|
| `gradle.properties` | 1 | Platform `2026.3`, range `263`–`263.*`, version prefix `263` |
| `build.gradle.kts` | 1 | IntelliJ Platform Gradle Plugin `2.20.1` |
| `build.gradle.kts` | 2 | `bundledPlugin("com.intellij.xml")`; drop the two XML structure-view modules only if they no longer resolve |
| `src/main/resources/META-INF/plugin.xml` | 2 | `<plugin id="com.intellij.modules.xml"/>` |
| Production or test sources | Additional commits | Only a `263` compile, test, or verifier failure |
| `CHANGELOG.md` | 1 and 2 | Platform bump, Gradle plugin, XML dependency |
| `CLAUDE.md` | 1 | `263.x` → 2026.3 row |

`qodana.yml`, workflow Java versions, and `ognl.xml` stay unchanged.

## Delivery

- Branch: `build/intellij-2026-3`.
- One pull request to `main`, opened when the tip is green.
- The pull request body links #149 and #150 and quotes the commit 1 XML error.
- Do not run prepare or publish release workflows.

## Success Criteria

These criteria apply when commit 1 reports a missing XML type. If it does not, the work stops after commit 1, #150 stays open, and the XML dependency is not added.

- `gradle.properties` targets `2026.3` and `263`–`263.*`, with `pluginVersion` `263.19043.1`.
- `org.jetbrains.intellij.platform` is `2.20.1`.
- `plugin.xml` depends on `com.intellij.modules.xml` and Gradle declares `bundledPlugin("com.intellij.xml")`, and the pull request quotes the error that required them.
- `./gradlew test -x rat` passes.
- `./gradlew runPluginVerifier` accepts `263.*`.
- `CHANGELOG.md` records the platform bump, the Gradle plugin upgrade, and the XML dependency.
- `CLAUDE.md` lists `263.x` → 2026.3.
- A Qodana follow-up issue exists, and the Qodana pins are unchanged.
- `ognl.xml` still registers `colorSettingsPage`.

## Out of Scope

- Publishing the `263.x` Marketplace release.
- #151 and any other `colorSettingsPage` migration.
- Re-enabling disabled tests.
- Widening compatibility back to 2026.2.

## Amendment: Bundled Plugin IDs (2026-11-13)

The first 2026.3 build (commit 1) failed at dependency resolution, before the XML gate. Three groups of IDs are not bundled plugins in 2026.3:

- `com.intellij.jsp` is no longer bundled. It is the Marketplace plugin (Ultimate subscription), pinned to `263.7307.101` as a required dependency, and it provides `intellij.javaee.jsp.shared`, so that bundled module declaration is removed. Users without an Ultimate subscription cannot load the plugin.
- `com.intellij.freemarker` and `com.intellij.velocity` are Marketplace plugins with the same IDs. They are pinned in Gradle (`plugin(id, version)`) to `263.7307.112` and `263.7307.101`, and their optional `<depends>` entries in plugin.xml are unchanged.
- `intellij.structureView.plugin`, `intellij.structuralSearch.plugin`, and `intellij.todo.plugin` are replaced by `com.intellij.structureView`, `com.intellij.structuralSearch`, and `com.intellij.todo`.

These changes ship in one commit (Task 2a) before the XML gate is re-run. The XML decision and the two-commit structure above are unchanged.
