# Struts 6.5 and 7.5 Configuration Grammar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the editor accept and assist the published Struts 6.5 and 7.5 `struts.xml` grammar.

**Architecture:** Copy the two published DTDs into the plugin and register them the same way as `struts-6.0.dtd`. Extend the existing DOM interfaces with `final`, `extension`, `<scoped-constant>`, and `http-method`, and check those values with converters plus one package-visitor rule. Pick the new-file template from the detected `struts2-core` version with an inclusive comparison.

**Tech Stack:** Java, IntelliJ Platform DOM (`com.intellij.util.xml`), JUnit 3-style plugin tests, Gradle (`./gradlew test -x rat`).

## Global Constraints

- Copy `struts-6.5.dtd` and `struts-7.5.dtd` from https://struts.apache.org/dtds/ unchanged.
- System URIs are `https://struts.apache.org/dtds/struts-6.5.dtd` and `https://struts.apache.org/dtds/struts-7.5.dtd`.
- Public IDs are `-//Apache Software Foundation//DTD Struts Configuration 6.5//EN` and `-//Apache Software Foundation//DTD Struts Configuration 7.5//EN`.
- One DOM model serves every DTD version. Completion is not switched per DTD version.
- Scoped-constant names are only `struts.ui.theme`, `struts.ui.templateDir`, and `struts.convention.result.path` when that constant is already registered for the module.
- `http-method` tokens are `GET`, `HEAD`, `POST`, `PUT`, `DELETE`, `TRACE`, `OPTIONS`, `CONNECT`, `PATCH`, matched ignoring case.
- An extension token is illegal when it starts with `.`, contains `/`, or contains whitespace. An empty token is valid. Do not trim.
- Versions at least 7.5 use the 7.5 template. Versions at least 6.5 and below 7.0 use the 6.5 template. 6.0 through 6.4 and 7.0.0 through 7.4.x keep the 6.0 template.
- Do not rewrite an existing file's DOCTYPE.
- Do not scan `ScopableConstants` beans, and do not change OGNL, `@StrutsParameter`, tag libraries, validators, or Struts 1 / Tiles / XWork DTDs.
- Implement on the current branch `docs/struts-7-5-configuration-grammar`, which already contains the spec.
- Every new Java file starts with the Apache license header from `src/main/java/com/intellij/struts2/inspection/StrutsParameterAnnotationUtil.java` (Copyright 2026).
- Every new test-data XML file starts with the license comment from `src/test/testData/strutsXml/highlighting/struts-action-allowedMethods.xml`.

---

## File structure

- `src/main/resources/resources/dtds/struts-6.5.dtd` — published 6.5 DTD, unmodified.
- `src/main/resources/resources/dtds/struts-7.5.dtd` — published 7.5 DTD, unmodified.
- `src/main/java/com/intellij/struts2/StrutsConstants.java` — URI and public-ID constants, plus `STRUTS_DTDS`.
- `src/main/java/com/intellij/struts2/Struts2ResourceProvider.java` — maps those IDs to the bundled files.
- `src/main/java/com/intellij/struts2/StrutsFileTemplateProvider.java` — `templateNameFor` and the version cascade.
- `src/main/java/com/intellij/struts2/StrutsFileTemplateGroupDescriptorFactory.java` — template descriptor names.
- `src/main/resources/fileTemplates/j2ee/struts_6_5.xml.ft` and `struts_7_5.xml.ft` — empty `<struts>` documents.
- `src/main/resources/fileTemplates/j2ee/struts_6_5.xml.html` and `struts_7_5.xml.html` — template descriptions.
- `src/main/java/com/intellij/struts2/dom/struts/strutspackage/StrutsPackage.java` — `final`, `extension`, `<scoped-constant>`.
- `src/main/java/com/intellij/struts2/dom/struts/action/Action.java` — `http-method`, `extension`.
- `src/main/java/com/intellij/struts2/dom/struts/strutspackage/ScopedConstant.java` — name and value element.
- `src/main/java/com/intellij/struts2/dom/struts/strutspackage/ScopedConstantNameConverter.java` — scopable names.
- `src/main/java/com/intellij/struts2/dom/struts/constant/ConstantValueConverterImpl.java` — value completion for `<constant>` and `<scoped-constant>`.
- `src/main/java/com/intellij/struts2/dom/struts/action/HttpMethodConverter.java` — comma-separated HTTP methods.
- `src/main/java/com/intellij/struts2/dom/struts/strutspackage/ExtensionListConverter.java` — comma-separated extensions, used by package and action.
- `src/main/java/com/intellij/struts2/dom/inspection/Struts2ModelInspectionVisitor.java` — final parent.
- `src/main/resources/messages/Struts2Bundle.properties` — final-parent message.
- `CHANGELOG.md` — unreleased note.
- Tests under `src/test/java/com/intellij/struts2/` and `src/test/testData/strutsXml/`.

`StrutsPackageImpl` and `ActionImpl` stay unchanged. They are abstract, and the DOM framework supplies the new getters.

### Task 1: Register the 6.5 and 7.5 DTDs

**Files:**
- Create: `src/main/resources/resources/dtds/struts-6.5.dtd`
- Create: `src/main/resources/resources/dtds/struts-7.5.dtd`
- Modify: `src/main/java/com/intellij/struts2/StrutsConstants.java`
- Modify: `src/main/java/com/intellij/struts2/Struts2ResourceProvider.java`
- Test: `src/test/java/com/intellij/struts2/dom/inspection/StrutsDtdValidatorTest.java`

**Interfaces:**
- Consumes: `StrutsDtdValidator.validate` and `StrutsConstants.STRUTS_DTDS`.
- Produces: `StrutsConstants.STRUTS_6_5_DTD_URI`, `STRUTS_6_5_DTD_ID`, `STRUTS_7_5_DTD_URI`, `STRUTS_7_5_DTD_ID`, both present in `STRUTS_DTDS`.

- [ ] **Step 1: Write the failing tests**

Add these methods to `StrutsDtdValidatorTest`:

```java
public void testValidStruts65HttpsDtdReturnsOk() {
    XmlFile file = (XmlFile) myFixture.configureByText("struts.xml",
            """
            <?xml version="1.0" encoding="UTF-8" ?>
            <!DOCTYPE struts PUBLIC
              "-//Apache Software Foundation//DTD Struts Configuration 6.5//EN"
              "https://struts.apache.org/dtds/struts-6.5.dtd">
            <struts/>
            """);
    assertEquals(StrutsDtdValidator.Result.OK, StrutsDtdValidator.validate(file));
}

public void testValidStruts75HttpsDtdReturnsOk() {
    XmlFile file = (XmlFile) myFixture.configureByText("struts.xml",
            """
            <?xml version="1.0" encoding="UTF-8" ?>
            <!DOCTYPE struts PUBLIC
              "-//Apache Software Foundation//DTD Struts Configuration 7.5//EN"
              "https://struts.apache.org/dtds/struts-7.5.dtd">
            <struts/>
            """);
    assertEquals(StrutsDtdValidator.Result.OK, StrutsDtdValidator.validate(file));
}

public void testHttpInsteadOfHttpsForStruts65ReturnsWarning() {
    XmlFile file = (XmlFile) myFixture.configureByText("struts.xml",
            """
            <?xml version="1.0" encoding="UTF-8" ?>
            <!DOCTYPE struts PUBLIC
              "-//Apache Software Foundation//DTD Struts Configuration 6.5//EN"
              "http://struts.apache.org/dtds/struts-6.5.dtd">
            <struts/>
            """);
    assertEquals(StrutsDtdValidator.Result.HTTP_INSTEAD_OF_HTTPS, StrutsDtdValidator.validate(file));
}

public void testHttpInsteadOfHttpsForStruts75ReturnsWarning() {
    XmlFile file = (XmlFile) myFixture.configureByText("struts.xml",
            """
            <?xml version="1.0" encoding="UTF-8" ?>
            <!DOCTYPE struts PUBLIC
              "-//Apache Software Foundation//DTD Struts Configuration 7.5//EN"
              "http://struts.apache.org/dtds/struts-7.5.dtd">
            <struts/>
            """);
    assertEquals(StrutsDtdValidator.Result.HTTP_INSTEAD_OF_HTTPS, StrutsDtdValidator.validate(file));
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test -x rat --tests "StrutsDtdValidatorTest" --no-configuration-cache`

Expected: FAIL. `testValidStruts65HttpsDtdReturnsOk` and `testValidStruts75HttpsDtdReturnsOk` expect `OK` and get `UNRECOGNIZED`.

- [ ] **Step 3: Bundle and register the DTDs**

Download the published files:

```bash
curl -fsSL -o src/main/resources/resources/dtds/struts-6.5.dtd https://struts.apache.org/dtds/struts-6.5.dtd
curl -fsSL -o src/main/resources/resources/dtds/struts-7.5.dtd https://struts.apache.org/dtds/struts-7.5.dtd
```

Confirm the public IDs are present and the files were not edited:

```bash
rg "DTD Struts Configuration" src/main/resources/resources/dtds/struts-6.5.dtd src/main/resources/resources/dtds/struts-7.5.dtd
```

Expected: the 6.5 file contains `DTD Struts Configuration 6.5` and the 7.5 file contains `DTD Struts Configuration 7.5`.

In `StrutsConstants`, after the `STRUTS_6_0_DTD_ID` field, add:

```java
@NonNls
public static final String STRUTS_6_5_DTD_URI = "https://struts.apache.org/dtds/struts-6.5.dtd";
@NonNls
public static final String STRUTS_6_5_DTD_ID = "-//Apache Software Foundation//DTD Struts Configuration 6.5//EN";

@NonNls
public static final String STRUTS_7_5_DTD_URI = "https://struts.apache.org/dtds/struts-7.5.dtd";
@NonNls
public static final String STRUTS_7_5_DTD_ID = "-//Apache Software Foundation//DTD Struts Configuration 7.5//EN";
```

Append both URI and ID pairs to `STRUTS_DTDS`, after the 6.0 pair:

```java
STRUTS_6_0_DTD_URI, STRUTS_6_0_DTD_ID,
STRUTS_6_5_DTD_URI, STRUTS_6_5_DTD_ID,
STRUTS_7_5_DTD_URI, STRUTS_7_5_DTD_ID
```

In `Struts2ResourceProvider.registerResources`, after the 6.0 `addDTDResource` call, add:

```java
addDTDResource(StrutsConstants.STRUTS_6_5_DTD_URI,
        StrutsConstants.STRUTS_6_5_DTD_ID,
        "struts-6.5.dtd", registrar);

addDTDResource(StrutsConstants.STRUTS_7_5_DTD_URI,
        StrutsConstants.STRUTS_7_5_DTD_ID,
        "struts-7.5.dtd", registrar);
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test -x rat --tests "StrutsDtdValidatorTest" --no-configuration-cache`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/resources/dtds/struts-6.5.dtd \
  src/main/resources/resources/dtds/struts-7.5.dtd \
  src/main/java/com/intellij/struts2/StrutsConstants.java \
  src/main/java/com/intellij/struts2/Struts2ResourceProvider.java \
  src/test/java/com/intellij/struts2/dom/inspection/StrutsDtdValidatorTest.java
git commit -m "$(cat <<'EOF'
feat: register Struts 6.5 and 7.5 configuration DTDs

Accept the published 6.5 and 7.5 DOCTYPEs from the bundled catalog copies.

EOF
)"
```

### Task 2: Select the 6.5 and 7.5 file templates

**Files:**
- Create: `src/main/resources/fileTemplates/j2ee/struts_6_5.xml.ft`
- Create: `src/main/resources/fileTemplates/j2ee/struts_6_5.xml.html`
- Create: `src/main/resources/fileTemplates/j2ee/struts_7_5.xml.ft`
- Create: `src/main/resources/fileTemplates/j2ee/struts_7_5.xml.html`
- Modify: `src/main/java/com/intellij/struts2/StrutsFileTemplateGroupDescriptorFactory.java`
- Modify: `src/main/java/com/intellij/struts2/StrutsFileTemplateProvider.java`
- Test: `src/test/java/com/intellij/struts2/StrutsFileTemplateProviderTest.java`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: `StrutsFileTemplateProvider.templateNameFor(@Nullable String version)` returning the descriptor name. `StrutsFileTemplateGroupDescriptorFactory.STRUTS_6_5_XML` is `"struts_6_5.xml"`. `STRUTS_7_5_XML` is `"struts_7_5.xml"`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/intellij/struts2/StrutsFileTemplateProviderTest.java`. This test does not start a fixture. It calls the mapping directly and reads the template resources.

```java
package com.intellij.struts2;

import junit.framework.TestCase;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class StrutsFileTemplateProviderTest extends TestCase {

  public void testVersionMapping() {
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_6_0_XML,
                 StrutsFileTemplateProvider.templateNameFor("6.4.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_6_0_XML,
                 StrutsFileTemplateProvider.templateNameFor("7.4.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_6_5_XML,
                 StrutsFileTemplateProvider.templateNameFor("6.5.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_6_5_XML,
                 StrutsFileTemplateProvider.templateNameFor("6.6.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_7_5_XML,
                 StrutsFileTemplateProvider.templateNameFor("7.5.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_7_5_XML,
                 StrutsFileTemplateProvider.templateNameFor("7.6.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_7_5_XML,
                 StrutsFileTemplateProvider.templateNameFor("8.0.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_2_0_XML,
                 StrutsFileTemplateProvider.templateNameFor(null));
  }

  public void testTemplateDoctypes() throws Exception {
    String struts65 = readTemplate("struts_6_5.xml.ft");
    assertTrue(struts65.contains("-//Apache Software Foundation//DTD Struts Configuration 6.5//EN"));
    assertTrue(struts65.contains("https://struts.apache.org/dtds/struts-6.5.dtd"));

    String struts75 = readTemplate("struts_7_5.xml.ft");
    assertTrue(struts75.contains("-//Apache Software Foundation//DTD Struts Configuration 7.5//EN"));
    assertTrue(struts75.contains("https://struts.apache.org/dtds/struts-7.5.dtd"));
  }

  private static String readTemplate(String fileName) throws Exception {
    try (InputStream in = StrutsFileTemplateProvider.class.getClassLoader()
            .getResourceAsStream("fileTemplates/j2ee/" + fileName)) {
      assertNotNull(fileName, in);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test -x rat --tests "StrutsFileTemplateProviderTest" --no-configuration-cache`

Expected: FAIL to compile. `templateNameFor`, `STRUTS_6_5_XML`, and `STRUTS_7_5_XML` do not exist.

- [ ] **Step 3: Add the templates and the version mapping**

`src/main/resources/fileTemplates/j2ee/struts_6_5.xml.ft`:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<!DOCTYPE struts PUBLIC
  "-//Apache Software Foundation//DTD Struts Configuration 6.5//EN"
  "https://struts.apache.org/dtds/struts-6.5.dtd">

<struts>
</struts>
```

`src/main/resources/fileTemplates/j2ee/struts_7_5.xml.ft`:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<!DOCTYPE struts PUBLIC
  "-//Apache Software Foundation//DTD Struts Configuration 7.5//EN"
  "https://struts.apache.org/dtds/struts-7.5.dtd">

<struts>
</struts>
```

`struts_6_5.xml.html`:

```html
<html>
<body style="padding: 2px;">
<span style="font-family: verdana,sans-serif; font-size: smaller;">
This is a built-in template used each time you create a new struts.xml for Struts 6.5.x
</span>
</body>
</html>
```

`struts_7_5.xml.html`:

```html
<html>
<body style="padding: 2px;">
<span style="font-family: verdana,sans-serif; font-size: smaller;">
This is a built-in template used each time you create a new struts.xml for Struts 7.5 and newer
</span>
</body>
</html>
```

In `StrutsFileTemplateGroupDescriptorFactory`, after `STRUTS_6_0_XML`, add:

```java
public static final String STRUTS_6_5_XML = "struts_6_5.xml";

public static final String STRUTS_7_5_XML = "struts_7_5.xml";
```

In `getFileTemplatesDescriptor`, after the `STRUTS_6_0_XML` descriptor, add:

```java
group.addTemplate(new FileTemplateDescriptor(STRUTS_6_5_XML,
                                             StrutsIcons.STRUTS_CONFIG_FILE));
group.addTemplate(new FileTemplateDescriptor(STRUTS_7_5_XML,
                                             StrutsIcons.STRUTS_CONFIG_FILE));
```

Replace `determineFileTemplate` so it loads `templateNameFor(myVersionName)`. Keep `is21orNewer`, `is25orNewer`, and the version fields. Add this package-visible method. A null version returns the 2.0 template name `struts.xml`, which is today's fall-through when every `isNewerThan` check is false.

```java
@NotNull
static String templateNameFor(@Nullable String version) {
  if (version == null) {
    return StrutsFileTemplateGroupDescriptorFactory.STRUTS_2_0_XML;
  }
  if (VersionComparatorUtil.compare(version, "7.5") >= 0) {
    return StrutsFileTemplateGroupDescriptorFactory.STRUTS_7_5_XML;
  }
  if (VersionComparatorUtil.compare(version, "6.5") >= 0
      && VersionComparatorUtil.compare(version, "7.0") < 0) {
    return StrutsFileTemplateGroupDescriptorFactory.STRUTS_6_5_XML;
  }
  if (VersionComparatorUtil.compare(version, "6.0") > 0) {
    return StrutsFileTemplateGroupDescriptorFactory.STRUTS_6_0_XML;
  }
  if (VersionComparatorUtil.compare(version, "2.5") > 0) {
    return StrutsFileTemplateGroupDescriptorFactory.STRUTS_2_5_XML;
  }
  if (VersionComparatorUtil.compare(version, "2.3") > 0) {
    return StrutsFileTemplateGroupDescriptorFactory.STRUTS_2_3_XML;
  }
  if (VersionComparatorUtil.compare(version, "2.1") > 0) {
    return VersionComparatorUtil.compare(version, "2.1.7") > 0
           ? StrutsFileTemplateGroupDescriptorFactory.STRUTS_2_1_7_XML
           : StrutsFileTemplateGroupDescriptorFactory.STRUTS_2_1_XML;
  }
  return StrutsFileTemplateGroupDescriptorFactory.STRUTS_2_0_XML;
}
```

`determineFileTemplate` becomes:

```java
@NotNull
public FileTemplate determineFileTemplate(Project project) {
  final FileTemplateManager fileTemplateManager = FileTemplateManager.getInstance(project);
  return fileTemplateManager.getJ2eeTemplate(templateNameFor(myVersionName));
}
```

The comparisons against `"7.5"` and `"6.5"` are `>=` so Maven versions `7.5.0` and `6.5.0` match. The existing `"6.0"` check stays a strict `>`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew test -x rat --tests "StrutsFileTemplateProviderTest" --no-configuration-cache`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/intellij/struts2/StrutsFileTemplateProvider.java \
  src/main/java/com/intellij/struts2/StrutsFileTemplateGroupDescriptorFactory.java \
  src/main/resources/fileTemplates/j2ee/struts_6_5.xml.ft \
  src/main/resources/fileTemplates/j2ee/struts_6_5.xml.html \
  src/main/resources/fileTemplates/j2ee/struts_7_5.xml.ft \
  src/main/resources/fileTemplates/j2ee/struts_7_5.xml.html \
  src/test/java/com/intellij/struts2/StrutsFileTemplateProviderTest.java
git commit -m "$(cat <<'EOF'
feat: select the Struts 6.5 and 7.5 struts.xml templates

Use the detected struts2-core version so 6.5 and 7.5 modules get the matching DOCTYPE.

EOF
)"
```

### Task 3: Highlight a package that extends a final package

**Files:**
- Modify: `src/main/java/com/intellij/struts2/dom/struts/strutspackage/StrutsPackage.java`
- Modify: `src/main/java/com/intellij/struts2/dom/inspection/Struts2ModelInspectionVisitor.java`
- Modify: `src/main/resources/messages/Struts2Bundle.properties`
- Create: `src/test/testData/strutsXml/highlighting/struts-package-final.xml`
- Test: `src/test/java/com/intellij/struts2/dom/struts/StrutsHighlightingTest.java`

**Interfaces:**
- Consumes: the bundled 7.5 DTD from Task 1.
- Produces: `StrutsPackage.getFinal()` as `GenericAttributeValue<Boolean>`. An omitted attribute means the package is not final.

- [ ] **Step 1: Write the failing test**

Create `src/test/testData/strutsXml/highlighting/struts-package-final.xml` with the same license comment as `struts-action-allowedMethods.xml`, then:

```xml
<!DOCTYPE struts PUBLIC
    "-//Apache Software Foundation//DTD Struts Configuration 7.5//EN"
    "https://struts.apache.org/dtds/struts-7.5.dtd">

<struts>
  <package name="open"/>
  <package name="locked" final="true"/>
  <package name="child" extends="open"/>
  <package name="blocked" extends="<error descr="Parent package is final and unextendable: locked">locked</error>"/>
</struts>
```

Add to `StrutsHighlightingTest`:

```java
public void testFinalPackageParent() {
  performHighlightingTest("struts-package-final.xml");
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test -x rat --tests "StrutsHighlightingTest.testFinalPackageParent" --no-configuration-cache`

Expected: FAIL. The `final` attribute is unknown to the DOM, or the extends value `locked` is not highlighted.

- [ ] **Step 3: Model `final` and report a final parent**

On `StrutsPackage`, next to `getAbstract()`, add:

```java
GenericAttributeValue<Boolean> getFinal();
```

In `Struts2Bundle.properties`, next to the namespace message, add:

```properties
dom.highlighting.struts.package.final.parent=Parent package is final and unextendable: {0}
```

At the end of `visitStrutsPackage`, after the namespace check, add:

```java
final List<StrutsPackage> parents = strutsPackage.getExtends().getValue();
if (parents == null) {
  return;
}
for (StrutsPackage parent : parents) {
  if (parent != null && Boolean.TRUE.equals(parent.getFinal().getValue())) {
    holder.createProblem(strutsPackage.getExtends(),
                         StrutsBundle.message("dom.highlighting.struts.package.final.parent",
                                              parent.getName().getStringValue()));
    return;
  }
}
```

Leave an unresolved parent to the existing `extends` converter. A null entry in `parents` is unresolved and is skipped here.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew test -x rat --tests "StrutsHighlightingTest.testFinalPackageParent" --no-configuration-cache`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/intellij/struts2/dom/struts/strutspackage/StrutsPackage.java \
  src/main/java/com/intellij/struts2/dom/inspection/Struts2ModelInspectionVisitor.java \
  src/main/resources/messages/Struts2Bundle.properties \
  src/test/testData/strutsXml/highlighting/struts-package-final.xml \
  src/test/java/com/intellij/struts2/dom/struts/StrutsHighlightingTest.java
git commit -m "$(cat <<'EOF'
feat: highlight a package that extends a final package

Struts rejects that parent at startup, so the extends value is marked in the editor.

EOF
)"
```

### Task 4: Complete and check scoped constants

**Files:**
- Create: `src/main/java/com/intellij/struts2/dom/struts/strutspackage/ScopedConstant.java`
- Create: `src/main/java/com/intellij/struts2/dom/struts/strutspackage/ScopedConstantNameConverter.java`
- Modify: `src/main/java/com/intellij/struts2/dom/struts/strutspackage/StrutsPackage.java`
- Modify: `src/main/java/com/intellij/struts2/dom/struts/constant/ConstantValueConverterImpl.java`
- Create: `src/test/testData/strutsXml/highlighting/struts-scoped-constant.xml`
- Create: `src/test/testData/strutsXml/completion/struts-completionvariants-scoped-constant-name.xml`
- Create: `src/test/testData/strutsXml/completion/struts-completionvariants-scoped-constant-theme-value.xml`
- Test: `src/test/java/com/intellij/struts2/dom/struts/StrutsHighlightingTest.java`
- Test: `src/test/java/com/intellij/struts2/dom/struts/StrutsCompletionTest.java`

**Interfaces:**
- Consumes: `StrutsPackage` from Task 3, `StrutsConstantManager.getConstants`, `ConstantValueConverter`.
- Produces: `StrutsPackage.getScopedConstants()` as `List<ScopedConstant>`. `ScopedConstant.getName()` and `getValue()`. `ScopedConstantNameConverter` accepts only the scopable names that are registered for the module.

- [ ] **Step 1: Write the failing tests**

`src/test/testData/strutsXml/highlighting/struts-scoped-constant.xml`, with the license comment and the 7.5 DOCTYPE from Task 3:

```xml
<struts>
  <package name="ui">
    <scoped-constant name="struts.ui.theme" value="html5"/>
    <scoped-constant name="<error descr="Constant 'struts.devMode' is not scopable">struts.devMode</error>" value="true"/>
  </package>
</struts>
```

`src/test/testData/strutsXml/completion/struts-completionvariants-scoped-constant-name.xml`:

```xml
<!DOCTYPE struts PUBLIC
    "-//Apache Software Foundation//DTD Struts Configuration 7.5//EN"
    "https://struts.apache.org/dtds/struts-7.5.dtd">

<struts>
  <package name="ui">
    <scoped-constant name="<caret>" value=""/>
  </package>
</struts>
```

`src/test/testData/strutsXml/completion/struts-completionvariants-scoped-constant-theme-value.xml`:

```xml
<!DOCTYPE struts PUBLIC
    "-//Apache Software Foundation//DTD Struts Configuration 7.5//EN"
    "https://struts.apache.org/dtds/struts-7.5.dtd">

<struts>
  <package name="ui">
    <scoped-constant name="struts.ui.theme" value="<caret>"/>
  </package>
</struts>
```

The two completion files also get the license comment.

Add to `StrutsHighlightingTest`:

```java
public void testScopedConstantName() {
  performHighlightingTest("struts-scoped-constant.xml");
}
```

Add to `StrutsCompletionTest`:

```java
public void testCompletionVariantsScopedConstantName() {
  performCompletionVariantTest("struts-completionvariants-scoped-constant-name.xml",
                               "struts.ui.templateDir", "struts.ui.theme");
}

public void testCompletionVariantsScopedConstantThemeValue() {
  performCompletionVariantTest("struts-completionvariants-scoped-constant-theme-value.xml",
                               "ajax", "css_xhtml", "html5", "simple", "xhtml");
}
```

`performCompletionVariantTest` requires the exact variant set. The default fixture has no Convention plugin, so `struts.convention.result.path` must not be offered.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test -x rat --tests "StrutsHighlightingTest.testScopedConstantName" --tests "StrutsCompletionTest.testCompletionVariantsScopedConstantName" --tests "StrutsCompletionTest.testCompletionVariantsScopedConstantThemeValue" --no-configuration-cache`

Expected: FAIL. `<scoped-constant>` is not a DOM element, so the name is not checked and completion has no variants.

- [ ] **Step 3: Add the element, the name converter, and value reuse**

Create `ScopedConstant.java` in package `com.intellij.struts2.dom.struts.strutspackage`. Import `com.intellij.ide.presentation.Presentation`, `com.intellij.struts2.dom.struts.constant.ConstantValueConverter`, `com.intellij.util.xml.Convert`, `com.intellij.util.xml.DomElement`, `com.intellij.util.xml.GenericAttributeValue`, `com.intellij.util.xml.NameValue`, `com.intellij.util.xml.Required`, and `com.intellij.util.xml.Stubbed`.

```java
@Stubbed
@Presentation(typeName = "Scoped Constant", icon = "AllIcons.Nodes.Parameter")
public interface ScopedConstant extends DomElement {

  @NameValue
  @Required
  @Convert(ScopedConstantNameConverter.class)
  @Stubbed
  GenericAttributeValue<String> getName();

  @Required(nonEmpty = false)
  @Convert(ConstantValueConverter.class)
  @Stubbed
  GenericAttributeValue<String> getValue();
}
```

Create `ScopedConstantNameConverter.java` in the same package. Import `com.intellij.openapi.module.Module`, `com.intellij.struts2.model.constant.StrutsConstant`, `com.intellij.struts2.model.constant.StrutsConstantManager`, `com.intellij.util.containers.ContainerUtil`, `com.intellij.util.xml.ConvertContext`, and `com.intellij.util.xml.ResolvingConverter`. Use `java.util.Collection`, `List`, and `Set`. It extends `ResolvingConverter.StringConverter`.

```java
public class ScopedConstantNameConverter extends ResolvingConverter.StringConverter {

  static final List<String> SCOPABLE_NAMES = List.of(
      "struts.ui.theme",
      "struts.ui.templateDir",
      "struts.convention.result.path");

  @Override
  public String fromString(@Nullable String name, ConvertContext context) {
    if (name == null) {
      return null;
    }
    return getVariants(context).contains(name) ? name : null;
  }

  @Override
  public @NotNull Collection<String> getVariants(@NotNull ConvertContext context) {
    final Module module = context.getModule();
    if (module == null) {
      return List.of();
    }
    final StrutsConstantManager constantManager = StrutsConstantManager.getInstance(module.getProject());
    final Set<String> registered = ContainerUtil.map2Set(constantManager.getConstants(module), StrutsConstant::getName);
    return ContainerUtil.filter(SCOPABLE_NAMES, registered::contains);
  }

  @Override
  public String getErrorMessage(@Nullable String name, ConvertContext context) {
    return "Constant '" + name + "' is not scopable";
  }
}
```

On `StrutsPackage`, add:

```java
@SubTagList("scoped-constant")
@NotNull
List<ScopedConstant> getScopedConstants();
```

In `ConstantValueConverterImpl.getConverter`, replace the cast to `Constant` with:

```java
final DomElement parent = domElement.getParent();
final String constantName;
if (parent instanceof Constant constant) {
  constantName = constant.getName().getStringValue();
} else if (parent instanceof ScopedConstant scopedConstant) {
  constantName = scopedConstant.getName().getStringValue();
} else {
  return null;
}
if (StringUtil.isEmpty(constantName)) {
  return null;
}
```

Keep the `xmlTag` null check and the `findConverter` call, and pass `constantName` to `StrutsConstantKey.create`. Import `com.intellij.util.xml.DomElement` and `ScopedConstant`. Delete the old cast to `Constant` and the `assert constant != null`.

No `plugin.xml` registration. Both converter classes are concrete, and `@Convert` points at them directly. `ConstantValueConverter` is already registered.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test -x rat --tests "StrutsHighlightingTest.testScopedConstantName" --tests "StrutsCompletionTest.testCompletionVariantsScopedConstantName" --tests "StrutsCompletionTest.testCompletionVariantsScopedConstantThemeValue" --no-configuration-cache`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/intellij/struts2/dom/struts/strutspackage/ScopedConstant.java \
  src/main/java/com/intellij/struts2/dom/struts/strutspackage/ScopedConstantNameConverter.java \
  src/main/java/com/intellij/struts2/dom/struts/strutspackage/StrutsPackage.java \
  src/main/java/com/intellij/struts2/dom/struts/constant/ConstantValueConverterImpl.java \
  src/test/testData/strutsXml/highlighting/struts-scoped-constant.xml \
  src/test/testData/strutsXml/completion/struts-completionvariants-scoped-constant-name.xml \
  src/test/testData/strutsXml/completion/struts-completionvariants-scoped-constant-theme-value.xml \
  src/test/java/com/intellij/struts2/dom/struts/StrutsHighlightingTest.java \
  src/test/java/com/intellij/struts2/dom/struts/StrutsCompletionTest.java
git commit -m "$(cat <<'EOF'
feat: complete and check package scoped constants

Only the built-in scopable names resolve, and their values reuse the constant value converter.

EOF
)"
```

### Task 5: Complete and check `http-method`

**Files:**
- Create: `src/main/java/com/intellij/struts2/dom/struts/action/HttpMethodConverter.java`
- Modify: `src/main/java/com/intellij/struts2/dom/struts/action/Action.java`
- Create: `src/test/testData/strutsXml/highlighting/struts-http-method.xml`
- Create: `src/test/testData/strutsXml/completion/struts-completionvariants-http-method.xml`
- Test: `src/test/java/com/intellij/struts2/dom/struts/StrutsHighlightingTest.java`
- Test: `src/test/java/com/intellij/struts2/dom/struts/StrutsCompletionTest.java`

**Interfaces:**
- Consumes: `Action`.
- Produces: `Action.getHttpMethod()` as `GenericAttributeValue<String>` with `@Attribute("http-method")` and `@Convert(HttpMethodConverter.class)`. `HttpMethodConverter.HTTP_METHODS` is the nine uppercase names.

- [ ] **Step 1: Write the failing tests**

`struts-http-method.xml` with the license comment and the 7.5 DOCTYPE:

```xml
<struts>
  <package name="web">
    <action name="save" http-method="post,<error descr="Cannot resolve http-method 'FOO'">FOO</error>"/>
  </package>
</struts>
```

`struts-completionvariants-http-method.xml`:

```xml
<!DOCTYPE struts PUBLIC
    "-//Apache Software Foundation//DTD Struts Configuration 7.5//EN"
    "https://struts.apache.org/dtds/struts-7.5.dtd">

<struts>
  <package name="web">
    <action name="save" http-method="<caret>"/>
  </package>
</struts>
```

Add:

```java
public void testHttpMethod() {
  performHighlightingTest("struts-http-method.xml");
}
```

```java
public void testCompletionVariantsHttpMethod() {
  performCompletionVariantTest("struts-completionvariants-http-method.xml",
                               "CONNECT", "DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT", "TRACE");
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew test -x rat --tests "StrutsHighlightingTest.testHttpMethod" --tests "StrutsCompletionTest.testCompletionVariantsHttpMethod" --no-configuration-cache`

Expected: FAIL. `http-method` has no converter, so `FOO` is not highlighted and completion is empty.

- [ ] **Step 3: Add the converter**

Create `HttpMethodConverter` in package `com.intellij.struts2.dom.struts.action`. Import `com.intellij.psi.PsiElement`, `com.intellij.util.xml.ConvertContext`, `com.intellij.util.xml.DomUtil`, `com.intellij.util.xml.GenericDomValue`, `com.intellij.util.xml.converters.DelimitedListConverter`, `org.jetbrains.annotations.Nullable`, `java.util.List`, and `java.util.Locale`. Extend `DelimitedListConverter<String>`.

```java
public class HttpMethodConverter extends DelimitedListConverter<String> {

  static final List<String> HTTP_METHODS = List.of(
      "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "OPTIONS", "CONNECT", "PATCH");

  public HttpMethodConverter() {
    super(",");
  }

  @Override
  protected String convertString(@Nullable String token, ConvertContext context) {
    if (token != null && HTTP_METHODS.contains(token.toUpperCase(Locale.ROOT))) {
      return token;
    }
    return null;
  }

  @Override
  protected String toString(@Nullable String token) {
    return token;
  }

  @Override
  protected Object[] getReferenceVariants(ConvertContext context,
                                          GenericDomValue<? extends List<String>> value) {
    return HTTP_METHODS.toArray();
  }

  @Override
  protected PsiElement resolveReference(@Nullable String token, ConvertContext context) {
    if (token != null && HTTP_METHODS.contains(token.toUpperCase(Locale.ROOT))) {
      return DomUtil.getValueElement(context.getInvocationElement());
    }
    return null;
  }

  @Override
  protected String getUnresolvedMessage(String token) {
    return "Cannot resolve http-method '" + token + "'";
  }
}
```

On `Action`, next to `getMethod()`, add:

```java
@Attribute("http-method")
@Convert(HttpMethodConverter.class)
GenericAttributeValue<String> getHttpMethod();
```

`post` is kept as written because `convertString` returns the original token. Completion offers the uppercase names.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew test -x rat --tests "StrutsHighlightingTest.testHttpMethod" --tests "StrutsCompletionTest.testCompletionVariantsHttpMethod" --no-configuration-cache`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/intellij/struts2/dom/struts/action/HttpMethodConverter.java \
  src/main/java/com/intellij/struts2/dom/struts/action/Action.java \
  src/test/testData/strutsXml/highlighting/struts-http-method.xml \
  src/test/testData/strutsXml/completion/struts-completionvariants-http-method.xml \
  src/test/java/com/intellij/struts2/dom/struts/StrutsHighlightingTest.java \
  src/test/java/com/intellij/struts2/dom/struts/StrutsCompletionTest.java
git commit -m "$(cat <<'EOF'
feat: complete and check action http-method

Accept the nine HTTP methods in any case and highlight any other token.

EOF
)"
```

### Task 6: Check `extension` on package and action

**Files:**
- Create: `src/main/java/com/intellij/struts2/dom/struts/strutspackage/ExtensionListConverter.java`
- Modify: `src/main/java/com/intellij/struts2/dom/struts/strutspackage/StrutsPackage.java`
- Modify: `src/main/java/com/intellij/struts2/dom/struts/action/Action.java`
- Create: `src/test/testData/strutsXml/highlighting/struts-extension.xml`
- Test: `src/test/java/com/intellij/struts2/dom/struts/StrutsHighlightingTest.java`

**Interfaces:**
- Consumes: `StrutsPackage` and `Action`.
- Produces: `StrutsPackage.getExtension()` and `Action.getExtension()`, both `GenericAttributeValue<String>` converted by `ExtensionListConverter`. There are no completion variants.

- [ ] **Step 1: Write the failing test**

`struts-extension.xml` with the license comment and the 7.5 DOCTYPE:

```xml
<struts>
  <package name="web" extension="action,<error descr="Invalid extension '.do'">.do</error>,<error descr="Invalid extension 'a/b'">a/b</error>,<error descr="Invalid extension 'x y'">x y</error>,">
    <action name="plain" extension=","/>
  </package>
</struts>
```

The package value ends with a comma, which is an empty token and must stay unmarked. The action value `","` is two empty tokens and must stay unmarked.

Add:

```java
public void testExtensionTokens() {
  performHighlightingTest("struts-extension.xml");
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test -x rat --tests "StrutsHighlightingTest.testExtensionTokens" --no-configuration-cache`

Expected: FAIL. `extension` is not checked, so `.do`, `a/b`, and `x y` are not highlighted.

- [ ] **Step 3: Add the converter to both elements**

Create `ExtensionListConverter` in package `com.intellij.struts2.dom.struts.strutspackage`. Import `com.intellij.psi.PsiElement`, `com.intellij.util.ArrayUtilRt`, `com.intellij.util.xml.ConvertContext`, `com.intellij.util.xml.DomUtil`, `com.intellij.util.xml.GenericDomValue`, `com.intellij.util.xml.converters.DelimitedListConverter`, `org.jetbrains.annotations.NotNull`, and `org.jetbrains.annotations.Nullable`. Extend `DelimitedListConverter<String>`.

```java
public class ExtensionListConverter extends DelimitedListConverter<String> {

  public ExtensionListConverter() {
    super(",");
  }

  static boolean isLegal(@NotNull String token) {
    return !token.startsWith(".") && !token.contains("/") && !containsWhitespace(token);
  }

  private static boolean containsWhitespace(String token) {
    for (int i = 0; i < token.length(); i++) {
      if (Character.isWhitespace(token.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected String convertString(@Nullable String token, ConvertContext context) {
    if (token == null || token.isEmpty() || isLegal(token)) {
      return token;
    }
    return null;
  }

  @Override
  protected String toString(@Nullable String token) {
    return token;
  }

  @Override
  protected Object[] getReferenceVariants(ConvertContext context,
                                          GenericDomValue<? extends List<String>> value) {
    return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
  }

  @Override
  protected PsiElement resolveReference(@Nullable String token, ConvertContext context) {
    if (token == null || token.isEmpty() || isLegal(token)) {
      return DomUtil.getValueElement(context.getInvocationElement());
    }
    return null;
  }

  @Override
  protected String getUnresolvedMessage(String token) {
    return "Invalid extension '" + token + "'";
  }
}
```

Do not trim. A token `" do"` contains whitespace and is illegal.

On `StrutsPackage` and on `Action`, add:

```java
@Convert(ExtensionListConverter.class)
GenericAttributeValue<String> getExtension();
```

`Action` must import `ExtensionListConverter`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew test -x rat --tests "StrutsHighlightingTest.testExtensionTokens" --no-configuration-cache`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/intellij/struts2/dom/struts/strutspackage/ExtensionListConverter.java \
  src/main/java/com/intellij/struts2/dom/struts/strutspackage/StrutsPackage.java \
  src/main/java/com/intellij/struts2/dom/struts/action/Action.java \
  src/test/testData/strutsXml/highlighting/struts-extension.xml \
  src/test/java/com/intellij/struts2/dom/struts/StrutsHighlightingTest.java
git commit -m "$(cat <<'EOF'
feat: check package and action extension tokens

Highlight an extension that starts with a dot, contains a slash, or contains whitespace.

EOF
)"
```

### Task 7: Note the change and run the suite

**Files:**
- Modify: `CHANGELOG.md`

**Interfaces:**
- Consumes: the behavior from Tasks 1–6.
- Produces: an unreleased changelog entry for issue 147.

- [ ] **Step 1: Add the changelog note**

Under `## [Unreleased]`, add an `### Added` section before `### Changed` if it is not already there:

```markdown
### Added

- Support the published Struts 6.5 and 7.5 configuration DTDs, including `<package final>` and `extension`, `<scoped-constant>`, and `<action http-method>` ([#147](https://github.com/apache/struts-intellij-plugin/issues/147))
```

- [ ] **Step 2: Run the focused tests and the suite**

```bash
./gradlew test -x rat --tests "StrutsDtdValidatorTest" --tests "StrutsHighlightingTest" --tests "StrutsCompletionTest" --tests "StrutsFileTemplateProviderTest" --no-configuration-cache
./gradlew test -x rat --no-configuration-cache
```

Expected: PASS. `StrutsCompletionTest.testCompletionVariantsPackageExtends` stays ignored; it is already named `_testCompletionVariantsPackageExtends`.

- [ ] **Step 3: Commit**

```bash
git add CHANGELOG.md
git commit -m "$(cat <<'EOF'
docs: note Struts 6.5 and 7.5 configuration grammar

EOF
)"
```
