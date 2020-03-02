/*
 * This file is part of the PDF Split And Merge source code
 * Created on 22 giu 2016
 * Copyright 2017 by Sober Lemur S.a.s. di Vacondio Andrea (info@pdfsam.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.support.params;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.pdfsam.i18n.DefaultI18nContext;
import org.sejda.common.collection.NullSafeSet;
import org.sejda.conversion.exception.ConversionException;
import org.sejda.model.pdf.page.PageRange;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sejda.conversion.AdapterUtils.splitAndTrim;

/**
 * @author Andrea Vacondio
 *
 */
public final class ConversionUtils {

    private ConversionUtils() {
        // hide
    }

    /**
     * Simplify a collection of page ranges to collapse adjacent ranges and deal
     * with intersections as well.
     *
     * This is not a descructive operation to the original set
     *
     * @param ranges
     * @return normalized ranges
     */
    public static Set<PageRange> normalizePageRangeSet(Set<PageRange> ranges) {
        if (ranges.isEmpty()) {
            return ranges;
        }

        AtomicInteger unboundStart = new AtomicInteger(-1);
        Set<Integer> allPages = new TreeSet<>();

        // First find any unbounded sets, pick the one with the lowest start number
        for (PageRange range : ranges) {
            if (range.isUnbounded()
                    && (unboundStart.get() < 0 || unboundStart.get() > range.getStart())) {
                unboundStart.set(range.getStart());
            }
        }

        ranges.stream()
                // Now for all bounded sets
                .filter((range) -> (!range.isUnbounded()))
                // track all page numbers
                .filter(r -> unboundStart.get() < 0 || r.getStart() < unboundStart.get())
                .forEach(
                        range -> IntStream.rangeClosed(range.getStart(), range.getEnd())
                                .forEach(allPages::add)
                );

        // Expand the unbounded range downward if possible
        for (int i = unboundStart.get() - 1; i > 0; i--) {
            if (allPages.contains(i)) {
                unboundStart.set(i);
                allPages.remove(i);
            } else {
                break;
            }
        }

        // Now we have an organized way to iterate all allowed pages
        // Rebuild the new set of page ranges
        Set<PageRange> normalizedRanges = new LinkedHashSet<>();
        int lastStart = -1;
        int lastEnd = -1;
        for (int pageNumber : allPages) {
            if (lastStart == -1) {
                lastStart = pageNumber;
            }
            if (lastEnd == -1 || lastEnd == pageNumber - 1) {
                lastEnd = pageNumber;
            } else {
                normalizedRanges.add(new PageRange(lastStart, lastEnd));
                lastStart = -1;
                lastEnd = -1;
            }
        }

        if (lastStart > 0 && lastEnd > 0) {
            normalizedRanges.add(new PageRange(lastStart, lastEnd));
        }

        if (unboundStart.get() > 0) {
            normalizedRanges.add(new PageRange(unboundStart.get()));
        }

        return normalizedRanges;
    }

    /**
     * @return the {@link PageRange} set for the given string, an empty set
     * otherwise.
     */
    public static Set<PageRange> toPageRangeSet(String selection) throws ConversionException {
        if (isNotBlank(selection)) {
            Set<PageRange> pageRangeSet = new NullSafeSet<>();
            String[] tokens = splitAndTrim(selection, ",");
            for (String current : tokens) {
                PageRange range = toPageRange(current);
                if (range.getEnd() < range.getStart()) {
                    throw new ConversionException(
                            DefaultI18nContext.getInstance().i18n("Invalid range: {0}.", range.toString()));
                }
                pageRangeSet.add(range);
            }
            return normalizePageRangeSet(pageRangeSet);
        }
        return Collections.emptySet();
    }

    private static PageRange toPageRange(String value) throws ConversionException {
        String[] limits = splitAndTrim(value, "-");
        if (limits.length > 2) {
            throw new ConversionException(DefaultI18nContext.getInstance().i18n(
                    "Ambiguous page range definition: {0}. Use following formats: [n] or [n1-n2] or [-n] or [n-]",
                    value));
        }
        if (limits.length == 1) {
            int limitNumber = parsePageNumber(limits[0]);
            if (value.endsWith("-")) {
                return new PageRange(limitNumber);
            }
            if (value.startsWith("-")) {
                return new PageRange(1, limitNumber);
            }
            return new PageRange(limitNumber, limitNumber);
        }
        return new PageRange(parsePageNumber(limits[0]), parsePageNumber(limits[1]));
    }

    private static int parsePageNumber(String value) throws ConversionException {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException nfe) {
            throw new ConversionException(DefaultI18nContext.getInstance().i18n("Invalid number: {0}.", value));
        }
    }
}
