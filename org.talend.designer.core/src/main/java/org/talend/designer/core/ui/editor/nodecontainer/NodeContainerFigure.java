// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.ui.editor.nodecontainer;

import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.swt.widgets.Display;
import org.talend.commons.ui.image.EImage;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.commons.utils.workbench.gef.SimpleHtmlFigure;
import org.talend.core.CorePlugin;
import org.talend.core.PluginChecker;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.Problem.ProblemStatus;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.ui.editor.process.Process;
import org.talend.designer.core.ui.views.problems.Problems;

/**
 * This class create a figure with the given image. <br/>eh
 * 
 * $Id$
 * 
 */
public class NodeContainerFigure extends Figure {

    private int alpha = -1;

    private NodeContainer nodeContainer;

    private ImageFigure breakpointFigure;

    private ImageFigure errorFigure;

    private ImageFigure warningFigure;

    private ImageFigure infoFigure;

    private SimpleHtmlFigure htmlStatusHint;

    public static final String BREAKPOINT_IMAGE = "icons/breakpoint.png"; //$NON-NLS-1$

    private LabelCenter parallelFigure;

    public NodeContainerFigure(NodeContainer nodeContainer) {
        this.nodeContainer = nodeContainer;
        this.setLayoutManager(new FreeformLayout());
        // this.setOpaque(true);
        // this.setBackgroundColor(new Color(null, new RGB(200, 100, 200)));

        breakpointFigure = new ImageFigure();
        breakpointFigure.setImage(ImageProvider.getImage(CorePlugin.getImageDescriptor(BREAKPOINT_IMAGE)));
        breakpointFigure.setVisible(false);
        breakpointFigure.setSize(breakpointFigure.getPreferredSize());
        this.add(breakpointFigure);

        errorFigure = new ImageFigure();
        errorFigure.setImage(ImageProvider.getImage(EImage.ERROR_SMALL));
        errorFigure.setVisible(false);
        errorFigure.setSize(errorFigure.getPreferredSize());
        this.add(errorFigure);

        warningFigure = new ImageFigure();
        warningFigure.setImage(ImageProvider.getImage(EImage.WARNING_SMALL));
        warningFigure.setVisible(false);
        warningFigure.setSize(warningFigure.getPreferredSize());
        this.add(warningFigure);

        infoFigure = new ImageFigure();
        infoFigure.setImage(ImageProvider.getImage(EImage.INFORMATION_SMALL));
        infoFigure.setVisible(false);
        infoFigure.setSize(infoFigure.getPreferredSize());
        this.add(infoFigure);

        if (PluginChecker.isTIS()) {
            addParallelFigure();
        }

        htmlStatusHint = new SimpleHtmlFigure();
    }

    /**
     * DOC bqian Comment method "addParallelFigure".
     */
    private void addParallelFigure() {
        parallelFigure = new LabelCenter();
        parallelFigure.setImage(ImageProvider.getImage(EImage.PARALLEL_EXECUTION));
        parallelFigure.setFont(Display.getDefault().getSystemFont());
        parallelFigure.setText("x0"); //$NON-NLS-1$
        parallelFigure.setToolTip(new Label("x0")); //$NON-NLS-1$

        boolean visible = false;
        IElementParameter enableParallelizeParameter = nodeContainer.getNode().getElementParameter(
                EParameterName.PARALLELIZE.getName());
        if (enableParallelizeParameter != null) {
            visible = (Boolean) enableParallelizeParameter.getValue();
        }
        parallelFigure.setVisible(visible);
        parallelFigure.setSize(parallelFigure.getPreferredSize());
        this.add(parallelFigure);
    }

    public void updateStatus(int status, boolean showInfoFlag) {
        if ((status & Process.BREAKPOINT_STATUS) != 0) {
            breakpointFigure.setVisible(true);
        } else {
            breakpointFigure.setVisible(false);
        }

        if ((status & Process.ERROR_STATUS) != 0) {
            warningFigure.setVisible(false);
            errorFigure.setVisible(true);
        } else {
            errorFigure.setVisible(false);
            errorFigure.setToolTip(null);
        }

        if (((status & Process.WARNING_STATUS) != 0) && !errorFigure.isVisible()) {
            warningFigure.setVisible(true);
        } else {
            warningFigure.setVisible(false);
            warningFigure.setToolTip(null);
        }

        if (((status & Process.INFO_STATUS) != 0) && !errorFigure.isVisible() && !warningFigure.isVisible() && showInfoFlag) {
            warningFigure.setVisible(false);
            errorFigure.setVisible(false);
            infoFigure.setVisible(true);
        } else {
            infoFigure.setVisible(false);
        }

        if (errorFigure.isVisible() || warningFigure.isVisible()) {
            List<String> problemsList;

            String text = "<b>" + nodeContainer.getNode().getUniqueName() + "</b><br><br>"; //$NON-NLS-1$ //$NON-NLS-2$

            if ((status & Process.WARNING_STATUS) != 0) {
                text += "<i>Warnings:</i><br>"; //$NON-NLS-1$

                problemsList = Problems.getStatusList(ProblemStatus.WARNING, nodeContainer.getNode());
                for (String str : problemsList) {
                    text += "\t- " + str + "<br>"; //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
            if ((status & Process.ERROR_STATUS) != 0) {
                text += "<i>Errors:</i><br>"; //$NON-NLS-1$
                problemsList = Problems.getStatusList(ProblemStatus.ERROR, nodeContainer.getNode());
                for (String str : problemsList) {
                    text += "\t- " + str + "<br>"; //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
            htmlStatusHint.setText(text);
            if (errorFigure.isVisible()) {
                warningFigure.setToolTip(null);
                errorFigure.setToolTip(htmlStatusHint);
            } else {
                errorFigure.setToolTip(null);
                warningFigure.setToolTip(htmlStatusHint);
            }
        }

        updateParallelFigure(status);
    }

    /**
     * DOC YeXiaowei Comment method "updateParallelFigure".
     * 
     * @param status
     */
    private void updateParallelFigure(int status) {

        if (!PluginChecker.isTIS() || parallelFigure == null) {
            return;
        }

        String numberParallel = "0"; //$NON-NLS-1$
        if ((status & Process.PARALLEL_STATUS) != 0) {
            IElementParameter numberParallelizeParameter = nodeContainer.getNode().getElementParameter(
                    EParameterName.PARALLELIZE_NUMBER.getName());
            if (numberParallelizeParameter != null) {
                numberParallel = (String) numberParallelizeParameter.getValue();
            }
            String paralString = "x" + numberParallel; //$NON-NLS-1$
            parallelFigure.setText(paralString);
            parallelFigure.setToolTip(new Label(paralString));
            parallelFigure.setVisible(true);
        } else {
            parallelFigure.setVisible(false);
        }
    }

    @Override
    public void paint(Graphics graphics) {
        if (alpha != -1) {
            graphics.setAlpha(alpha);
        } else {
            graphics.setAlpha(255);
        }
        breakpointFigure.setLocation(nodeContainer.getBreakpointLocation());
        errorFigure.setLocation(nodeContainer.getErrorLocation());
        infoFigure.setLocation(nodeContainer.getInfoLocation());
        warningFigure.setLocation(nodeContainer.getWarningLocation());
        if (parallelFigure != null) {
            parallelFigure.setLocation(nodeContainer.getParallelLocation());
        }

        super.paint(graphics);
    }

    public int getAlpha() {
        return this.alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }
}
