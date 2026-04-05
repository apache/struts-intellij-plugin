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

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.diagram.model.StrutsConfigDiagramModel;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tests that {@link StrutsConfigDiagramModel} builds a file-local snapshot,
 * not a merged view of the entire file set.
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
}
