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

import com.intellij.diagram.DiagramProvider;
import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.diagram.extras.EditNodeHandler;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.diagram.model.StrutsConfigDiagramModel;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.struts2.diagram.provider.StrutsDiagramApiNode;
import com.intellij.struts2.diagram.provider.StrutsDiagramExtras;
import com.intellij.struts2.diagram.provider.StrutsDiagramItem;
import com.intellij.struts2.diagram.provider.StrutsDiagramProvider;
import org.jetbrains.annotations.NotNull;

public class StrutsDiagramProviderTest extends BasicLightHighlightingTestCase {

    @Override
    @NotNull
    protected String getTestDataLocation() {
        return "diagram";
    }

    public void testProviderExtensionIsRegistered() {
        DiagramProvider<?> provider = DiagramProvider.findByID(StrutsDiagramProvider.ID);
        assertNotNull("Struts DiagramProvider must be registered via com.intellij.diagram.Provider",
                provider);
        assertInstanceOf(provider, StrutsDiagramProvider.class);
    }

    public void testAcceptsStrutsConfigWithoutFileSet() {
        VirtualFile file = myFixture.copyFileToProject("struts-diagram.xml");
        XmlFile xml = (XmlFile) PsiManager.getInstance(getProject()).findFile(file);
        assertNotNull(xml);
        StrutsDiagramProvider provider = getProvider();
        DataContext ctx = SimpleDataContext.builder()
                .add(CommonDataKeys.PSI_FILE, xml)
                .add(CommonDataKeys.PROJECT, getProject())
                .build();
        StrutsDiagramItem item = provider.getElementManager().findInDataContext(ctx);
        assertNotNull("Show Diagram must accept Struts config outside a file set", item);
        assertEquals(xml, item.getXmlFile());
        assertNull(item.getSnapshotNode());
    }

    public void testRejectsPlainXml() {
        XmlFile xml = (XmlFile) myFixture.configureByText("plain.xml", "<root/>");
        StrutsDiagramProvider provider = getProvider();
        DataContext ctx = SimpleDataContext.builder()
                .add(CommonDataKeys.PSI_FILE, xml)
                .add(CommonDataKeys.PROJECT, getProject())
                .build();
        assertNull(provider.getElementManager().findInDataContext(ctx));
        assertFalse(provider.getElementManager().canBeBuiltFrom(xml));
    }

    public void testVfsResolverRoundTripsSnapshotNodeFqn() {
        createStrutsFileSet("struts-diagram.xml");
        VirtualFile file = myFixture.findFileInTempDir("struts-diagram.xml");
        assertNotNull(file);
        XmlFile xml = (XmlFile) PsiManager.getInstance(getProject()).findFile(file);
        assertNotNull(xml);

        StrutsConfigDiagramModel model = ReadAction.compute(() -> StrutsConfigDiagramModel.build(xml));
        assertNotNull(model);
        StrutsDiagramNode node = model.getNodes().get(0);
        DiagramVfsResolver<StrutsDiagramItem> resolver = getProvider().getVfsResolver();
        String fqn = resolver.getQualifiedName(StrutsDiagramItem.forNode(xml, node));
        assertNotNull(fqn);

        StrutsDiagramItem resolved = resolver.resolveElementByFQN(fqn, getProject());

        assertNotNull(resolved);
        assertNotNull(resolved.getSnapshotNode());
        assertEquals(node.getId(), resolved.getSnapshotNode().getId());
    }

    public void testExtrasProvideEditNodeHandlerForEditorDoubleClick() {
        StrutsDiagramProvider provider = getProvider();
        EditNodeHandler<StrutsDiagramItem> handler = provider.getExtras().getEditNodeHandler();
        assertNotNull(
                "Editor-mode double-click uses EditNodeHandler, not DiagramNode.navigate()",
                handler);
    }

    public void testResolvePsiElementFromSnapshotNode() {
        createStrutsFileSet("struts-diagram.xml");
        VirtualFile file = myFixture.findFileInTempDir("struts-diagram.xml");
        assertNotNull(file);
        XmlFile xml = (XmlFile) PsiManager.getInstance(getProject()).findFile(file);
        assertNotNull(xml);

        StrutsConfigDiagramModel model = ReadAction.compute(() -> StrutsConfigDiagramModel.build(xml));
        assertNotNull(model);
        StrutsDiagramNode snapshotNode = model.getNodes().stream()
                .filter(n -> n.getKind() == StrutsDiagramNode.Kind.ACTION)
                .findFirst()
                .orElseThrow();
        StrutsDiagramItem item = StrutsDiagramItem.forNode(xml, snapshotNode);

        PsiElement psi = StrutsDiagramExtras.resolvePsiElement(item);
        assertNotNull(psi);
        assertTrue(psi.isValid());
        assertEquals(xml, psi.getContainingFile());

        StrutsDiagramApiNode apiNode = new StrutsDiagramApiNode(getProvider(), item);
        assertTrue(apiNode.canNavigate());
    }

    private StrutsDiagramProvider getProvider() {
        DiagramProvider<?> provider = DiagramProvider.findByID(StrutsDiagramProvider.ID);
        assertNotNull(provider);
        assertInstanceOf(provider, StrutsDiagramProvider.class);
        return (StrutsDiagramProvider) provider;
    }
}
