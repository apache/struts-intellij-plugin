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
 * <p>The types in this package ({@link com.intellij.struts2.diagram.model.StrutsConfigDiagramModel},
 * {@link com.intellij.struts2.diagram.model.StrutsDiagramNode},
 * {@link com.intellij.struts2.diagram.model.StrutsDiagramEdge}) and the presentation helpers in
 * {@code com.intellij.struts2.diagram.presentation} are intentionally independent of any UI toolkit.</p>
 *
 * <p>Two hosts currently consume this snapshot for comparison:</p>
 * <ul>
 *   <li>Custom Swing Diagram editor tab ({@code com.intellij.struts2.diagram.ui} /
 *       {@code com.intellij.struts2.diagram.fileEditor})</li>
 *   <li>IntelliJ Diagrams API Show Diagram ({@code com.intellij.struts2.diagram.provider})</li>
 * </ul>
 */
package com.intellij.struts2.diagram.model;
