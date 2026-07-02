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
