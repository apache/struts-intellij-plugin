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
package com.intellij.struts2.diagram.ui;

import com.intellij.struts2.diagram.model.StrutsConfigDiagramModel;
import com.intellij.struts2.diagram.model.StrutsDiagramEdge;
import com.intellij.struts2.diagram.model.StrutsDiagramNode;
import com.intellij.struts2.diagram.presentation.StrutsDiagramPresentation;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.*;

/**
 * Lightweight read-only Swing panel that renders a Struts config diagram with a
 * deterministic hierarchical layout: packages at left, actions in center, results at right.
 * Supports hover tooltips and click-to-navigate.
 */
public final class Struts2DiagramComponent extends JPanel {

    private static final int NODE_WIDTH = 160;
    private static final int NODE_HEIGHT = 30;
    private static final int H_GAP = 80;
    private static final int V_GAP = 16;
    private static final int PADDING = 24;
    private static final int ICON_TEXT_GAP = 4;
    private static final int ARC = 8;

    private final Map<StrutsDiagramNode, Rectangle> nodeBounds = new LinkedHashMap<>();
    private final List<StrutsDiagramEdge> edges = new ArrayList<>();
    private @Nullable StrutsDiagramNode hoveredNode;

    public Struts2DiagramComponent(@Nullable StrutsConfigDiagramModel model) {
        setBackground(JBColor.background());
        if (model != null) {
            layoutModel(model);
        }

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                StrutsDiagramNode hit = hitTest(e.getPoint());
                if (!Objects.equals(hit, hoveredNode)) {
                    hoveredNode = hit;
                    setToolTipText(hit != null ? StrutsDiagramPresentation.getTooltipHtml(hit) : null);
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                StrutsDiagramNode hit = hitTest(e.getPoint());
                if (hit != null && e.getClickCount() == 2) {
                    StrutsDiagramPresentation.navigateToElement(hit);
                }
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void rebuild(@Nullable StrutsConfigDiagramModel model) {
        nodeBounds.clear();
        edges.clear();
        if (model != null) {
            layoutModel(model);
        }
        revalidate();
        repaint();
    }

    private void layoutModel(@NotNull StrutsConfigDiagramModel model) {
        edges.addAll(model.getEdges());

        List<StrutsDiagramNode> packages = new ArrayList<>();
        List<StrutsDiagramNode> actions = new ArrayList<>();
        List<StrutsDiagramNode> results = new ArrayList<>();

        for (StrutsDiagramNode node : model.getNodes()) {
            switch (node.getKind()) {
                case PACKAGE -> packages.add(node);
                case ACTION -> actions.add(node);
                case RESULT -> results.add(node);
            }
        }

        int colX = PADDING;
        int maxY = placeColumn(packages, colX, PADDING);
        colX += NODE_WIDTH + H_GAP;
        maxY = Math.max(maxY, placeColumn(actions, colX, PADDING));
        colX += NODE_WIDTH + H_GAP;
        maxY = Math.max(maxY, placeColumn(results, colX, PADDING));

        int totalWidth = colX + NODE_WIDTH + PADDING;
        int totalHeight = maxY + PADDING;
        setPreferredSize(new Dimension(totalWidth, totalHeight));
    }

    private int placeColumn(List<StrutsDiagramNode> nodes, int x, int startY) {
        int y = startY;
        for (StrutsDiagramNode node : nodes) {
            nodeBounds.put(node, new Rectangle(x, y, NODE_WIDTH, NODE_HEIGHT));
            y += NODE_HEIGHT + V_GAP;
        }
        return y;
    }

    private @Nullable StrutsDiagramNode hitTest(Point p) {
        for (Map.Entry<StrutsDiagramNode, Rectangle> entry : nodeBounds.entrySet()) {
            if (entry.getValue().contains(p)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            paintEdges(g2);
            paintNodes(g2);
        } finally {
            g2.dispose();
        }
    }

    private void paintEdges(Graphics2D g2) {
        g2.setStroke(new BasicStroke(1.2f));
        for (StrutsDiagramEdge edge : edges) {
            Rectangle srcRect = nodeBounds.get(edge.getSource());
            Rectangle tgtRect = nodeBounds.get(edge.getTarget());
            if (srcRect == null || tgtRect == null) continue;

            int x1 = srcRect.x + srcRect.width;
            int y1 = srcRect.y + srcRect.height / 2;
            int x2 = tgtRect.x;
            int y2 = tgtRect.y + tgtRect.height / 2;

            g2.setColor(JBColor.namedColor("Diagram.edgeColor", JBColor.GRAY));
            int midX = (x1 + x2) / 2;
            Path2D path = new Path2D.Float();
            path.moveTo(x1, y1);
            path.curveTo(midX, y1, midX, y2, x2, y2);
            g2.draw(path);

            drawArrowHead(g2, midX, y2, x2, y2);

            String label = edge.getLabel();
            if (!label.isEmpty()) {
                g2.setFont(JBUI.Fonts.smallFont());
                g2.setColor(JBColor.namedColor("Diagram.edgeLabelColor", JBColor.DARK_GRAY));
                FontMetrics fm = g2.getFontMetrics();
                int labelX = midX - fm.stringWidth(label) / 2;
                int labelY = (y1 + y2) / 2 - 3;
                g2.drawString(label, labelX, labelY);
            }
        }
    }

    private static void drawArrowHead(Graphics2D g2, int fromX, int fromY, int toX, int toY) {
        double angle = Math.atan2(toY - fromY, toX - fromX);
        int arrowLen = 8;
        int x1 = (int) (toX - arrowLen * Math.cos(angle - Math.PI / 6));
        int y1 = (int) (toY - arrowLen * Math.sin(angle - Math.PI / 6));
        int x2 = (int) (toX - arrowLen * Math.cos(angle + Math.PI / 6));
        int y2 = (int) (toY - arrowLen * Math.sin(angle + Math.PI / 6));
        g2.fillPolygon(new int[]{toX, x1, x2}, new int[]{toY, y1, y2}, 3);
    }

    private void paintNodes(Graphics2D g2) {
        Font nodeFont = JBUI.Fonts.label();
        g2.setFont(nodeFont);
        FontMetrics fm = g2.getFontMetrics();

        for (Map.Entry<StrutsDiagramNode, Rectangle> entry : nodeBounds.entrySet()) {
            StrutsDiagramNode node = entry.getKey();
            Rectangle r = entry.getValue();
            boolean hovered = Objects.equals(node, hoveredNode);

            Color fill = switch (node.getKind()) {
                case PACKAGE -> JBColor.namedColor("Diagram.packageNodeFill",
                        new JBColor(new Color(0xE8F0FE), new Color(0x2B3A4C)));
                case ACTION -> JBColor.namedColor("Diagram.actionNodeFill",
                        new JBColor(new Color(0xE6F4EA), new Color(0x1E3A2C)));
                case RESULT -> JBColor.namedColor("Diagram.resultNodeFill",
                        new JBColor(new Color(0xFFF3E0), new Color(0x3A2E1E)));
            };
            Color border = hovered
                    ? JBColor.namedColor("Diagram.nodeHoverBorder", JBColor.BLUE)
                    : JBColor.namedColor("Diagram.nodeBorder", JBColor.GRAY);

            g2.setColor(fill);
            g2.fillRoundRect(r.x, r.y, r.width, r.height, ARC, ARC);
            g2.setColor(border);
            g2.setStroke(new BasicStroke(hovered ? 2f : 1f));
            g2.drawRoundRect(r.x, r.y, r.width, r.height, ARC, ARC);

            Icon icon = node.getIcon();
            int textX = r.x + 6;
            if (icon != null) {
                int iconY = r.y + (r.height - icon.getIconHeight()) / 2;
                icon.paintIcon(this, g2, textX, iconY);
                textX += icon.getIconWidth() + ICON_TEXT_GAP;
            }

            g2.setColor(JBColor.foreground());
            String label = node.getName();
            int availableWidth = r.x + r.width - textX - 4;
            if (fm.stringWidth(label) > availableWidth) {
                while (label.length() > 1 && fm.stringWidth(label + "...") > availableWidth) {
                    label = label.substring(0, label.length() - 1);
                }
                label += "...";
            }
            int textY = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(label, textX, textY);
        }
    }
}
