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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.struts2.facet.ui.StrutsVersionDetector;
import com.intellij.struts2.model.constant.StrutsConstantManager;
import com.intellij.struts2.model.constant.contributor.StrutsCoreConstantContributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class StrutsParameterConfigUtil {
  private StrutsParameterConfigUtil() {
  }

  static boolean isRequireAnnotationsEnabled(@NotNull PsiElement context) {
    final PsiFile containingFile = context.getContainingFile();
    if (containingFile == null) {
      return false;
    }

    final String configuredValue = StrutsConstantManager.getInstance(context.getProject())
      .getConvertedValue(containingFile, StrutsCoreConstantContributor.REQUIRE_ANNOTATIONS);
    if (configuredValue != null) {
      return Boolean.parseBoolean(configuredValue.trim());
    }

    final Module module = ModuleUtilCore.findModuleForPsiElement(context);
    if (module == null) {
      return false;
    }

    return isStruts7OrNewer(StrutsVersionDetector.detectStrutsVersion(module));
  }

  static boolean isStruts7OrNewer(@Nullable String version) {
    if (version == null || version.isBlank()) {
      return false;
    }

    final int firstDot = version.indexOf('.');
    final String majorVersion = firstDot == -1 ? version : version.substring(0, firstDot);
    try {
      return Integer.parseInt(majorVersion) >= 7;
    }
    catch (NumberFormatException e) {
      return false;
    }
  }
}
