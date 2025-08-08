/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.intellij.lang.ognl.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.JavaLookupElementBuilder;
import com.intellij.lang.ognl.psi.OgnlFqnTypeExpression;
import com.intellij.openapi.project.DumbAware;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * Provides Java class name completion for OGNL FQN type expressions.
 * This enables completion of Java class names in contexts like "new Str&lt;caret&gt;" or "#@ &lt;caret&gt;".
 */
public class OgnlJavaClassCompletionContributor extends CompletionContributor implements DumbAware {

  private static final PsiElementPattern.Capture<PsiElement> FQN_TYPE_EXPRESSION =
    psiElement().inside(OgnlFqnTypeExpression.class);

  public OgnlJavaClassCompletionContributor() {
    extend(CompletionType.BASIC,
           FQN_TYPE_EXPRESSION,
           new CompletionProvider<>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           @NotNull ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               // Use IntelliJ's standard class name completion
               addJavaClassNameCompletions(parameters, result);
             }
           });

    // Also provide completion for CLASS_NAME completion type (triggered by Ctrl+Space)
    extend(CompletionType.CLASS_NAME,
           FQN_TYPE_EXPRESSION,
           new CompletionProvider<>() {
             @Override
             protected void addCompletions(@NotNull CompletionParameters parameters,
                                           @NotNull ProcessingContext context,
                                           @NotNull CompletionResultSet result) {
               addJavaClassNameCompletions(parameters, result);
             }
           });
  }

  private static void addJavaClassNameCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull CompletionResultSet result) {
    final PsiElement position = parameters.getPosition();
    final PsiShortNamesCache cache = PsiShortNamesCache.getInstance(position.getProject());
    final GlobalSearchScope scope = GlobalSearchScope.allScope(position.getProject());
    
    cache.processAllClassNames((className) -> {
      if (result.getPrefixMatcher().prefixMatches(className)) {
        final PsiClass[] classes = cache.getClassesByName(className, scope);
        for (PsiClass psiClass : classes) {
          if (psiClass != null && psiClass.getQualifiedName() != null) {
            result.addElement(JavaLookupElementBuilder.forClass(psiClass));
          }
        }
      }
      return true;
    }, scope, null);
  }
}