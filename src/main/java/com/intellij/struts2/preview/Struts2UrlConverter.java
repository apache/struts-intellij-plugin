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
package com.intellij.struts2.preview;

import com.intellij.javaee.web.DeployedFileUrlConverter;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.paths.PathReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.struts2.dom.struts.action.Result;
import com.intellij.struts2.dom.struts.model.StrutsManager;
import com.intellij.struts2.dom.struts.model.StrutsModel;
import com.intellij.struts2.dom.struts.strutspackage.StrutsPackage;
import com.intellij.struts2.model.constant.StrutsConstantHelper;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Dmitry Avdeev
 */
final class Struts2UrlConverter extends DeployedFileUrlConverter {
  @Override
  public Collection<String> getTargetPaths(@NotNull final PsiFile sourceFile, @NotNull final WebFacet webFacet) {
    final StrutsModel combinedModel = StrutsManager.getInstance(sourceFile.getProject()).getCombinedModel(webFacet.getModule());
    if (combinedModel == null) {
      return Collections.emptyList();
    }

    final List<String> actionExtensions = StrutsConstantHelper.getActionExtensions(sourceFile);
    if (actionExtensions.isEmpty()) {
      return Collections.emptyList();
    }

    final String actionExtension = actionExtensions.get(0);

    @NonNls final ArrayList<String> list = new ArrayList<>();
    combinedModel.processActions(action -> {
      for (final Result result : action.getResults()) {
        final PathReference pathReference = result.getValue();
        if (pathReference != null) {
          final PsiElement psiElement = pathReference.resolve();
          if (psiElement != null && psiElement.equals(sourceFile)) {
            String namespace = action.getNamespace();
            if (!Objects.equals(namespace, StrutsPackage.DEFAULT_NAMESPACE)) {
              namespace += "/";
            }
            list.add(namespace + action.getName().getStringValue() + actionExtension);
          }
        }
      }
      return true;
    });

    return list;
  }
}
