# StrutsParameter Java Inspection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Java inspection that warns when Struts action setters or public fields will not bind because `struts.parameters.requireAnnotations=true` and `@StrutsParameter` is missing.

**Architecture:** Implement a focused `LocalInspectionTool` for Java files. The inspection delegates action-class recognition, annotation/member checks, and configuration checks to small utilities so later quick-fixes or getter/depth validation can build on the same foundation.

**Tech Stack:** IntelliJ Platform PSI/inspection APIs, Struts facet/model APIs, JUnit 3 light fixture tests, Gradle.

---

## File Structure

- Create `src/main/java/com/intellij/struts2/inspection/StrutsParameterAnnotationInspection.java`
  - Java inspection entry point and PSI visitor.
- Create `src/main/java/com/intellij/struts2/inspection/StrutsActionClassUtil.java`
  - Shared action-class predicate used by the inspection.
- Create `src/main/java/com/intellij/struts2/inspection/StrutsParameterAnnotationUtil.java`
  - `@StrutsParameter` availability, annotation, setter, and field checks.
- Create `src/main/java/com/intellij/struts2/inspection/StrutsParameterConfigUtil.java`
  - Determines whether annotation-based parameter binding is active.
- Modify `src/main/java/com/intellij/struts2/StrutsConstants.java`
  - Add the `@StrutsParameter` FQN constant.
- Modify `src/main/java/com/intellij/struts2/model/constant/contributor/StrutsCoreConstantContributor.java`
  - Add a typed constant key for `struts.parameters.requireAnnotations`.
- Modify `src/main/resources/META-INF/plugin.xml`
  - Register the Java inspection.
- Modify `src/main/resources/messages/Struts2Bundle.properties`
  - Add inspection display name and warning message.
- Create `src/test/java/com/intellij/struts2/inspection/StrutsParameterAnnotationInspectionTest.java`
  - Focused inspection tests.
- Create `src/test/testData/inspection/strutsParameter/struts-require-annotations.xml`
  - Struts config with `requireAnnotations=true` and one mapped action.
- Create `src/test/testData/inspection/strutsParameter/struts-disable-annotations.xml`
  - Struts config with `requireAnnotations=false` and one mapped action.
- Modify `CHANGELOG.md`
  - Add an unreleased entry for the inspection.

---

### Task 1: Add Failing Inspection Tests

**Files:**
- Create: `src/test/java/com/intellij/struts2/inspection/StrutsParameterAnnotationInspectionTest.java`
- Create: `src/test/testData/inspection/strutsParameter/struts-require-annotations.xml`
- Create: `src/test/testData/inspection/strutsParameter/struts-disable-annotations.xml`

- [ ] **Step 1: Add Struts config fixture with required annotations**

Create `src/test/testData/inspection/strutsParameter/struts-require-annotations.xml`:

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

<!DOCTYPE struts PUBLIC
  "-//Apache Software Foundation//DTD Struts Configuration 6.0//EN"
  "https://struts.apache.org/dtds/struts-6.0.dtd">

<struts>
  <constant name="struts.parameters.requireAnnotations" value="true"/>

  <package name="default" extends="struts-default">
    <action name="sample" class="test.SampleAction"/>
  </package>
</struts>
```

- [ ] **Step 2: Add Struts config fixture with disabled annotations**

Create `src/test/testData/inspection/strutsParameter/struts-disable-annotations.xml`:

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

<!DOCTYPE struts PUBLIC
  "-//Apache Software Foundation//DTD Struts Configuration 6.0//EN"
  "https://struts.apache.org/dtds/struts-6.0.dtd">

<struts>
  <constant name="struts.parameters.requireAnnotations" value="false"/>

  <package name="default" extends="struts-default">
    <action name="sample" class="test.SampleAction"/>
  </package>
</struts>
```

- [ ] **Step 3: Add the inspection test class**

Create `src/test/java/com/intellij/struts2/inspection/StrutsParameterAnnotationInspectionTest.java`:

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
package com.intellij.struts2.inspection;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import org.jetbrains.annotations.NotNull;

public class StrutsParameterAnnotationInspectionTest extends BasicLightHighlightingTestCase {
  private static final String WARNING =
    "Parameter injection requires @StrutsParameter when struts.parameters.requireAnnotations is enabled";

  @Override
  protected InspectionProfileEntry[] getHighlightingInspections() {
    return new InspectionProfileEntry[]{new StrutsParameterAnnotationInspection()};
  }

  @NotNull
  @Override
  protected String getTestDataLocation() {
    return "inspection/strutsParameter/";
  }

  public void testWarnsAboutUnannotatedSetterAndPublicField() {
    configureStrutsParameterAnnotation();
    createStrutsFileSet("struts-require-annotations.xml");

    myFixture.configureByText("test/SampleAction.java", """
      package test;

      public class SampleAction {
        public String <warning descr="%s">username</warning>;

        public void <warning descr="%s">setPassword</warning>(String password) {
        }
      }
      """.formatted(WARNING, WARNING));

    myFixture.checkHighlighting();
  }

  public void testDoesNotWarnAboutAnnotatedMembers() {
    configureStrutsParameterAnnotation();
    createStrutsFileSet("struts-require-annotations.xml");

    myFixture.configureByText("test/SampleAction.java", """
      package test;

      import org.apache.struts2.interceptor.parameter.StrutsParameter;

      public class SampleAction {
        @StrutsParameter
        public String username;

        @StrutsParameter
        public void setPassword(String password) {
        }
      }
      """);

    myFixture.checkHighlighting();
  }

  public void testDoesNotWarnWhenRequireAnnotationsIsFalse() {
    configureStrutsParameterAnnotation();
    createStrutsFileSet("struts-disable-annotations.xml");

    myFixture.configureByText("test/SampleAction.java", """
      package test;

      public class SampleAction {
        public String username;

        public void setPassword(String password) {
        }
      }
      """);

    myFixture.checkHighlighting();
  }

  public void testDoesNotWarnInNonActionClass() {
    configureStrutsParameterAnnotation();
    createStrutsFileSet("struts-require-annotations.xml");

    myFixture.configureByText("test/NotAction.java", """
      package test;

      public class NotAction {
        public String username;

        public void setPassword(String password) {
        }
      }
      """);

    myFixture.checkHighlighting();
  }

  public void testDoesNotWarnAboutNonPublicMembersOrGetters() {
    configureStrutsParameterAnnotation();
    createStrutsFileSet("struts-require-annotations.xml");

    myFixture.configureByText("test/SampleAction.java", """
      package test;

      public class SampleAction {
        private String privateField;
        protected String protectedField;
        String packagePrivateField;
        public static String staticField;

        private void setPrivateValue(String value) {
        }

        protected void setProtectedValue(String value) {
        }

        void setPackagePrivateValue(String value) {
        }

        public static void setStaticValue(String value) {
        }

        public String getUser() {
          return "";
        }
      }
      """);

    myFixture.checkHighlighting();
  }

  private void configureStrutsParameterAnnotation() {
    myFixture.configureByText("org/apache/struts2/interceptor/parameter/StrutsParameter.java", """
      package org.apache.struts2.interceptor.parameter;

      public @interface StrutsParameter {
        int depth() default 0;
      }
      """);
  }
}
```

- [ ] **Step 4: Run the failing test**

Run:

```bash
./gradlew test -x rat --tests "StrutsParameterAnnotationInspectionTest" --no-configuration-cache
```

Expected: compilation fails because `StrutsParameterAnnotationInspection` does not exist.

- [ ] **Step 5: Commit the red tests**

```bash
git add src/test/java/com/intellij/struts2/inspection/StrutsParameterAnnotationInspectionTest.java \
  src/test/testData/inspection/strutsParameter/struts-require-annotations.xml \
  src/test/testData/inspection/strutsParameter/struts-disable-annotations.xml
git commit -m "$(cat <<'EOF'
test: cover StrutsParameter annotation inspection

EOF
)"
```

---

### Task 2: Add Constants And Utility Classes

**Files:**
- Modify: `src/main/java/com/intellij/struts2/StrutsConstants.java`
- Modify: `src/main/java/com/intellij/struts2/model/constant/contributor/StrutsCoreConstantContributor.java`
- Create: `src/main/java/com/intellij/struts2/inspection/StrutsActionClassUtil.java`
- Create: `src/main/java/com/intellij/struts2/inspection/StrutsParameterAnnotationUtil.java`
- Create: `src/main/java/com/intellij/struts2/inspection/StrutsParameterConfigUtil.java`

- [ ] **Step 1: Add the annotation FQN constant**

In `src/main/java/com/intellij/struts2/StrutsConstants.java`, add this constant near the other plugin-wide class-name constants:

```java
  @NonNls
  public static final String STRUTS_PARAMETER_ANNOTATION =
      "org.apache.struts2.interceptor.parameter.StrutsParameter";
```

- [ ] **Step 2: Add a typed key for `struts.parameters.requireAnnotations`**

In `src/main/java/com/intellij/struts2/model/constant/contributor/StrutsCoreConstantContributor.java`, add this public key after `ACTION_EXTENSION`:

```java
  /**
   * {@code struts.parameters.requireAnnotations}.
   */
  public static final StrutsConstantKey<Boolean> REQUIRE_ANNOTATIONS = StrutsConstantKey.create(
      "struts.parameters.requireAnnotations");
```

Then replace the string literal in the constant list:

```java
      addBooleanProperty(REQUIRE_ANNOTATIONS.getKey()),
```

- [ ] **Step 3: Add action-class detection utility**

Create `src/main/java/com/intellij/struts2/inspection/StrutsActionClassUtil.java`:

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
package com.intellij.struts2.inspection;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.struts2.StrutsConstants;
import com.intellij.struts2.dom.struts.model.StrutsManager;
import com.intellij.struts2.dom.struts.model.StrutsModel;
import com.intellij.struts2.model.jam.convention.StrutsConventionConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class StrutsActionClassUtil {
  private StrutsActionClassUtil() {
  }

  static boolean isActionClass(@NotNull PsiClass psiClass) {
    if (!isConcretePublicClass(psiClass)) {
      return false;
    }

    final Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
    if (module == null) {
      return false;
    }

    final StrutsModel strutsModel = StrutsManager.getInstance(psiClass.getProject()).getCombinedModel(module);
    if (strutsModel != null && strutsModel.isActionClass(psiClass)) {
      return true;
    }

    if (AnnotationUtil.isAnnotated(psiClass, StrutsConventionConstants.ACTION, 0) ||
        AnnotationUtil.isAnnotated(psiClass, StrutsConventionConstants.ACTIONS, 0)) {
      return true;
    }

    if (!isConventionPluginPresent(psiClass)) {
      return false;
    }

    final String className = psiClass.getName();
    if (className != null && StringUtil.endsWith(className, "Action")) {
      return true;
    }

    return InheritanceUtil.isInheritor(psiClass, StrutsConstants.XWORK_ACTION_CLASS);
  }

  private static boolean isConcretePublicClass(@Nullable PsiClass psiClass) {
    return psiClass != null &&
           !psiClass.isInterface() &&
           !psiClass.isEnum() &&
           !psiClass.isAnnotationType() &&
           psiClass.hasModifierProperty(PsiModifier.PUBLIC) &&
           !psiClass.hasModifierProperty(PsiModifier.ABSTRACT);
  }

  private static boolean isConventionPluginPresent(@NotNull PsiElement element) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) {
      return false;
    }

    final GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false);
    return JavaPsiFacade.getInstance(element.getProject())
             .findClass(StrutsConventionConstants.CONVENTIONS_SERVICE, scope) != null;
  }
}
```

- [ ] **Step 4: Add annotation/member utility**

Create `src/main/java/com/intellij/struts2/inspection/StrutsParameterAnnotationUtil.java`:

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
package com.intellij.struts2.inspection;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.struts2.StrutsConstants;
import org.jetbrains.annotations.NotNull;

final class StrutsParameterAnnotationUtil {
  private StrutsParameterAnnotationUtil() {
  }

  static boolean isStrutsParameterAvailable(@NotNull PsiElement context) {
    return JavaPsiFacade.getInstance(context.getProject())
             .findClass(StrutsConstants.STRUTS_PARAMETER_ANNOTATION, context.getResolveScope()) != null;
  }

  static boolean isAnnotatedWithStrutsParameter(@NotNull PsiModifierListOwner owner) {
    return AnnotationUtil.isAnnotated(owner, StrutsConstants.STRUTS_PARAMETER_ANNOTATION, 0);
  }

  static boolean isInjectableSetter(@NotNull PsiMethod method) {
    return method.hasModifierProperty(PsiModifier.PUBLIC) &&
           !method.hasModifierProperty(PsiModifier.STATIC) &&
           !method.hasModifierProperty(PsiModifier.ABSTRACT) &&
           PropertyUtilBase.isSimplePropertySetter(method);
  }

  static boolean isInjectableField(@NotNull PsiField field) {
    return field.hasModifierProperty(PsiModifier.PUBLIC) &&
           !field.hasModifierProperty(PsiModifier.STATIC);
  }
}
```

- [ ] **Step 5: Add configuration utility**

Create `src/main/java/com/intellij/struts2/inspection/StrutsParameterConfigUtil.java`:

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
package com.intellij.struts2.inspection;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.struts2.facet.ui.StrutsVersionDetector;
import com.intellij.struts2.model.constant.StrutsConstantManager;
import com.intellij.struts2.model.constant.contributor.StrutsCoreConstantContributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class StrutsParameterConfigUtil {
  private StrutsParameterConfigUtil() {
  }

  static boolean isRequireAnnotationsEnabled(@NotNull PsiElement context) {
    final PsiFile containingFile = context.getContainingFile();
    if (containingFile == null) {
      return false;
    }

    final Boolean configuredValue = StrutsConstantManager.getInstance(context.getProject())
      .getConvertedValue(containingFile, StrutsCoreConstantContributor.REQUIRE_ANNOTATIONS);
    if (configuredValue != null) {
      return configuredValue;
    }

    final Module module = ModuleUtilCore.findModuleForPsiElement(context);
    if (module == null) {
      return false;
    }

    return isStruts7OrNewer(StrutsVersionDetector.detectStrutsVersion(module));
  }

  static boolean isStruts7OrNewer(@Nullable String version) {
    if (version == null || version.isBlank()) {
      return false;
    }

    final int firstDot = version.indexOf('.');
    final String majorVersion = firstDot == -1 ? version : version.substring(0, firstDot);
    try {
      return Integer.parseInt(majorVersion) >= 7;
    }
    catch (NumberFormatException e) {
      return false;
    }
  }
}
```

- [ ] **Step 6: Run compilation-targeted tests**

Run:

```bash
./gradlew test -x rat --tests "StrutsParameterAnnotationInspectionTest" --no-configuration-cache
```

Expected: compilation still fails because `StrutsParameterAnnotationInspection` does not exist, but the new utilities compile.

- [ ] **Step 7: Commit constants and utilities**

```bash
git add src/main/java/com/intellij/struts2/StrutsConstants.java \
  src/main/java/com/intellij/struts2/model/constant/contributor/StrutsCoreConstantContributor.java \
  src/main/java/com/intellij/struts2/inspection/StrutsActionClassUtil.java \
  src/main/java/com/intellij/struts2/inspection/StrutsParameterAnnotationUtil.java \
  src/main/java/com/intellij/struts2/inspection/StrutsParameterConfigUtil.java
git commit -m "$(cat <<'EOF'
feat: add StrutsParameter inspection utilities

EOF
)"
```

---

### Task 3: Implement And Register The Inspection

**Files:**
- Create: `src/main/java/com/intellij/struts2/inspection/StrutsParameterAnnotationInspection.java`
- Modify: `src/main/resources/META-INF/plugin.xml`
- Modify: `src/main/resources/messages/Struts2Bundle.properties`

- [ ] **Step 1: Add bundle messages**

In `src/main/resources/messages/Struts2Bundle.properties`, add these keys near the other inspection keys:

```properties
inspections.struts.parameter.annotation.display.name=Missing StrutsParameter annotation
inspections.struts.parameter.annotation.message=Parameter injection requires @StrutsParameter when struts.parameters.requireAnnotations is enabled
```

- [ ] **Step 2: Add the inspection class**

Create `src/main/java/com/intellij/struts2/inspection/StrutsParameterAnnotationInspection.java`:

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
package com.intellij.struts2.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.struts2.StrutsBundle;
import com.intellij.struts2.facet.StrutsFacet;
import org.jetbrains.annotations.NotNull;

public final class StrutsParameterAnnotationInspection extends LocalInspectionTool {
  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    if (!(holder.getFile() instanceof PsiJavaFile) ||
        StrutsFacet.getInstance(holder.getFile()) == null ||
        !StrutsParameterAnnotationUtil.isStrutsParameterAvailable(holder.getFile()) ||
        !StrutsParameterConfigUtil.isRequireAnnotationsEnabled(holder.getFile())) {
      return new PsiElementVisitor() {
      };
    }

    return new JavaElementVisitor() {
      @Override
      public void visitClass(@NotNull PsiClass psiClass) {
        if (!StrutsActionClassUtil.isActionClass(psiClass)) {
          return;
        }

        for (PsiMethod method : psiClass.getMethods()) {
          if (StrutsParameterAnnotationUtil.isInjectableSetter(method) &&
              !StrutsParameterAnnotationUtil.isAnnotatedWithStrutsParameter(method)) {
            registerProblem(method.getNameIdentifier());
          }
        }

        for (PsiField field : psiClass.getFields()) {
          if (StrutsParameterAnnotationUtil.isInjectableField(field) &&
              !StrutsParameterAnnotationUtil.isAnnotatedWithStrutsParameter(field)) {
            registerProblem(field.getNameIdentifier());
          }
        }
      }

      private void registerProblem(PsiIdentifier identifier) {
        if (identifier == null) {
          return;
        }

        holder.registerProblem(identifier,
                               StrutsBundle.message("inspections.struts.parameter.annotation.message"));
      }
    };
  }
}
```

- [ ] **Step 3: Register the Java inspection**

In `src/main/resources/META-INF/plugin.xml`, add this entry after `HardcodedActionUrlInspection`:

```xml
        <localInspection language="JAVA" groupPath="Struts" shortName="StrutsParameterAnnotation" applyToDialects="false"
                         bundle="messages.Struts2Bundle" key="inspections.struts.parameter.annotation.display.name"
                         groupKey="inspections.group.display.name" enabledByDefault="true" level="WARNING"
                         implementationClass="com.intellij.struts2.inspection.StrutsParameterAnnotationInspection"/>
```

- [ ] **Step 4: Run the targeted inspection tests**

Run:

```bash
./gradlew test -x rat --tests "StrutsParameterAnnotationInspectionTest" --no-configuration-cache
```

Expected: all tests in `StrutsParameterAnnotationInspectionTest` pass.

- [ ] **Step 5: Commit inspection implementation**

```bash
git add src/main/java/com/intellij/struts2/inspection/StrutsParameterAnnotationInspection.java \
  src/main/resources/META-INF/plugin.xml \
  src/main/resources/messages/Struts2Bundle.properties
git commit -m "$(cat <<'EOF'
feat: inspect missing StrutsParameter annotations

EOF
)"
```

---

### Task 4: Final Verification And Changelog

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Add changelog entry**

In `CHANGELOG.md`, add this under `[Unreleased]` → `### Added`:

```markdown
- Add Java inspection for Struts action setters and public fields missing `@StrutsParameter` when annotation-based parameter binding is required
```

- [ ] **Step 2: Run targeted tests**

Run:

```bash
./gradlew test -x rat --tests "StrutsParameterAnnotationInspectionTest" --no-configuration-cache
```

Expected: build succeeds and the inspection tests pass.

- [ ] **Step 3: Run the broader suite**

Run:

```bash
./gradlew test -x rat --no-configuration-cache
```

Expected: build succeeds.

- [ ] **Step 4: Check IDE lints for edited files**

Use the IDE diagnostics for these files:

```text
src/main/java/com/intellij/struts2/inspection/StrutsParameterAnnotationInspection.java
src/main/java/com/intellij/struts2/inspection/StrutsActionClassUtil.java
src/main/java/com/intellij/struts2/inspection/StrutsParameterAnnotationUtil.java
src/main/java/com/intellij/struts2/inspection/StrutsParameterConfigUtil.java
src/main/java/com/intellij/struts2/StrutsConstants.java
src/main/java/com/intellij/struts2/model/constant/contributor/StrutsCoreConstantContributor.java
```

Expected: no newly introduced errors.

- [ ] **Step 5: Commit changelog and verification-ready state**

```bash
git add CHANGELOG.md
git commit -m "$(cat <<'EOF'
docs: add changelog entry for StrutsParameter inspection

EOF
)"
```

- [ ] **Step 6: Review final branch state**

Run:

```bash
git status --short
git log --oneline -8
```

Expected: working tree is clean and the latest commits are the spec, tests, utilities, inspection, and changelog.
