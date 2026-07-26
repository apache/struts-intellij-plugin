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
import com.intellij.diagram.DiagramRelationshipInfo;
import com.intellij.diagram.DiagramRelationships;
import com.intellij.diagram.presentation.DiagramLineType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.diagram.model.StrutsConfigDiagramModel;
import com.intellij.struts2.diagram.model.StrutsDiagramEdge;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.struts2.diagram.provider.StrutsDiagramDataModel;
import com.intellij.struts2.diagram.provider.StrutsDiagramItem;
import com.intellij.struts2.diagram.provider.StrutsDiagramProvider;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

            Set<EdgeTriple> snapshotEdgeTriples = snapshot.getEdges().stream()
                    .map(StrutsDiagramDataModelMappingTest::snapshotEdgeTriple)
                    .collect(Collectors.toSet());
            Set<EdgeTriple> apiEdgeTriples = dataModel.getEdges().stream()
                    .map(StrutsDiagramDataModelMappingTest::apiEdgeTriple)
                    .collect(Collectors.toSet());
            assertEquals("API edges must mirror snapshot endpoints and labels", snapshotEdgeTriples, apiEdgeTriples);

            for (DiagramEdge<StrutsDiagramItem> edge : dataModel.getEdges()) {
                assertNotNull(edge.getSource().getIdentifyingElement().getSnapshotNode());
                assertNotNull(edge.getTarget().getIdentifyingElement().getSnapshotNode());
                verifyRelationshipMapping(edge);
            }
        } finally {
            Disposer.dispose(dataModel);
        }
    }

    public void testExposedNodeAndEdgeCollectionsAreUnmodifiable() {
        createStrutsFileSet("struts-diagram.xml");
        VirtualFile vf = myFixture.findFileInTempDir("struts-diagram.xml");
        assertNotNull(vf);
        XmlFile xml = (XmlFile) PsiManager.getInstance(getProject()).findFile(vf);
        assertNotNull(xml);

        DiagramProvider<?> diagramProvider = DiagramProvider.findByID(StrutsDiagramProvider.ID);
        assertInstanceOf(diagramProvider, StrutsDiagramProvider.class);
        StrutsDiagramDataModel dataModel = new StrutsDiagramDataModel(
                getProject(), (StrutsDiagramProvider) diagramProvider, StrutsDiagramItem.forFile(xml));
        try {
            ReadAction.run(dataModel::refreshDataModel);

            assertThrows(UnsupportedOperationException.class, () -> dataModel.getNodes().clear());
            assertThrows(UnsupportedOperationException.class, () -> dataModel.getEdges().clear());
        } finally {
            Disposer.dispose(dataModel);
        }
    }

    public void testSameFileDomEventRefreshesLiveDataModel() throws InterruptedException {
        createStrutsFileSet("struts-diagram.xml");
        VirtualFile vf = myFixture.findFileInTempDir("struts-diagram.xml");
        assertNotNull(vf);
        XmlFile xml = (XmlFile) PsiManager.getInstance(getProject()).findFile(vf);
        assertNotNull(xml);

        DiagramProvider<?> diagramProvider = DiagramProvider.findByID(StrutsDiagramProvider.ID);
        assertInstanceOf(diagramProvider, StrutsDiagramProvider.class);
        StrutsDiagramDataModel dataModel = new StrutsDiagramDataModel(
                getProject(), (StrutsDiagramProvider) diagramProvider, StrutsDiagramItem.forFile(xml));
        try {
            ReadAction.run(dataModel::refreshDataModel);
            int initialNodeCount = dataModel.getNodes().size();

            Document document = PsiDocumentManager.getInstance(getProject()).getDocument(xml);
            assertNotNull(document);
            WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                String updated = document.getText().replace(
                        "</package>",
                        "<action name=\"liveRefresh\" class=\"MyClass\"/>\n</package>");
                document.setText(updated);
                PsiDocumentManager.getInstance(getProject()).commitDocument(document);
            });

            long deadline = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < deadline
                    && dataModel.getNodes().size() == initialNodeCount) {
                UIUtil.dispatchAllInvocationEvents();
                Thread.sleep(50);
            }

            assertTrue("Same-file DomEvent must refresh the live diagram data model",
                    dataModel.getNodes().size() > initialNodeCount);
        } finally {
            Disposer.dispose(dataModel);
        }
    }

    private record EdgeTriple(@NotNull String sourceId, @NotNull String targetId, @NotNull String label) {
    }

    private static @NotNull EdgeTriple snapshotEdgeTriple(@NotNull StrutsDiagramEdge edge) {
        return new EdgeTriple(edge.getSource().getId(), edge.getTarget().getId(), edge.getLabel());
    }

    private static @NotNull EdgeTriple apiEdgeTriple(@NotNull DiagramEdge<StrutsDiagramItem> edge) {
        StrutsDiagramNode source = edge.getSource().getIdentifyingElement().getSnapshotNode();
        StrutsDiagramNode target = edge.getTarget().getIdentifyingElement().getSnapshotNode();
        assertNotNull(source);
        assertNotNull(target);
        return new EdgeTriple(source.getId(), target.getId(), apiEdgeLabel(edge));
    }

    private static @NotNull String apiEdgeLabel(@NotNull DiagramEdge<StrutsDiagramItem> edge) {
        DiagramRelationshipInfo relationship = edge.getRelationship();
        if (relationship == DiagramRelationships.DEPENDENCY) {
            return "";
        }
        String label = centerLabelText(relationship.getUpperCenterLabel());
        if (label.isEmpty()) {
            label = centerLabelText(relationship.getBottomCenterLabel());
        }
        return label;
    }

    private static @NotNull String centerLabelText(@Nullable DiagramRelationshipInfo.Label label) {
        if (label == null) {
            return "";
        }
        String text = label.getText();
        return text != null ? text : "";
    }

    private static void verifyRelationshipMapping(@NotNull DiagramEdge<StrutsDiagramItem> edge) {
        String label = apiEdgeLabel(edge);
        DiagramRelationshipInfo relationship = edge.getRelationship();
        if (label.isEmpty()) {
            assertSame("Unlabeled edges must use DEPENDENCY", DiagramRelationships.DEPENDENCY, relationship);
        }
        else {
            assertNotSame("Labeled edges must not use DEPENDENCY", DiagramRelationships.DEPENDENCY, relationship);
            assertEquals(label, centerLabelText(relationship.getUpperCenterLabel()));
            assertEquals("Labeled edges must be solid", DiagramLineType.SOLID, relationship.getLineType());
            assertSame("Labeled edges must have ANGLE target arrow",
                    DiagramRelationshipInfo.ANGLE, relationship.getTargetArrow());
        }
    }
}
