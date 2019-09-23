/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.ui.views;

import org.eclipse.ease.modules.ScriptParameter;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.incubator.internal.scripting.ui.views.timegraph.ScriptedTimeGraphView;
import org.eclipse.tracecompass.incubator.internal.scripting.ui.views.xychart.ScriptedXYView;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfTreeXYDataProvider;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Scripting module that allow to interact with views using EASE in the Trace
 * Compass UI.
 * <p>
 * Example scripts using views can be found here:
 * <ul>
 * <li><a href="../../core/analysis/doc-files/scriptedDataProvider.js">A
 * scripted time graph data provider</a> with script-defined entries and arrows,
 * in javascript</li>
 * <li><a href="../../core/analysis/doc-files/basicAnalysis.py">A basic
 * analysis,</a> building an state system and showing its data in a time graph,
 * in python</li>
 * </ul>
 *
 * @author Geneviève Bastien
 */
public class ViewModule {

    /** Module identifier. */
    public static final String MODULE_ID = "/TraceCompass/Views"; //$NON-NLS-1$

    /**
     * Open a time graph view with a data provider
     *
     * @param dataProvider
     *            The data provider used to populate the view
     */
    @WrapToScript
    public void openTimeGraphView(ITimeGraphDataProvider<TimeGraphEntryModel> dataProvider) {

        Display.getDefault().asyncExec(() -> {
            try {
                IViewPart view = openView(dataProvider.getId());
                if (view instanceof ScriptedTimeGraphView) {
                    ((ScriptedTimeGraphView) view).refreshIfNeeded();
                }
            } catch (final PartInitException e) {
                // Do nothing
            }
        });
    }

    private static @Nullable IViewPart openView(String name) throws PartInitException {
        final IWorkbench wb = PlatformUI.getWorkbench();
        final IWorkbenchPage activePage = wb.getActiveWorkbenchWindow().getActivePage();

        return activePage.showView(ScriptedTimeGraphView.ID, name.replace(":", ScriptedTimeGraphView.COLON), IWorkbenchPage.VIEW_ACTIVATE); //$NON-NLS-1$
    }

    /**
     * Open any view in Trace Compass, using its view ID, with an optional
     * secondary ID.
     * <p>
     * For many views that are common for many analyses, for example, the table
     * views, scatter views, statistics, flame chart/graph, the secondary ID is
     * the ID of the analysis. This value can be found by selecting the analysis
     * that will be the source of the view and looking in the 'Properties' view
     * for the ID property.
     * <p>
     * For example, to open a table view for the LTTng-UST CallStack (Incubator) analysis, the follow call can be make
     * <p>
     * <pre>
     * showView(org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.table.SegmentStoreTableView.ID, "org.eclipse.tracecompass.incubator.callstack.core.lttng.ust");
     * </pre>
     *
     * @param viewId
     *            The ID of the view to display
     * @param secondaryId
     *            The secondary ID, if required.
     */
    @WrapToScript
    public void showView(String viewId, @ScriptParameter(defaultValue = "") String secondaryId) {

        Display.getDefault().asyncExec(() -> {
            try {
                final IWorkbench wb = PlatformUI.getWorkbench();
                final IWorkbenchPage activePage = wb.getActiveWorkbenchWindow().getActivePage();

                IViewPart view;
                if (secondaryId.isEmpty()) {
                    view = activePage.showView(viewId);
                } else {
                    view = activePage.showView(viewId, secondaryId, IWorkbenchPage.VIEW_ACTIVATE);
                }
                if (view instanceof ScriptedTimeGraphView) {
                    ((ScriptedTimeGraphView) view).refreshIfNeeded();
                }
            } catch (final PartInitException e) {
                // Do nothing
            }
        });
    }

    /**
     * Open a XY chart for a scripted data provider
     *
     * @param dataProvider
     *            The data provider used to populate the view
     */
    @WrapToScript
    public void openXYChartView(ITmfTreeXYDataProvider<ITmfTreeDataModel> dataProvider) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    IViewPart view = openXYView(dataProvider.getId());
                    if (view == null) {
                        return;
                    }
                } catch (final PartInitException e) {
                    // Do nothing
                }
            }
        });
    }

    private static @Nullable IViewPart openXYView(String name) throws PartInitException {
        final IWorkbench wb = PlatformUI.getWorkbench();
        final IWorkbenchPage activePage = wb.getActiveWorkbenchWindow().getActivePage();

        return activePage.showView(ScriptedXYView.ID, name.replace(":", ScriptedXYView.COLON), IWorkbenchPage.VIEW_ACTIVATE); //$NON-NLS-1$
    }
}