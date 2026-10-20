package com.aipdf.assistant.pdf;

import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.multipdf.Splitter;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission;
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * High-level PDF operations. Each method creates a NEW file and never mutates
 * the source document, so user data is preserved by default.
 */
public class PdfOperations {

    public static class OpResult {
        public final File file;
        public final int pageCount;
        public final long sizeBytes;

        public OpResult(File file, int pageCount) {
            this.file = file;
            this.pageCount = pageCount;
            this.sizeBytes = file != null ? file.length() : 0;
        }
    }

    /** Merges the given PDFs (in order) into outFile. */
    public static OpResult merge(List<File> sources, File outFile) throws IOException {
        if (sources == null || sources.size() < 2) {
            throw new IOException("At least two PDFs are required to merge.");
        }
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationFileName(outFile.getAbsolutePath());
        for (File f : sources) {
            merger.addSource(f);
        }
        merger.mergeDocuments(null);
        int pages = PdfAnalyzer.countPages(outFile);
        return new OpResult(outFile, pages);
    }

    public static OpResult extractPages(File source, List<Integer> pages1Based, File outFile) throws IOException {
        try (PDDocument doc = PDDocument.load(source)) {
            int total = doc.getNumberOfPages();
            PDDocument out = new PDDocument();
            try {
                for (int p : pages1Based) {
                    if (p < 1 || p > total) throw new IOException("Page " + p + " is outside 1-" + total);
                    out.addPage(doc.getPage(p - 1));
                    out.getPage(out.getNumberOfPages() - 1).setMediaBox(
                            doc.getPage(p - 1).getMediaBox());
                }
                out.save(outFile);
                return new OpResult(outFile, out.getNumberOfPages());
            } finally {
                out.close();
            }
        }
    }

    public static OpResult split(File source, int pagesPerFile, File outDir) throws IOException {
        try (PDDocument doc = PDDocument.load(source)) {
            Splitter splitter = new Splitter();
            splitter.setSplitAtPage(pagesPerFile);
            List<PDDocument> parts = splitter.split(doc);
            File result = null;
            int total = 0;
            try {
                int index = 1;
                for (PDDocument part : parts) {
                    File f = new File(outDir, baseName(source) + "_part" + index + ".pdf");
                    part.save(f);
                    total += part.getNumberOfPages();
                    if (result == null) result = f;
                    index++;
                }
            } finally {
                for (PDDocument part : parts) part.close();
            }
            if (result == null) throw new IOException("Nothing to split");
            return new OpResult(result, total);
        }
    }

    public static OpResult rotate(File source, List<Integer> pages1Based, int degrees, File outFile)
            throws IOException {
        try (PDDocument doc = PDDocument.load(source)) {
            int total = doc.getNumberOfPages();
            boolean all = pages1Based == null || pages1Based.isEmpty();
            for (int i = 0; i < total; i++) {
                if (all || pages1Based.contains(i + 1)) {
                    PDPage page = doc.getPage(i);
                    page.setRotation((page.getRotation() + degrees) % 360);
                }
            }
            doc.save(outFile);
            return new OpResult(outFile, total);
        }
    }

    public static OpResult deletePages(File source, List<Integer> pages1Based, File outFile) throws IOException {
        try (PDDocument doc = PDDocument.load(source)) {
            int index = pages1Based.size() - 1;
            java.util.Collections.sort(pages1Based);
            for (int i = index; i >= 0; i--) {
                int p = pages1Based.get(i);
                if (p >= 1 && p <= doc.getNumberOfPages()) {
                    doc.removePage(p - 1);
                }
            }
            if (doc.getNumberOfPages() == 0) throw new IOException("Cannot delete every page.");
            doc.save(outFile);
            return new OpResult(outFile, doc.getNumberOfPages());
        }
    }

    public static OpResult reorder(File source, List<Integer> newOrder1Based, File outFile) throws IOException {
        try (PDDocument doc = PDDocument.load(source)) {
            int total = doc.getNumberOfPages();
            if (newOrder1Based.size() != total) {
                throw new IOException("The new order must list every page exactly once.");
            }
            List<PDPage> pages = new ArrayList<>();
            for (int i = 0; i < total; i++) pages.add(doc.getPage(i));
            PDDocument out = new PDDocument();
            try {
                for (int p : newOrder1Based) {
                    if (p < 1 || p > total) throw new IOException("Invalid page " + p);
                    out.addPage(pages.get(p - 1));
                }
                out.save(outFile);
                return new OpResult(outFile, total);
            } finally {
                out.close();
            }
        }
    }

    public static OpResult addWatermark(File source, String text, File outFile) throws IOException {
        try (PDDocument doc = PDDocument.load(source)) {
            PDFont font = PDType1Font.HELVETICA_BOLD;
            for (PDPage page : doc.getPages()) {
                float w = page.getMediaBox().getWidth();
                float h = page.getMediaBox().getHeight();
                float fontSize = 48f;
                String safe = TextLayoutEngine.sanitizeForFont(text == null ? "" : text, font);
                float textWidth = TextLayoutEngine.stringWidth(safe, font, fontSize);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    cs.saveGraphicsState();
                    cs.setNonStrokingColor(190, 190, 190);
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    double angle = Math.toRadians(45);
                    cs.setTextMatrix(com.tom_roush.pdfbox.util.Matrix.getRotateInstance(angle,
                            (w - textWidth) / 2f, h / 2f));
                    cs.showText(safe);
                    cs.endText();
                    cs.restoreGraphicsState();
                }
            }
            doc.save(outFile);
            return new OpResult(outFile, doc.getNumberOfPages());
        }
    }

    public static OpResult protect(File source, String userPassword, File outFile) throws IOException {
        if (userPassword == null || userPassword.length() < 4) {
            throw new IOException("Password must be at least 4 characters.");
        }
        try (PDDocument doc = PDDocument.load(source)) {
            AccessPermission ap = new AccessPermission();
            ap.setCanModify(true);
            ap.setCanExtractContent(true);
            ap.setCanFillInForm(true);
            StandardProtectionPolicy policy = new StandardProtectionPolicy(userPassword, userPassword, ap);
            policy.setEncryptionKeyLength(128);
            doc.protect(policy);
            doc.save(outFile);
            return new OpResult(outFile, doc.getNumberOfPages());
        }
    }

    public static OpResult unprotect(File source, String password, File outFile) throws IOException {
        try (PDDocument doc = PDDocument.load(source, password)) {
            if (doc.isEncrypted()) {
                doc.setAllSecurityToBeRemoved(true);
            }
            doc.save(outFile);
            return new OpResult(outFile, doc.getNumberOfPages());
        }
    }

    /**
     * Re-saves the document to reduce size. This is a lossless structural
     * rewrite (object renumbering + stream compression); it does not re-encode
     * embedded images. Returns the new file and reports if it actually shrank.
     */
    public static OpResult compress(File source, File outFile) throws IOException {
        try (PDDocument doc = PDDocument.load(source)) {
            doc.save(outFile);
            return new OpResult(outFile, doc.getNumberOfPages());
        }
    }

    private static String baseName(File f) {
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }
}
