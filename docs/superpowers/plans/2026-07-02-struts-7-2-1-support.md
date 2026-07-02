# Struts 7.2.1 Metadata Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update static Struts metadata so Struts 7.2.1 projects get current completion values and modern web.xml filter support.

**Architecture:** Keep the change local to existing metadata providers and web.xml filter predicates. Extend the string-value converter to support deprecated completion presentation, then reuse it for `struts.ui.theme`; update JSP tag theme completion independently with the same visible values. Tests stay focused on completion variants, deprecated lookup presentation, and web.xml filter matching.

**Tech Stack:** Java, IntelliJ Platform PSI/completion APIs, IntelliJ light fixture tests, Gradle test tasks.

---

## File Structure

- Modify `src/main/java/com/intellij/struts2/model/constant/contributor/StringValuesConverter.java` to support deprecated lookup presentation for selected values.
- Modify `src/main/java/com/intellij/struts2/model/constant/contributor/StrutsConstantContributorBase.java` to add a helper for string-valued constants with deprecated values.
- Modify `src/main/java/com/intellij/struts2/model/constant/contributor/StrutsCoreConstantContributor.java` to add Struts 7.2.1 constants and updated value variants.
- Modify `src/main/java/com/intellij/struts2/reference/jsp/ThemeReferenceProvider.java` to add `css_xhtml`, `html5`, and deprecated `ajax` tag theme completion.
- Modify `src/main/java/com/intellij/struts2/model/constant/StrutsConstantManagerImpl.java` to include `STRUTS_2_5_FILTER_CLASS` in web.xml filter detection.
- Modify `src/main/java/com/intellij/struts2/reference/StrutsReferenceContributor.java` to register web.xml param references for `STRUTS_2_5_FILTER_CLASS`.
- Modify `src/test/java/com/intellij/struts2/dom/struts/StrutsCompletionTest.java` to cover new constant names and value variants.
- Create `src/test/java/com/intellij/struts2/model/constant/contributor/StringValuesConverterTest.java` to verify deprecated lookup presentation.
- Modify `src/test/java/com/intellij/struts2/reference/web/WebXmlConstantTest.java` and add test fixtures for modern filter completion.
- Add test data under `src/test/testData/strutsXml/completion/` and `src/test/testData/reference/web/constant/WEB-INF/`.

---

### Task 1: Add Completion Tests For Struts 7.2.1 Constant Metadata

**Files:**
- Modify: `src/test/java/com/intellij/struts2/dom/struts/StrutsCompletionTest.java`
- Create: `src/test/testData/strutsXml/completion/struts-completionvariants-constant_theme_value.xml`
- Create: `src/test/testData/strutsXml/completion/struts-completionvariants-multipart_parser_value.xml`

- [ ] **Step 1: Add constant value completion fixtures**

Create `src/test/testData/strutsXml/completion/struts-completionvariants-constant_theme_value.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<!DOCTYPE struts PUBLIC
    "-//Apache Software Foundation//DTD Struts Configuration 2.0//EN"
    "http://struts.apache.org/dtds/struts-2.0.dtd">

<struts>
  <constant name="struts.ui.theme" value="<caret>"/>
</struts>
```

Create `src/test/testData/strutsXml/completion/struts-completionvariants-multipart_parser_value.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<!DOCTYPE struts PUBLIC
    "-//Apache Software Foundation//DTD Struts Configuration 2.0//EN"
    "http://struts.apache.org/dtds/struts-2.0.dtd">

<struts>
  <constant name="struts.multipart.parser" value="<caret>"/>
</struts>
```

- [ ] **Step 2: Add failing completion tests**

In `StrutsCompletionTest`, add these methods after `testCompletionVariantsConstantName()`:

```java
  public void testCompletionVariantsStruts721ConstantNames() {
    performCompletionVariantTest("struts-completionvariants-constant_name.xml",
                                 "struts.parameters.requireAnnotations",
                                 "struts.parameters.requireAnnotations.transitionMode",
                                 "struts.chaining.requireAnnotations");
  }

  public void testCompletionVariantsThemeConstantValue() {
    performCompletionVariantTest("struts-completionvariants-constant_theme_value.xml",
                                 "ajax", "css_xhtml", "html5", "simple", "xhtml");
  }

  public void testCompletionVariantsMultipartParserConstantValue() {
    performCompletionVariantTest("struts-completionvariants-multipart_parser_value.xml",
                                 "cos", "jakarta", "jakarta-stream", "pell");
  }
```

- [ ] **Step 3: Run tests and verify failure**

Run:

```bash
./gradlew test -x rat --tests "StrutsCompletionTest" --no-configuration-cache
```

Expected: `testCompletionVariantsStruts721ConstantNames`, `testCompletionVariantsThemeConstantValue`, and `testCompletionVariantsMultipartParserConstantValue` fail because the new constants/values are not yet present.

- [ ] **Step 4: Commit failing tests**

```bash
git add src/test/java/com/intellij/struts2/dom/struts/StrutsCompletionTest.java \
  src/test/testData/strutsXml/completion/struts-completionvariants-constant_theme_value.xml \
  src/test/testData/strutsXml/completion/struts-completionvariants-multipart_parser_value.xml
git commit -m "$(cat <<'EOF'
test: cover Struts 7.2.1 constant metadata completions

EOF
)"
```

---

### Task 2: Implement Struts 7.2.1 Constant Metadata

**Files:**
- Modify: `src/main/java/com/intellij/struts2/model/constant/contributor/StringValuesConverter.java`
- Modify: `src/main/java/com/intellij/struts2/model/constant/contributor/StrutsConstantContributorBase.java`
- Modify: `src/main/java/com/intellij/struts2/model/constant/contributor/StrutsCoreConstantContributor.java`
- Create: `src/test/java/com/intellij/struts2/model/constant/contributor/StringValuesConverterTest.java`

- [ ] **Step 1: Add deprecated lookup presentation test**

Create `src/test/java/com/intellij/struts2/model/constant/contributor/StringValuesConverterTest.java`:

```java
/*
 * Copyright 2026 The authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.struts2.model.constant.contributor;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import junit.framework.TestCase;

import java.util.Collections;

public class StringValuesConverterTest extends TestCase {
  public void testDeprecatedValueUsesStrikeoutLookupPresentation() {
    StringValuesConverter converter = new StringValuesConverter(
      new String[]{"ajax", "html5"},
      Collections.singleton("ajax")
    );

    LookupElement lookupElement = converter.createLookupElement("ajax");
    LookupElementPresentation presentation = new LookupElementPresentation();
    lookupElement.renderElement(presentation);

    assertTrue(presentation.isStrikeout());
  }

  public void testRegularValueDoesNotUseStrikeoutLookupPresentation() {
    StringValuesConverter converter = new StringValuesConverter(
      new String[]{"ajax", "html5"},
      Collections.singleton("ajax")
    );

    LookupElement lookupElement = converter.createLookupElement("html5");
    LookupElementPresentation presentation = new LookupElementPresentation();
    lookupElement.renderElement(presentation);

    assertFalse(presentation.isStrikeout());
  }
}
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
./gradlew test -x rat --tests "StringValuesConverterTest" --no-configuration-cache
```

Expected: compilation fails because `StringValuesConverter(String[], Collection<String>)` does not exist.

- [ ] **Step 3: Implement deprecated string value support**

Update `StringValuesConverter.java` to this full content:

```java
/*
 * Copyright 2009 The authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.struts2.model.constant.contributor;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * Resolves to list of given Strings.
 */
class StringValuesConverter extends ResolvingConverter.StringConverter {

  private final String[] values;
  private final Set<String> deprecatedValues;

  StringValuesConverter(@NonNls final String... values) {
    this(values, Set.of());
  }

  StringValuesConverter(@NonNls final String[] values, @NotNull final Collection<String> deprecatedValues) {
    Arrays.sort(values);
    this.values = values;
    this.deprecatedValues = ContainerUtil.newHashSet(deprecatedValues);
  }

  @Override
  public String fromString(final String s, final ConvertContext context) {
    return Arrays.binarySearch(values, s) > -1 ? s : null;
  }

  @Override
  public @NotNull LookupElement createLookupElement(String s) {
    return LookupElementBuilder.create(s).withStrikeoutness(deprecatedValues.contains(s));
  }

  @Override
  @NotNull
  public Collection<String> getVariants(final ConvertContext context) {
    return Arrays.asList(values);
  }

}
```

- [ ] **Step 4: Add base helper for deprecated string values**

In `StrutsConstantContributorBase.java`, add `import java.util.Arrays;`.

Then add this helper after `addStringValuesProperty(...)`:

```java
  protected static StrutsConstant addStringValuesPropertyWithDeprecatedValues(@NonNls final String propertyName,
                                                                             @NonNls final String[] values,
                                                                             @NonNls final String... deprecatedValues) {
    return new StrutsConstant(propertyName, new StringValuesConverter(values, Arrays.asList(deprecatedValues)));
  }
```

- [ ] **Step 5: Update core metadata**

In `StrutsCoreConstantContributor.java`, replace:

```java
      addStringValuesProperty("struts.multipart.parser", "cos", "pell", "jakarta"),
```

with:

```java
      addStringValuesProperty("struts.multipart.parser", "cos", "pell", "jakarta", "jakarta-stream"),
```

Replace:

```java
      addStringValuesProperty("struts.ui.theme", "simple", "xhtml", "ajax"),
```

with:

```java
      addStringValuesPropertyWithDeprecatedValues("struts.ui.theme",
                                                 new String[]{"simple", "xhtml", "css_xhtml", "html5", "ajax"},
                                                 "ajax"),
```

Replace the final constant:

```java
      addBooleanProperty("struts.ognl.allowStaticMethodAccess")
```

with:

```java
      addBooleanProperty("struts.ognl.allowStaticMethodAccess"),
      addBooleanProperty("struts.parameters.requireAnnotations"),
      addBooleanProperty("struts.parameters.requireAnnotations.transitionMode"),
      addBooleanProperty("struts.chaining.requireAnnotations")
```

- [ ] **Step 6: Run constant tests**

Run:

```bash
./gradlew test -x rat --tests "StringValuesConverterTest" --tests "StrutsCompletionTest" --no-configuration-cache
```

Expected: all tests in `StringValuesConverterTest` pass, and the new `StrutsCompletionTest` methods pass.

- [ ] **Step 7: Commit constant metadata implementation**

```bash
git add src/main/java/com/intellij/struts2/model/constant/contributor/StringValuesConverter.java \
  src/main/java/com/intellij/struts2/model/constant/contributor/StrutsConstantContributorBase.java \
  src/main/java/com/intellij/struts2/model/constant/contributor/StrutsCoreConstantContributor.java \
  src/test/java/com/intellij/struts2/model/constant/contributor/StringValuesConverterTest.java
git commit -m "$(cat <<'EOF'
feat: update Struts 7.2.1 constant metadata

EOF
)"
```

---

### Task 3: Update Tag Theme Completion

**Files:**
- Modify: `src/main/java/com/intellij/struts2/reference/jsp/ThemeReferenceProvider.java`
- Create: `src/test/java/com/intellij/struts2/reference/jsp/ThemeReferenceProviderTest.java`

- [ ] **Step 1: Add direct provider test for tag theme lookup values**

Create `src/test/java/com/intellij/struts2/reference/jsp/ThemeReferenceProviderTest.java`:

```java
/*
 * Copyright 2026 The authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.struts2.reference.jsp;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ThemeReferenceProviderTest extends TestCase {
  public void testDefaultThemeVariantsIncludeCurrentAndLegacyThemes() {
    Object[] variants = ThemeReferenceProvider.getDefaultThemes();
    Set<String> lookupStrings = Arrays.stream(variants)
      .map(variant -> ((LookupElement) variant).getLookupString())
      .collect(Collectors.toSet());

    assertEquals(Set.of("simple", "xhtml", "css_xhtml", "html5", "ajax"), lookupStrings);
  }

  public void testAjaxThemeVariantIsDeprecated() {
    LookupElement ajax = Arrays.stream(ThemeReferenceProvider.getDefaultThemes())
      .map(variant -> (LookupElement) variant)
      .filter(variant -> "ajax".equals(variant.getLookupString()))
      .findFirst()
      .orElseThrow();

    LookupElementPresentation presentation = new LookupElementPresentation();
    ajax.renderElement(presentation);

    assertTrue(presentation.isStrikeout());
  }
}
```

- [ ] **Step 2: Run provider test and verify failure**

Run:

```bash
./gradlew test -x rat --tests "ThemeReferenceProviderTest" --no-configuration-cache
```

Expected: compilation fails because `ThemeReferenceProvider.getDefaultThemes()` does not exist.

- [ ] **Step 3: Implement tag theme variants**

In `ThemeReferenceProvider.java`, replace the existing `DEFAULT_THEMES` block with:

```java
  private static final Object[] DEFAULT_THEMES = new Object[]{
    LookupElementBuilder.create("simple").withIcon(StrutsIcons.THEME),
    LookupElementBuilder.create("xhtml").withIcon(StrutsIcons.THEME),
    LookupElementBuilder.create("css_xhtml").withIcon(StrutsIcons.THEME),
    LookupElementBuilder.create("html5").withIcon(StrutsIcons.THEME),
    LookupElementBuilder.create("ajax").withIcon(StrutsIcons.THEME).withStrikeoutness(true)
  };

  static Object[] getDefaultThemes() {
    return DEFAULT_THEMES;
  }
```

- [ ] **Step 4: Run tag theme tests**

Run:

```bash
./gradlew test -x rat --tests "ThemeReferenceProviderTest" --no-configuration-cache
```

Expected: `ThemeReferenceProviderTest` passes.

- [ ] **Step 5: Commit tag theme completion implementation**

```bash
git add src/main/java/com/intellij/struts2/reference/jsp/ThemeReferenceProvider.java \
  src/test/java/com/intellij/struts2/reference/jsp/ThemeReferenceProviderTest.java
git commit -m "$(cat <<'EOF'
feat: update Struts theme completion variants

EOF
)"
```

---

### Task 4: Add Modern Struts Filter Detection For web.xml Constants

**Files:**
- Modify: `src/main/java/com/intellij/struts2/model/constant/StrutsConstantManagerImpl.java`
- Modify: `src/main/java/com/intellij/struts2/reference/StrutsReferenceContributor.java`
- Modify: `src/test/java/com/intellij/struts2/reference/web/WebXmlConstantTest.java`
- Create: `src/test/testData/reference/web/constant/WEB-INF/web_name_completion_modern_filter.xml`
- Create: `src/test/testData/reference/web/constant/WEB-INF/web_value_completion_modern_filter.xml`

- [ ] **Step 1: Add web.xml fixtures for modern filter class**

Create `src/test/testData/reference/web/constant/WEB-INF/web_name_completion_modern_filter.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<web-app version="2.4"
         xmlns="http://java.sun.com/xml/ns/j2ee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd">

  <filter>
    <filter-name>struts2</filter-name>
    <filter-class>org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter</filter-class>

    <init-param>
      <param-name><caret></param-name>
      <param-value></param-value>
    </init-param>
  </filter>

</web-app>
```

Create `src/test/testData/reference/web/constant/WEB-INF/web_value_completion_modern_filter.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<web-app version="2.4"
         xmlns="http://java.sun.com/xml/ns/j2ee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd">

  <filter>
    <filter-name>struts2</filter-name>
    <filter-class>org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter</filter-class>

    <init-param>
      <param-name>struts.url.includeParams</param-name>
      <param-value><caret></param-value>
    </init-param>
  </filter>

</web-app>
```

- [ ] **Step 2: Add failing web.xml tests**

In `WebXmlConstantTest`, add:

```java
  public void testNameCompletionWithModernFilterClass() {
    final StrutsCoreConstantContributor coreConstantContributor = new StrutsCoreConstantContributor();
    final List<StrutsConstant> constants = coreConstantContributor.getStrutsConstantDefinitions(getModule());
    final String[] variants = ContainerUtil.map2Array(constants, String.class, strutsConstant -> strutsConstant.getName());
    myFixture.testCompletionVariants("/WEB-INF/web_name_completion_modern_filter.xml", variants);
  }

  public void testValueCompletionWithModernFilterClass() {
    myFixture.testCompletionVariants("/WEB-INF/web_value_completion_modern_filter.xml",
                                     "none", "get", "all");
  }
```

- [ ] **Step 3: Run tests and verify failure**

Run:

```bash
./gradlew test -x rat --tests "WebXmlConstantTest" --no-configuration-cache
```

Expected: the new modern-filter completion tests fail because reference providers are not registered for `STRUTS_2_5_FILTER_CLASS`.

- [ ] **Step 4: Update constant manager filter detection**

In `StrutsConstantManagerImpl`, replace:

```java
    return InheritanceUtil.isInheritor(filterClass, StrutsConstants.STRUTS_2_0_FILTER_CLASS) ||
           InheritanceUtil.isInheritor(filterClass, StrutsConstants.STRUTS_2_1_FILTER_CLASS);
```

with:

```java
    return InheritanceUtil.isInheritor(filterClass, StrutsConstants.STRUTS_2_0_FILTER_CLASS) ||
           InheritanceUtil.isInheritor(filterClass, StrutsConstants.STRUTS_2_1_FILTER_CLASS) ||
           InheritanceUtil.isInheritor(filterClass, StrutsConstants.STRUTS_2_5_FILTER_CLASS);
```

- [ ] **Step 5: Update web.xml reference contributor pattern**

In `StrutsReferenceContributor`, replace:

```java
                                            or(psiClass().inheritorOf(false, StrutsConstants.STRUTS_2_0_FILTER_CLASS),
                                               psiClass().inheritorOf(false, StrutsConstants.STRUTS_2_1_FILTER_CLASS)
                                            )
```

with:

```java
                                            or(psiClass().inheritorOf(false, StrutsConstants.STRUTS_2_0_FILTER_CLASS),
                                               psiClass().inheritorOf(false, StrutsConstants.STRUTS_2_1_FILTER_CLASS),
                                               psiClass().inheritorOf(false, StrutsConstants.STRUTS_2_5_FILTER_CLASS)
                                            )
```

- [ ] **Step 6: Run web.xml tests**

Run:

```bash
./gradlew test -x rat --tests "WebXmlConstantTest" --no-configuration-cache
```

Expected: `WebXmlConstantTest` passes.

- [ ] **Step 7: Commit web.xml filter detection implementation**

```bash
git add src/main/java/com/intellij/struts2/model/constant/StrutsConstantManagerImpl.java \
  src/main/java/com/intellij/struts2/reference/StrutsReferenceContributor.java \
  src/test/java/com/intellij/struts2/reference/web/WebXmlConstantTest.java \
  src/test/testData/reference/web/constant/WEB-INF/web_name_completion_modern_filter.xml \
  src/test/testData/reference/web/constant/WEB-INF/web_value_completion_modern_filter.xml
git commit -m "$(cat <<'EOF'
fix: support modern Struts filter constants in web.xml

EOF
)"
```

---

### Task 5: Final Verification And Changelog

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Add changelog entry**

Under `CHANGELOG.md` → `[Unreleased]` → `### Fixed`, add:

```markdown
- Update Struts 7.2.1 metadata support: add current constants and completion values for annotation-required parameters, chaining annotation checks, `html5`/`css_xhtml` themes, `jakarta-stream` multipart parsing, and modern web.xml Struts filters
```

- [ ] **Step 2: Run focused verification**

Run:

```bash
./gradlew test -x rat --tests "StringValuesConverterTest" --tests "ThemeReferenceProviderTest" --tests "StrutsCompletionTest" --tests "WebXmlConstantTest" --no-configuration-cache
```

Expected: all targeted tests pass.

- [ ] **Step 3: Run broader test verification**

Run:

```bash
./gradlew test -x rat --no-configuration-cache
```

Expected: test suite passes, excluding Apache RAT checks.

- [ ] **Step 4: Check lints for edited files**

Use Cursor's linter diagnostics for:

```text
src/main/java/com/intellij/struts2/model/constant/contributor/StringValuesConverter.java
src/main/java/com/intellij/struts2/model/constant/contributor/StrutsConstantContributorBase.java
src/main/java/com/intellij/struts2/model/constant/contributor/StrutsCoreConstantContributor.java
src/main/java/com/intellij/struts2/reference/jsp/ThemeReferenceProvider.java
src/main/java/com/intellij/struts2/model/constant/StrutsConstantManagerImpl.java
src/main/java/com/intellij/struts2/reference/StrutsReferenceContributor.java
```

Expected: no new diagnostics from the implementation.

- [ ] **Step 5: Commit changelog and final verification fixes**

```bash
git add CHANGELOG.md
git commit -m "$(cat <<'EOF'
docs: note Struts 7.2.1 metadata support

EOF
)"
```

If verification required code/test fixes, include those files in the same commit only when they are directly related to final cleanup.

