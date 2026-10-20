package com.aipdf.assistant.pdf;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Local, on-device PDF text/metadata extraction. No network involved. */
public final class PdfAnalyzer {

    private PdfAnalyzer() {
    }

    public static class PageText {
        public final int pageNumber;
        public final String text;

        public PageText(int pageNumber, String text) {
            this.pageNumber = pageNumber;
            this.text = text == null ? "" : text;
        }
    }

    public static int countPages(File file) {
        try (PDDocument doc = PDDocument.load(file)) {
            return doc.getNumberOfPages();
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isEncrypted(File file) {
        try (PDDocument doc = PDDocument.load(file)) {
            return false;
        } catch (com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String extractAllText(File file) throws IOException {
        try (PDDocument doc = load(file, null)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    public static String extractText(File file, String password) throws IOException {
        try (PDDocument doc = load(file, password)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    public static List<PageText> extractByPage(File file, String password) throws IOException {
        List<PageText> out = new ArrayList<>();
        try (PDDocument doc = load(file, password)) {
            int pages = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int i = 1; i <= pages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                out.add(new PageText(i, stripper.getText(doc)));
            }
        }
        return out;
    }

    /**
     * Returns a text excerpt for the given 1-based page range, capped at
     * maxChars so large documents respect the AI context window.
     */
    public static String excerpt(File file, int startPage, int endPage, int maxChars) throws IOException {
        try (PDDocument doc = load(file, null)) {
            int pages = doc.getNumberOfPages();
            int from = Math.max(1, startPage);
            int to = Math.min(pages, endPage);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(from);
            stripper.setEndPage(to);
            String text = stripper.getText(doc);
            if (text.length() > maxChars) {
                text = text.substring(0, maxChars);
            }
            return text;
        }
    }

    public static boolean isLikelyScanned(String extractedText) {
        if (extractedText == null) return true;
        String stripped = extractedText.replaceAll("\\s", "");
        return stripped.length() < 40;
    }

    public static PDDocument load(File file, String password) throws IOException {
        if (password == null) {
            return PDDocument.load(file);
        }
        return PDDocument.load(file, password);
    }
}
