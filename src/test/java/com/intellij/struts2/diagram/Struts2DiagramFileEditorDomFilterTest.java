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
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.diagram.fileEditor.Struts2DiagramFileEditor;
import com.intellij.struts2.dom.struts.StrutsRoot;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NotNull;

public class Struts2DiagramFileEditorDomFilterTest extends BasicLightHighlightingTestCase {

    @Override
    @NotNull
    protected String getTestDataLocation() {
        return "diagram";
    }

    public void testIsDomElementInFileMatchesSameVirtualFile() {
        createStrutsFileSet("struts-local-a.xml", "struts-local-b.xml");
        VirtualFile vfA = myFixture.findFileInTempDir("struts-local-a.xml");
        VirtualFile vfB = myFixture.findFileInTempDir("struts-local-b.xml");
        assertNotNull(vfA);
        assertNotNull(vfB);

        ReadAction.run(() -> {
            XmlFile fileA = (XmlFile) PsiManager.getInstance(getProject()).findFile(vfA);
            assertNotNull(fileA);
            DomFileElement<StrutsRoot> root =
                    DomManager.getDomManager(getProject()).getFileElement(fileA, StrutsRoot.class);
            assertNotNull(root);
            DomElement pkg = root.getRootElement().getPackages().getFirst();
            assertTrue(Struts2DiagramFileEditor.isDomElementInFile(pkg, vfA));
            assertFalse(Struts2DiagramFileEditor.isDomElementInFile(pkg, vfB));
        });
    }

    public void testIsDomElementInFileRejectsNullElement() {
        createStrutsFileSet("struts-local-a.xml");
        VirtualFile vfA = myFixture.findFileInTempDir("struts-local-a.xml");
        assertNotNull(vfA);
        assertFalse(Struts2DiagramFileEditor.isDomElementInFile(null, vfA));
    }
}
