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

<%-- generic attribute --%>
<<info descr="null">s</info>:url action="<inject descr="null">%{1 + 2}</inject>"/>

<%-- no prefix necessary --%>
<<info descr="null">s</info>:iterator value="<inject descr="null">1 + 2</inject>"/>

<%-- list expression --%>
<<info descr="null">s</info>:select label="label" name="name" list="<inject descr="null">{1, 2, 3}</inject>" />

<%-- map expression --%>
<<info descr="null">s</info>:select label="label" name="name" list="<inject descr="null">#{'foo':'foovalue', 'bar':'barvalue'}</inject>" />

