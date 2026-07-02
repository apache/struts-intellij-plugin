# Struts 7.2.1 Metadata Compatibility

**Date:** 2026-07-02  
**Status:** Approved for implementation planning

## Problem

Apache Struts 7.2.1 adds or emphasizes configuration values that the plugin does not currently suggest because several completion sources are maintained as static metadata. The plugin still works for typical Struts 7.2.1 projects that use the published `struts-6.0.dtd`, but completion and web.xml reference support lag behind current Struts metadata.

The public Struts DTD catalog at <https://struts.apache.org/dtds/> does not publish `struts-6.5.dtd`, so this work should not add that DTD as part of 7.2.1 support.

## Goals

1. Suggest Struts 7.2.1-relevant core constants in `<constant name="...">` and web.xml init-param contexts.
2. Suggest current UI themes, including `html5` and `css_xhtml`.
3. Keep the legacy `ajax` theme available for existing projects, but mark it as deprecated in completion.
4. Suggest the `jakarta-stream` multipart parser value.
5. Treat the modern Struts filter class as a Struts filter for web.xml constant references.
6. Keep the implementation small, static, and easy to review for the upcoming plugin release.

## Non-Goals

- Implementing an `@StrutsParameter` inspection. That requires Java/property-flow analysis and should be a separate feature.
- Adding `struts-6.5.dtd`, since it is not published in the public DTD catalog.
- Dynamically loading all constants from the project's Struts JAR.
- Broadly refreshing every historical Struts constant in one pass.

## Decision

Use a focused static metadata update.

This approach keeps the release change narrow: update the existing hardcoded completion providers and filter detection logic instead of introducing a new dynamic metadata subsystem. It covers the visible Struts 7.2.1 gaps while avoiding a larger design for Java inspections or runtime JAR introspection.

## Alternatives Considered

### Minimal release hygiene

Only add the three annotation-related constants and `html5` theme.

Rejected because it leaves known stale completions (`css_xhtml`, `jakarta-stream`, and modern web.xml filter detection) that are cheap to fix in the same PR.

### Full Struts 7.x metadata refresh

Mirror a much larger set of constants from Struts 7.2.1's `org.apache.struts2.StrutsConstants`.

Rejected for this release because it increases review noise and maintenance burden. A dynamic or generated metadata approach can be considered separately if the static list keeps drifting.

## Components

### `StrutsCoreConstantContributor`

Update static core constant definitions:

- Add boolean constants:
  - `struts.parameters.requireAnnotations`
  - `struts.parameters.requireAnnotations.transitionMode`
  - `struts.chaining.requireAnnotations`
- Update `struts.multipart.parser` values to include `jakarta-stream`.
- Update `struts.ui.theme` values to include:
  - `simple`
  - `xhtml`
  - `css_xhtml`
  - `html5`
  - `ajax` as deprecated

If the existing helper methods cannot mark a single string value as deprecated, add a small local helper for lookup variants rather than changing unrelated contributor behavior.

### `ThemeReferenceProvider`

Update tag `theme` attribute completion to match the core theme list:

- `simple`
- `xhtml`
- `css_xhtml`
- `html5`
- `ajax` as deprecated

Use IntelliJ lookup presentation support to strike through or otherwise mark `ajax` deprecated while still offering it.

### `StrutsConstantManagerImpl`

Include `StrutsConstants.STRUTS_2_5_FILTER_CLASS` in web.xml Struts filter detection. This lets constants from filter init-params participate in the same precedence/resolution logic for modern Struts applications.

### `StrutsReferenceContributor`

Include `StrutsConstants.STRUTS_2_5_FILTER_CLASS` in the web.xml filter pattern so `<param-name>` and `<param-value>` references are registered for modern Struts filters.

## Data Flow

```text
struts.xml <constant name="...">
        |
        v
ConstantNameConverterImpl
        |
        v
StrutsConstantManager.getConstants(module)
        |
        v
StrutsCoreConstantContributor static metadata

web.xml modern Struts filter
        |
        v
filter-class matches STRUTS_2_5_FILTER_CLASS
        |
        v
constant name/value reference providers register and resolve
```

## Error Handling

No new runtime error handling is needed. These changes affect completion variants and reference registration only.

If the deprecated `ajax` theme is present in existing code, it should not be flagged as invalid by this work. It should remain selectable/resolvable, with only the completion lookup indicating deprecation.

## Testing

Add or update focused tests:

1. Constant name completion includes:
   - `struts.parameters.requireAnnotations`
   - `struts.parameters.requireAnnotations.transitionMode`
   - `struts.chaining.requireAnnotations`
2. `struts.ui.theme` value completion includes `html5`, `css_xhtml`, and deprecated-but-present `ajax`.
3. `theme` tag attribute completion includes `html5`, `css_xhtml`, and deprecated-but-present `ajax`.
4. `struts.multipart.parser` value completion includes `jakarta-stream`.
5. web.xml reference tests cover `org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter`.

Run targeted tests first, then the broader suite before completion:

```bash
./gradlew test -x rat --tests "StrutsCompletionTest"
./gradlew test -x rat --tests "WebXmlConstantTest"
./gradlew test -x rat
```

## Follow-Up

Create a separate design for `@StrutsParameter` awareness. Struts 7.2.1 relies heavily on `struts.parameters.requireAnnotations=true`, but a useful inspection needs to understand action classes, setters/fields, OGNL/property paths, and configuration constants. That scope is larger than this metadata compatibility update.
