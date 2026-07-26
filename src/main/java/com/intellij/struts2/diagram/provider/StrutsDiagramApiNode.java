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

import com.intellij.diagram.DiagramNodeBase;
import com.intellij.diagram.DiagramProvider;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.struts2.diagram.presentation.StrutsDiagramPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class StrutsDiagramApiNode extends DiagramNodeBase<StrutsDiagramItem> {

    private final @NotNull StrutsDiagramItem item;

    public StrutsDiagramApiNode(@NotNull DiagramProvider<StrutsDiagramItem> provider,
                                @NotNull StrutsDiagramItem item) {
        super(provider);
        this.item = item;
    }

    @Override
    public @NotNull StrutsDiagramItem getIdentifyingElement() {
        return item;
    }

    @Override
    public @Nullable String getTooltip() {
        StrutsDiagramNode node = item.getSnapshotNode();
        return node != null ? node.getTooltipHtml() : null;
    }

    @Override
    public @Nullable Icon getIcon() {
        StrutsDiagramNode node = item.getSnapshotNode();
        return node != null ? node.getIcon() : null;
    }

    @Override
    public void navigate(boolean requestFocus) {
        StrutsDiagramNode node = item.getSnapshotNode();
        if (node != null) {
            StrutsDiagramPresentation.navigateToElement(node);
        }
    }

    @Override
    public boolean canNavigate() {
        StrutsDiagramNode node = item.getSnapshotNode();
        return node != null && node.getNavigationPointer() != null;
    }
}
