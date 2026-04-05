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
package com.intellij.struts2.dom.inspection;

import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class StrutsDtdValidatorTest extends BasePlatformTestCase {

    public void testHttpUriDetected() {
        XmlFile file = createStrutsXmlWithDoctype("http://struts.apache.org/dtds/struts-6.0.dtd");
        assertEquals(StrutsDtdValidator.Result.HTTP_INSTEAD_OF_HTTPS, StrutsDtdValidator.validate(file));
    }

    public void testHttpsUriOk() {
        XmlFile file = createStrutsXmlWithDoctype("https://struts.apache.org/dtds/struts-6.0.dtd");
        assertEquals(StrutsDtdValidator.Result.OK, StrutsDtdValidator.validate(file));
    }

    public void testOldHttpUriOk() {
        XmlFile file = createStrutsXmlWithDoctype("http://struts.apache.org/dtds/struts-2.0.dtd");
        assertEquals(StrutsDtdValidator.Result.OK, StrutsDtdValidator.validate(file));
    }

    public void testUnrecognizedUri() {
        XmlFile file = createStrutsXmlWithDoctype("http://example.com/bogus.dtd");
        assertEquals(StrutsDtdValidator.Result.UNRECOGNIZED, StrutsDtdValidator.validate(file));
    }

    public void testNoDoctype() {
        PsiFile psiFile = myFixture.configureByText("struts.xml", "<struts></struts>");
        assertEquals(StrutsDtdValidator.Result.OK, StrutsDtdValidator.validate((XmlFile) psiFile));
    }

    public void testSuggestedUri() {
        assertEquals("https://struts.apache.org/dtds/struts-6.0.dtd",
                StrutsDtdValidator.suggestedUri("http://struts.apache.org/dtds/struts-6.0.dtd"));
    }

    public void testHttp25UriDetected() {
        XmlFile file = createStrutsXmlWithDoctype("http://struts.apache.org/dtds/struts-2.5.dtd");
        assertEquals(StrutsDtdValidator.Result.HTTP_INSTEAD_OF_HTTPS, StrutsDtdValidator.validate(file));
    }

    public void testHttps25UriOk() {
        XmlFile file = createStrutsXmlWithDoctype("https://struts.apache.org/dtds/struts-2.5.dtd");
        assertEquals(StrutsDtdValidator.Result.OK, StrutsDtdValidator.validate(file));
    }

    private XmlFile createStrutsXmlWithDoctype(String systemUri) {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE struts PUBLIC\n" +
                "  \"-//Apache Software Foundation//DTD Struts Configuration 6.0//EN\"\n" +
                "  \"" + systemUri + "\">\n" +
                "<struts></struts>";
        PsiFile psiFile = myFixture.configureByText("struts.xml", content);
        return (XmlFile) psiFile;
    }
}
