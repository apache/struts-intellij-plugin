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
