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
package com.intellij.struts2.diagram.provider;

import com.intellij.diagram.AbstractDiagramElementManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.struts2.dom.struts.model.StrutsManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StrutsDiagramElementManager extends AbstractDiagramElementManager<StrutsDiagramItem> {

    @Override
    public @Nullable StrutsDiagramItem findInDataContext(@NotNull DataContext context) {
        PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(context);
        if (!(psiFile instanceof XmlFile xmlFile)) {
            return null;
        }
        if (!StrutsManager.getInstance(xmlFile.getProject()).isStruts2ConfigFile(xmlFile)) {
            return null;
        }
        return StrutsDiagramItem.forFile(xmlFile);
    }

    @Override
    public boolean canBeBuiltFrom(Object element) {
        if (element instanceof StrutsDiagramItem item) {
            XmlFile file = item.getXmlFile();
            return file != null
                    && StrutsManager.getInstance(file.getProject()).isStruts2ConfigFile(file);
        }
        if (element instanceof XmlFile xmlFile) {
            return StrutsManager.getInstance(xmlFile.getProject()).isStruts2ConfigFile(xmlFile);
        }
        return false;
    }

    @Override
    public boolean isAcceptableAsNode(Object element) {
        return element instanceof StrutsDiagramItem item && !item.isRoot();
    }

    @Override
    public @Nullable @Nls String getElementTitle(StrutsDiagramItem item) {
        if (item == null) return null;
        StrutsDiagramNode node = item.getSnapshotNode();
        if (node != null) return node.getName();
        XmlFile file = item.getXmlFile();
        return file != null ? file.getName() : null;
    }

    @Override
    public @Nullable String getNodeTooltip(StrutsDiagramItem item) {
        if (item == null || item.getSnapshotNode() == null) return null;
        return item.getSnapshotNode().getTooltipHtml();
    }

    @Override
    public Object[] getNodeItems(StrutsDiagramItem element) {
        return EMPTY_ARRAY;
    }

    @Override
    public boolean canCollapse(StrutsDiagramItem element) {
        return false;
    }

    @Override
    public boolean isContainerFor(StrutsDiagramItem parent, StrutsDiagramItem child) {
        return false;
    }
}
