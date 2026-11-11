# Struts 6.5 and 7.5 Configuration Grammar

**Date:** 2026-11-11
**Status:** Approved for implementation planning
**Issue:** https://github.com/apache/struts-intellij-plugin/issues/147

## Problem

Struts 7.5.0 publishes new `struts.xml` grammar. The plugin bundles configuration DTDs through `struts-6.0.dtd`, so a 6.5 or 7.5 DOCTYPE is unrecognized and the new syntax is invalid in the editor.

The published catalog is https://struts.apache.org/dtds/. Compared with the bundled 6.0 DTD:

- `struts-6.5.dtd` adds `final` on `<package>`.
- `struts-7.5.dtd` adds `final` and `extension` on `<package>`, `<scoped-constant name value>`, and `http-method` and `extension` on `<action>`.

Release notes: https://cwiki.apache.org/confluence/spaces/WW/pages/462816288/Version+Notes+7.5.0

## Goals

1. Validate files that use the published 6.5 and 7.5 DOCTYPEs against bundled copies of those DTDs.
2. Complete and check `<scoped-constant>`, `http-method`, `extension`, and `final`.
3. Select the 6.5 or 7.5 DOCTYPE for a new `struts.xml` from the detected `struts2-core` version, using the table in File Templates.
4. Keep the change static, in the existing DTD, DOM, and file-template paths.

## Non-Goals

- OGNL property resolution and the `@StrutsParameter` inspection.
- Tag libraries and validators. Those already come from the Struts JAR on the classpath.
- Struts 1, Tiles, and XWork DTDs on the same catalog.
- Reading `ScopableConstants` implementations from the classpath.
- Rewriting the DOCTYPE of an existing `struts.xml`.

## Decision

Use a static DTD and DOM update, the same style as the 7.2.1 constant metadata.

One DOM model serves every DTD version. The bundled DTD for a file decides whether the new syntax is legal in that file. Completion is not switched per DTD version.

## Alternatives Considered

### DTD registration only

Bundle and register the two files so DOCTYPE checks and XML validation accept 6.5 and 7.5 documents. Leave the DOM unchanged.

Rejected because `http-method`, `extension`, and `<scoped-constant>` would get no value completion or semantic checks.

### Classpath discovery for scopable names

Read `<scoped-constant>` names from `ScopableConstants` beans in the module's Struts JAR.

Rejected because the built-in set has three names, and the rest of the plugin's constants are static lists.

## DTD Registration

Copy these files from https://struts.apache.org/dtds/ unchanged into `src/main/resources/resources/dtds/`:

- `struts-6.5.dtd`
- `struts-7.5.dtd`

Register each system URI and public ID in `StrutsConstants` and `Struts2ResourceProvider`. Add the `https` URIs to `StrutsConstants.STRUTS_DTDS`.

| File | System URI | Public ID |
|---|---|---|
| `struts-6.5.dtd` | `https://struts.apache.org/dtds/struts-6.5.dtd` | `-//Apache Software Foundation//DTD Struts Configuration 6.5//EN` |
| `struts-7.5.dtd` | `https://struts.apache.org/dtds/struts-7.5.dtd` | `-//Apache Software Foundation//DTD Struts Configuration 7.5//EN` |

An `http://` URI for either file is reported as "use https" by the existing `http://struts.apache.org/dtds/struts-` prefix check in `StrutsDtdValidator`.

## File Templates

Add `struts_6_5.xml.ft` and `struts_7_5.xml.ft` under `src/main/resources/fileTemplates/j2ee/`, beside `struts_6_0.xml.ft`, plus a matching `.html` description for each. The descriptor names registered in `StrutsFileTemplateGroupDescriptorFactory` are `struts_6_5.xml` and `struts_7_5.xml`. Each template body is an empty `<struts>` document with the DOCTYPE from the table above.

`StrutsFileTemplateProvider` selects a template from the `struts2-core` version detected in the module. The detected value is a Maven version such as `6.5.0` or `7.5.0`. The existing `isNewerThan` helper is a strict greater-than, so the new steps use an inclusive comparison and the boundary versions match.

The mapping from a version string to a template name is a package-visible method on `StrutsFileTemplateProvider`. `determineFileTemplate` calls that method and then loads the J2EE template.

| Detected version | Template |
|---|---|
| At least 7.5, including 7.5.0, 7.6, and 8.0 | `struts_7_5.xml` |
| At least 6.5 and below 7.0, including 6.5.0 and 6.6 | `struts_6_5.xml` |
| Above 6.0 and below 6.5, and from 7.0.0 through 7.4.x | `struts_6_0.xml`, unchanged |
| Older than the 6.0 step | The existing older templates, unchanged |
| No version detected | The current fallback, unchanged |

There is no `struts-7.0.dtd`. Modules on 7.0.0 through 7.4.x keep the 6.0 template they get today.

## DOM

`StrutsPackage` gains:

- `final`, a boolean attribute, the same as `abstract`. An omitted `final` means the package is not final.
- `extension`, a comma-separated string.
- a list of `<scoped-constant>` children.

`Action` gains:

- `http-method`, a comma-separated string.
- `extension`, a comma-separated string.

`<scoped-constant>` is a new element with required `name` and `value`, the same shape as `<constant>`. Required `name` and `value` use the normal DOM required-attribute check. Omitted optional attributes stay valid.

## Checks

### Final parent

When `extends` resolves to one or more packages and any resolved parent has `final` true, highlight that `extends` value. Struts rejects that parent at startup with "Parent package is final and unextendable". An unresolved parent stays with the existing `extends` resolver.

`Struts2ModelInspectionVisitor` reports this when it visits the package.

### Scoped constant

Name completion is this static set intersected with the constants already registered for the module:

- `struts.ui.theme`
- `struts.ui.templateDir`
- `struts.convention.result.path`, only when the Convention contributor is active for that module

`value` uses the existing constant-value converter. For `struts.ui.theme` that offers `simple`, `xhtml`, `css_xhtml`, `html5`, and deprecated `ajax`. `struts.ui.templateDir` and `struts.convention.result.path` stay free text.

A name outside the scopable set is highlighted. A name registered only by a custom `ScopableConstants` bean is outside the set, so it is highlighted too.

### HTTP method

Completion offers `GET`, `HEAD`, `POST`, `PUT`, `DELETE`, `TRACE`, `OPTIONS`, `CONNECT`, and `PATCH`. The list is comma-separated, in the same converter style as `allowed-methods`. A token matches one of those nine names ignoring case. Any other token is highlighted.

### Extension

The same comma-separated rules apply on `<package>` and `<action>`. There is no fixed completion list. The tokens are application-specific, such as `action`, `do`, or an empty token for an extensionless URL.

Split on `,` and do not trim. A token is highlighted when it starts with `.`, contains `/`, or contains whitespace. An empty token is valid, including a trailing comma that produces one.

## Data Flow

```text
struts.xml DOCTYPE system id
        |
        v
Struts2ResourceProvider  -->  bundled struts-6.5.dtd or struts-7.5.dtd
        |
        v
StrutsDtdValidator accepts the same https URI

<scoped-constant>, http-method, extension
        |
        v
DOM converters  -->  completion variants and unresolved tokens

<package extends="...">
        |
        v
Struts2ModelInspectionVisitor highlights a resolved final parent

Create struts.xml
        |
        v
detected struts2-core version  -->  struts_6_5.xml or struts_7_5.xml
```

## Testing

Add cases to the existing test classes. Highlighting fixtures use the `<error descr="...">` style already used for `allowed-methods`.

`StrutsDtdValidatorTest`:

- The 6.5 and 7.5 `https` URIs are `OK`.
- The `http://` form of each is `HTTP_INSTEAD_OF_HTTPS`.

`StrutsHighlightingTest`:

- Extending a `final` package is highlighted.
- An unknown `<scoped-constant>` name is highlighted, and `struts.ui.theme` is clean.
- An unknown `http-method` token is highlighted, and `post` is clean.
- An `extension` token with a leading `.`, a `/`, or whitespace is highlighted, and an empty token is clean.

`StrutsCompletionTest`, in the default fixture where the Convention contributor is inactive:

- `http-method` offers the nine methods.
- A scoped-constant name offers `struts.ui.theme` and `struts.ui.templateDir`, and does not offer `struts.convention.result.path`.
- The value for `struts.ui.theme` offers `simple`, `xhtml`, `css_xhtml`, `html5`, and `ajax`.

`StrutsFileTemplateProviderTest` calls the package-visible version mapping directly, with no fake JAR:

- `6.4.0` and `7.4.0` stay on the 6.0 template.
- `6.5.0` selects the 6.5 template.
- `7.5.0` and `7.6.0` select the 7.5 template.
- A missing version keeps today's fallback.

`CHANGELOG.md` gets an unreleased note for the 6.5 and 7.5 DTD and DOM support.

Run the focused tests, then the suite:

```bash
./gradlew test -x rat --tests "StrutsDtdValidatorTest" --tests "StrutsHighlightingTest" --tests "StrutsCompletionTest" --tests "StrutsFileTemplateProviderTest"
./gradlew test -x rat
```
