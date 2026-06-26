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

package com.intellij.struts2.dom.struts.impl.path;

import com.intellij.javaee.web.WebRoot;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.jsp.highlighter.NewJspFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.util.OpenSourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates a JSP file under a web facet root for a Struts result path.
 */
public final class JspResultFileCreator {

  private static final String JSP_TEMPLATE = "<%@ page contentType=\"text/html;charset=UTF-8\" %>\n";

  private JspResultFileCreator() {
  }

  public static void create(@NotNull final Project project,
                            @NotNull final PsiElement context,
                            @NotNull final String absoluteWebPath,
                            @NotNull final String commandName,
                            @NotNull final String commandGroup) {
    final String fileName = StringUtil.substringAfterLast(absoluteWebPath, "/");
    if (StringUtil.isEmpty(fileName)) {
      return;
    }

    final WebFacet webFacet = WebUtil.getWebFacet(context);
    if (webFacet == null) {
      return;
    }

    WriteCommandAction.runWriteCommandAction(project, commandName, commandGroup, () -> {
      final PsiDirectory parentDirectory = findOrCreateParentDirectory(project, webFacet, absoluteWebPath);
      if (parentDirectory == null) {
        return;
      }

      final PsiFile jspFile = PsiFileFactory.getInstance(project)
        .createFileFromText(fileName, NewJspFileType.INSTANCE, JSP_TEMPLATE);
      final PsiElement created = parentDirectory.add(jspFile);
      if (created instanceof Navigatable navigatable) {
        OpenSourceUtil.navigate(navigatable);
      }
    });
  }

  @Nullable
  private static PsiDirectory findOrCreateParentDirectory(@NotNull final Project project,
                                                          @NotNull final WebFacet webFacet,
                                                          @NotNull final String absoluteWebPath) {
    final String relativePath = absoluteWebPath.startsWith("/")
                                ? absoluteWebPath.substring(1)
                                : absoluteWebPath;
    final int lastSlash = relativePath.lastIndexOf('/');
    final String parentPath = lastSlash >= 0 ? relativePath.substring(0, lastSlash) : "";

    for (final WebRoot webRoot : webFacet.getWebRoots()) {
      final VirtualFile rootFile = webRoot.getFile();
      if (rootFile == null) {
        continue;
      }

      final PsiDirectory rootDir = PsiManager.getInstance(project).findDirectory(rootFile);
      if (rootDir == null) {
        continue;
      }

      if (!isPathInWebRoot(absoluteWebPath, webRoot.getRelativePath())) {
        continue;
      }

      PsiDirectory current = rootDir;
      if (StringUtil.isNotEmpty(parentPath)) {
        for (final String segment : parentPath.split("/")) {
          if (segment.isEmpty()) {
            continue;
          }
          PsiDirectory subdirectory = current.findSubdirectory(segment);
          if (subdirectory == null) {
            subdirectory = current.createSubdirectory(segment);
          }
          current = subdirectory;
        }
      }
      return current;
    }
    return null;
  }

  private static boolean isPathInWebRoot(@NotNull final String absoluteWebPath,
                                         @NotNull final String webRootRelativePath) {
    if ("/".equals(webRootRelativePath)) {
      return true;
    }
    final String prefix = webRootRelativePath.endsWith("/") ? webRootRelativePath : webRootRelativePath + "/";
    return absoluteWebPath.startsWith(prefix) || absoluteWebPath.equals(webRootRelativePath);
  }
}
