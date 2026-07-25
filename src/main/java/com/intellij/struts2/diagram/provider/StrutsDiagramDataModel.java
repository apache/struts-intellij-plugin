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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public final class StrutsDiagramDataModel extends DiagramDataModel<StrutsDiagramItem> {

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
        return Collections.emptyList();
    }

    @Override
    public @NotNull Collection<? extends DiagramEdge<StrutsDiagramItem>> getEdges() {
        return Collections.emptyList();
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

    @Override
    public void refreshDataModel() {
        // Task 4
    }

    @Override
    public void dispose() {
    }
}
