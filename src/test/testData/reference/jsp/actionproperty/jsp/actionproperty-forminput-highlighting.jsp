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

<%-- all form input elements --%>
<s:form action="myAction">

  <s:checkbox name="myField"/>
  <s:checkbox name="<error>INVALID_VALUE</error>"/>

  <s:checkboxlist name="myField"/>
  <s:checkboxlist name="<error>INVALID_VALUE</error>"/>

  <s:combobox name="myField"/>
  <s:combobox name="<error>INVALID_VALUE</error>"/>

  <s:doubleselect name="myField"/>
  <s:doubleselect name="<error>INVALID_VALUE</error>"/>
  <s:doubleselect doubleName="myField"/>
  <s:doubleselect doubleName="<error>INVALID_VALUE</error>"/>
  <s:doubleselect list="myField"/>
  <s:doubleselect list="readonlyList"/>
  <s:doubleselect list="<error>INVALID_VALUE</error>"/>
  <s:doubleselect doubleList="myField"/>
  <s:doubleselect doubleList="<error>INVALID_VALUE</error>"/>

  <s:file name="myField"/>
  <s:file name="<error>INVALID_VALUE</error>"/>

  <s:hidden name="myField"/>
  <s:hidden name="<error>INVALID_VALUE</error>"/>

  <s:inputtransferselect name="myField"/>
  <s:inputtransferselect name="<error>INVALID_VALUE</error>"/>
  <s:inputtransferselect list="myField"/>
  <s:inputtransferselect list="readonlyList"/>
  <s:inputtransferselect list="<error>INVALID_VALUE</error>"/>

  <s:optiontransferselect name="myField"/>
  <s:optiontransferselect name="<error>INVALID_VALUE</error>"/>
  <s:optiontransferselect doubleName="myField"/>
  <s:optiontransferselect doubleName="<error>INVALID_VALUE</error>"/>
  <s:optiontransferselect list="myField"/>
  <s:optiontransferselect list="readonlyList"/>
  <s:optiontransferselect list="<error>INVALID_VALUE</error>"/>
  <s:optiontransferselect doubleList="myField"/>
  <s:optiontransferselect doubleList="readonlyList"/>
  <s:optiontransferselect doubleList="<error>INVALID_VALUE</error>"/>

  <s:password name="myField"/>
  <s:password name="<error>INVALID_VALUE</error>"/>

  <s:radio name="myField"/>
  <s:radio name="<error>INVALID_VALUE</error>"/>

  <s:select name="myField"/>
  <s:select name="<error>INVALID_VALUE</error>"/>
  <s:select list="myField"/>
  <s:select list="readonlyList"/>
  <s:select list="{ 'opt1', 'opt2'}"/> <%-- no resolve in OGNL expr --%>
  <s:select list="<error>INVALID_VALUE</error>"/>

  <s:textarea name="myField"/>
  <s:textarea name="<error>INVALID_VALUE</error>"/>

  <s:textfield name="myField"/>
  <s:textfield name="<error>INVALID_VALUE</error>"/>
  <s:textfield name="user.foreName"/>
  <s:textfield name="user.<error>INVALID_VALUE</error>"/>

  <s:updownselect name="myField"/>
  <s:updownselect name="<error>INVALID_VALUE</error>"/>
  <s:updownselect list="myField"/>
  <s:updownselect list="<error>INVALID_VALUE</error>"/>

</s:form>
