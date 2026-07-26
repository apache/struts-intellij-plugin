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

import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramProvider;
import com.intellij.diagram.DiagramVfsResolver;
import com.intellij.diagram.components.DiagramNodeContainer;
import com.intellij.diagram.extras.DiagramExtras;
import com.intellij.diagram.extras.EditNodeHandler;
import com.intellij.diagram.extras.custom.CommonDiagramExtras;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.graph.layout.CanonicMultiStageLayouter;
import com.intellij.openapi.graph.layout.Layouter;
import com.intellij.openapi.graph.layout.LayoutOrientation;
import com.intellij.openapi.graph.layout.hierarchic.HierarchicGroupLayouter;
import com.intellij.openapi.graph.settings.GraphSettings;
import com.intellij.openapi.graph.view.Graph2D;
import com.intellij.openapi.graph.view.NodeRealizer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.diagram.model.StrutsConfigDiagramModel;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.struts2.diagram.provider.StrutsDiagramApiNode;
import com.intellij.struts2.diagram.provider.StrutsDiagramDataModel;
import com.intellij.struts2.diagram.provider.StrutsDiagramExtras;
import com.intellij.struts2.diagram.provider.StrutsDiagramItem;
import com.intellij.struts2.diagram.provider.StrutsDiagramProvider;
import com.intellij.ui.SimpleColoredComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.lang.reflect.Proxy;

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

    public void testExtrasCreateCompactLabelNodeComponents() {
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
        assertNotNull(snapshotNode.getIcon());
        assertFalse(snapshotNode.getName().isEmpty());

        StrutsDiagramProvider provider = getProvider();
        DiagramExtras<StrutsDiagramItem> extras = provider.getExtras();
        assertInstanceOf(extras, CommonDiagramExtras.class);

        StrutsDiagramItem item = StrutsDiagramItem.forNode(xml, snapshotNode);
        StrutsDiagramApiNode apiNode = new StrutsDiagramApiNode(provider, item);

        StrutsDiagramDataModel dataModel =
                new StrutsDiagramDataModel(getProject(), provider, StrutsDiagramItem.forFile(xml));
        try {
            DiagramBuilder builder = stubBuilderForLabelNodes(provider, dataModel);
            JPanel wrapper = new JPanel();
            NodeRealizer nodeRealizer = (NodeRealizer) Proxy.newProxyInstance(
                    NodeRealizer.class.getClassLoader(),
                    new Class<?>[]{NodeRealizer.class},
                    (proxy, method, args) -> proxyDefaultValue(method.getReturnType()));
            JComponent component = extras.createNodeComponent(apiNode, builder, nodeRealizer, wrapper);

            assertNotNull(component);
            assertFalse(
                    "Show Diagram nodes must not use UML DiagramNodeContainer chrome",
                    component instanceof DiagramNodeContainer);
            assertInstanceOf(component, SimpleColoredComponent.class);

            SimpleColoredComponent label = (SimpleColoredComponent) component;
            assertNotNull("Package/action/result icon must remain visible", label.getIcon());
            assertTrue(
                    "Node name must remain visible on the label",
                    label.getCharSequence(true).toString().contains(snapshotNode.getName()));
        } finally {
            com.intellij.openapi.util.Disposer.dispose(dataModel);
        }
    }

    public void testExtrasKeepZoomAnimationsDisabled() {
        assertFalse(
                "Preserve the pre-#120 DiagramExtras zoom-animation default",
                getProvider().getExtras().isZoomAnimationsEnabled());
    }

    public void testCustomLayouterIsHierarchicLeftToRight() {
        StrutsDiagramProvider provider = getProvider();
        DiagramExtras<StrutsDiagramItem> extras = provider.getExtras();
        GraphSettings settings = new GraphSettings();

        Layouter layouter = extras.getCustomLayouter(settings, getProject());
        assertNotNull("Show Diagram must provide a custom layouter for LTR hierarchy", layouter);
        assertInstanceOf(layouter, HierarchicGroupLayouter.class);

        CanonicMultiStageLayouter multiStage = (CanonicMultiStageLayouter) layouter;
        assertEquals("Custom layouter must be left-to-right (Maven/Gradle pattern)",
                LayoutOrientation.LEFT_TO_RIGHT,
                multiStage.getLayoutOrientation());

        // Stable custom path — soft preference is "don't reset GraphSettings", not reading orientation
        Layouter again = extras.getCustomLayouter(settings, getProject());
        assertNotNull(again);
        assertEquals(LayoutOrientation.LEFT_TO_RIGHT,
                ((CanonicMultiStageLayouter) again).getLayoutOrientation());
    }

    /**
     * Minimal DiagramBuilder for createLabelNode / setNodeBorders.
     * No Mockito in this project — use JDK proxies.
     */
    private DiagramBuilder stubBuilderForLabelNodes(@NotNull StrutsDiagramProvider provider,
                                                    @NotNull DiagramDataModel<StrutsDiagramItem> dataModel) {
        var scheme = EditorColorsManager.getInstance().getGlobalScheme();
        Graph2D graph = (Graph2D) Proxy.newProxyInstance(
                Graph2D.class.getClassLoader(),
                new Class<?>[]{Graph2D.class},
                (proxy, method, args) -> {
                    if ("isSelected".equals(method.getName())) {
                        return false;
                    }
                    return proxyDefaultValue(method.getReturnType());
                });
        return (DiagramBuilder) Proxy.newProxyInstance(
                DiagramBuilder.class.getClassLoader(),
                new Class<?>[]{DiagramBuilder.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getColorScheme" -> scheme;
                    case "getProvider" -> provider;
                    case "getDataModel" -> dataModel;
                    case "getGraph" -> graph;
                    case "getNode" -> null;
                    case "toString" -> "StubDiagramBuilder";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> proxyDefaultValue(method.getReturnType());
                });
    }

    private static @Nullable Object proxyDefaultValue(@NotNull Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) return false;
        if (returnType == byte.class) return (byte) 0;
        if (returnType == short.class) return (short) 0;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        if (returnType == float.class) return 0f;
        if (returnType == double.class) return 0d;
        if (returnType == char.class) return '\0';
        return null;
    }

    private StrutsDiagramProvider getProvider() {
        DiagramProvider<?> provider = DiagramProvider.findByID(StrutsDiagramProvider.ID);
        assertNotNull(provider);
        assertInstanceOf(provider, StrutsDiagramProvider.class);
        return (StrutsDiagramProvider) provider;
    }
}
