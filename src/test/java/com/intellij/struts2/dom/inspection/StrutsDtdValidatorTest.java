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

import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * Unit tests for {@link StrutsDtdValidator} proving that the three DTD
 * validation outcomes (OK, HTTP_INSTEAD_OF_HTTPS, UNRECOGNIZED) are
 * correctly classified.
 */
public class StrutsDtdValidatorTest extends BasePlatformTestCase {

    public void testValidHttpsDtdReturnsOk() {
        XmlFile file = (XmlFile) myFixture.configureByText("struts.xml",
                """
                <?xml version="1.0" encoding="UTF-8" ?>
                <!DOCTYPE struts PUBLIC
                  "-//Apache Software Foundation//DTD Struts Configuration 6.0//EN"
                  "https://struts.apache.org/dtds/struts-6.0.dtd">
                <struts/>
                """);
        assertEquals(StrutsDtdValidator.Result.OK, StrutsDtdValidator.validate(file));
    }

    public void testValidOldHttpDtdReturnsOk() {
        XmlFile file = (XmlFile) myFixture.configureByText("struts.xml",
                """
                <?xml version="1.0" encoding="UTF-8" ?>
                <!DOCTYPE struts PUBLIC
                  "-//Apache Software Foundation//DTD Struts Configuration 2.0//EN"
                  "http://struts.apache.org/dtds/struts-2.0.dtd">
                <struts/>
                """);
        assertEquals(StrutsDtdValidator.Result.OK, StrutsDtdValidator.validate(file));
    }

    public void testHttpInsteadOfHttpsForNewDtdReturnsWarning() {
        XmlFile file = (XmlFile) myFixture.configureByText("struts.xml",
                """
                <?xml version="1.0" encoding="UTF-8" ?>
                <!DOCTYPE struts PUBLIC
                  "-//Apache Software Foundation//DTD Struts Configuration 6.0//EN"
                  "http://struts.apache.org/dtds/struts-6.0.dtd">
                <struts/>
                """);
        assertEquals(StrutsDtdValidator.Result.HTTP_INSTEAD_OF_HTTPS, StrutsDtdValidator.validate(file));
    }

    public void testHttpInsteadOfHttpsForStrutsLikeDtdReturnsWarning() {
        XmlFile file = (XmlFile) myFixture.configureByText("struts.xml",
                """
                <?xml version="1.0" encoding="UTF-8" ?>
                <!DOCTYPE struts PUBLIC
                  "-//Apache Software Foundation//DTD Struts Configuration 2.5//EN"
                  "http://struts.apache.org/dtds/struts-2.5.dtd">
                <struts/>
                """);
        assertEquals(StrutsDtdValidator.Result.HTTP_INSTEAD_OF_HTTPS, StrutsDtdValidator.validate(file));
    }

    public void testUnrecognizedDtdReturnsUnrecognized() {
        XmlFile file = (XmlFile) myFixture.configureByText("struts.xml",
                """
                <?xml version="1.0" encoding="UTF-8" ?>
                <!DOCTYPE struts PUBLIC
                  "-//Unknown//DTD Something//EN"
                  "https://example.com/bogus-struts.dtd">
                <struts/>
                """);
        assertEquals(StrutsDtdValidator.Result.UNRECOGNIZED, StrutsDtdValidator.validate(file));
    }

    public void testNoDoctypeReturnsOk() {
        XmlFile file = (XmlFile) myFixture.configureByText("struts.xml",
                """
                <?xml version="1.0" encoding="UTF-8" ?>
                <struts/>
                """);
        assertEquals(StrutsDtdValidator.Result.OK, StrutsDtdValidator.validate(file));
    }

    public void testSuggestedUriReplacesHttpWithHttps() {
        assertEquals("https://struts.apache.org/dtds/struts-6.0.dtd",
                StrutsDtdValidator.suggestedUri("http://struts.apache.org/dtds/struts-6.0.dtd"));
    }

    public void testExtractSystemIdReturnsNullForNoDoctype() {
        XmlFile file = (XmlFile) myFixture.configureByText("struts.xml", "<struts/>");
        assertNull(StrutsDtdValidator.extractSystemId(file));
    }

    public void testExtractSystemIdReturnsDtdUri() {
        XmlFile file = (XmlFile) myFixture.configureByText("struts.xml",
                """
                <?xml version="1.0" encoding="UTF-8" ?>
                <!DOCTYPE struts PUBLIC
                  "-//Apache Software Foundation//DTD Struts Configuration 6.0//EN"
                  "https://struts.apache.org/dtds/struts-6.0.dtd">
                <struts/>
                """);
        assertEquals("https://struts.apache.org/dtds/struts-6.0.dtd",
                StrutsDtdValidator.extractSystemId(file));
    }
}
