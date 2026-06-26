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

package com.intellij.struts2.dom.struts.impl.path;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlElement;
import com.intellij.struts2.dom.ConverterUtil;
import com.intellij.struts2.dom.params.ParamsElement;
import com.intellij.struts2.dom.struts.HasResultType;
import com.intellij.struts2.dom.struts.action.StrutsPathReferenceConverter;
import com.intellij.struts2.dom.struts.strutspackage.StrutsPackage;
import com.intellij.util.io.URLUtil;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Validates dispatch-type {@code <result>} JSP paths when generic DOM checking is suppressed.
 */
public final class StrutsDispatchResultPathInspection {

  private StrutsDispatchResultPathInspection() {
  }

  /**
   * Whether generic DOM resolve checking should be skipped for this result path value.
   */
  public static boolean skipGenericDomResolveCheck(@NotNull final GenericDomValue<?> value,
                                                 @Nullable final String stringValue) {
    if (!(value.getConverter() instanceof StrutsPathReferenceConverter)) {
      return false;
    }
    return isDispatchJspPath(value, stringValue);
  }

  /**
   * Whether the given dispatch JSP path does not resolve to an existing JSP file.
   */
  public static boolean isUnresolvedDispatchJspPath(@NotNull final GenericDomValue<?> resultValue,
                                                    @Nullable final String stringValue) {
    if (!isDispatchJspPath(resultValue, stringValue)) {
      return false;
    }

    final XmlElement xmlElement = DomUtil.getValueElement(resultValue);
    if (xmlElement == null) {
      return false;
    }

    return !isResolvedToJsp(resolveReferences(xmlElement));
  }

  @NotNull
  public static String resolveErrorMessage(@NotNull final XmlElement xmlElement, @NotNull final String fileName) {
    return resolveErrorMessage(resolveReferences(xmlElement), fileName);
  }

  @NotNull
  private static PsiReference[] resolveReferences(@NotNull final XmlElement xmlElement) {
    final PsiReference[] references = xmlElement.getReferences();
    if (references.length > 0) {
      return references;
    }
    return new StrutsPathReferenceConverterImpl().createReferences(xmlElement, true);
  }

  private static boolean isResolvedToJsp(final PsiReference @NotNull [] references) {
    for (int i = references.length - 1; i >= 0; i--) {
      final PsiElement resolved = references[i].resolve();
      if (resolved != null) {
        return isJspFile(resolved);
      }
    }
    return false;
  }

  private static boolean isJspFile(@Nullable final PsiElement element) {
    if (!(element instanceof PsiFile file)) {
      return false;
    }
    if (file.getVirtualFile() == null) {
      return false;
    }
    return "jsp".equalsIgnoreCase(file.getVirtualFile().getExtension());
  }

  @NotNull
  private static String resolveErrorMessage(final PsiReference @NotNull [] references, @NotNull final String fileName) {
    for (int i = references.length - 1; i >= 0; i--) {
      final PsiReference reference = references[i];
      if (reference.resolve() != null) {
        continue;
      }
      if (reference instanceof EmptyResolveMessageProvider provider) {
        final String pattern = provider.getUnresolvedMessagePattern();
        if (StringUtil.isNotEmpty(pattern)) {
          return pattern;
        }
      }
    }
    return "Cannot resolve file '" + fileName + "'";
  }

  private static boolean isDispatchJspPath(@NotNull final GenericDomValue<?> value,
                                           @Nullable final String stringValue) {
    if (stringValue == null) {
      return false;
    }

    if (!(value instanceof ParamsElement paramsElement) || !(value instanceof HasResultType hasResultType)) {
      return false;
    }

    if (!paramsElement.getParams().isEmpty()) {
      return false;
    }

    final String resultTypeName = ResultTypeUtil.resolveEffectiveResultTypeName(hasResultType);
    if (resultTypeName == null ||
        !ResultTypeResolver.hasResultTypeContributor(resultTypeName)) {
      return false;
    }

    if (!ResultTypeResolver.isDispatchType(resultTypeName)) {
      return false;
    }

    if (ConverterUtil.hasWildcardReference(stringValue)) {
      return false;
    }

    if (StringUtil.startsWith(stringValue, "${") || URLUtil.containsScheme(stringValue)) {
      return false;
    }

    final StrutsPackage strutsPackage = DomUtil.getParentOfType(value, StrutsPackage.class, true);
    if (strutsPackage == null) {
      return false;
    }

    final String namespace = strutsPackage.searchNamespace();
    if (namespace == null) {
      return false;
    }

    return StrutsResultPathUtil.toAbsoluteWebPath(stringValue, namespace).endsWith(".jsp");
  }
}
