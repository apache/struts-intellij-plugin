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

    myFixture.configureByText("test/UserService.java", """
      package test;

      public class UserService {
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
