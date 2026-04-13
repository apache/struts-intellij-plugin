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
package com.intellij.struts2.diagram.model;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts2.Struts2Icons;
import com.intellij.struts2.diagram.presentation.StrutsDiagramPresentation;
import com.intellij.struts2.dom.params.Param;
import com.intellij.struts2.dom.struts.StrutsRoot;
import com.intellij.struts2.dom.struts.action.Action;
import com.intellij.struts2.dom.struts.action.Result;
import com.intellij.struts2.dom.struts.impl.path.ResultTypeResolver;
import com.intellij.struts2.dom.struts.model.StrutsManager;
import com.intellij.struts2.dom.struts.model.StrutsModel;
import com.intellij.struts2.dom.struts.strutspackage.ResultType;
import com.intellij.struts2.dom.struts.strutspackage.StrutsPackage;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * Builds a toolkit-neutral snapshot of the Struts configuration for diagram rendering.
 * Walks the <b>file-local</b> {@link StrutsRoot} DOM to produce package, action, and
 * result nodes with directed edges (package->action, action->result).
 * Only elements declared in the currently opened {@code struts.xml} are included;
 * inherited framework packages (e.g. {@code struts-default}) are not expanded into
 * full diagram nodes — their names appear only in the package tooltip's "Extends" line.
 * <p>
 * For results whose effective type is {@code chain}, {@code redirectAction}, or
 * {@code redirect-action}, the model resolves the target action (from tag body text
 * or {@code actionName}/{@code namespace} params). When the target action is declared
 * in the same file, an action→action edge is emitted instead of a separate result
 * node. External or unresolvable targets fall back to a labeled {@code Kind.RESULT} node.
 * <p>
 * <b>Must be called under a read action.</b> All DOM/PSI access (tooltip computation,
 * navigation pointer creation) happens here so that Swing event handlers on the EDT
 * never need to touch PSI directly.
 */
public final class StrutsConfigDiagramModel {

    private static final Logger LOG = Logger.getInstance(StrutsConfigDiagramModel.class);
    static final String UNRESOLVED_RESULT = "(unresolved path)";
    static final String UNNAMED = "(unnamed)";

    private final List<StrutsDiagramNode> nodes = new ArrayList<>();
    private final List<StrutsDiagramEdge> edges = new ArrayList<>();

    private StrutsConfigDiagramModel() {}

    public @NotNull List<StrutsDiagramNode> getNodes() { return Collections.unmodifiableList(nodes); }
    public @NotNull List<StrutsDiagramEdge> getEdges() { return Collections.unmodifiableList(edges); }

    /**
     * Build a snapshot from the packages declared in the given XML file only.
     * Uses the file-local {@link StrutsRoot} DOM when available, falling back
     * to the merged {@link StrutsModel} filtered to packages whose XML tag
     * belongs to the current file.
     * <p>
     * <b>Must be called under a read action</b> — all DOM/PSI access happens here.
     *
     * @return populated model, or {@code null} if the file is not a Struts config.
     */
    public static @Nullable StrutsConfigDiagramModel build(@NotNull XmlFile xmlFile) {
        List<StrutsPackage> packages = getLocalPackages(xmlFile);
        if (packages == null) return null;

        StrutsModel strutsModel = StrutsManager.getInstance(xmlFile.getProject()).getModelByFile(xmlFile);
        SmartPointerManager pointerManager = SmartPointerManager.getInstance(xmlFile.getProject());
        StrutsConfigDiagramModel model = new StrutsConfigDiagramModel();

        // Pass 1: create package and action nodes; collect XmlTag→node mapping
        // Use XmlTag keys rather than Action DOM proxies, because findActionsByName
        // may return different proxy instances for the same underlying XML element.
        Map<XmlTag, StrutsDiagramNode> actionNodeMap = new IdentityHashMap<>();
        record PendingResult(StrutsDiagramNode actionNode, Result result, String currentNamespace) {}
        List<PendingResult> pendingResults = new ArrayList<>();

        for (StrutsPackage strutsPackage : packages) {
            String pkgName = Objects.toString(strutsPackage.getName().getStringValue(), UNNAMED);
            StrutsDiagramNode pkgNode = createNode(
                    StrutsDiagramNode.Kind.PACKAGE, pkgName, AllIcons.Nodes.Package,
                    strutsPackage, pointerManager);
            model.nodes.add(pkgNode);

            String namespace = strutsPackage.searchNamespace();
            for (Action action : strutsPackage.getActions()) {
                String actionName = Objects.toString(action.getName().getStringValue(), UNNAMED);
                StrutsDiagramNode actionNode = createNode(
                        StrutsDiagramNode.Kind.ACTION, actionName, Struts2Icons.Action,
                        action, pointerManager);
                model.nodes.add(actionNode);
                model.edges.add(new StrutsDiagramEdge(pkgNode, actionNode, ""));
                XmlTag actionTag = action.getXmlTag();
                if (actionTag != null) {
                    actionNodeMap.put(actionTag, actionNode);
                }

                for (Result result : action.getResults()) {
                    pendingResults.add(new PendingResult(actionNode, result, namespace));
                }
            }
        }

        // Pass 2: process results — chain/redirect targets become action→action edges
        for (PendingResult pr : pendingResults) {
            String resultName = pr.result.getName().getStringValue();
            String edgeLabel = resultName != null ? resultName : Result.DEFAULT_NAME;

            Action targetAction = resolveChainOrRedirectTarget(pr.result, strutsModel, pr.currentNamespace);
            if (targetAction != null) {
                XmlTag targetTag = targetAction.getXmlTag();
                StrutsDiagramNode targetNode = targetTag != null ? actionNodeMap.get(targetTag) : null;
                if (targetNode != null) {
                    // Target is in the same file — direct action→action edge
                    model.edges.add(new StrutsDiagramEdge(pr.actionNode, targetNode, edgeLabel));
                    continue;
                }
                // Target is in another file — show as labeled result node
                String targetLabel = formatExternalActionLabel(targetAction);
                StrutsDiagramNode resultNode = createNode(
                        StrutsDiagramNode.Kind.RESULT, targetLabel, Struts2Icons.Action,
                        pr.result, pointerManager);
                model.nodes.add(resultNode);
                model.edges.add(new StrutsDiagramEdge(pr.actionNode, resultNode, edgeLabel));
                continue;
            }

            // Non-chain/redirect or unresolvable — standard result node
            PathReference pathRef = pr.result.getValue();
            String path = pathRef != null ? pathRef.getPath() : UNRESOLVED_RESULT;
            Icon resultIcon = resolveResultIcon(pr.result);
            StrutsDiagramNode resultNode = createNode(
                    StrutsDiagramNode.Kind.RESULT, path, resultIcon,
                    pr.result, pointerManager);
            model.nodes.add(resultNode);
            model.edges.add(new StrutsDiagramEdge(pr.actionNode, resultNode, edgeLabel));
        }
        return model;
    }

    /**
     * Resolves the target {@link Action} for chain/redirect result types.
     * Mirrors the resolution logic of
     * {@link com.intellij.struts2.dom.struts.impl.path.ActionChainOrRedirectResultContributor}.
     *
     * @return the uniquely resolved action, or {@code null} if the result is not a
     *         chain/redirect type or the target cannot be resolved unambiguously.
     */
    static @Nullable Action resolveChainOrRedirectTarget(@NotNull Result result,
                                                         @Nullable StrutsModel strutsModel,
                                                         @NotNull String currentNamespace) {
        if (!result.isValid()) return null;

        String typeName = null;
        ResultType effectiveType = result.getEffectiveResultType();
        if (effectiveType != null) {
            typeName = effectiveType.getName().getStringValue();
        }
        if (typeName == null) {
            // Fall back to the raw XML attribute when the ResultType DOM can't be resolved
            // (e.g. the result-type definition is in struts-default and not in the model)
            typeName = result.getType().getStringValue();
        }
        if (typeName == null || !ResultTypeResolver.isChainOrRedirectType(typeName)) return null;

        // Determine action path: prefer tag body, fall back to <param name="actionName">
        String actionPath = null;
        XmlTag xmlTag = result.getXmlTag();
        if (xmlTag != null) {
            String bodyText = xmlTag.getValue().getTrimmedText();
            if (!bodyText.isEmpty()) {
                actionPath = bodyText;
            }
        }
        if (actionPath == null) {
            actionPath = getParamValue(result, "actionName");
        }
        if (actionPath == null || actionPath.isEmpty()) return null;

        // Strip query parameters (e.g. "actionPath2?myParam=myValue")
        int queryIdx = actionPath.indexOf('?');
        if (queryIdx != -1) {
            actionPath = actionPath.substring(0, queryIdx);
        }

        // Determine namespace: from path prefix, explicit param, or current package
        String namespace = currentNamespace;
        int lastSlash = actionPath.lastIndexOf('/');
        if (lastSlash != -1) {
            namespace = actionPath.substring(0, lastSlash);
            actionPath = actionPath.substring(lastSlash + 1);
        } else {
            String nsParam = getParamValue(result, "namespace");
            if (nsParam != null && !nsParam.isEmpty()) {
                namespace = nsParam;
            }
        }

        if (strutsModel == null) return null;
        List<Action> actions = strutsModel.findActionsByName(actionPath, namespace);
        return actions.size() == 1 ? actions.get(0) : null;
    }

    private static @Nullable String getParamValue(@NotNull Result result, @NotNull String paramName) {
        for (Param param : result.getParams()) {
            XmlTag tag = param.getXmlTag();
            if (tag != null && paramName.equals(tag.getAttributeValue("name"))) {
                String value = tag.getValue().getTrimmedText();
                if (!value.isEmpty()) return value;
            }
        }
        return null;
    }

    private static @NotNull String formatExternalActionLabel(@NotNull Action action) {
        String ns = action.getNamespace();
        String name = action.getName().getStringValue();
        if (name == null) name = UNNAMED;
        if (ns != null && !StrutsPackage.DEFAULT_NAMESPACE.equals(ns)) {
            return "\u2192 " + ns + "/" + name;
        }
        return "\u2192 " + name;
    }

    /**
     * Resolves the list of packages local to the given file.
     * Finds the {@link StrutsRoot} for the current file from the model's individual
     * roots (not the concatenated merged packages) so we get only this file's packages.
     */
    private static @Nullable List<StrutsPackage> getLocalPackages(@NotNull XmlFile xmlFile) {
        VirtualFile targetVFile = xmlFile.getOriginalFile().getVirtualFile();

        StrutsModel strutsModel = StrutsManager.getInstance(xmlFile.getProject()).getModelByFile(xmlFile);
        if (strutsModel != null) {
            for (DomFileElement<StrutsRoot> root : strutsModel.getRoots()) {
                VirtualFile rootVFile = root.getOriginalFile().getVirtualFile();
                if (Objects.equals(targetVFile, rootVFile)) {
                    List<StrutsPackage> packages = root.getRootElement().getPackages();
                    LOG.debug("Found matching root for " + xmlFile.getName() +
                            ", " + packages.size() + " packages");
                    return packages;
                }
            }
            LOG.debug("Merged model has " + strutsModel.getRoots().size() +
                    " roots but none matched " + targetVFile);
        }

        DomFileElement<StrutsRoot> fileElement =
                DomManager.getDomManager(xmlFile.getProject()).getFileElement(xmlFile, StrutsRoot.class);
        if (fileElement != null) {
            List<StrutsPackage> packages = fileElement.getRootElement().getPackages();
            LOG.debug("File-local DOM returned " + packages.size() + " packages for " + xmlFile.getName());
            return packages;
        }

        LOG.debug("No model available for " + xmlFile.getName());
        return null;
    }

    private static @NotNull StrutsDiagramNode createNode(
            @NotNull StrutsDiagramNode.Kind kind,
            @NotNull String name,
            @Nullable Icon icon,
            @NotNull DomElement domElement,
            @NotNull SmartPointerManager pointerManager) {

        String tooltipHtml = StrutsDiagramPresentation.computeTooltipHtml(domElement);
        SmartPsiElementPointer<XmlElement> navPointer = createNavigationPointer(domElement, pointerManager);
        String id = buildNodeId(kind, domElement);
        return new StrutsDiagramNode(id, kind, name, icon, tooltipHtml, navPointer);
    }

    private static @NotNull String buildNodeId(@NotNull StrutsDiagramNode.Kind kind,
                                               @NotNull DomElement domElement) {
        XmlElement xml = domElement.getXmlElement();
        if (xml != null) {
            return kind.name() + "@" + xml.getTextOffset();
        }
        return kind.name() + "@" + System.identityHashCode(domElement);
    }

    private static @Nullable SmartPsiElementPointer<XmlElement> createNavigationPointer(
            @NotNull DomElement domElement,
            @NotNull SmartPointerManager pointerManager) {
        XmlElement xmlElement = domElement.getXmlElement();
        if (xmlElement == null) return null;
        return pointerManager.createSmartPsiElementPointer(xmlElement);
    }

    private static @NotNull Icon resolveResultIcon(@NotNull Result result) {
        if (!result.isValid()) return AllIcons.FileTypes.Unknown;
        PathReference ref = result.getValue();
        if (ref == null || ref.resolve() == null) return AllIcons.FileTypes.Unknown;
        Icon icon = ref.getIcon();
        return icon != null ? icon : AllIcons.FileTypes.Unknown;
    }
}
