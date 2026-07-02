# StrutsParameter Java Inspection

**Date:** 2026-07-02  
**Status:** Draft for review

## Problem

Struts 7.2.x defaults to requiring `@StrutsParameter` for request parameter injection. When `struts.parameters.requireAnnotations=true`, public setters and public fields on action classes no longer receive request values unless they are explicitly annotated with `org.apache.struts2.interceptor.parameter.StrutsParameter`.

The plugin now suggests the Struts 7.2.1 constants, but it does not help users find action members that will stop binding after migration. This leaves a common upgrade failure mode invisible until runtime.

## Goals

1. Add a Java inspection that warns about action injection points missing `@StrutsParameter`.
2. Run the inspection only when annotation-based parameter binding is active.
3. Keep the first implementation focused on likely request injection points: public setters and public instance fields.
4. Reuse existing Struts facet, action detection, and constant resolution patterns where possible.
5. Avoid quick-fixes, depth validation, and XML/OGNL cross-file analysis in this first slice.

## Non-Goals

- Add a quick-fix to insert `@StrutsParameter`.
- Validate getter `depth` values.
- Validate `struts.xml`, JSP, or OGNL property paths against annotations.
- Inspect `struts.chaining.requireAnnotations` behavior.
- Dynamically infer every possible runtime action mapping.

## Decision

Implement a focused Java `LocalInspectionTool` for action classes.

The inspection will report public JavaBean setters and public non-static fields in Struts action classes when those members lack `@StrutsParameter` and parameter annotations are required. This gives users a practical migration signal while keeping the feature small enough to review and release independently.

## Alternatives Considered

### Reference-driven inspection

Only warn on properties that are referenced from `struts.xml`, JSP tags, form fields, or OGNL expressions.

Rejected for the first slice because the plugin does not currently resolve OGNL property paths to action members, and a reference-driven approach would miss runtime request parameters not represented in project files.

### Getter and depth validation

Inspect getters annotated with `@StrutsParameter(depth = ...)` and compare the depth against nested request property paths.

Deferred because it requires cross-file property-path analysis and a more nuanced model of collection, map, and object traversal. It is a good follow-up after the basic migration inspection exists.

### Quick-fix first

Offer an intention to add `@StrutsParameter` wherever the inspection reports a problem.

Deferred because adding the annotation has security implications. The first slice should identify likely migration issues without automatically broadening the request-binding surface.

## Inspection Scope

### Enabled Context

The inspection runs for Java files only when all of these are true:

1. The file belongs to a module with a Struts facet.
2. `org.apache.struts2.interceptor.parameter.StrutsParameter` is available in the resolve scope.
3. Parameter annotations are required.

Parameter annotations are considered required when:

- `struts.parameters.requireAnnotations` resolves to `true`, or
- the project uses Struts 7+ and the constant is not explicitly set.

If the constant resolves to `false`, the inspection should not report missing annotations. If the plugin cannot determine whether annotations are required, it should stay quiet to avoid noisy warnings in older Struts projects.

### Action Class Detection

The inspection should reuse the same broad action-class signals already used in the plugin:

- classes referenced as actions in the Struts model, when available;
- convention actions marked with `@Action` or `@Actions`;
- public non-abstract classes ending with `Action` when the Convention plugin is present;
- public non-abstract classes inheriting from `com.opensymphony.xwork2.Action`.

Shared action-detection helpers should be extracted if needed so the inspection does not duplicate the logic from `StrutsConventionImplicitUsageProvider`.

### Reported Members

Report a problem on the member name when a member is a likely direct injection point and lacks `@StrutsParameter`.

Reported members:

- public, non-static, non-abstract setter methods with one parameter;
- public, non-static fields.

Ignored members:

- getters;
- constructors;
- static, abstract, private, protected, or package-private members;
- action execution methods such as `execute()`;
- members already annotated with `@StrutsParameter`;
- members declared outside the inspected action class, including inherited framework members.

Getter support is intentionally excluded from v1 because a useful warning for getters needs to understand nested property paths and `depth`.

## User Experience

Inspection group: `Struts`  
Language: `JAVA`  
Suggested short name: `StrutsParameterAnnotation`

Example message:

> Parameter injection requires `@StrutsParameter` when `struts.parameters.requireAnnotations` is enabled.

The inspection should explain that request parameters will not bind to the member without the annotation. It should not claim the annotation is always safe to add.

## Components

### `StrutsParameterAnnotationInspection`

New Java `LocalInspectionTool` that creates a `JavaElementVisitor`.

Responsibilities:

- skip files without Struts support or without required annotation mode;
- visit candidate `PsiClass` instances;
- report missing annotations on candidate methods and fields.

### `StrutsActionClassUtil`

New utility for action-class recognition if extracting shared logic keeps the inspection simple.

It can reuse predicates from `StrutsConventionImplicitUsageProvider` and add Struts model lookup later if needed.

### `StrutsParameterAnnotationUtil`

Small utility for annotation and member checks:

- `isStrutsParameterAvailable(PsiElement)`;
- `isAnnotatedWithStrutsParameter(PsiModifierListOwner)`;
- `isInjectableSetter(PsiMethod)`;
- `isInjectableField(PsiField)`.

### `StrutsParameterConfigUtil`

Small utility for checking whether `struts.parameters.requireAnnotations` is active for the current file.

It should call `StrutsConstantManager.getConvertedValue(...)` for the new constant key. If a Struts 7+ version can be detected from the module library, use default `true` when no explicit constant exists.

## Data Flow

```text
Java file
  |
  v
Struts facet present?
  |
  v
@StrutsParameter available and requireAnnotations active?
  |
  v
PsiClass is a Struts action?
  |
  v
public setter/public field lacks @StrutsParameter
  |
  v
inspection warning
```

## Testing

Add focused inspection tests using `BasicLightHighlightingTestCase`.

Core test cases:

1. Public setter on an action class without `@StrutsParameter` is warned when `struts.parameters.requireAnnotations=true`.
2. Public field on an action class without `@StrutsParameter` is warned when `struts.parameters.requireAnnotations=true`.
3. Annotated setter and annotated field are not warned.
4. Unannotated setter is not warned when `struts.parameters.requireAnnotations=false`.
5. Non-action classes are not warned.
6. Private/protected/package-private setters and fields are not warned.
7. Getter methods are not warned in v1.

Fixtures should include a lightweight `org.apache.struts2.interceptor.parameter.StrutsParameter` test stub if the current test dependency does not provide the annotation.

Run targeted tests first, then the broader suite:

```bash
./gradlew test -x rat --tests "StrutsParameterAnnotationInspectionTest" --no-configuration-cache
./gradlew test -x rat --no-configuration-cache
```

## Follow-Up

- Add a quick-fix to insert `@StrutsParameter` after the warning text and security caveats are settled.
- Add getter and `depth` validation based on actual nested property paths.
- Validate XML/JSP/OGNL parameter paths against annotated action members.
- Consider support for `struts.chaining.requireAnnotations` after direct request-parameter support is stable.
