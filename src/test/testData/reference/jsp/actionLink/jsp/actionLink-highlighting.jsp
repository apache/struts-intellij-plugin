<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<%-- "normal" links --%>
<a href="actionLink-highlighting.jsp"/>
<a href="/"/>
<a href="/index.jsp"></a>

<%-- VALID Action Links --%>
<a href="rootActionLink.action"/>
<a href="/rootActionLink.action"/> 
<a href="/actionLink/actionLink1.action"/>
<a href="/actionLink/actionLink2.action"/>

<%-- INVALID Action Links --%>
<a href="/actionLink/<warning descr="Cannot resolve file 'INVALID_VALUE.action'">INVALID_VALUE.action</warning>"/>
<a href="<warning descr="Cannot resolve file 'INVALID_VALUE.action'">INVALID_VALUE.action</warning>"/>
<a href="/<warning descr="Cannot resolve file 'INVALID_VALUE.action'">INVALID_VALUE.action</warning>"/>


<%-- Action Links with dynamic context --%>
<a href="<%=request.getContextPath()%>/actionLink/actionLink1.action"/>
<a href="${pageContext.request.contextPath}/actionLink/actionLink2.action"/>
