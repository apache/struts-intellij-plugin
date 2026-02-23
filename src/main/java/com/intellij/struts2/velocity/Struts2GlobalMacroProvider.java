/*
 * Copyright 2013 The authors
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
package com.intellij.struts2.velocity;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.velocity.VtlGlobalMacroProvider;
import com.intellij.velocity.psi.VtlMacro;
import com.intellij.velocity.psi.files.VtlFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Expose macros defined in {@code struts.vm}.
 *
 * @author Yann C&eacute;bron
 */
final class Struts2GlobalMacroProvider extends VtlGlobalMacroProvider {
    @NonNls
    private static final String STRUTS_MACROS_FILENAME = "struts.vm";

    @NotNull
    @Override
    public Collection<VtlMacro> getGlobalMacros(@NotNull final VtlFile vtlFile) {
        final Module module = ModuleUtilCore.findModuleForPsiElement(vtlFile);
        if (module == null) {
            return Collections.emptySet();
        }

        final Collection<VirtualFile> virtualFiles = FilenameIndex.getVirtualFilesByName(
                STRUTS_MACROS_FILENAME,
                GlobalSearchScope.moduleRuntimeScope(module, false));
        if (virtualFiles.size() == 1) {
            final VirtualFile virtualFile = virtualFiles.iterator().next();
            final PsiFile psiFile = PsiManager.getInstance(vtlFile.getProject()).findFile(virtualFile);
            if (psiFile instanceof VtlFile) {
                return ((VtlFile) psiFile).getDefinedMacros();
            }
        }

        return Collections.emptySet();
    }
}