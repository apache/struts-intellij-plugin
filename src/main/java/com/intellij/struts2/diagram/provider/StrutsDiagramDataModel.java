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

import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.diagram.model.StrutsConfigDiagramModel;
import com.intellij.struts2.diagram.model.StrutsDiagramEdge;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class StrutsDiagramDataModel extends DiagramDataModel<StrutsDiagramItem> {

    private final List<DiagramNode<StrutsDiagramItem>> nodes = new ArrayList<>();
    private final List<DiagramEdge<StrutsDiagramItem>> edges = new ArrayList<>();

    public StrutsDiagramDataModel(@NotNull Project project,
                                  @NotNull StrutsDiagramProvider provider,
                                  @Nullable StrutsDiagramItem seed) {
        super(project, provider);
        if (seed != null) {
            setOriginalElement(seed);
        }
    }

    @Override
    public @NotNull ModificationTracker getModificationTracker() {
        return PsiManager.getInstance(getProject()).getModificationTracker();
    }

    @Override
    public @NotNull Collection<? extends DiagramNode<StrutsDiagramItem>> getNodes() {
        return nodes;
    }

    @Override
    public @NotNull Collection<? extends DiagramEdge<StrutsDiagramItem>> getEdges() {
        return edges;
    }

    @Override
    public @NotNull String getNodeName(@NotNull DiagramNode<StrutsDiagramItem> node) {
        StrutsDiagramItem item = node.getIdentifyingElement();
        String title = getProvider().getElementManager().getElementTitle(item);
        return title != null ? title : "";
    }

    @Override
    public @Nullable DiagramNode<StrutsDiagramItem> addElement(@Nullable StrutsDiagramItem element) {
        return null;
    }

    /**
     * Rebuilds the API model from the current Struts configuration snapshot.
     * Must be invoked under a read action because snapshot construction accesses PSI/DOM.
     */
    @Override
    public void refreshDataModel() {
        nodes.clear();
        edges.clear();
        StrutsDiagramItem seed = getOriginalElement();
        XmlFile xmlFile = seed != null ? seed.getXmlFile() : null;
        if (xmlFile == null) {
            return;
        }
        StrutsConfigDiagramModel snapshot = StrutsConfigDiagramModel.build(xmlFile);
        if (snapshot == null) {
            return;
        }
        Map<StrutsDiagramNode, DiagramNode<StrutsDiagramItem>> map = new IdentityHashMap<>();
        for (StrutsDiagramNode snapshotNode : snapshot.getNodes()) {
            StrutsDiagramItem item = StrutsDiagramItem.forNode(xmlFile, snapshotNode);
            StrutsDiagramApiNode apiNode = new StrutsDiagramApiNode(getProvider(), item);
            nodes.add(apiNode);
            map.put(snapshotNode, apiNode);
        }
        for (StrutsDiagramEdge snapshotEdge : snapshot.getEdges()) {
            DiagramNode<StrutsDiagramItem> source = map.get(snapshotEdge.getSource());
            DiagramNode<StrutsDiagramItem> target = map.get(snapshotEdge.getTarget());
            if (source != null && target != null) {
                edges.add(new StrutsDiagramApiEdge(source, target, snapshotEdge));
            }
        }
    }

    @Override
    public void dispose() {
    }
}
