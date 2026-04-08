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
package com.intellij.struts2.diagram.presentation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.xml.XmlElement;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.struts2.dom.struts.action.Action;
import com.intellij.struts2.dom.struts.action.Result;
import com.intellij.struts2.dom.struts.strutspackage.ResultType;
import com.intellij.struts2.dom.struts.strutspackage.StrutsPackage;
import com.intellij.util.OpenSourceUtil;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Presentation helpers for Struts diagram nodes.
 * <p>
 * <b>Threading contract:</b>
 * <ul>
 *   <li>{@link #computeTooltipHtml(DomElement)} accesses DOM/PSI and <b>must be called
 *       under a read action</b> (typically during snapshot creation).</li>
 *   <li>{@link #navigateToElement(StrutsDiagramNode)} is safe to call from the EDT —
 *       it resolves the precomputed {@link SmartPsiElementPointer} inside a
 *       short synchronous read action via {@code Application.runReadAction}.</li>
 * </ul>
 * Intentionally free of any {@code com.intellij.openapi.graph} dependencies.
 */
public final class StrutsDiagramPresentation {

    private StrutsDiagramPresentation() {}

    /**
     * Compute tooltip HTML for a DOM element. <b>Must be called under a read action.</b>
     */
    public static @Nullable String computeTooltipHtml(@NotNull DomElement element) {
        if (element instanceof StrutsPackage pkg) {
            return new HtmlTableBuilder()
                    .addLine("Package", pkg.getName().getStringValue())
                    .addLine("Namespace", pkg.searchNamespace())
                    .addLine("Extends", pkg.getExtends().getStringValue())
                    .build();
        }

        if (element instanceof Action action) {
            StrutsPackage strutsPackage = action.getStrutsPackage();
            PsiClass actionClass = action.searchActionClass();
            return new HtmlTableBuilder()
                    .addLine("Action", action.getName().getStringValue())
                    .addLine("Class", actionClass != null ? actionClass.getQualifiedName() : null)
                    .addLine("Method", action.getMethod().getStringValue())
                    .addLine("Package", strutsPackage.getName().getStringValue())
                    .addLine("Namespace", strutsPackage.searchNamespace())
                    .build();
        }

        if (element instanceof Result result) {
            PathReference ref = result.getValue();
            String displayPath = ref != null ? ref.getPath() : "(unresolved)";
            ResultType resultType = result.getEffectiveResultType();
            String resultTypeValue = resultType != null ? resultType.getName().getStringValue() : "(unknown type)";
            return new HtmlTableBuilder()
                    .addLine("Path", displayPath)
                    .addLine("Type", resultTypeValue)
                    .build();
        }

        return null;
    }

    /**
     * Navigate to the XML element backing a diagram node.
     * Safe to call from the EDT — resolves the smart pointer via
     * {@code Application.runReadAction(Computable)} which, unlike
     * {@code ReadAction.nonBlocking().executeSynchronously()}, does not
     * assert a background thread.
     */
    public static void navigateToElement(@NotNull StrutsDiagramNode node) {
        SmartPsiElementPointer<XmlElement> pointer = node.getNavigationPointer();
        if (pointer == null) return;

        Navigatable navigatable = ApplicationManager.getApplication().runReadAction(
                (com.intellij.openapi.util.Computable<Navigatable>) () -> {
                    XmlElement element = pointer.getElement();
                    return element instanceof Navigatable ? (Navigatable) element : null;
                });

        if (navigatable != null) {
            OpenSourceUtil.navigate(navigatable);
        }
    }

    private static final class HtmlTableBuilder {
        private final StringBuilder sb = new StringBuilder("<html><table>");

        HtmlTableBuilder addLine(@NotNull String label, @Nullable String content) {
            sb.append("<tr><td><strong>").append(label).append(":</strong></td>")
                    .append("<td>").append(StringUtil.isNotEmpty(content) ? content : "-").append("</td></tr>");
            return this;
        }

        String build() {
            sb.append("</table></html>");
            return sb.toString();
        }
    }
}
