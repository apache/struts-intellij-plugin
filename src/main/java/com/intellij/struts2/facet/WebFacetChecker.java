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
package com.intellij.struts2.facet;

import com.intellij.openapi.module.Module;
import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Checks whether a module with a Struts facet has the required {@link WebFacet}
 * for JSP/dispatch-style result path resolution.
 * <p>
 * Shared between the file-level annotator and the framework initialization
 * notification so the condition is defined in one place.
 */
public final class WebFacetChecker {

    private WebFacetChecker() {}

    /**
     * Returns {@code true} when the given module has a Struts facet but lacks
     * a usable {@link WebFacet}, meaning dispatch-style result paths
     * (e.g. {@code /index.jsp}) cannot be resolved.
     */
    public static boolean isWebFacetMissing(@NotNull Module module) {
        StrutsFacet strutsFacet = StrutsFacet.getInstance(module);
        if (strutsFacet == null) return false;
        return strutsFacet.getWebFacet() == null;
    }

    /**
     * Variant that also checks via {@link WebUtil#getWebFacet(PsiElement)},
     * which searches the module and its dependants.
     */
    public static boolean isWebFacetMissing(@NotNull PsiElement element) {
        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) return false;
        StrutsFacet strutsFacet = StrutsFacet.getInstance(module);
        if (strutsFacet == null) return false;
        return WebUtil.getWebFacet(element) == null;
    }
}
