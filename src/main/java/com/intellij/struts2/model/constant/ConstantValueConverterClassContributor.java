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
package com.intellij.struts2.model.constant;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiClass;
import com.intellij.util.xml.ConvertContext;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extend resolving to class.
 *
 * @author Yann C&eacute;bron
 */
public interface ConstantValueConverterClassContributor {

  /**
   * Extend possible resolving to class.
   */
  ExtensionPointName<ConstantValueConverterClassContributor> EP_NAME =
    new ExtensionPointName<>(
      "com.intellij.struts2.constantValueClassContributor");

  /**
   * Performs the actual conversion.
   *
   * @param s              Constant value to convert.
   * @param convertContext Current context.
   * @return {@code null} if unable to convert class.
   */
  @Nullable
  PsiClass fromString(@NotNull @NonNls final String s, final ConvertContext convertContext);

}
