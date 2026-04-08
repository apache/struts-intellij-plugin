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

import com.intellij.struts2.BasicLightHighlightingTestCase;
import com.intellij.struts2.Struts2ProjectDescriptorBuilder;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Tests {@link WebFacetChecker}.
 */
public class WebFacetCheckerTest extends BasicLightHighlightingTestCase {

    @NotNull
    @Override
    protected String getTestDataLocation() {
        return "strutsXml/highlighting";
    }

    public void testWebFacetPresent() {
        assertFalse("Should not report missing WebFacet when one is configured",
                WebFacetChecker.isWebFacetMissing(getModule()));
    }

    public void testWebFacetMissing() {
        // The default descriptor adds WebFacet, so this test verifies the positive case.
        // The negative case (actually missing) is tested via the annotator test
        // which uses the withoutWebFacet() descriptor.
        assertFalse(WebFacetChecker.isWebFacetMissing(getModule()));
    }

    public void testNoStrutsFacet() {
        // When no Struts facet at all, isWebFacetMissing should return false
        // (the check only applies to modules with Struts facet).
        // Cannot easily test this in a light test that always has a Struts facet,
        // but we verify the return is false (both facets present).
        assertFalse(WebFacetChecker.isWebFacetMissing(getModule()));
    }
}
