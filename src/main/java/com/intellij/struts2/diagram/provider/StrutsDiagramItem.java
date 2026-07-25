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

import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Identifying element for Struts Diagrams API nodes.
 * Root items ({@code snapshotNode == null}) seed Show Diagram from an XML file.
 */
public final class StrutsDiagramItem {

    private final @NotNull SmartPsiElementPointer<XmlFile> filePointer;
    private final @Nullable StrutsDiagramNode snapshotNode;

    private StrutsDiagramItem(@NotNull SmartPsiElementPointer<XmlFile> filePointer,
                              @Nullable StrutsDiagramNode snapshotNode) {
        this.filePointer = filePointer;
        this.snapshotNode = snapshotNode;
    }

    public static @NotNull StrutsDiagramItem forFile(@NotNull XmlFile file) {
        return new StrutsDiagramItem(
                SmartPointerManager.getInstance(file.getProject()).createSmartPsiElementPointer(file),
                null);
    }

    public static @NotNull StrutsDiagramItem forNode(@NotNull XmlFile file, @NotNull StrutsDiagramNode node) {
        return new StrutsDiagramItem(
                SmartPointerManager.getInstance(file.getProject()).createSmartPsiElementPointer(file),
                node);
    }

    public @Nullable XmlFile getXmlFile() {
        return filePointer.getElement();
    }

    public @Nullable StrutsDiagramNode getSnapshotNode() {
        return snapshotNode;
    }

    public boolean isRoot() {
        return snapshotNode == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StrutsDiagramItem that)) return false;
        if (snapshotNode == null || that.snapshotNode == null) {
            return snapshotNode == that.snapshotNode
                    && Objects.equals(fileUrl(), that.fileUrl());
        }
        return snapshotNode.equals(that.snapshotNode);
    }

    @Override
    public int hashCode() {
        return snapshotNode != null ? snapshotNode.hashCode() : Objects.hash(fileUrl());
    }

    private @Nullable String fileUrl() {
        PsiFile file = filePointer.getElement();
        return file != null && file.getVirtualFile() != null ? file.getVirtualFile().getUrl() : null;
    }
}
