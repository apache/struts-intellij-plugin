/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.intellij.lang.ognl.completion;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.lang.ognl.OgnlFileType;
import com.intellij.lang.ognl.OgnlTestUtils;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.Struts2ProjectDescriptorBuilder;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OgnlFqnTypeExpressionCompletionTest extends BasicLightHighlightingTestCase {

  @NotNull
  @Override
  protected String getTestDataLocation() {
    return "ognl/completion";
  }

  @NotNull
  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return new Struts2ProjectDescriptorBuilder()
      .withStrutsLibrary()
      .withStrutsFacet()
      .build();
  }

  public void testNewExpressionBasicCompletion() {
    myFixture.configureByText(OgnlFileType.INSTANCE,
                              OgnlTestUtils.createExpression("new java.util.Co<caret>ll"));

    myFixture.completeBasic();
    final List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Completion should return non-null results", lookupStrings);
    assertContainsElements(lookupStrings,
                           "Collections");
  }

  public void testNewExpressionClassNameCompletionDoesNotLimitToConcreteAndNonInterface() {
    myFixture.configureByText(OgnlFileType.INSTANCE,
                              OgnlTestUtils.createExpression("new C<caret>o"));

    myFixture.complete(CompletionType.CLASS_NAME);
    final List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Completion should return non-null results", lookupStrings);
    assertContainsElements(lookupStrings,
                           "Collection",
                           "Collections",
                           "Comparator");
  }

  public void testJavaLangClassesAreSuggested() {
    myFixture.configureByText(OgnlFileType.INSTANCE, OgnlTestUtils.createExpression("new St<caret>r"));
    myFixture.completeBasic();
    final List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Completion should return non-null results", lookupStrings);
    assertContainsElements(lookupStrings,
                           "String", "StringBuilder");
  }

  public void testMapTypeExpressionLimitsToMapClasses() {
    myFixture.configureByText(OgnlFileType.INSTANCE,
                              OgnlTestUtils.createExpression("#@ <caret>"));
    myFixture.complete(CompletionType.SMART);

    final List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Completion should return non-null results", lookupStrings);
    assertContainsElements(lookupStrings,
                           "java.util.HashMap",
                           "java.util.Hashtable",
                           "java.util.IdentityHashMap",
                           "java.util.Properties",
                           "java.util.TreeMap");
  }

  public void testNewArrayExpressionBasicCompletion() {
    myFixture.configureByText(OgnlFileType.INSTANCE,
                              OgnlTestUtils.createExpression("new C<caret>["));

    myFixture.completeBasic();
    final List<String> lookupStrings = myFixture.getLookupElementStrings();
    assertNotNull("Completion should return non-null results", lookupStrings);
    assertContainsElements(lookupStrings,
                           "Character", "ThreadLocal");
    assertDoesntContain(lookupStrings,
                        "Comparable", "Deprecated");
  }
}
