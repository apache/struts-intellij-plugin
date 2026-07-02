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
