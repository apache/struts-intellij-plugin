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
package com.intellij.struts2.annotators;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.jsp.JspFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.StrutsBundle;
import com.intellij.struts2.dom.struts.model.StrutsManager;
import com.intellij.struts2.facet.StrutsFacet;
import com.intellij.struts2.facet.WebFacetChecker;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * Warns when a {@code struts.xml} file is opened in a module that has a Struts
 * facet but no {@link com.intellij.javaee.web.facet.WebFacet}, which prevents
 * dispatch-style result paths (JSP etc.) from resolving.
 */
public class StrutsWebFacetCheckingAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if (!(psiElement instanceof XmlFile xmlFile)) {
            return;
        }

        if (psiElement instanceof JspFile) {
            return;
        }

        Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
        if (module == null) {
            return;
        }

        StrutsFacet strutsFacet = StrutsFacet.getInstance(module);
        if (strutsFacet == null) {
            return;
        }

        StrutsManager strutsManager = StrutsManager.getInstance(psiElement.getProject());
        if (!strutsManager.isStruts2ConfigFile(xmlFile)) {
            return;
        }

        if (!WebFacetChecker.isWebFacetMissing(module)) {
            return;
        }

        holder.newAnnotation(HighlightSeverity.WARNING,
                        StrutsBundle.message("annotators.webfacet.missing"))
                .range(xmlFile)
                .fileLevel()
                .withFix(new ConfigureWebFacetFix(strutsFacet))
                .create();
    }

    private static final class ConfigureWebFacetFix implements IntentionAction {

        private final StrutsFacet myStrutsFacet;

        private ConfigureWebFacetFix(@NotNull StrutsFacet strutsFacet) {
            myStrutsFacet = strutsFacet;
        }

        @Override
        @NotNull
        public String getText() {
            return StrutsBundle.message("annotators.webfacet.configure");
        }

        @Override
        @NotNull
        public String getFamilyName() {
            return StrutsBundle.message("intentions.family.name");
        }

        @Override
        public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
            return true;
        }

        @Override
        public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile)
                throws IncorrectOperationException {
            ModulesConfigurator.showFacetSettingsDialog(myStrutsFacet, null);
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }
    }
}
