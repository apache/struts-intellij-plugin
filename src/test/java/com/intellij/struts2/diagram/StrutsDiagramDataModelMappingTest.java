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
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class StrutsDiagramDataModelMappingTest extends BasicLightHighlightingTestCase {

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        return WEB;
    }

    @Override
    protected void performSetUp() {
        myFixture.addFileToProject("pages/test.jsp", "<html></html>");
        myFixture.addFileToProject("pages/delete.jsp", "<html></html>");
    }

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

    public void testDomPathEditUpdatesResultTitleOnRetainedApiNode() throws InterruptedException {
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
            DiagramNode<StrutsDiagramItem> resultNode = dataModel.getNodes().stream()
                    .filter(n -> {
                        StrutsDiagramNode snap = n.getIdentifyingElement().getSnapshotNode();
                        return snap != null && snap.getKind() == StrutsDiagramNode.Kind.RESULT;
                    })
                    .findFirst()
                    .orElseThrow();
            String oldPath = resultNode.getIdentifyingElement().getSnapshotNode().getName();
            assertTrue(oldPath.contains("test.jsp"));

            Document document = PsiDocumentManager.getInstance(getProject()).getDocument(xml);
            assertNotNull(document);
            WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                document.setText(document.getText().replace("/pages/test.jsp", "/pages/delete.jsp"));
                PsiDocumentManager.getInstance(getProject()).commitDocument(document);
            });

            long deadline = System.currentTimeMillis() + 10_000;
            boolean updated = false;
            while (System.currentTimeMillis() < deadline) {
                UIUtil.dispatchAllInvocationEvents();
                StrutsDiagramNode snap = resultNode.getIdentifyingElement().getSnapshotNode();
                if (snap != null && snap.getName().contains("delete.jsp")) {
                    updated = true;
                    break;
                }
                Thread.sleep(50);
            }
            assertTrue("Dom refresh must update retained API node presentable path", updated);

            boolean sameInstance = dataModel.getNodes().stream().anyMatch(n -> n == resultNode);
            assertTrue("Live merge must retain DiagramNode instance for soft layout", sameInstance);

            // Original path must be gone from result titles.
            boolean stale = dataModel.getNodes().stream()
                    .map(DiagramNode::getIdentifyingElement)
                    .map(StrutsDiagramItem::getSnapshotNode)
                    .filter(Objects::nonNull)
                    .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                    .anyMatch(n -> n.getName().contains("test.jsp"));
            assertFalse(stale);
        } finally {
            Disposer.dispose(dataModel);
        }
    }

    public void testCopyPasteResultGetsDistinctPathAfterDomRefresh() throws InterruptedException {
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
            int initialResults = (int) dataModel.getNodes().stream()
                    .map(DiagramNode::getIdentifyingElement)
                    .map(StrutsDiagramItem::getSnapshotNode)
                    .filter(Objects::nonNull)
                    .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                    .count();
            assertEquals(1, initialResults);

            Document document = PsiDocumentManager.getInstance(getProject()).getDocument(xml);
            assertNotNull(document);
            WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                String updated = document.getText().replace(
                        "<result>/pages/test.jsp</result>",
                        "<result name=\"success\">/pages/test.jsp</result>\n" +
                                "      <result name=\"delete\">/pages/delete.jsp</result>");
                document.setText(updated);
                PsiDocumentManager.getInstance(getProject()).commitDocument(document);
            });

            long deadline = System.currentTimeMillis() + 10_000;
            Set<String> resultNames = Set.of();
            while (System.currentTimeMillis() < deadline) {
                UIUtil.dispatchAllInvocationEvents();
                resultNames = dataModel.getNodes().stream()
                        .map(DiagramNode::getIdentifyingElement)
                        .map(StrutsDiagramItem::getSnapshotNode)
                        .filter(Objects::nonNull)
                        .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                        .map(StrutsDiagramNode::getName)
                        .collect(Collectors.toSet());
                if (resultNames.size() >= 2) {
                    break;
                }
                Thread.sleep(50);
            }

            assertTrue("Expected success path present, got: " + resultNames,
                    resultNames.stream().anyMatch(n -> n.contains("test.jsp")));
            assertTrue("Expected delete path present, got: " + resultNames,
                    resultNames.stream().anyMatch(n -> n.contains("delete.jsp")));
            assertFalse("Delete must not reuse success path label",
                    resultNames.size() == 1 && resultNames.iterator().next().contains("test.jsp"));

            Set<String> edgeLabels = dataModel.getEdges().stream()
                    .map(StrutsDiagramDataModelMappingTest::apiEdgeLabel)
                    .collect(Collectors.toSet());
            assertTrue(edgeLabels.contains("success"));
            assertTrue(edgeLabels.contains("delete"));

            assertApiEdgeTargetsResultPath(dataModel, "success", "test.jsp");
            assertApiEdgeTargetsResultPath(dataModel, "delete", "delete.jsp");
        } finally {
            Disposer.dispose(dataModel);
        }
    }

    private static void assertApiEdgeTargetsResultPath(@NotNull StrutsDiagramDataModel dataModel,
                                                       @NotNull String edgeLabel,
                                                       @NotNull String expectedPath) {
        StrutsDiagramNode target = dataModel.getEdges().stream()
                .filter(edge -> edgeLabel.equals(apiEdgeLabel(edge)))
                .map(DiagramEdge::getTarget)
                .map(DiagramNode::getIdentifyingElement)
                .map(StrutsDiagramItem::getSnapshotNode)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing target for edge labeled " + edgeLabel));
        assertEquals(StrutsDiagramNode.Kind.RESULT, target.getKind());
        assertTrue("Edge labeled " + edgeLabel + " must target result path containing " + expectedPath
                + ", got: " + target.getName(), target.getName().contains(expectedPath));
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
