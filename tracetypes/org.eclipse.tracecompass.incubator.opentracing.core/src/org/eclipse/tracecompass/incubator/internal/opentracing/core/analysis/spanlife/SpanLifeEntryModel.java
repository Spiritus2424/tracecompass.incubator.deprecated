/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.opentracing.core.analysis.spanlife;

import java.util.List;

import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;

/**
 * {@link TimeGraphEntryModel} for the Span life data provider
 *
 * @author Katherine Nadeau
 *
 */
public class SpanLifeEntryModel extends TimeGraphEntryModel {

    /**
     * Log event
     */
    public static class LogEvent {
        private final long fTime;
        private final String fType;

        /**
         * Constructor
         *
         * @param time
         *            timestamp of the log
         * @param type
         *            type of the log
         */
        public LogEvent(long time, String type) {
            fTime = time;
            fType = type;
        }

        /**
         * Timestamp of the event
         *
         * @return timestamp
         */
        public long getTime() {
            return fTime;
        }

        /**
         * Type of event
         *
         * @return event
         */
        public String getType() {
            return fType;
        }
    }

    private final List<LogEvent> fLogs;

    private final boolean fErrorTag;

    /**
     * Constructor
     *
     * @param id
     *            Entry ID
     * @param parentId
     *            Parent ID
     * @param name
     *            Entry name to be displayed
     * @param startTime
     *            Start time
     * @param endTime
     *            End time
     * @param logs
     *            Span logs timestamps
     * @param errorTag
     *            true if the span has an error tag
     */
    public SpanLifeEntryModel(long id, long parentId, String name, long startTime, long endTime, List<LogEvent> logs, boolean errorTag) {
        super(id, parentId, name, startTime, endTime);
        fLogs = logs;
        fErrorTag = errorTag;
    }

    /**
     * Getter for the logs
     *
     * @return the logs timestamps
     */
    public List<LogEvent> getLogs() {
        return fLogs;
    }

    /**
     * Getter for the error tag
     *
     * @return true if the span was tagged with an error
     */
    public boolean getErrorTag() {
        return fErrorTag;
    }

}