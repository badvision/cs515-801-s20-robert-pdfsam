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

import java.util.Set;
import org.junit.Test;
import org.sejda.conversion.exception.ConversionException;
import org.sejda.model.pdf.page.PageRange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Vacondio
 *
 */
public class ConversionUtilsTest {

    @Test(expected = ConversionException.class)
    public void invalid() {
        ConversionUtils.toPageRangeSet("Chuck Norris");
    }

    @Test(expected = ConversionException.class)
    public void invalidRange() {
        ConversionUtils.toPageRangeSet("1-2-3");
    }

    @Test(expected = ConversionException.class)
    public void endLower() {
        ConversionUtils.toPageRangeSet("10-5");
    }

    @Test
    public void singlePage() {
        Set<PageRange> pageSet = ConversionUtils.toPageRangeSet("5");
        assertEquals(1, pageSet.size());
        assertContainsRange(pageSet, 5, 5);
    }

    @Test
    public void rangePage() {
        Set<PageRange> pageSet = ConversionUtils.toPageRangeSet("5-10");
        assertEquals(1, pageSet.size());
        assertContainsRange(pageSet, 5, 10);
    }

    @Test
    public void endPage() {
        Set<PageRange> pageSet = ConversionUtils.toPageRangeSet("-10");
        assertEquals(1, pageSet.size());
        assertContainsRange(pageSet, 1, 10);
    }

    @Test
    public void startPage() {
        Set<PageRange> pageSet = ConversionUtils.toPageRangeSet("10-");
        assertEquals(1, pageSet.size());
        assertContainsUnboundedRange(pageSet, 10);
    }

    @Test
    public void multiple() {
        Set<PageRange> pageSet = ConversionUtils.toPageRangeSet("2-4,10-");
        assertEquals(2, pageSet.size());
        assertContainsRange(pageSet, 2, 4);
        assertContainsUnboundedRange(pageSet, 10);
    }

    public static void assertContainsRange(Set<PageRange> pages, int start, int end) {
        assertTrue(String.format("Should include range %s-%s", start, end),
                pages.stream().anyMatch(range -> range.getStart() == start && range.getEnd() == end));
    }

    public static void assertContainsUnboundedRange(Set<PageRange> pages, int start) {
        assertTrue(String.format("Should include unbounded range starting at %s", start),
                pages.stream().anyMatch(range -> range.getStart() == start && range.isUnbounded()));
    }

    @Test
    public void normalizeRanges() {
        Set<PageRange> pageSetAdjacent = ConversionUtils.toPageRangeSet("2-4,5-6");
        assertEquals("Adjacent sets should merge to one", 1, pageSetAdjacent.size());
        assertContainsRange(pageSetAdjacent, 2, 6);

        Set<PageRange> pageSetAdjacentUnbounded = ConversionUtils.toPageRangeSet("2-4,5-");
        assertEquals("Adjacent unbounded sets should merge to one", 1, pageSetAdjacentUnbounded.size());
        assertContainsUnboundedRange(pageSetAdjacentUnbounded, 2);

        Set<PageRange> pageSetMerge = ConversionUtils.toPageRangeSet("2-10,5,7");
        assertEquals("Inclusive sets should merge to one", 1, pageSetMerge.size());
        assertContainsRange(pageSetMerge, 2, 10);

        Set<PageRange> pageSet2Merge = ConversionUtils.toPageRangeSet("1-5,4-10,7,15");
        assertEquals("Inclusive sets should merge to two", 2, pageSet2Merge.size());
        assertContainsRange(pageSet2Merge, 1, 10);
        assertContainsRange(pageSet2Merge, 15, 15);

        Set<PageRange> pageSet3Merge = ConversionUtils.toPageRangeSet("5,9-14,13-20,10,25");
        assertEquals("Inclusive sets should merge to three", 3, pageSet3Merge.size());
        assertContainsRange(pageSet3Merge, 5, 5);
        assertContainsRange(pageSet3Merge, 9, 20);
        assertContainsRange(pageSet3Merge, 25, 25);

        Set<PageRange> pageSetMergeTriple = ConversionUtils.toPageRangeSet("1-4,3-6,7-10");
        assertEquals("Triple set case should merge to 1", 1, pageSetMergeTriple.size());
        assertContainsRange(pageSetMergeTriple, 1, 10);

        Set<PageRange> pageSetMergeAll = ConversionUtils.toPageRangeSet("1-,3-6,7-10,100");
        assertEquals("All case should merge to 1", 1, pageSetMergeAll.size());
        assertContainsUnboundedRange(pageSetMergeAll, 1);

        Set<PageRange> pageSetBackwarsd = ConversionUtils.toPageRangeSet("10,9,8,7,6,5,4,3,2,1");
        assertEquals("Backward case should merge to 1", 1, pageSetBackwarsd.size());
        assertContainsRange(pageSetBackwarsd, 1, 10);
    }
}
