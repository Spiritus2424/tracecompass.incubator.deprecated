/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.trace.server.jersey.rest.core.model.views;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.xy.ITmfCommonXAxisModel;
import org.eclipse.tracecompass.internal.provisional.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Object to encapsulate the values returned by a query for an XY view
 *
 * @author Loic Prieur-Drevon
 */
@SuppressWarnings("restriction")
@XmlRootElement
public class XYView {
    private final ITmfTrace fTrace;
    private final TmfModelResponse<@NonNull ITmfCommonXAxisModel> fResponse;

    /**
     * Compose the {@link ITmfTrace} and {@link ITmfCommonXAxisModel} in an
     * {@link XYView}
     *
     * @param trace
     *            the queried trace
     * @param response
     *            XY model response for the query
     */
    public XYView(@Nullable ITmfTrace trace, TmfModelResponse<@NonNull ITmfCommonXAxisModel> response) {
        fTrace = trace;
        fResponse = response;
    }

    /**
     * Getter for the trace
     *
     * @return the trace
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Getter for the time values in this XY view
     *
     * @return the time values
     */
    @XmlElement
    public TmfModelResponse<@NonNull ITmfCommonXAxisModel> getResponse() {
        return fResponse;
    }
}
