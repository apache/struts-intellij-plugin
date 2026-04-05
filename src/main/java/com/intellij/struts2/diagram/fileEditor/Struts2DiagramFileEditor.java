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
package com.intellij.struts2.diagram.fileEditor;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.diagram.model.StrutsConfigDiagramModel;
import com.intellij.struts2.diagram.ui.Struts2DiagramComponent;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ui.PerspectiveFileEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Read-only file editor that hosts the lightweight Struts config diagram.
 */
public class Struts2DiagramFileEditor extends PerspectiveFileEditor {

    private static final Logger LOG = Logger.getInstance(Struts2DiagramFileEditor.class);

    private final XmlFile myXmlFile;
    private Struts2DiagramComponent myComponent;

    public Struts2DiagramFileEditor(final Project project, final VirtualFile file) {
        super(project, file);

        final PsiFile psiFile = getPsiFile();
        assert psiFile instanceof XmlFile;
        myXmlFile = (XmlFile) psiFile;
    }

    @Override
    @Nullable
    protected DomElement getSelectedDomElement() {
        return null;
    }

    @Override
    protected void setSelectedDomElement(final DomElement domElement) {
    }

    @Override
    @NotNull
    protected JComponent createCustomComponent() {
        return getDiagramComponent();
    }

    @Override
    @Nullable
    public JComponent getPreferredFocusedComponent() {
        return getDiagramComponent();
    }

    @Override
    public void commit() {
    }

    @Override
    public void reset() {
        StrutsConfigDiagramModel model = ReadAction.nonBlocking(() -> StrutsConfigDiagramModel.build(myXmlFile))
                .executeSynchronously();
        if (model != null) {
            getDiagramComponent().rebuild(model);
        } else {
            LOG.debug("reset() got null model for " + myXmlFile.getName() + ", keeping existing content");
        }
    }

    @Override
    @NotNull
    public String getName() {
        return "Diagram";
    }

    private Struts2DiagramComponent getDiagramComponent() {
        if (myComponent == null) {
            final StrutsConfigDiagramModel[] model = {null};
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    () -> model[0] = ReadAction.nonBlocking(() -> StrutsConfigDiagramModel.build(myXmlFile))
                            .executeSynchronously(),
                    "Building Diagram", false, myXmlFile.getProject());
            myComponent = new Struts2DiagramComponent(model[0]);
        }
        return myComponent;
    }
}
