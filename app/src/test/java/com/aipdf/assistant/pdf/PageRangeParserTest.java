package com.aipdf.assistant.pdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class PageRangeParserTest {

    @Test
    public void parsesSinglePage() {
        List<Integer> pages = PageRangeParser.parse("3", 10);
        assertEquals(1, pages.size());
        assertEquals(Integer.valueOf(3), pages.get(0));
    }

    @Test
    public void parsesRangeAndList() {
        List<Integer> pages = PageRangeParser.parse("1-3, 5, 8-10", 10);
        assertEquals(7, pages.size());
        assertEquals(Integer.valueOf(1), pages.get(0));
        assertEquals(Integer.valueOf(3), pages.get(2));
        assertEquals(Integer.valueOf(5), pages.get(3));
        assertEquals(Integer.valueOf(8), pages.get(4));
        assertEquals(Integer.valueOf(10), pages.get(6));
    }

    @Test
    public void normalizesReversedRange() {
        List<Integer> pages = PageRangeParser.parse("5-3", 10);
        assertEquals(3, pages.size());
        assertEquals(Integer.valueOf(3), pages.get(0));
        assertEquals(Integer.valueOf(5), pages.get(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmpty() {
        PageRangeParser.parse("", 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOutOfBounds() {
        PageRangeParser.parse("11", 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonNumeric() {
        PageRangeParser.parse("abc", 10);
    }

    @Test
    public void allowsAnyPageWhenTotalUnknown() {
        List<Integer> pages = PageRangeParser.parse("1,2,3", 0);
        assertEquals(3, pages.size());
    }
}
