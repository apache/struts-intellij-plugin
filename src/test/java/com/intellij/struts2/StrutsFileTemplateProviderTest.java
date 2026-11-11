package com.intellij.struts2;

import junit.framework.TestCase;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class StrutsFileTemplateProviderTest extends TestCase {

  public void testVersionMapping() {
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_6_0_XML,
                 StrutsFileTemplateProvider.templateNameFor("6.4.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_6_0_XML,
                 StrutsFileTemplateProvider.templateNameFor("7.4.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_6_5_XML,
                 StrutsFileTemplateProvider.templateNameFor("6.5.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_6_5_XML,
                 StrutsFileTemplateProvider.templateNameFor("6.6.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_7_5_XML,
                 StrutsFileTemplateProvider.templateNameFor("7.5.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_7_5_XML,
                 StrutsFileTemplateProvider.templateNameFor("7.6.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_7_5_XML,
                 StrutsFileTemplateProvider.templateNameFor("8.0.0"));
    assertEquals(StrutsFileTemplateGroupDescriptorFactory.STRUTS_2_0_XML,
                 StrutsFileTemplateProvider.templateNameFor(null));
  }

  public void testTemplateDoctypes() throws Exception {
    String struts65 = readTemplate("struts_6_5.xml.ft");
    assertTrue(struts65.contains("-//Apache Software Foundation//DTD Struts Configuration 6.5//EN"));
    assertTrue(struts65.contains("https://struts.apache.org/dtds/struts-6.5.dtd"));

    String struts75 = readTemplate("struts_7_5.xml.ft");
    assertTrue(struts75.contains("-//Apache Software Foundation//DTD Struts Configuration 7.5//EN"));
    assertTrue(struts75.contains("https://struts.apache.org/dtds/struts-7.5.dtd"));
  }

  private static String readTemplate(String fileName) throws Exception {
    try (InputStream in = StrutsFileTemplateProvider.class.getClassLoader()
            .getResourceAsStream("fileTemplates/j2ee/" + fileName)) {
      assertNotNull(fileName, in);
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
