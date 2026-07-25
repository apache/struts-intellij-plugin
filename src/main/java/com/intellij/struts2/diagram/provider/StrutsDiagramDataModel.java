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
import com.intellij.diagram.DiagramDataKeys;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramNode;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.diagram.model.StrutsConfigDiagramModel;
import com.intellij.struts2.diagram.model.StrutsDiagramEdge;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.util.Alarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class StrutsDiagramDataModel extends DiagramDataModel<StrutsDiagramItem> {

    private static final int DOM_UPDATE_DELAY_MS = 300;

    private final List<DiagramNode<StrutsDiagramItem>> nodes = new ArrayList<>();
    private final List<DiagramEdge<StrutsDiagramItem>> edges = new ArrayList<>();
    private final Alarm updateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
    private final @Nullable XmlFile xmlFile;
    private final @Nullable VirtualFile virtualFile;

    public StrutsDiagramDataModel(@NotNull Project project,
                                  @NotNull StrutsDiagramProvider provider,
                                  @Nullable StrutsDiagramItem seed) {
        super(project, provider);
        xmlFile = seed != null ? seed.getXmlFile() : null;
        virtualFile = xmlFile != null ? xmlFile.getVirtualFile() : null;
        if (seed != null) {
            setOriginalElement(seed);
        }
        if (virtualFile != null) {
            DomManager.getDomManager(project).addDomEventListener(event -> {
                if (StrutsDiagramDomRefresh.isEventForMyFile(event, virtualFile)) {
                    queueDebouncedRefresh();
                }
            }, this);
        }
    }

    @Override
    public @NotNull ModificationTracker getModificationTracker() {
        return PsiManager.getInstance(getProject()).getModificationTracker();
    }

    @Override
    public @NotNull Collection<? extends DiagramNode<StrutsDiagramItem>> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    @Override
    public @NotNull Collection<? extends DiagramEdge<StrutsDiagramItem>> getEdges() {
        return Collections.unmodifiableList(edges);
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
     * Must be invoked under a read action because model construction accesses PSI/DOM.
     */
    @Override
    public void refreshDataModel() {
        applyApiModel(buildApiModel());
    }

    private @NotNull ApiModel buildApiModel() {
        StrutsConfigDiagramModel snapshot =
                xmlFile != null ? StrutsConfigDiagramModel.build(xmlFile) : null;
        if (xmlFile == null || snapshot == null) {
            return ApiModel.EMPTY;
        }
        List<DiagramNode<StrutsDiagramItem>> newNodes = new ArrayList<>();
        List<DiagramEdge<StrutsDiagramItem>> newEdges = new ArrayList<>();
        Map<StrutsDiagramNode, DiagramNode<StrutsDiagramItem>> map = new IdentityHashMap<>();
        for (StrutsDiagramNode snapshotNode : snapshot.getNodes()) {
            StrutsDiagramItem item = StrutsDiagramItem.forNode(xmlFile, snapshotNode);
            StrutsDiagramApiNode apiNode = new StrutsDiagramApiNode(getProvider(), item);
            newNodes.add(apiNode);
            map.put(snapshotNode, apiNode);
        }
        for (StrutsDiagramEdge snapshotEdge : snapshot.getEdges()) {
            DiagramNode<StrutsDiagramItem> source = map.get(snapshotEdge.getSource());
            DiagramNode<StrutsDiagramItem> target = map.get(snapshotEdge.getTarget());
            if (source != null && target != null) {
                newEdges.add(new StrutsDiagramApiEdge(source, target, snapshotEdge));
            }
        }
        return new ApiModel(newNodes, newEdges);
    }

    private void applyApiModel(@NotNull ApiModel model) {
        nodes.clear();
        nodes.addAll(model.nodes());
        edges.clear();
        edges.addAll(model.edges());
    }

    private void applyLiveUpdate(@NotNull ApiModel model) {
        applyApiModel(model);
        DiagramBuilder builder = getUserData(DiagramDataKeys.GRAPH_BUILDER);
        if (builder != null) {
            DiagramDataModel.refreshDataModelInSmartMode(builder);
        }
    }

    private void queueDebouncedRefresh() {
        updateAlarm.cancelAllRequests();
        updateAlarm.addRequest(this::scheduleRefresh, DOM_UPDATE_DELAY_MS);
    }

    private void scheduleRefresh() {
        ReadAction.nonBlocking(this::buildApiModel)
                .expireWith(this)
                .coalesceBy(this)
                .finishOnUiThread(ModalityState.defaultModalityState(), this::applyLiveUpdate)
                .submit(AppExecutorUtil.getAppExecutorService());
    }

    private record ApiModel(@NotNull List<DiagramNode<StrutsDiagramItem>> nodes,
                            @NotNull List<DiagramEdge<StrutsDiagramItem>> edges) {

        private static final ApiModel EMPTY = new ApiModel(List.of(), List.of());
    }

    @Override
    public void dispose() {
    }
}
