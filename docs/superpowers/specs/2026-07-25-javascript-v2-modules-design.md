# JavaScript v2 Content Modules for IntelliJ Platform 2026.2

**Issue:** [#103](https://github.com/apache/struts-intellij-plugin/issues/103)  
**Date:** 2026-07-25  
**Status:** Approved for implementation planning

## Problem

In IntelliJ Platform **2026.2**, the JavaScript plugin migrated to the **v2 plugin model**. Functionality is split into content modules; a plain dependency on the `JavaScript` plugin may no longer expose required classes at runtime.

Typical linkage errors:

```
java.lang.NoClassDefFoundError: com/intellij/lang/javascript/JSElementTypes
java.lang.ClassNotFoundException: com.intellij.lang.javascript.JSElementTypes
```

This plugin depends on JavaScript for language injection in Struts taglib attributes (JSP and FreeMarker). The platform target is already **2026.2** (`262.*`) via [#115](https://github.com/apache/struts-intellij-plugin/pull/115); what remains is declaring the v2 content modules that host the classes we use.

## Goals

1. Declare the JavaScript v2 content modules required for our injectors at runtime.
2. Keep the existing `<plugin id="JavaScript"/>` / `bundledPlugin("JavaScript")` dependency — v2 `<module>` entries are additional.
3. Match Gradle `bundledModule(...)` entries to `plugin.xml`.
4. Expand the module set only when build, plugin verifier, or `runIde` shows a real classloader gap.
5. Confirm JSP and FreeMarker Struts taglib JS injection works under 2026.2 without classloader errors.
6. Document the change in `CHANGELOG.md`.

## Non-Goals

- Making the JavaScript dependency optional.
- Changing injector logic or removing `com.intellij.lang.javascript.*` usage.
- Marketplace release / prepare-publish as part of this work.
- Adding new automated tests for JS injection (manual `runIde` smoke is the functional bar).
- Re-enabling historically disabled tests.
- Broader platform upgrades (already completed in #115).

## Decisions

| Question | Decision |
|---|---|
| Approach | Minimal declarative fix — declare modules, keep injectors unchanged |
| Initial modules | `intellij.javascript.parser` + `intellij.javascript.backend` |
| Escalation | Add `intellij.javascript.common` only if residual classloader errors appear; then broader sets (`psi.impl`, `analysis.impl`) if still needed |
| Keep `JavaScript` plugin dependency? | Yes — modules are additional, not a replacement |
| Verification bar | `build` + `runPluginVerifier` + `test -x rat` + manual `runIde` smoke |
| New automated JS injector tests? | No |

## Scope of JavaScript API usage

Only two production files import `com.intellij.lang.javascript.*`:

| File | Classes used |
|------|--------------|
| `src/main/java/com/intellij/struts2/jsp/TaglibJavaScriptInjector.java` | `JavaScriptSupportLoader`, `JSInXmlLanguagesInjector` |
| `src/main/java/com/intellij/struts2/freemarker/FreeMarkerJavaScriptInjector.java` | `JavaScriptSupportLoader` |

No direct usage of `JSElementTypes`, `JSLanguage`, `JSReferenceExpression`, or JS lexers/parsers.

## Architecture / components

No production code changes. Wiring only, same pattern as existing `intellij.xml.structureView*` modules.

| Unit | Role |
|------|------|
| `src/main/resources/META-INF/plugin.xml` | Keep `<plugin id="JavaScript"/>`; add `<module name="intellij.javascript.parser"/>` and `<module name="intellij.javascript.backend"/>` |
| `build.gradle.kts` | Keep `bundledPlugin("JavaScript")`; add matching `bundledModule(...)` entries |
| `TaglibJavaScriptInjector` | Unchanged |
| `FreeMarkerJavaScriptInjector` | Unchanged (still loaded via optional `struts2-freemarker.xml`) |
| `CHANGELOG.md` | Record JavaScript v2 content module dependencies |

### Class → module mapping (initial)

| Class | v2 module |
|-------|-----------|
| `JavaScriptSupportLoader` | `intellij.javascript.parser` |
| `JSInXmlLanguagesInjector` | `intellij.javascript.backend` |

### Descriptor sketch

```xml
<dependencies>
    ...
    <plugin id="JavaScript"/>
    <module name="intellij.javascript.parser"/>
    <module name="intellij.javascript.backend"/>
    ...
</dependencies>
```

### Gradle sketch

```kotlin
bundledPlugin("JavaScript")
bundledModule("intellij.javascript.parser")
bundledModule("intellij.javascript.backend")
```

## Verification and escalation

Execute in this order:

1. Apply the two module declarations in `plugin.xml` and `build.gradle.kts`.
2. `./gradlew build`
3. `./gradlew runPluginVerifier` against 2026.2
4. `./gradlew test -x rat`
5. Manual `./gradlew runIde` smoke: open a JSP (and FreeMarker if available) with Struts UI/jQuery `on*` attributes; confirm JS injection works and the IDE log has no `com.intellij.lang.javascript.*` classloader errors.
6. Update `CHANGELOG.md`.

### Escalation on classloader failure

If `NoClassDefFoundError` / `ClassNotFoundException` for `com.intellij.lang.javascript.*` remains after the two modules:

1. Add `intellij.javascript.common` in both descriptor and Gradle; re-verify.
2. Only then consider broader modules (`psi.impl`, `analysis.impl`).
3. Record whatever was actually required in `CHANGELOG.md`.

## Delivery

- Short-lived branch → PR closing #103.
- Do **not** run prepare/publish release workflows as part of this work.
- After merge, Marketplace impact depends on the next normal 262.x release.

## Success criteria

- [ ] `plugin.xml` declares `intellij.javascript.parser` and `intellij.javascript.backend` (plus any evidence-driven extras)
- [ ] Gradle declares matching `bundledModule(...)` entries
- [ ] `./gradlew build` and `./gradlew runPluginVerifier` pass against 2026.2
- [ ] `./gradlew test -x rat` passes for the enabled suite
- [ ] `./gradlew runIde` — JSP and FreeMarker Struts taglib JS injection works (no classloader errors)
- [ ] `CHANGELOG.md` updated

## References

- [Plugin Dependencies — modular plugins](https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html)
- [IntelliJ Platform Gradle Plugin — `bundledModule()`](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html)
- JetBrains intellij-plugins commit WEB-77676: JavaScript plugin converted to fully-v2 modules
- Prior platform bump: [#115](https://github.com/apache/struts-intellij-plugin/pull/115) / `docs/superpowers/specs/2026-07-24-intellij-2026-2-compatibility-design.md`
