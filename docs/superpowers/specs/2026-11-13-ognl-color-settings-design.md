# OGNL Color Settings Page on `colorSettings`

**Date:** 2026-11-13
**Status:** Draft for review
**Issue:** [#151](https://github.com/apache/struts-intellij-plugin/issues/151)

## Problem

IntelliJ Platform 2026.3 deprecates `com.intellij.colorSettingsPage`. The replacement is `com.intellij.colorSettings`. The IDE builds Settings | Editor | Color Scheme from that declaration and creates the `ColorSettingsPage` only when the page is opened.

`src/main/resources/META-INF/ognl.xml` still registers the old extension:

```xml
<colorSettingsPage implementation="com.intellij.lang.ognl.highlight.OgnlColorSettingsPage"/>
```

The plugin already targets 2026.3 only (`pluginSinceBuild = 263`, `pluginUntilBuild = 263.*`). The old extension does not need to stay for older IDEs.

## Goals

1. Register the OGNL color page with `com.intellij.colorSettings`, with display name `OGNL` and page id `OGNL`.
2. Make `OgnlColorSettingsPage.getId()` return that same id.
3. Leave the page's descriptors, demo text, highlighter, and icon as they are.
4. Keep the page in the language group at weight 0 by omitting `priority` and `groupWeight`, which is the extension default.
5. Record the move in `CHANGELOG.md`.

## Non-Goals

- Implementing `DisplayPrioritySortable`, or setting `priority` and `groupWeight` in XML.
- Registering `colorSettingsPage` beside `colorSettings`.
- A message bundle for the page name.
- Changing attribute keys, descriptor labels, demo text, the highlighter, or the file-type icon.
- A platform, Gradle, or since-build bump.
- An automated test of the Settings tree, or of applying a scheme color to an editor.

## Decision

Change the existing page class in place. The declaration carries the name and the id. The class returns the same id and the same display name. Order stays the extension default: language group, weight 0. The class does not implement `DisplayPrioritySortable`, so the declaration does not set `priority` or `groupWeight`.

The values in the declaration must match the class. The tree reads the declaration. The opened page reads the class. The IDE reports a mismatch. This change adds no plugin-side check.

## Registration

In `src/main/resources/META-INF/ognl.xml`, replace the `colorSettingsPage` element with:

```xml
<colorSettings implementation="com.intellij.lang.ognl.highlight.OgnlColorSettingsPage"
               displayName="OGNL"
               pageId="OGNL"/>
```

`pageId` sets the page id. The common extension `id` attribute does not.

`displayName` is the literal `OGNL`. It matches `OgnlColorSettingsPage.getDisplayName()`, which returns `OgnlLanguage.INSTANCE.getDisplayName()`. `OgnlLanguage` does not override `getDisplayName()`, and its id is `OGNL`, so that call returns `OGNL`.

## Page class

In `OgnlColorSettingsPage`, add:

```java
@NotNull
@Override
public String getId() {
  return OgnlLanguage.ID;
}
```

`OgnlLanguage.ID` is `"OGNL"`. The class continues to implement `ColorSettingsPage` only.

These members stay:

| Member | Behavior |
|---|---|
| `getDisplayName()` | `OgnlLanguage.INSTANCE.getDisplayName()` |
| `getIcon()` | `OgnlFileType.INSTANCE.getIcon()` |
| `getAttributeDescriptors()` | The twelve descriptors below, in this order |
| `getColorDescriptors()` | `ColorDescriptor.EMPTY_ARRAY` |
| `getHighlighter()` | `new OgnlHighlighter()` |
| `getDemoText()` | The current OGNL sample |
| `getAdditionalHighlightingTagToDescriptorMap()` | `fqnType` → `OgnlHighlighter.FQN_TYPE` |

Descriptor labels, in order:

1. Background
2. Expression bounds
3. Keyword
4. Operations
5. Identifier
6. String
7. Number
8. Comma
9. Parentheses
10. Brackets
11. Braces
12. Class reference

Attribute keys stay on `OgnlHighlighter`. Saved color schemes keep their OGNL colors because those colors are stored under the keys, and the keys do not change.

## Changelog

Under `CHANGELOG.md` `[Unreleased]` → `Changed`, add:

- Migrate the OGNL color settings page to the declarative colorSettings extension ([#151](https://github.com/apache/struts-intellij-plugin/issues/151))

## Testing

### Automated

Add `src/test/java/com/intellij/lang/ognl/highlight/OgnlColorSettingsPageTest.java`. It extends `LightPlatformTestCase`, in the same style as `OgnlHighlightingLexerTest`, and it starts with that file's Apache license header. It constructs `OgnlColorSettingsPage` and asserts:

- `getId()` equals `OgnlLanguage.ID` and equals `"OGNL"`
- `getDisplayName()` equals `"OGNL"`
- attribute descriptor display names are the twelve labels above, in that order
- `getColorDescriptors()` is empty

Run:

```bash
./gradlew test --tests "OgnlColorSettingsPageTest" -x rat
./gradlew test -x rat
./gradlew runPluginVerifier
```

`runPluginVerifier` must complete successfully, and its output must not mention `colorSettingsPage`.

### Manual

With `./gradlew runIde`:

1. Open Settings | Editor | Color Scheme | OGNL.
2. Confirm the twelve descriptors are listed.
3. Change the Keyword color.
4. Confirm an OGNL fragment uses the new Keyword color.

## Files

| File | Change |
|---|---|
| `src/main/resources/META-INF/ognl.xml` | Replace `colorSettingsPage` with `colorSettings` |
| `src/main/java/com/intellij/lang/ognl/highlight/OgnlColorSettingsPage.java` | Add `getId()` |
| `src/test/java/com/intellij/lang/ognl/highlight/OgnlColorSettingsPageTest.java` | New test |
| `CHANGELOG.md` | One `Changed` bullet under `[Unreleased]` |
