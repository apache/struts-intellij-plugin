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
