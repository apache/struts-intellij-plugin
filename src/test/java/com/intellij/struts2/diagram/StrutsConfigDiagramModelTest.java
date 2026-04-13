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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.diagram.model.StrutsConfigDiagramModel;
import com.intellij.struts2.diagram.model.StrutsDiagramEdge;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.struts2.diagram.ui.Struts2DiagramComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tests for {@link StrutsConfigDiagramModel} covering file-local snapshot,
 * fallback states, unresolved result labeling, and component state mapping.
 */
public class StrutsConfigDiagramModelTest extends BasicLightHighlightingTestCase {

    @Override
    @NotNull
    protected String getTestDataLocation() {
        return "diagram";
    }

    public void testBuildReturnsOnlyLocalPackagesAndActions() {
        createStrutsFileSet("struts-local-a.xml", "struts-local-b.xml");

        VirtualFile vfA = myFixture.findFileInTempDir("struts-local-a.xml");
        assertNotNull("struts-local-a.xml should exist in temp dir", vfA);

        PsiFile psiA = PsiManager.getInstance(getProject()).findFile(vfA);
        assertInstanceOf(psiA, XmlFile.class);

        StrutsConfigDiagramModel modelA = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psiA)).executeSynchronously();
        assertNotNull("Model should be built for struts-local-a.xml", modelA);

        Set<String> nodeNames = modelA.getNodes().stream()
                .map(StrutsDiagramNode::getName)
                .collect(Collectors.toSet());

        assertTrue("Should contain local package, got: " + nodeNames, nodeNames.contains("packageA"));
        assertTrue("Should contain local action actionA1, got: " + nodeNames, nodeNames.contains("actionA1"));
        assertTrue("Should contain local action actionA2, got: " + nodeNames, nodeNames.contains("actionA2"));

        long resultCount = modelA.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                .count();
        assertEquals("Should have 2 local results, got names: " + nodeNames, 2, resultCount);

        assertFalse("Should NOT contain package from other file", nodeNames.contains("packageB"));
        assertFalse("Should NOT contain action from other file", nodeNames.contains("actionB1"));
        assertFalse("Should NOT contain result from other file", nodeNames.contains("/b/page1.jsp"));
    }

    public void testBuildReturnsCorrectNodeKinds() {
        createStrutsFileSet("struts-local-a.xml");

        VirtualFile vfA = myFixture.findFileInTempDir("struts-local-a.xml");
        assertNotNull("struts-local-a.xml should exist in temp dir", vfA);

        PsiFile psiA = PsiManager.getInstance(getProject()).findFile(vfA);
        assertInstanceOf(psiA, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psiA)).executeSynchronously();
        assertNotNull(model);

        List<StrutsDiagramNode> nodes = model.getNodes();
        long packages = nodes.stream().filter(n -> n.getKind() == StrutsDiagramNode.Kind.PACKAGE).count();
        long actions = nodes.stream().filter(n -> n.getKind() == StrutsDiagramNode.Kind.ACTION).count();
        long results = nodes.stream().filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT).count();

        assertEquals("One local package", 1, packages);
        assertEquals("Two local actions", 2, actions);
        assertEquals("Two local results", 2, results);
    }

    public void testBuildReturnsNullForNonStrutsXml() {
        XmlFile plainXml = (XmlFile) myFixture.configureByText("plain.xml", "<root/>");
        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build(plainXml)).executeSynchronously();
        assertNull("Should return null for non-Struts XML", model);
    }

    public void testNavigationPointerResolvesUnderSynchronousReadAction() {
        createStrutsFileSet("struts-local-a.xml");

        VirtualFile vfA = myFixture.findFileInTempDir("struts-local-a.xml");
        assertNotNull(vfA);
        PsiFile psiA = PsiManager.getInstance(getProject()).findFile(vfA);
        assertInstanceOf(psiA, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psiA)).executeSynchronously();
        assertNotNull(model);

        StrutsDiagramNode actionNode = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.ACTION)
                .findFirst().orElse(null);
        assertNotNull("Model should contain at least one ACTION node", actionNode);

        SmartPsiElementPointer<XmlElement> pointer = actionNode.getNavigationPointer();
        assertNotNull("ACTION node should have a navigation pointer", pointer);

        Navigatable navigatable = ApplicationManager.getApplication().runReadAction(
                (com.intellij.openapi.util.Computable<Navigatable>) () -> {
                    XmlElement element = pointer.getElement();
                    return element instanceof Navigatable ? (Navigatable) element : null;
                });
        assertNotNull("Smart pointer should resolve to a Navigatable element", navigatable);
    }

    // --- Fallback state tests ---

    public void testBuildReturnsEmptyModelForStrutsFileWithNoPackages() {
        createStrutsFileSet("struts-empty.xml");

        VirtualFile vf = myFixture.findFileInTempDir("struts-empty.xml");
        assertNotNull(vf);

        PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
        assertInstanceOf(psi, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();
        assertNotNull("Should return a model (not null) for a valid but empty Struts file", model);
        assertTrue("Model should have no nodes", model.getNodes().isEmpty());
        assertTrue("Model should have no edges", model.getEdges().isEmpty());
    }

    public void testComponentStateUnavailableForNullModel() {
        Struts2DiagramComponent component = new Struts2DiagramComponent(null);
        assertEquals(Struts2DiagramComponent.State.UNAVAILABLE, component.getState());
    }

    public void testComponentStateEmptyForEmptyModel() {
        createStrutsFileSet("struts-empty.xml");

        VirtualFile vf = myFixture.findFileInTempDir("struts-empty.xml");
        assertNotNull(vf);
        PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
        assertInstanceOf(psi, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();
        assertNotNull(model);

        Struts2DiagramComponent component = new Struts2DiagramComponent(model);
        assertEquals(Struts2DiagramComponent.State.EMPTY, component.getState());
    }

    public void testComponentStateLoadedForNormalModel() {
        createStrutsFileSet("struts-local-a.xml");

        VirtualFile vf = myFixture.findFileInTempDir("struts-local-a.xml");
        assertNotNull(vf);
        PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
        assertInstanceOf(psi, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();
        assertNotNull(model);

        Struts2DiagramComponent component = new Struts2DiagramComponent(model);
        assertEquals(Struts2DiagramComponent.State.LOADED, component.getState());
    }

    public void testRebuildClearsStaleThenShowsFallback() {
        createStrutsFileSet("struts-local-a.xml");

        VirtualFile vf = myFixture.findFileInTempDir("struts-local-a.xml");
        PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
        StrutsConfigDiagramModel loaded = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();

        Struts2DiagramComponent component = new Struts2DiagramComponent(loaded);
        assertEquals(Struts2DiagramComponent.State.LOADED, component.getState());

        component.rebuild(null);
        assertEquals("rebuild(null) should switch to UNAVAILABLE, not keep stale content",
                Struts2DiagramComponent.State.UNAVAILABLE, component.getState());
    }

    // --- Unresolved result label tests ---

    public void testUnresolvedResultUsesDescriptiveLabel() {
        createStrutsFileSet("struts-unresolved.xml");

        VirtualFile vf = myFixture.findFileInTempDir("struts-unresolved.xml");
        assertNotNull(vf);
        PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
        assertInstanceOf(psi, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();
        assertNotNull(model);

        List<StrutsDiagramNode> results = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                .collect(Collectors.toList());
        assertFalse("Should have result nodes", results.isEmpty());

        for (StrutsDiagramNode result : results) {
            assertFalse("Result node label should not be raw '???': " + result.getName(),
                    "???".equals(result.getName()));
        }
    }

    public void testUnresolvedResultTooltipDoesNotContainRawPlaceholders() {
        createStrutsFileSet("struts-unresolved.xml");

        VirtualFile vf = myFixture.findFileInTempDir("struts-unresolved.xml");
        assertNotNull(vf);
        PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
        assertInstanceOf(psi, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();
        assertNotNull(model);

        List<StrutsDiagramNode> results = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                .collect(Collectors.toList());

        for (StrutsDiagramNode result : results) {
            String tooltip = result.getTooltipHtml();
            if (tooltip != null) {
                assertFalse("Tooltip should not contain raw '???' for path: " + tooltip,
                        tooltip.contains(">???<"));
            }
        }
    }

    // --- Duplicate name and identity tests ---

    public void testDuplicateActionNamesAcrossPackagesProduceDistinctNodes() {
        createStrutsFileSet("struts-duplicate-names.xml");

        VirtualFile vf = myFixture.findFileInTempDir("struts-duplicate-names.xml");
        assertNotNull(vf);
        PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
        assertInstanceOf(psi, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();
        assertNotNull(model);

        List<StrutsDiagramNode> actions = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.ACTION)
                .collect(Collectors.toList());
        assertEquals("Both 'index' actions should be present as separate nodes", 2, actions.size());

        assertEquals("Both share the same display name", actions.get(0).getName(), actions.get(1).getName());
        assertFalse("Must have different IDs", actions.get(0).getId().equals(actions.get(1).getId()));
        assertFalse("Must not be equal() to each other", actions.get(0).equals(actions.get(1)));
    }

    public void testDuplicateResultPathsProduceDistinctNodes() {
        createStrutsFileSet("struts-duplicate-names.xml");

        VirtualFile vf = myFixture.findFileInTempDir("struts-duplicate-names.xml");
        assertNotNull(vf);
        PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
        assertInstanceOf(psi, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();
        assertNotNull(model);

        List<StrutsDiagramNode> results = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                .collect(Collectors.toList());
        assertEquals("Should have 3 result nodes total", 3, results.size());

        long distinctIds = results.stream().map(StrutsDiagramNode::getId).distinct().count();
        assertEquals("All result nodes must have distinct IDs", 3, distinctIds);
    }

    // --- Edge structure tests ---

    public void testEdgesConnectPackagesToActionsAndActionsToResults() {
        createStrutsFileSet("struts-local-a.xml");

        VirtualFile vf = myFixture.findFileInTempDir("struts-local-a.xml");
        assertNotNull(vf);
        PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
        assertInstanceOf(psi, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();
        assertNotNull(model);

        List<StrutsDiagramEdge> edges = model.getEdges();
        assertEquals("struts-local-a.xml has 2 package->action + 2 action->result edges", 4, edges.size());

        long pkgToAction = edges.stream()
                .filter(e -> e.getSource().getKind() == StrutsDiagramNode.Kind.PACKAGE
                        && e.getTarget().getKind() == StrutsDiagramNode.Kind.ACTION)
                .count();
        assertEquals("Two package->action edges", 2, pkgToAction);

        long actionToResult = edges.stream()
                .filter(e -> e.getSource().getKind() == StrutsDiagramNode.Kind.ACTION
                        && e.getTarget().getKind() == StrutsDiagramNode.Kind.RESULT)
                .count();
        assertEquals("Two action->result edges", 2, actionToResult);
    }

    public void testResultEdgeLabelsReflectResultNames() {
        createStrutsFileSet("struts-local-a.xml");

        VirtualFile vf = myFixture.findFileInTempDir("struts-local-a.xml");
        assertNotNull(vf);
        PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
        assertInstanceOf(psi, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();
        assertNotNull(model);

        List<StrutsDiagramEdge> resultEdges = model.getEdges().stream()
                .filter(e -> e.getTarget().getKind() == StrutsDiagramNode.Kind.RESULT)
                .collect(Collectors.toList());
        assertEquals(2, resultEdges.size());

        Set<String> labels = resultEdges.stream()
                .map(StrutsDiagramEdge::getLabel)
                .collect(Collectors.toSet());
        assertTrue("Should have default 'success' label, got: " + labels, labels.contains("success"));
        assertTrue("Should have explicit 'input' label, got: " + labels, labels.contains("input"));
    }

    // --- Chain/redirect result type tests ---

    public void testRedirectActionBodyCreatesActionToActionEdge() {
        createStrutsFileSet("struts-redirect-body.xml");

        VirtualFile vf = myFixture.findFileInTempDir("struts-redirect-body.xml");
        assertNotNull(vf);
        PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
        assertInstanceOf(psi, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();
        assertNotNull(model);

        // Two packages, two actions, one dispatcher result (dashboard's JSP)
        List<StrutsDiagramNode> actions = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.ACTION)
                .collect(Collectors.toList());
        assertEquals("Should have 2 actions (index, dashboard)", 2, actions.size());

        // The redirectAction result should NOT produce a RESULT node (resolved same-file)
        List<StrutsDiagramNode> results = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                .collect(Collectors.toList());
        assertEquals("Only dashboard's JSP result should be a RESULT node", 1, results.size());

        List<StrutsDiagramEdge> actionToActionEdges = model.getEdges().stream()
                .filter(e -> e.getSource().getKind() == StrutsDiagramNode.Kind.ACTION
                        && e.getTarget().getKind() == StrutsDiagramNode.Kind.ACTION)
                .collect(Collectors.toList());
        assertEquals("Should have one action→action edge", 1, actionToActionEdges.size());

        StrutsDiagramEdge redirectEdge = actionToActionEdges.get(0);
        assertEquals("index", redirectEdge.getSource().getName());
        assertEquals("dashboard", redirectEdge.getTarget().getName());
        assertEquals("success", redirectEdge.getLabel());
    }

    public void testRedirectActionParamCreatesActionToActionEdge() {
        createStrutsFileSet("struts-redirect-param.xml");

        VirtualFile vf = myFixture.findFileInTempDir("struts-redirect-param.xml");
        assertNotNull(vf);
        PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
        assertInstanceOf(psi, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();
        assertNotNull(model);

        // One package, two actions (index, upload)
        List<StrutsDiagramNode> actions = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.ACTION)
                .collect(Collectors.toList());
        assertEquals("Should have 2 actions (index, upload)", 2, actions.size());

        // The param-only redirectAction should NOT produce a RESULT node
        List<StrutsDiagramNode> results = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.RESULT)
                .collect(Collectors.toList());
        assertEquals("Only upload's JSP result should be a RESULT node", 1, results.size());

        // action→action edge from index to upload
        List<StrutsDiagramEdge> actionToActionEdges = model.getEdges().stream()
                .filter(e -> e.getSource().getKind() == StrutsDiagramNode.Kind.ACTION
                        && e.getTarget().getKind() == StrutsDiagramNode.Kind.ACTION)
                .collect(Collectors.toList());
        assertEquals("Should have one action→action edge", 1, actionToActionEdges.size());

        StrutsDiagramEdge redirectEdge = actionToActionEdges.get(0);
        assertEquals("index", redirectEdge.getSource().getName());
        assertEquals("upload", redirectEdge.getTarget().getName());
        assertEquals("success", redirectEdge.getLabel());
    }

    public void testEdgesInDuplicateNameFileAreCorrectlyWired() {
        createStrutsFileSet("struts-duplicate-names.xml");

        VirtualFile vf = myFixture.findFileInTempDir("struts-duplicate-names.xml");
        assertNotNull(vf);
        PsiFile psi = PsiManager.getInstance(getProject()).findFile(vf);
        assertInstanceOf(psi, XmlFile.class);

        StrutsConfigDiagramModel model = ReadAction.nonBlocking(
                () -> StrutsConfigDiagramModel.build((XmlFile) psi)).executeSynchronously();
        assertNotNull(model);

        List<StrutsDiagramNode> packages = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.PACKAGE)
                .collect(Collectors.toList());
        assertEquals("Two packages", 2, packages.size());

        for (StrutsDiagramNode pkg : packages) {
            long outgoing = model.getEdges().stream()
                    .filter(e -> e.getSource().equals(pkg))
                    .count();
            assertEquals("Each package should have exactly one outgoing edge to its 'index' action", 1, outgoing);
        }

        List<StrutsDiagramNode> actions = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.ACTION)
                .collect(Collectors.toList());
        StrutsDiagramNode adminAction = actions.get(0);
        StrutsDiagramNode publicAction = actions.get(1);

        long adminResults = model.getEdges().stream()
                .filter(e -> e.getSource().equals(adminAction)
                        && e.getTarget().getKind() == StrutsDiagramNode.Kind.RESULT)
                .count();
        assertEquals("admin/index has 1 result", 1, adminResults);

        long publicResults = model.getEdges().stream()
                .filter(e -> e.getSource().equals(publicAction)
                        && e.getTarget().getKind() == StrutsDiagramNode.Kind.RESULT)
                .count();
        assertEquals("public/index has 2 results", 2, publicResults);
    }
}
