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
package com.intellij.lang.ognl.highlight;

import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.testFramework.LightPlatformTestCase;

import java.util.Arrays;
import java.util.List;

public class OgnlColorSettingsPageTest extends LightPlatformTestCase {

  private final OgnlColorSettingsPage myPage = new OgnlColorSettingsPage();

  public void testId() {
    assertEquals("OGNL", myPage.getId());
  }

  public void testDisplayName() {
    assertEquals("OGNL", myPage.getDisplayName());
  }

  public void testAttributeDescriptorsInOrder() {
    List<String> names = Arrays.stream(myPage.getAttributeDescriptors())
      .map(AttributesDescriptor::getDisplayName)
      .toList();

    assertEquals(List.of("Background",
                         "Expression bounds",
                         "Keyword",
                         "Operations",
                         "Identifier",
                         "String",
                         "Number",
                         "Comma",
                         "Parentheses",
                         "Brackets",
                         "Braces",
                         "Class reference"), names);
  }

  public void testNoColorDescriptors() {
    assertEquals(0, myPage.getColorDescriptors().length);
  }
}
