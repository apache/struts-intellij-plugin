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

import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.diagram.model.StrutsConfigDiagramModel;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StrutsDiagramVfsResolver implements DiagramVfsResolver<StrutsDiagramItem> {

    @Override
    public @Nullable String getQualifiedName(StrutsDiagramItem item) {
        if (item == null) return null;
        XmlFile file = item.getXmlFile();
        if (file == null || file.getVirtualFile() == null) return null;
        if (item.getSnapshotNode() != null) {
            return file.getVirtualFile().getUrl() + "#" + item.getSnapshotNode().getId();
        }
        return file.getVirtualFile().getUrl();
    }

    @Override
    public @Nullable StrutsDiagramItem resolveElementByFQN(@NotNull String fqn, @NotNull Project project) {
        int nodeSeparator = fqn.indexOf('#');
        String url = nodeSeparator >= 0 ? fqn.substring(0, nodeSeparator) : fqn;
        VirtualFile vf = VirtualFileManager.getInstance().findFileByUrl(url);
        if (vf == null) return null;
        PsiFile psi = PsiManager.getInstance(project).findFile(vf);
        if (!(psi instanceof XmlFile xmlFile)) return null;
        if (nodeSeparator >= 0) {
            String nodeId = fqn.substring(nodeSeparator + 1);
            StrutsConfigDiagramModel model = ApplicationManager.getApplication().isReadAccessAllowed()
                    ? StrutsConfigDiagramModel.build(xmlFile)
                    : ReadAction.nonBlocking(() -> StrutsConfigDiagramModel.build(xmlFile))
                            .executeSynchronously();
            if (model != null) {
                StrutsDiagramNode node = model.getNodes().stream()
                        .filter(candidate -> candidate.getId().equals(nodeId))
                        .findFirst()
                        .orElse(null);
                if (node != null) {
                    return StrutsDiagramItem.forNode(xmlFile, node);
                }
            }
        }
        return StrutsDiagramItem.forFile(xmlFile);
    }
}
