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
package com.intellij.struts2.diagram.provider;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomUtil;
import com.intellij.util.xml.events.DomEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Pure helpers for same-file DomEvent filtering.
 * Kept for shared/test use; Show Diagram no longer auto-refreshes on DomEvents
 * (users invoke Refresh Data Model instead).
 */
public final class StrutsDiagramDomRefresh {

    private StrutsDiagramDomRefresh() {}

    public static boolean isEventForMyFile(@NotNull DomEvent event, @NotNull VirtualFile file) {
        return isDomElementInFile(event.getElement(), file);
    }

    public static boolean isDomElementInFile(@Nullable DomElement element, @NotNull VirtualFile file) {
        if (element == null) {
            return false;
        }
        VirtualFile elementFile = DomUtil.getFile(element).getOriginalFile().getVirtualFile();
        return file.equals(elementFile);
    }
}
