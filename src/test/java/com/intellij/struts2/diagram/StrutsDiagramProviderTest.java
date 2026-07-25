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
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.BasicLightHighlightingTestCase;
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

    private StrutsDiagramProvider getProvider() {
        DiagramProvider<?> provider = DiagramProvider.findByID(StrutsDiagramProvider.ID);
        assertNotNull(provider);
        assertInstanceOf(provider, StrutsDiagramProvider.class);
        return (StrutsDiagramProvider) provider;
    }
}
