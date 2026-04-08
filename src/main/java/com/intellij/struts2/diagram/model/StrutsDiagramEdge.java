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

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Toolkit-neutral directed edge between two {@link StrutsDiagramNode}s.
 */
public final class StrutsDiagramEdge {

    private final @NotNull StrutsDiagramNode source;
    private final @NotNull StrutsDiagramNode target;
    private final @NotNull String label;

    public StrutsDiagramEdge(@NotNull StrutsDiagramNode source,
                             @NotNull StrutsDiagramNode target,
                             @NotNull String label) {
        this.source = source;
        this.target = target;
        this.label = label;
    }

    public @NotNull StrutsDiagramNode getSource() { return source; }
    public @NotNull StrutsDiagramNode getTarget() { return target; }
    public @NotNull String getLabel() { return label; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StrutsDiagramEdge that)) return false;
        return source.equals(that.source) && target.equals(that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }
}
