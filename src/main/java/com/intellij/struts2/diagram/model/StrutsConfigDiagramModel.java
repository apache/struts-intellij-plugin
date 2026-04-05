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
package com.intellij.struts2.diagram.model;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.paths.PathReference;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.Struts2Icons;
import com.intellij.struts2.diagram.presentation.StrutsDiagramPresentation;
import com.intellij.struts2.dom.struts.action.Action;
import com.intellij.struts2.dom.struts.action.Result;
import com.intellij.struts2.dom.struts.model.StrutsManager;
import com.intellij.struts2.dom.struts.model.StrutsModel;
import com.intellij.struts2.dom.struts.strutspackage.StrutsPackage;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * Builds a toolkit-neutral snapshot of the Struts configuration for diagram rendering.
 * Walks {@link StrutsModel} to produce package, action, and result nodes with directed
 * edges (package->action, action->result).
 * <p>
 * <b>Must be called under a read action.</b> All DOM/PSI access (tooltip computation,
 * navigation pointer creation) happens here so that Swing event handlers on the EDT
 * never need to touch PSI directly.
 */
public final class StrutsConfigDiagramModel {

    private static final String UNKNOWN = "???";

    private final List<StrutsDiagramNode> nodes = new ArrayList<>();
    private final List<StrutsDiagramEdge> edges = new ArrayList<>();

    private StrutsConfigDiagramModel() {}

    public @NotNull List<StrutsDiagramNode> getNodes() { return Collections.unmodifiableList(nodes); }
    public @NotNull List<StrutsDiagramEdge> getEdges() { return Collections.unmodifiableList(edges); }

    /**
     * Build a snapshot from the Struts model associated with the given XML file.
     * <b>Must be called under a read action</b> — all DOM/PSI access happens here.
     *
     * @return populated model, or {@code null} if no Struts model is available.
     */
    public static @Nullable StrutsConfigDiagramModel build(@NotNull XmlFile xmlFile) {
        StrutsModel strutsModel = StrutsManager.getInstance(xmlFile.getProject()).getModelByFile(xmlFile);
        if (strutsModel == null) return null;

        SmartPointerManager pointerManager = SmartPointerManager.getInstance(xmlFile.getProject());
        StrutsConfigDiagramModel model = new StrutsConfigDiagramModel();

        for (StrutsPackage strutsPackage : strutsModel.getStrutsPackages()) {
            String pkgName = Objects.toString(strutsPackage.getName().getStringValue(), UNKNOWN);
            StrutsDiagramNode pkgNode = createNode(
                    StrutsDiagramNode.Kind.PACKAGE, pkgName, AllIcons.Nodes.Package,
                    strutsPackage, pointerManager);
            model.nodes.add(pkgNode);

            for (Action action : strutsPackage.getActions()) {
                String actionName = Objects.toString(action.getName().getStringValue(), UNKNOWN);
                StrutsDiagramNode actionNode = createNode(
                        StrutsDiagramNode.Kind.ACTION, actionName, Struts2Icons.Action,
                        action, pointerManager);
                model.nodes.add(actionNode);
                model.edges.add(new StrutsDiagramEdge(pkgNode, actionNode, ""));

                for (Result result : action.getResults()) {
                    PathReference pathRef = result.getValue();
                    String path = pathRef != null ? pathRef.getPath() : UNKNOWN;
                    Icon resultIcon = resolveResultIcon(result);
                    StrutsDiagramNode resultNode = createNode(
                            StrutsDiagramNode.Kind.RESULT, path, resultIcon,
                            result, pointerManager);
                    model.nodes.add(resultNode);

                    String resultName = result.getName().getStringValue();
                    model.edges.add(new StrutsDiagramEdge(actionNode, resultNode,
                            resultName != null ? resultName : Result.DEFAULT_NAME));
                }
            }
        }
        return model;
    }

    private static @NotNull StrutsDiagramNode createNode(
            @NotNull StrutsDiagramNode.Kind kind,
            @NotNull String name,
            @Nullable Icon icon,
            @NotNull DomElement domElement,
            @NotNull SmartPointerManager pointerManager) {

        String tooltipHtml = StrutsDiagramPresentation.computeTooltipHtml(domElement);
        SmartPsiElementPointer<XmlElement> navPointer = createNavigationPointer(domElement, pointerManager);
        return new StrutsDiagramNode(kind, name, icon, tooltipHtml, navPointer);
    }

    private static @Nullable SmartPsiElementPointer<XmlElement> createNavigationPointer(
            @NotNull DomElement domElement,
            @NotNull SmartPointerManager pointerManager) {
        XmlElement xmlElement = domElement.getXmlElement();
        if (xmlElement == null) return null;
        return pointerManager.createSmartPsiElementPointer(xmlElement);
    }

    private static @NotNull Icon resolveResultIcon(@NotNull Result result) {
        if (!result.isValid()) return AllIcons.FileTypes.Unknown;
        PathReference ref = result.getValue();
        if (ref == null || ref.resolve() == null) return AllIcons.FileTypes.Unknown;
        Icon icon = ref.getIcon();
        return icon != null ? icon : AllIcons.FileTypes.Unknown;
    }
}
