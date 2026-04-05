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

import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * Toolkit-neutral node representing a Struts config element (package, action, or result).
 * Intentionally free of any {@code com.intellij.openapi.graph} dependencies so it can
 * serve as the data layer for both the current lightweight Swing renderer and a future
 * {@code com.intellij.diagram.Provider} migration.
 */
public final class StrutsDiagramNode {

    public enum Kind { PACKAGE, ACTION, RESULT }

    private final @NotNull Kind kind;
    private final @NotNull String name;
    private final @NotNull DomElement domElement;
    private final @Nullable Icon icon;

    public StrutsDiagramNode(@NotNull Kind kind,
                             @NotNull String name,
                             @NotNull DomElement domElement,
                             @Nullable Icon icon) {
        this.kind = kind;
        this.name = name;
        this.domElement = domElement;
        this.icon = icon;
    }

    public @NotNull Kind getKind() { return kind; }
    public @NotNull String getName() { return name; }
    public @NotNull DomElement getDomElement() { return domElement; }
    public @Nullable Icon getIcon() { return icon; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StrutsDiagramNode that)) return false;
        return kind == that.kind && name.equals(that.name) && domElement.equals(that.domElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, name, domElement);
    }

    @Override
    public String toString() {
        return kind + ":" + name;
    }
}
