package com.aipdf.assistant.pdf;

import java.util.ArrayList;
import java.util.List;

/** Parses page-range strings like "1-3, 5, 8-10" into 1-based page numbers. */
public final class PageRangeParser {

    private PageRangeParser() {
    }

    public static List<Integer> parse(String input, int totalPages) throws IllegalArgumentException {
        List<Integer> pages = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter at least one page or range.");
        }
        String[] parts = input.split(",");
        for (String part : parts) {
            String token = part.trim();
            if (token.isEmpty()) continue;
            if (token.contains("-")) {
                String[] bounds = token.split("-");
                if (bounds.length != 2) throw new IllegalArgumentException("Invalid range: " + token);
                int start = parsePositive(bounds[0].trim());
                int end = parsePositive(bounds[1].trim());
                if (end < start) {
                    int tmp = start;
                    start = end;
                    end = tmp;
                }
                for (int i = start; i <= end; i++) {
                    validate(i, totalPages);
                    pages.add(i);
                }
            } else {
                int p = parsePositive(token);
                validate(p, totalPages);
                pages.add(p);
            }
        }
        if (pages.isEmpty()) throw new IllegalArgumentException("No valid pages selected.");
        return pages;
    }

    private static int parsePositive(String s) {
        try {
            int v = Integer.parseInt(s);
            if (v < 1) throw new IllegalArgumentException("Page numbers start at 1.");
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a number: " + s);
        }
    }

    private static void validate(int page, int totalPages) {
        if (totalPages > 0 && page > totalPages) {
            throw new IllegalArgumentException("Page " + page + " is outside 1-" + totalPages);
        }
    }
}
