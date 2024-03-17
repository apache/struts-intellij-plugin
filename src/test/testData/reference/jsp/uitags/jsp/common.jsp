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

<%@ taglib prefix="s" uri="/struts-tags" %>

<%-- common attributes ============== --%>

<%-- "id" duplicates --%>
<s:textfield id="id22"/>
<s:textfield id="<error descr="Duplicate id reference">id1</error>"/>
<s:textfield id="<error descr="Duplicate id reference">id1</error>"/>

<%-- "disabled" --%>
<s:textfield disabled="true"/>
<s:textfield disabled="false"/>
<s:textfield disabled="<error>INVALID_VALUE</error>"/>

<%-- "labelposition" --%>
<s:textfield labelposition="left"/>
<s:textfield labelposition="top"/>
<s:textfield labelposition="<error>INVALID_VALUE</error>"/>

<%-- "requiredposition" --%>
<s:textfield requiredposition="left"/>
<s:textfield requiredposition="right"/>
<s:textfield requiredposition="<error>INVALID_VALUE</error>"/>

<%-- "readonly" --%>
<s:textfield readonly="true"/>
<s:textfield readonly="false"/>
<s:textfield readonly="<error>INVALID_VALUE</error>"/>

<%-- "emptyOption" --%>
<s:doubleselect emptyOption="true"/>
<s:doubleselect emptyOption="false"/>
<s:doubleselect emptyOption="<error>INVALID_VALUE</error>"/>

<%-- "doubleEmptyOption" --%>
<s:doubleselect doubleEmptyOption="true"/>
<s:doubleselect doubleEmptyOption="false"/>
<s:doubleselect doubleEmptyOption="<error>INVALID_VALUE</error>"/>

<%-- "multiple" --%>
<s:doubleselect multiple="true"/>
<s:doubleselect multiple="false"/>
<s:doubleselect multiple="<error>INVALID_VALUE</error>"/>

<%-- "key" --%>
<s:doubleselect key="validKey"/>
<s:doubleselect key="<error descr="Cannot resolve property key">INVALID_VALUE</error>"/>

