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
package com.intellij.struts2.annotators;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.Struts2ProjectDescriptorBuilder;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Tests {@link StrutsWebFacetCheckingAnnotator}.
 */
public class StrutsWebFacetCheckingAnnotatorTest extends BasicLightHighlightingTestCase {

    private static final LightProjectDescriptor NO_WEB_FACET =
            new Struts2ProjectDescriptorBuilder()
                    .withStrutsLibrary()
                    .withStrutsFacet()
                    .withoutWebFacet()
                    .build();

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return NO_WEB_FACET;
    }

    @Override
    @NotNull
    protected String getTestDataLocation() {
        return "strutsXml/highlighting";
    }

    public void testWebFacetMissingWarning() {
        createStrutsFileSet("struts-simple.xml");
        IntentionAction intention = myFixture.getAvailableIntention(
                "Configure Web facet in module settings", "struts-simple.xml");
        assertNotNull("WebFacet warning quick-fix should be available",
                intention);
    }
}
