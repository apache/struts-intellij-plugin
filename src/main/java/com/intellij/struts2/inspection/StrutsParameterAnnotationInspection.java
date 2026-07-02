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

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.struts2.StrutsBundle;
import com.intellij.struts2.facet.StrutsFacet;
import org.jetbrains.annotations.NotNull;

public final class StrutsParameterAnnotationInspection extends LocalInspectionTool {
  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    if (!(holder.getFile() instanceof PsiJavaFile) ||
        StrutsFacet.getInstance(holder.getFile()) == null ||
        !StrutsParameterAnnotationUtil.isStrutsParameterAvailable(holder.getFile()) ||
        !StrutsParameterConfigUtil.isRequireAnnotationsEnabled(holder.getFile())) {
      return new PsiElementVisitor() {
      };
    }

    return new JavaElementVisitor() {
      @Override
      public void visitClass(@NotNull PsiClass psiClass) {
        if (!StrutsActionClassUtil.isActionClass(psiClass)) {
          return;
        }

        for (PsiMethod method : psiClass.getMethods()) {
          if (StrutsParameterAnnotationUtil.isInjectableSetter(method) &&
              !StrutsParameterAnnotationUtil.isAnnotatedWithStrutsParameter(method)) {
            registerProblem(method.getNameIdentifier());
          }
        }

        for (PsiField field : psiClass.getFields()) {
          if (StrutsParameterAnnotationUtil.isInjectableField(field) &&
              !StrutsParameterAnnotationUtil.isAnnotatedWithStrutsParameter(field)) {
            registerProblem(field.getNameIdentifier());
          }
        }
      }

      private void registerProblem(PsiIdentifier identifier) {
        if (identifier == null) {
          return;
        }

        holder.registerProblem(identifier,
                               StrutsBundle.message("inspections.struts.parameter.annotation.message"));
      }
    };
  }
}
