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

import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.xml.XmlElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * Toolkit-neutral node representing a Struts config element (package, action, or result).
 * <p>
 * Each node carries a stable {@link #id} captured during model build that uniquely
 * identifies it even when two elements share the same display {@link #name} (e.g.
 * duplicate action names across packages, or identical result paths).  The UI
 * renderer uses node identity for layout maps and edge lookup, so uniqueness here
 * is critical.
 * <p>
 * UI-safe fields ({@link #getTooltipHtml()}, {@link #getNavigationPointer()}, {@link #getIcon()})
 * are precomputed during snapshot creation under a read action so that Swing event handlers
 * on the EDT never need to touch PSI/DOM directly.
 * <p>
 * Intentionally free of any {@code com.intellij.openapi.graph} dependencies so it can
 * serve as the data layer for both the current lightweight Swing renderer and a future
 * {@code com.intellij.diagram.Provider} migration.
 */
public final class StrutsDiagramNode {

    public enum Kind { PACKAGE, ACTION, RESULT }

    private final @NotNull String id;
    private final @NotNull Kind kind;
    private final @NotNull String name;
    private final @Nullable Icon icon;
    private final @Nullable String tooltipHtml;
    private final @Nullable SmartPsiElementPointer<XmlElement> navigationPointer;

    public StrutsDiagramNode(@NotNull String id,
                             @NotNull Kind kind,
                             @NotNull String name,
                             @Nullable Icon icon,
                             @Nullable String tooltipHtml,
                             @Nullable SmartPsiElementPointer<XmlElement> navigationPointer) {
        this.id = id;
        this.kind = kind;
        this.name = name;
        this.icon = icon;
        this.tooltipHtml = tooltipHtml;
        this.navigationPointer = navigationPointer;
    }

    public @NotNull String getId() { return id; }
    public @NotNull Kind getKind() { return kind; }
    public @NotNull String getName() { return name; }
    public @Nullable Icon getIcon() { return icon; }
    public @Nullable String getTooltipHtml() { return tooltipHtml; }
    public @Nullable SmartPsiElementPointer<XmlElement> getNavigationPointer() { return navigationPointer; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StrutsDiagramNode that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return kind + ":" + name + "(" + id + ")";
    }
}
