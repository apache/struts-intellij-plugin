/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.struts2.diagram.fileEditor;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.jsp.JspFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.dom.struts.model.StrutsManager;
import com.intellij.struts2.facet.ui.StrutsFileSet;
import com.intellij.util.xml.ui.PerspectiveFileEditor;
import com.intellij.util.xml.ui.PerspectiveFileEditorProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Provides the read-only "Diagram" tab for struts.xml files registered in a Struts file set.
 * Uses the same eligibility rules as the legacy Graph tab but does not depend on
 * deprecated {@code GraphBuilder} APIs.
 */
public class Struts2DiagramFileEditorProvider extends PerspectiveFileEditorProvider {

    @Override
    public boolean accept(@NotNull final Project project, @NotNull final VirtualFile file) {
        if (!file.isValid()) {
            return false;
        }

        final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

        if (!(psiFile instanceof XmlFile)) {
            return false;
        }

        if (psiFile instanceof JspFile) {
            return false;
        }

        if (!StrutsManager.getInstance(project).isStruts2ConfigFile((XmlFile) psiFile)) {
            return false;
        }

        final Module module = ModuleUtilCore.findModuleForFile(file, project);
        if (module == null) {
            return false;
        }

        final Set<StrutsFileSet> fileSets = StrutsManager.getInstance(project).getAllConfigFileSets(module);
        for (final StrutsFileSet fileSet : fileSets) {
            if (fileSet.hasFile(file)) {
                return true;
            }
        }

        return false;
    }

    @Override
    @NotNull
    public PerspectiveFileEditor createEditor(@NotNull final Project project, @NotNull final VirtualFile file) {
        return new Struts2DiagramFileEditor(project, file);
    }

    @Override
    public boolean isDumbAware() {
        return false;
    }

    @Override
    public double getWeight() {
        return 0;
    }
}
