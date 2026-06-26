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

import com.intellij.struts2.dom.struts.strutspackage.StrutsPackage;
import junit.framework.TestCase;

public class StrutsResultPathUtilTest extends TestCase {

  public void testRootNamespacePrependsSlash() {
    assertEquals("/WEB-INF/upload.jsp",
                 StrutsResultPathUtil.toAbsoluteWebPath("WEB-INF/upload.jsp", StrutsPackage.DEFAULT_NAMESPACE));
  }

  public void testAbsolutePathUnchanged() {
    assertEquals("/WEB-INF/upload.jsp",
                 StrutsResultPathUtil.toAbsoluteWebPath("/WEB-INF/upload.jsp", StrutsPackage.DEFAULT_NAMESPACE));
  }

  public void testNonRootNamespacePrependsNamespace() {
    assertEquals("/admin/list.jsp",
                 StrutsResultPathUtil.toAbsoluteWebPath("list.jsp", "/admin"));
  }

  public void testNonRootNamespaceWithTrailingSlash() {
    assertEquals("/admin/list.jsp",
                 StrutsResultPathUtil.toAbsoluteWebPath("list.jsp", "/admin/"));
  }

  public void testOgnlExpressionUnchanged() {
    assertEquals("${someProperty}",
                 StrutsResultPathUtil.toAbsoluteWebPath("${someProperty}", StrutsPackage.DEFAULT_NAMESPACE));
  }

  public void testUrlUnchanged() {
    assertEquals("http://example.com/page",
                 StrutsResultPathUtil.toAbsoluteWebPath("http://example.com/page", StrutsPackage.DEFAULT_NAMESPACE));
  }
}
