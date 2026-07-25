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
package com.intellij.struts2.diagram;

import com.intellij.diagram.DiagramEdge;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.DiagramProvider;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.diagram.model.StrutsConfigDiagramModel;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.struts2.diagram.provider.StrutsDiagramDataModel;
import com.intellij.struts2.diagram.provider.StrutsDiagramItem;
import com.intellij.struts2.diagram.provider.StrutsDiagramProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

public class StrutsDiagramDataModelMappingTest extends BasicLightHighlightingTestCase {

    @Override
    @NotNull
    protected String getTestDataLocation() {
        return "diagram";
    }

    public void testRefreshMapsSnapshotNodesAndEdges() {
        createStrutsFileSet("struts-diagram.xml");
        VirtualFile vf = myFixture.findFileInTempDir("struts-diagram.xml");
        assertNotNull(vf);
        XmlFile xml = (XmlFile) PsiManager.getInstance(getProject()).findFile(vf);
        assertNotNull(xml);

        StrutsConfigDiagramModel snapshot = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build(xml)).executeSynchronously();
        assertNotNull(snapshot);
        assertFalse(snapshot.getNodes().isEmpty());

        DiagramProvider<?> diagramProvider = DiagramProvider.findByID(StrutsDiagramProvider.ID);
        assertNotNull(diagramProvider);
        assertInstanceOf(diagramProvider, StrutsDiagramProvider.class);
        StrutsDiagramProvider provider = (StrutsDiagramProvider) diagramProvider;
        StrutsDiagramDataModel dataModel =
                new StrutsDiagramDataModel(getProject(), provider, StrutsDiagramItem.forFile(xml));
        try {
            ReadAction.run(dataModel::refreshDataModel);

            assertEquals(snapshot.getNodes().size(), dataModel.getNodes().size());
            assertEquals(snapshot.getEdges().size(), dataModel.getEdges().size());

            Set<String> snapshotIds = snapshot.getNodes().stream()
                    .map(StrutsDiagramNode::getId).collect(Collectors.toSet());
            Set<String> apiIds = dataModel.getNodes().stream()
                    .map(DiagramNode::getIdentifyingElement)
                    .map(StrutsDiagramItem::getSnapshotNode)
                    .map(StrutsDiagramNode::getId)
                    .collect(Collectors.toSet());
            assertEquals(snapshotIds, apiIds);

            for (DiagramEdge<StrutsDiagramItem> edge : dataModel.getEdges()) {
                assertNotNull(edge.getSource().getIdentifyingElement().getSnapshotNode());
                assertNotNull(edge.getTarget().getIdentifyingElement().getSnapshotNode());
            }
        } finally {
            dataModel.dispose();
        }
    }
}
