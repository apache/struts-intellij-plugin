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

import com.intellij.openapi.module.Module;
import com.intellij.struts2.model.constant.StrutsConstant;
import com.intellij.struts2.model.constant.StrutsConstantManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Resolves {@code <scoped-constant>} names to registered scopable constants.
 */
public class ScopedConstantNameConverter extends ResolvingConverter.StringConverter {

  static final List<String> SCOPABLE_NAMES = List.of(
      "struts.ui.theme",
      "struts.ui.templateDir",
      "struts.convention.result.path");

  @Override
  public String fromString(@Nullable String name, ConvertContext context) {
    if (name == null) {
      return null;
    }
    return getVariants(context).contains(name) ? name : null;
  }

  @Override
  public @NotNull Collection<String> getVariants(@NotNull ConvertContext context) {
    final Module module = context.getModule();
    if (module == null) {
      return List.of();
    }
    final StrutsConstantManager constantManager = StrutsConstantManager.getInstance(module.getProject());
    final Set<String> registered = ContainerUtil.map2Set(constantManager.getConstants(module), StrutsConstant::getName);
    return ContainerUtil.filter(SCOPABLE_NAMES, registered::contains);
  }

  @Override
  public String getErrorMessage(@Nullable String name, ConvertContext context) {
    return "Constant '" + name + "' is not scopable";
  }
}
