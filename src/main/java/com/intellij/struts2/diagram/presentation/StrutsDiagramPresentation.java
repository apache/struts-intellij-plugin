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

import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
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
 * Toolkit-neutral presentation helpers for Struts diagram nodes: tooltips, navigation,
 * and labels. Intentionally free of any {@code com.intellij.openapi.graph} dependencies
 * so it can serve both the current lightweight Swing renderer and a future
 * {@code com.intellij.diagram.Provider} migration.
 */
public final class StrutsDiagramPresentation {

    private StrutsDiagramPresentation() {}

    public static @Nullable String getTooltipHtml(@NotNull StrutsDiagramNode node) {
        DomElement element = node.getDomElement();

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
            String displayPath = ref != null ? ref.getPath() : "???";
            ResultType resultType = result.getEffectiveResultType();
            String resultTypeValue = resultType != null ? resultType.getName().getStringValue() : "???";
            return new HtmlTableBuilder()
                    .addLine("Path", displayPath)
                    .addLine("Type", resultTypeValue)
                    .build();
        }

        return null;
    }

    public static void navigateToElement(@NotNull StrutsDiagramNode node) {
        XmlElement xmlElement = node.getDomElement().getXmlElement();
        if (xmlElement instanceof Navigatable) {
            OpenSourceUtil.navigate((Navigatable) xmlElement);
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
