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
package com.intellij.lang.ognl;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * @author Yann C&eacute;bron
 */
public class OgnlTypedHandlerTest extends BasePlatformTestCase {

  public void testTypeExpressionPrefix() {
    myFixture.configureByText(com.intellij.lang.ognl.OgnlFileType.INSTANCE,
                              "%<caret>");
    myFixture.type('{');
    myFixture.checkResult(com.intellij.lang.ognl.OgnlLanguage.EXPRESSION_PREFIX + com.intellij.lang.ognl.OgnlLanguage.EXPRESSION_SUFFIX);
  }

  public void testTypeOpeningBrace() {
    myFixture.configureByText(com.intellij.lang.ognl.OgnlFileType.INSTANCE,
                              com.intellij.lang.ognl.OgnlLanguage.EXPRESSION_PREFIX + " a in <caret>" + com.intellij.lang.ognl.OgnlLanguage.EXPRESSION_SUFFIX);
    myFixture.type('{');
    myFixture.checkResult(com.intellij.lang.ognl.OgnlLanguage.EXPRESSION_PREFIX + " a in {}" + com.intellij.lang.ognl.OgnlLanguage.EXPRESSION_SUFFIX);
  }

  public void testTypeOpeningBraceWithoutExpressionPrefix() {
    myFixture.configureByText(com.intellij.lang.ognl.OgnlFileType.INSTANCE,
                              "<caret>");
    myFixture.type('{');
    myFixture.checkResult("{}");
  }

}