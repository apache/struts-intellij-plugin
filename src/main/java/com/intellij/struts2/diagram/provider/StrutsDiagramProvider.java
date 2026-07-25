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

import com.intellij.diagram.BaseDiagramProvider;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramElementManager;
import com.intellij.diagram.DiagramPresentationModel;
import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.struts2.Struts2Icons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class StrutsDiagramProvider extends BaseDiagramProvider<StrutsDiagramItem> {

    public static final String ID = "ApacheStrutsConfig";

    private final DiagramElementManager<StrutsDiagramItem> elementManager = new StrutsDiagramElementManager();
    private final DiagramVfsResolver<StrutsDiagramItem> vfsResolver = new StrutsDiagramVfsResolver();

    @Override
    public @NotNull String getID() {
        return ID;
    }

    @Override
    public @NotNull @Nls String getPresentableName() {
        return "Struts Configuration";
    }

    @Override
    public @Nullable Icon getActionIcon(boolean isPopup) {
        return Struts2Icons.Action;
    }

    @Override
    public @NotNull DiagramElementManager<StrutsDiagramItem> getElementManager() {
        return elementManager;
    }

    @Override
    public @NotNull DiagramVfsResolver<StrutsDiagramItem> getVfsResolver() {
        return vfsResolver;
    }

    @Override
    public @NotNull DiagramDataModel<StrutsDiagramItem> createDataModel(@NotNull Project project,
                                                                       @Nullable StrutsDiagramItem element,
                                                                       @Nullable VirtualFile file,
                                                                       @NotNull DiagramPresentationModel presentationModel) {
        return new StrutsDiagramDataModel(project, this, element);
    }
}
