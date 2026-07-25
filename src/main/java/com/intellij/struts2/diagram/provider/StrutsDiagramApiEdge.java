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

import com.intellij.diagram.DiagramEdgeBase;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramRelationshipInfo;
import com.intellij.diagram.DiagramRelationshipInfoAdapter;
import com.intellij.diagram.DiagramRelationships;
import com.intellij.diagram.presentation.DiagramLineType;
import com.intellij.struts2.diagram.model.StrutsDiagramEdge;
import org.jetbrains.annotations.NotNull;

public final class StrutsDiagramApiEdge extends DiagramEdgeBase<StrutsDiagramItem> {

    public StrutsDiagramApiEdge(@NotNull DiagramNode<StrutsDiagramItem> source,
                                @NotNull DiagramNode<StrutsDiagramItem> target,
                                @NotNull StrutsDiagramEdge snapshotEdge) {
        super(source, target, relationshipFor(snapshotEdge));
    }

    private static @NotNull DiagramRelationshipInfo relationshipFor(@NotNull StrutsDiagramEdge edge) {
        String label = edge.getLabel();
        if (label.isEmpty()) {
            return DiagramRelationships.DEPENDENCY;
        }
        return new DiagramRelationshipInfoAdapter(label, DiagramLineType.SOLID, label);
    }
}
