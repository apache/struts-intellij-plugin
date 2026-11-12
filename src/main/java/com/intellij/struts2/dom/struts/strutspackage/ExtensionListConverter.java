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
package com.intellij.struts2.dom.struts.strutspackage;

import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.converters.DelimitedListConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
    if (token != null && (token.isEmpty() || isLegal(token))) {
      return DomUtil.getValueElement((GenericDomValue<?>) context.getInvocationElement());
    }
    return null;
  }

  @Override
  protected String getUnresolvedMessage(String token) {
    return "Invalid extension '" + token + "'";
  }
}
