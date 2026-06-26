/*
 * Copyright 2026 The authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.struts2.annotators;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.intellij.struts2.StrutsBundle;
import com.intellij.struts2.dom.struts.impl.path.JspResultFileCreator;
import com.intellij.struts2.dom.struts.impl.path.StrutsDispatchResultPathInspection;
import com.intellij.struts2.dom.struts.impl.path.StrutsResultPathUtil;
import com.intellij.struts2.dom.struts.model.StrutsManager;
import com.intellij.struts2.dom.struts.strutspackage.StrutsPackage;
import com.intellij.struts2.facet.ui.StrutsFileSet;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Highlights unresolved {@link FileReference}s in dispatch-type {@code <result>} JSP paths.
 */
public final class StrutsResultPathAnnotator implements Annotator {

  @Override
  public void annotate(@NotNull final PsiElement element, @NotNull final AnnotationHolder holder) {
    if (!(element instanceof XmlText xmlText)) {
      return;
    }

    final XmlTag tag = xmlText.getParentTag();
    if (tag == null) {
      return;
    }

    final String localName = tag.getLocalName();
    if (!"result".equals(localName) && !"global-result".equals(localName)) {
      return;
    }

    final PsiFile containingFile = tag.getContainingFile();
    if (!(containingFile instanceof XmlFile xmlFile)) {
      return;
    }

    final Module module = ModuleUtilCore.findModuleForPsiElement(tag);
    if (module == null || !isInConfiguredFileSet(xmlFile, module)) {
      return;
    }

    final var domElement = DomUtil.getDomElement(tag);
    if (!(domElement instanceof GenericDomValue<?> resultValue)) {
      return;
    }

    final String stringValue = resultValue.getStringValue();
    if (!StrutsDispatchResultPathInspection.isUnresolvedDispatchJspPath(resultValue, stringValue)) {
      return;
    }

    final com.intellij.psi.xml.XmlElement valueElement = DomUtil.getValueElement(resultValue);
    if (valueElement == null) {
      return;
    }

    final PsiReference unresolvedReference = findUnresolvedFileReference(valueElement.getReferences());
    if (unresolvedReference == null) {
      return;
    }

    final StrutsPackage strutsPackage = DomUtil.getParentOfType(resultValue, StrutsPackage.class, true);
    if (strutsPackage == null) {
      return;
    }

    final String namespace = strutsPackage.searchNamespace();
    if (namespace == null) {
      return;
    }

    final String absolutePath = StrutsResultPathUtil.toAbsoluteWebPath(stringValue, namespace);
    final String fileName = StringUtil.substringAfterLast(absolutePath, "/");

    final String message = unresolvedReference instanceof EmptyResolveMessageProvider provider
                           ? provider.getUnresolvedMessagePattern()
                           : "Cannot resolve file '" + fileName + "'";

    holder.newAnnotation(HighlightSeverity.ERROR, message)
      .withFix(new CreateJspResultFileIntention(tag, absolutePath, fileName))
      .create();
  }

  private static PsiReference findUnresolvedFileReference(final PsiReference @NotNull [] references) {
    for (int i = references.length - 1; i >= 0; i--) {
      final PsiReference reference = references[i];
      if (reference.resolve() != null) {
        return null;
      }
      if (reference instanceof FileReference) {
        return reference;
      }
    }
    return null;
  }

  private static boolean isInConfiguredFileSet(@NotNull final XmlFile xmlFile, @NotNull final Module module) {
    final VirtualFile virtualFile = xmlFile.getVirtualFile();
    if (virtualFile == null) {
      return false;
    }

    final Set<StrutsFileSet> fileSets = StrutsManager.getInstance(xmlFile.getProject()).getAllConfigFileSets(module);
    for (final StrutsFileSet fileSet : fileSets) {
      if (fileSet.hasFile(virtualFile)) {
        return true;
      }
    }
    return false;
  }

  private static final class CreateJspResultFileIntention implements IntentionAction {

    private final @NotNull PsiElement myContext;
    private final @NotNull String myAbsoluteWebPath;
    private final @NotNull String myFileName;

    private CreateJspResultFileIntention(@NotNull final PsiElement context,
                                         @NotNull final String absoluteWebPath,
                                         @NotNull final String fileName) {
      myContext = context;
      myAbsoluteWebPath = absoluteWebPath;
      myFileName = fileName;
    }

    @Override
    @NotNull
    public String getText() {
      return StrutsBundle.message("dom.result.path.create.file", myFileName);
    }

    @Override
    @NotNull
    public String getFamilyName() {
      return StrutsBundle.message("dom.result.path.create.file.family");
    }

    @Override
    public boolean isAvailable(@NotNull final Project project, final Editor editor, final PsiFile psiFile) {
      return true;
    }

    @Override
    public void invoke(@NotNull final Project project, final Editor editor, final PsiFile psiFile)
      throws IncorrectOperationException {
      JspResultFileCreator.create(project, myContext, myAbsoluteWebPath, getText(), getFamilyName());
    }

    @Override
    public boolean startInWriteAction() {
      return false;
    }
  }
}
