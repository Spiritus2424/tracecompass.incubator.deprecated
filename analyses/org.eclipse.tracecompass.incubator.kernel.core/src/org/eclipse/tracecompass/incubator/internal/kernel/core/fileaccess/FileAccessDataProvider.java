/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.kernel.core.fileaccess;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.analysis.core.model.ModelManager;
import org.eclipse.tracecompass.incubator.internal.kernel.core.Activator;
import org.eclipse.tracecompass.incubator.internal.kernel.core.fileaccess.FileEntryModel.Type;
import org.eclipse.tracecompass.incubator.internal.kernel.core.filedescriptor.FileDescriptorStateProvider;
import org.eclipse.tracecompass.incubator.internal.kernel.core.filedescriptor.ThreadEntryModel;
import org.eclipse.tracecompass.incubator.internal.kernel.core.filedescriptor.TidTimeQueryFilter;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.internal.tmf.core.model.timegraph.AbstractTimeGraphDataProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;

/**
 * <p>
 * File Descriptor data Provider
 * </p>
 * <p>
 * Shows per-file access
 * </p>
 *
 * @author Matthew Khouzam
 */
public class FileAccessDataProvider extends AbstractTimeGraphDataProvider<@NonNull FileAccessAnalysis, @NonNull TimeGraphEntryModel> {

    /**
     * Suffix for dataprovider ID
     */
    public static final String SUFFIX = ".dataprovider"; //$NON-NLS-1$ ;

    private static final Pattern IS_INTEGER = Pattern.compile("\\d+"); //$NON-NLS-1$

    private static final int OFFSET = 100000;
    private static final AtomicInteger STRING_VALUE = new AtomicInteger(OFFSET);
    private Map<String, Integer> fileIds = new HashMap<>();

    /**
     * Constructor
     *
     * @param trace
     *            the trace this provider represents
     * @param analysisModule
     *            the analysis encapsulated by this provider
     */
    public FileAccessDataProvider(ITmfTrace trace, FileAccessAnalysis analysisModule) {
        super(trace, analysisModule);
    }

    @Deprecated
    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        Map<String, Object> parameters = FetchParametersUtils.timeQueryToMap(filter);
        return fetchArrows(parameters, monitor);
    }

    @Override
    public TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public @NonNull String getId() {
        return getAnalysisModule().getId() + SUFFIX;
    }

    @Override
    protected @Nullable TimeGraphModel getRowModel(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(parameters);
        if (filter == null) {
            return null;
        }
        TreeMultimap<Integer, ITmfStateInterval> intervals = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));
        Map<@NonNull Long, @NonNull Integer> entries = getSelectedEntries(filter);
        Collection<Long> times = getTimes(filter, ss.getStartTime(), ss.getCurrentEndTime());
        /* Do the actual query */
        for (ITmfStateInterval interval : ss.query2D(entries.values(), times)) {
            if (monitor != null && monitor.isCanceled()) {
                return new TimeGraphModel(Collections.emptyList());
            }
            intervals.put(interval.getAttribute(), interval);
        }
        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(parameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }
        List<@NonNull ITimeGraphRowModel> rows = new ArrayList<>();
        for (Map.Entry<@NonNull Long, @NonNull Integer> entry : entries.entrySet()) {
            if (monitor != null && monitor.isCanceled()) {
                return new TimeGraphModel(Collections.emptyList());
            }

            List<ITimeGraphState> eventList = new ArrayList<>();
            for (ITmfStateInterval interval : intervals.get(entry.getValue())) {
                long startTime = interval.getStartTime();
                long duration = interval.getEndTime() - startTime + 1;
                Object state = interval.getValue();
                String label = null;
                TimeGraphState value = null;
                if (Integer.valueOf(1).equals(state)) {
                    ITmfTrace trace = getTrace();
                    List<Integer> sub = ss.getSubAttributes(interval.getAttribute(), false);
                    for (ITmfStateInterval threadInterval : ss.query2D(sub, Collections.singleton(startTime))) {
                        Object sv = threadInterval.getValue();
                        if (sv != null) {
                            int tid = getTid(ss, threadInterval.getAttribute());
                            label = getThreadName(tid, interval.getStartTime(), trace) + " (" + tid + ")"; //$NON-NLS-1$ //$NON-NLS-2$
                            value = new TimeGraphState(startTime, duration, tid, label);
                            break;
                        }
                    }
                }
                if (state != null && value == null) {
                    value = new TimeGraphState(startTime, duration, 1);
                }
                if (value == null) {
                    value = new TimeGraphState(startTime, duration, Integer.MIN_VALUE);
                }
                applyFilterAndAddState(eventList, value, entry.getKey(), predicates, monitor);
            }
            rows.add(new TimeGraphRowModel(entry.getKey(), eventList));

        }
        return new TimeGraphModel(rows);
    }

    @Override
    protected boolean isCacheable() {
        return false;
    }

    @Deprecated
    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        Map<String, Object> parameters = FetchParametersUtils.selectionTimeQueryToMap(filter);
        return fetchTooltip(parameters, monitor);
    }

    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ITmfStateSystem ss = getAnalysisModule().getStateSystem();
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }
        Collection<@NonNull Integer> quarks = getSelectedEntries(filter).values();
        Map<String, String> retMap = new LinkedHashMap<>();

        if (quarks.size() != 1) {
            return new TmfModelResponse<>(retMap, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        long start = filter.getStart();
        if (ss == null || quarks.size() != 1 || !getAnalysisModule().isQueryable(start)) {
            /*
             * We need the ss to query, we should only be querying one attribute
             * and the query times should be valid.
             */
            return new TmfModelResponse<>(retMap, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }
        int quark = quarks.iterator().next();
        try {
            ITmfStateInterval current = ss.querySingleState(start, quark);

            int resQuark = ss.optQuarkAbsolute(FileDescriptorStateProvider.RESOURCES);
            if (resQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
                return new TmfModelResponse<>(retMap, ITmfResponse.Status.FAILED, "Bizarre quark value for the file resources"); //$NON-NLS-1$
            }
            int parentQuark = ss.getParentAttributeQuark(quark);
            if (parentQuark == resQuark) {
                // This is a file name, add the number of opened fd and full
                // file name
                String fileName = ss.getAttributeName(quark);
                retMap.put("File Name", fileName); //$NON-NLS-1$
                Object value = current.getValue();
                retMap.put("Number of opened FD", (value instanceof Number) ? String.valueOf(value) : "0"); //$NON-NLS-1$ //$NON-NLS-2$
                return new TmfModelResponse<>(retMap, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            }

            int parentQuark2 = ss.getParentAttributeQuark(parentQuark);
            if (parentQuark2 == resQuark) {
                // This is a thread, add the fd and full file name
                String fileName = ss.getAttributeName(parentQuark);
                retMap.put("File Name", fileName); //$NON-NLS-1$
                Object value = current.getValue();
                if (value instanceof Number) {
                    retMap.put("FD", String.valueOf(value)); //$NON-NLS-1$
                }
                return new TmfModelResponse<>(retMap, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            }

            // Not something we know what to do

            return new TmfModelResponse<>(retMap, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        } catch (StateSystemDisposedException e) {
            // Ignore, nothing to do
        }

        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }


    @Override
    protected TmfTreeModel<TimeGraphEntryModel> getTree(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        Collection<Integer> selectedTids = Collections.emptySet();
        // TODO Why not SelectionTimeQueryFilter ?
        TimeQueryFilter filter = FetchParametersUtils.createTimeQuery(parameters);
        if (filter instanceof TidTimeQueryFilter) {
            selectedTids = ((TidTimeQueryFilter) filter).getTids();
        }
        Builder<@NonNull TimeGraphEntryModel> builder = new Builder<>();
        long rootId = getId(ITmfStateSystem.ROOT_ATTRIBUTE);
        ITmfTrace trace = getTrace();
        builder.add(new TimeGraphEntryModel(rootId, -1, String.valueOf(trace.getName()), ss.getStartTime(), ss.getCurrentEndTime()));
        try {
            addResources(ss, builder, ss.getQuarkAbsolute(FileDescriptorStateProvider.RESOURCES), rootId, selectedTids);
        } catch (AttributeNotFoundException e) {
            Activator.getInstance().logError(e.getMessage(), e);
        }

        return new TmfTreeModel<>(Collections.emptyList(), builder.build());
    }

    private void addResources(ITmfStateSystem ss, Builder<@NonNull TimeGraphEntryModel> builder, int quark, long parentId, Collection<Integer> filter) {
        List<@NonNull Integer> fileQuarks = ss.getSubAttributes(quark, false);

        String ramFiles = "in memory"; //$NON-NLS-1$
        boolean hasMemfile = false;
        int ramHash = fileIds.computeIfAbsent(ramFiles, a -> STRING_VALUE.incrementAndGet());
        long ramId = getId(ramHash);
        TimeGraphEntryModel ramElement = new TimeGraphEntryModel(ramId, parentId, ramFiles, ss.getStartTime(), ss.getCurrentEndTime());

        for (Integer fileQuark : fileQuarks) {
            String name = ss.getAttributeName(fileQuark);
            long id = getId(fileQuark);
            List<Integer> sub = ss.getSubAttributes(fileQuark, false);
            Set<Integer> contributingTids = sub.stream()
                    .map(ss::getAttributeName)
                    .filter(FileAccessDataProvider::isInteger)
                    .map(Integer::parseInt)
                    .collect(Collectors.toSet());
            if (filter.isEmpty() || !Sets.intersection(contributingTids, new HashSet<>(filter)).isEmpty()) {
                if (name.startsWith(File.separator)) {
                    String[] segments = name.split(File.separator);
                    StringBuilder sb = new StringBuilder();
                    long parent = parentId;
                    builder.add(new FileEntryModel(getId(fileIds.computeIfAbsent(File.separator, a -> STRING_VALUE.incrementAndGet())), parentId, File.separator, ss.getStartTime(), ss.getCurrentEndTime(), false, Type.Directory));
                    for (int i = 0; i < segments.length - 1; i++) {
                        sb.append('/').append(segments[i]);
                        String fileName = sb.toString();
                        Long fileId = getId(fileIds.computeIfAbsent(fileName, a -> STRING_VALUE.incrementAndGet()));
                        builder.add(new FileEntryModel(fileId, parent, segments[i] + File.separator, ss.getStartTime(), ss.getCurrentEndTime(), false, Type.Directory));
                        parent = fileId;
                    }
                    builder.add(new FileEntryModel(id, parent, name.substring(1 + name.lastIndexOf(File.separator)), ss.getStartTime(), ss.getCurrentEndTime(), true, Type.File));

                } else {
                    if (!hasMemfile) {
                        builder.add(ramElement);
                        hasMemfile = true;
                    }
                    builder.add(new FileEntryModel(id, ramId, name, ss.getStartTime(), ss.getCurrentEndTime(), true, FileEntryModel.Type.InRam));
                }
                List<@NonNull Integer> threadQuarks = ss.getSubAttributes(fileQuark, false);
                if (threadQuarks.size() == 1) {
                    continue;
                }
                for (Integer threadQuark : threadQuarks) {
                    ITmfTrace trace = getTrace();
                    // Broken
                    int tid = getTid(ss, threadQuark);
                    if (tid != -1) {
                        String threadName = getThreadName(tid, ss.getStartTime(), trace);
                        builder.add(new ThreadEntryModel(getId(threadQuark), id, String.valueOf(threadName), ss.getStartTime(), ss.getCurrentEndTime(), true, tid));
                    }
                }
            }
        }
    }

    private static @Nullable String getThreadName(int tid, long time, ITmfTrace trace) {
        if (tid != -1) {
            return ModelManager.getModelFor(trace.getHostId()).getExecName(tid, time);
        }
        return null;
    }

    private static int getTid(ITmfStateSystem ss, int threadQuark) {
        String attributeName = ss.getAttributeName(threadQuark);
        if (isInteger(attributeName)) {
            return Integer.parseInt(attributeName);
        }
        return -1;
    }

    private static boolean isInteger(String attributeName) {
        return IS_INTEGER.matcher(attributeName).matches();
    }

    public @Nullable Long getBytesRead(long start, long end, long attributeId) {
        ITmfStateSystem ss = getAnalysisModule().getStateSystem();
        if (ss == null) {
            return null;
        }

        Map<Long, Integer> selectedEntries = getSelectedEntries(new SelectionTimeQueryFilter(Arrays.asList(start, end), Collections.singleton(attributeId)));
        Integer startingNodeQuark = selectedEntries.get(attributeId);
        if (startingNodeQuark == null || startingNodeQuark >= OFFSET) {
            return null;
        }
        int readQuark = ss.optQuarkRelative(startingNodeQuark, FileDescriptorStateProvider.READ);
        return getdelta(start, end, ss, readQuark);
    }

    public @Nullable Long getBytesWrite(long start, long end, long attributeId) {
        ITmfStateSystem ss = getAnalysisModule().getStateSystem();
        if (ss == null) {
            return null;
        }

        Map<Long, Integer> selectedEntries = getSelectedEntries(new SelectionTimeQueryFilter(Arrays.asList(start, end), Collections.singleton(attributeId)));
        Integer startingNodeQuark = selectedEntries.get(attributeId);
        if (startingNodeQuark == null || startingNodeQuark >= OFFSET) {
            return null;
        }
        int readQuark = ss.optQuarkRelative(startingNodeQuark, FileDescriptorStateProvider.WRITE);
        return getdelta(start, end, ss, readQuark);
    }

    private static @Nullable Long getdelta(long start, long end, ITmfStateSystem ss, int readQuark) {
        if (readQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return null;
        }
        try {
            ITmfStateInterval startInterval = ss.querySingleState(start, readQuark);
            ITmfStateInterval endInterval = ss.querySingleState(end, readQuark);
            return endInterval.getValueLong() - startInterval.getValueLong();
        } catch (StateSystemDisposedException e) {
            return null;
        }
    }

    @Override
    public @NonNull Multimap<@NonNull String, @NonNull Object> getFilterData(long entryId, long time, @Nullable IProgressMonitor monitor) {
        Multimap<@NonNull String, @NonNull Object> data = HashMultimap.create();
        data.putAll(super.getFilterData(entryId, time, monitor));

        SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(Collections.singletonList(time), Collections.singleton(Objects.requireNonNull(entryId)));
        TmfModelResponse<Map<String, String>> response = fetchTooltip(filter, monitor);
        Map<@NonNull String, @NonNull String> model = response.getModel();
        if (model != null) {
            for (Entry<String, String> entry : model.entrySet()) {
                data.put(entry.getKey(), entry.getValue());
            }
        }
        return data;
    }

}
