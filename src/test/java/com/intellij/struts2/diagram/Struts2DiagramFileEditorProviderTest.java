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

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.diagram.fileEditor.Struts2DiagramFileEditor;
import com.intellij.struts2.diagram.fileEditor.Struts2DiagramFileEditorProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Tests for {@link Struts2DiagramFileEditorProvider} covering both acceptance
 * gating and basic editor lifecycle (creation, name, reset).
 */
public class Struts2DiagramFileEditorProviderTest extends BasicLightHighlightingTestCase {

    private final Struts2DiagramFileEditorProvider myProvider = new Struts2DiagramFileEditorProvider();

    @Override
    @NotNull
    protected String getTestDataLocation() {
        return "diagram";
    }

    public void testAcceptsStrutsConfigInFileSet() {
        createStrutsFileSet("struts-diagram.xml");
        VirtualFile file = myFixture.copyFileToProject("struts-diagram.xml");
        assertTrue("Diagram provider should accept struts.xml registered in file set",
                myProvider.accept(getProject(), file));
    }

    public void testRejectsStrutsConfigNotInFileSet() {
        VirtualFile file = myFixture.copyFileToProject("struts-diagram.xml");
        assertFalse("Diagram provider should reject struts.xml not in any file set",
                myProvider.accept(getProject(), file));
    }

    public void testRejectsPlainXml() {
        VirtualFile file = myFixture.configureByText("plain.xml", "<root/>").getVirtualFile();
        assertFalse("Diagram provider should reject non-Struts XML",
                myProvider.accept(getProject(), file));
    }

    public void testRejectsJavaFile() {
        VirtualFile file = myFixture.configureByText("Foo.java", "class Foo {}").getVirtualFile();
        assertFalse("Diagram provider should reject non-XML files",
                myProvider.accept(getProject(), file));
    }

    // --- Editor lifecycle tests ---

    public void testEditorCreationProducesValidDiagramEditor() {
        createStrutsFileSet("struts-diagram.xml");
        VirtualFile file = myFixture.findFileInTempDir("struts-diagram.xml");
        assertNotNull(file);

        FileEditor editor = myProvider.createEditor(getProject(), file);
        try {
            assertInstanceOf(editor, Struts2DiagramFileEditor.class);
            assertEquals("Diagram", editor.getName());
        } finally {
            Disposer.dispose(editor);
        }
    }

    public void testResetDoesNotThrow() {
        createStrutsFileSet("struts-diagram.xml");
        VirtualFile file = myFixture.findFileInTempDir("struts-diagram.xml");
        assertNotNull(file);

        Struts2DiagramFileEditor editor =
                (Struts2DiagramFileEditor) myProvider.createEditor(getProject(), file);
        try {
            editor.reset();
        } finally {
            Disposer.dispose(editor);
        }
    }
}
