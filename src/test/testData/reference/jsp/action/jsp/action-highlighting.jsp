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

<%-- valid --%>
<s:url action="namespace1Action"/>
<s:a action="namespace1Action"/>
<s:url action="namespace2Action"/>
<s:url action="namespace1Action" namespace="/namespace1"/>
<s:url action="namespace2Action" namespace="/namespace2"/>

<s:url action="myWildCard" namespace="/wildcard"/>
<s:url action="myWildCardAnythingGoesHere" namespace="/wildcard"/>

<%-- "method" --%>
<s:url action="bangAction" namespace="/bang" method="methodB"/>
<s:url action="bangAction" namespace="/bang" method="<error>INVALID_VALUE</error>"/>


<%-- bang-notation --%>
<s:url action="bangAction!methodA" namespace="/bang"/>
<s:url action="bangAction!methodB" namespace="/bang"/>

<s:url action="myWildCard%{anythingDynamic}"/>
<s:url action="%{anythingDynamic}"/>

<s:form action="namespace1Action"/>
<s:form action="<error>INVALID_VALUE</error>"/>
<s:submit action="namespace1Action"/>
<s:submit action="<error>INVALID_VALUE</error>"/>

<%-- invalid --%>
<s:url action="<error></error>"/>
<s:url action="<error>INVALID_VALUE</error>"/>
<s:a action="<error>INVALID_VALUE</error>"/>
<s:url action="<error>namespace2Action</error>" namespace="/namespace1"/>

<s:url action="bangAction!<error>INVALID_VALUE</error>" namespace="/bang"/>
<s:url action="namespace2Action!<error>INVALID_VALUE</error>" namespace="/namespace2"/>
