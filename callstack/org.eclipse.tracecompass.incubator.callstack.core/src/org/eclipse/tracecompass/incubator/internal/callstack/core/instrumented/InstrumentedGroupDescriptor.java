/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.core.instrumented;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.callstack.core.base.CallStackGroupDescriptor;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;

/**
 * These group descriptors describe each group of a callstack hierarchy with
 * patterns in the state system.
 *
 * @author Geneviève Bastien
 */
public class InstrumentedGroupDescriptor extends CallStackGroupDescriptor {

    private final String[] fSubPattern;
    private final ITmfStateSystem fSs;
    private final String fHostId;

    /**
     * Constructor
     *
     * @param ss
     *            The state system containing the callstack
     * @param subPath
     *            The sub-path to this group
     * @param nextGroup
     *            The next group of the hierarchy, ie the child of the group being
     *            constructed or <code>null</code> if this group is the leaf
     * @param isSymbolKeyGroup
     *            Whether this level contains the symbol key
     * @param hostId
     *            The ID of the host these callstack groups are from
     */
    public InstrumentedGroupDescriptor(ITmfStateSystem ss, String[] subPath, @Nullable InstrumentedGroupDescriptor nextGroup, boolean isSymbolKeyGroup, String hostId) {
        super(String.valueOf(StringUtils.join(subPath, '/')), nextGroup, isSymbolKeyGroup);
        fSubPattern = subPath;
        fSs = ss;
        fHostId = hostId;
    }

    /**
     * Get the pattern in the state system corresponding to this level. From the
     * quark of the parent, the elements corresponding to this sub-pattern will be
     * the elements of this group.
     *
     * @return The pattern of the elements at this level
     */
    public String[] getSubPattern() {
        return fSubPattern;
    }

    /**
     * Get the state system where this group belongs
     *
     * @return The state system
     */
    public ITmfStateSystem getStateSystem() {
        return fSs;
    }

    /**
     * Get the host ID this group descriptor is for
     *
     * TODO: Do we need this?
     *
     * @return The host ID
     */
    public String getHostId() {
        return fHostId;
    }

    @Override
    public @Nullable InstrumentedGroupDescriptor getNextGroup() {
        return (InstrumentedGroupDescriptor) super.getNextGroup();
    }

    @Override
    public @NonNull String getName() {
        return String.valueOf(StringUtils.join(fSubPattern, '/'));
    }

}
