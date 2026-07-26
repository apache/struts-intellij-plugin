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

import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramPresentationModel;
import com.intellij.diagram.extras.EditNodeHandler;
import com.intellij.diagram.extras.custom.CommonDiagramExtras;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.graph.GraphManager;
import com.intellij.openapi.graph.layout.Layouter;
import com.intellij.openapi.graph.layout.LayoutOrientation;
import com.intellij.openapi.graph.layout.hierarchic.HierarchicGroupLayouter;
import com.intellij.openapi.graph.settings.GraphSettings;
import com.intellij.openapi.graph.view.NodeRealizer;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.xml.XmlElement;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.struts2.diagram.presentation.StrutsDiagramPresentation;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Point;
import java.util.List;

/**
 * Diagrams host hooks for Struts config diagrams, including compact label chrome
 * provided by {@link #createLabelNode}.
 * <p>
 * Double-click in the diagram <em>editor</em> (non-popup) goes through
 * {@link EditNodeHandler}, not {@link DiagramNode}'s {@link Navigatable} API.
 * Popup mode and Jump to Source use {@link #uiDataSnapshot}.
 */
public final class StrutsDiagramExtras extends CommonDiagramExtras<StrutsDiagramItem> {

    private final EditNodeHandler<StrutsDiagramItem> editNodeHandler = this::navigateNode;

    /**
     * Preserves the pre-#120 {@code DiagramExtras} default.
     */
    @Override
    public boolean isZoomAnimationsEnabled() {
        return false;
    }

    /**
     * Prefer package → action → result left-to-right on Show Diagram.
     * {@code settings} is deliberately not consulted: the platform default orientation is
     * top-to-bottom, and the soft preference on Dom refresh is not resetting the user's
     * toolbar layout choice rather than reading orientation from {@link GraphSettings}.
     * <p>
     * Minimal node/layer distances match Maven/Gradle UML extras so multi-result edge
     * labels stay readable under LTR hierarchy.
     */
    @Override
    public @NotNull Layouter getCustomLayouter(GraphSettings settings,
                                               Project project) {
        GraphManager graphManager = GraphManager.getGraphManager();
        HierarchicGroupLayouter layouter = graphManager.createHierarchicGroupLayouter();
        layouter.setOrientationLayouter(
                graphManager.createOrientationLayouter(LayoutOrientation.LEFT_TO_RIGHT));
        layouter.setMinimalNodeDistance(20);
        layouter.setMinimalLayerDistance(20);
        return layouter;
    }

    @Override
    public @NotNull JComponent createNodeComponent(@NotNull DiagramNode<StrutsDiagramItem> node,
                                                   @NotNull DiagramBuilder builder,
                                                   @NotNull NodeRealizer nodeRealizer,
                                                   @NotNull JPanel wrapper) {
        return createLabelNode(node, builder, wrapper);
    }

    @Override
    public @NotNull JComponent createNodeComponent(@NotNull DiagramNode<StrutsDiagramItem> node,
                                                   @NotNull DiagramBuilder builder,
                                                   @NotNull Point basePoint,
                                                   @NotNull JPanel wrapper) {
        return createLabelNode(node, builder, wrapper);
    }

    @Override
    public @NotNull EditNodeHandler<StrutsDiagramItem> getEditNodeHandler() {
        return editNodeHandler;
    }

    @Override
    public void uiDataSnapshot(@NotNull DataSink sink,
                               @NotNull List<DiagramNode<StrutsDiagramItem>> nodes,
                               @NotNull DiagramBuilder builder) {
        DiagramNode<StrutsDiagramItem> only = ContainerUtil.getOnlyItem(nodes);
        if (only == null) {
            return;
        }
        sink.lazy(CommonDataKeys.PSI_ELEMENT, () -> resolvePsiElement(only.getIdentifyingElement()));
        sink.lazy(CommonDataKeys.NAVIGATABLE, () -> {
            if (only instanceof Navigatable navigatable && navigatable.canNavigate()) {
                return navigatable;
            }
            PsiElement psi = resolvePsiElement(only.getIdentifyingElement());
            return psi instanceof Navigatable ? (Navigatable) psi : null;
        });
    }

    private void navigateNode(@NotNull DiagramNode<StrutsDiagramItem> node,
                              DiagramPresentationModel presentationModel) {
        StrutsDiagramItem item = node.getIdentifyingElement();
        if (item == null) {
            return;
        }
        StrutsDiagramNode snapshotNode = item.getSnapshotNode();
        if (snapshotNode != null) {
            StrutsDiagramPresentation.navigateToElement(snapshotNode);
        }
    }

    public static @Nullable PsiElement resolvePsiElement(@Nullable StrutsDiagramItem item) {
        if (item == null) {
            return null;
        }
        StrutsDiagramNode snapshotNode = item.getSnapshotNode();
        if (snapshotNode == null) {
            return item.getXmlFile();
        }
        SmartPsiElementPointer<XmlElement> pointer = snapshotNode.getNavigationPointer();
        if (pointer == null) {
            return null;
        }
        return ApplicationManager.getApplication().runReadAction(
                (com.intellij.openapi.util.Computable<PsiElement>) pointer::getElement);
    }
}
