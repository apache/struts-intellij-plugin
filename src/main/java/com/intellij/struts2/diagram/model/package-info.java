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

/**
 * Toolkit-neutral Struts configuration diagram model.
 *
 * <h2>Migration boundary</h2>
 * <p>The types in this package ({@link com.intellij.struts2.diagram.model.StrutsConfigDiagramModel},
 * {@link com.intellij.struts2.diagram.model.StrutsDiagramNode},
 * {@link com.intellij.struts2.diagram.model.StrutsDiagramEdge}) are intentionally independent of both:</p>
 * <ul>
 *   <li>The deprecated {@code com.intellij.openapi.graph.builder} (GraphBuilder) APIs used by the legacy
 *       {@code com.intellij.struts2.graph} package, and</li>
 *   <li>The newer {@code com.intellij.diagram.Provider} (Diagrams API) that may be adopted in the future.</li>
 * </ul>
 * <p>This isolation means that the rendering/editor layer (currently a lightweight Swing panel in
 * {@code com.intellij.struts2.diagram.ui}) can be replaced without touching the DOM traversal or
 * presentation logic. A future migration to {@code com.intellij.diagram.Provider} should:</p>
 * <ol>
 *   <li>Implement {@code DiagramProvider} / {@code DiagramDataModel} consuming the snapshot produced
 *       by {@link com.intellij.struts2.diagram.model.StrutsConfigDiagramModel#build}.</li>
 *   <li>Reuse {@link com.intellij.struts2.diagram.presentation.StrutsDiagramPresentation} for
 *       tooltips and navigation.</li>
 *   <li>Replace only the {@code diagram.ui} and {@code diagram.fileEditor} packages.</li>
 * </ol>
 */
package com.intellij.struts2.diagram.model;
