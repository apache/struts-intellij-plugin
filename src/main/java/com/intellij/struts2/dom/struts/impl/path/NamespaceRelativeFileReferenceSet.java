/*
 * Copyright 2011 The authors
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

import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceHelper;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceHelperRegistrar;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import org.jetbrains.annotations.NotNull;

/**
 * {@link FileReferenceSet} that resolves namespace-relative Struts result paths.
 */
final class NamespaceRelativeFileReferenceSet extends FileReferenceSet {

  private final @NotNull String myNamespace;
  private final boolean mySoft;

  NamespaceRelativeFileReferenceSet(@NotNull final PsiElement element,
                                    final boolean soft,
                                    @NotNull final String namespace) {
    super(createPath(element), element, createOffset(element), null, true, true);
    mySoft = soft;
    myNamespace = namespace;
  }

  @Override
  protected boolean isSoft() {
    return mySoft;
  }

  @Override
  protected boolean isUrlEncoded() {
    return true;
  }

  @Override
  public @NotNull String getPathString() {
    return StrutsResultPathUtil.toAbsoluteWebPath(super.getPathString(), myNamespace);
  }

  @NotNull
  private static String createPath(@NotNull final PsiElement element) {
    String text = ElementManipulators.getValueText(element);
    for (final FileReferenceHelper helper : FileReferenceHelperRegistrar.getHelpers()) {
      text = helper.trimUrl(text);
    }
    return text;
  }

  private static int createOffset(@NotNull final PsiElement element) {
    return ElementManipulators.getValueTextRange(element).getStartOffset();
  }
}
