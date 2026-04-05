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

import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlProlog;
import com.intellij.struts2.StrutsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Validates the DOCTYPE SYSTEM URI of a Struts configuration file against the
 * known DTD URIs registered in {@link StrutsConstants#STRUTS_DTDS}.
 * <p>
 * Shared between the DOM model inspection and the Diagram file editor so the
 * validation rule is defined in one place.
 */
public final class StrutsDtdValidator {

    private StrutsDtdValidator() {}

    public enum Result {
        OK,
        HTTP_INSTEAD_OF_HTTPS,
        UNRECOGNIZED
    }

    public static @NotNull Result validate(@NotNull XmlFile xmlFile) {
        String systemId = extractSystemId(xmlFile);
        if (systemId == null) return Result.OK;

        for (String dtd : StrutsConstants.STRUTS_DTDS) {
            if (dtd.equals(systemId)) return Result.OK;
        }

        if (systemId.startsWith("http://struts.apache.org/dtds/struts-")) {
            return Result.HTTP_INSTEAD_OF_HTTPS;
        }

        return Result.UNRECOGNIZED;
    }

    public static @Nullable String extractSystemId(@NotNull XmlFile xmlFile) {
        XmlDocument document = xmlFile.getDocument();
        if (document == null) return null;
        XmlProlog prolog = document.getProlog();
        if (prolog == null || prolog.getDoctype() == null) return null;

        String systemId = prolog.getDoctype().getDtdUri();
        if (systemId == null || systemId.isEmpty()) return null;
        return systemId;
    }

    public static @NotNull String suggestedUri(@NotNull String httpUri) {
        return httpUri.replace("http://", "https://");
    }
}
