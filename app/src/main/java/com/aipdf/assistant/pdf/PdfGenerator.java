package com.aipdf.assistant.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.aipdf.assistant.ai.models.DocumentSpec;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Renders a {@link DocumentSpec} into a real PDF file using PdfBox-Android.
 * Supports cover pages, table of contents, headings, paragraphs, lists,
 * tables, page breaks, page numbers and a footer.
 */
public class PdfGenerator {

    private static final float MARGIN = 56f;

    private final Context context;

    public PdfGenerator(Context context) {
        this.context = context.getApplicationContext();
    }

    public static class Result {
        public final File file;
        public final int pageCount;

        public Result(File file, int pageCount) {
            this.file = file;
            this.pageCount = pageCount;
        }
    }

    private static final class Canvas implements TextLayoutEngine.Canvas {
        private final PDDocument doc;
        private final PDRectangle size;

        Canvas(PDDocument doc, PDRectangle size) {
            this.doc = doc;
            this.size = size;
        }

        @Override
        public PDPage newPage() {
            PDPage page = new PDPage(size);
            doc.addPage(page);
            return page;
        }

        @Override
        public float pageWidth() {
            return size.getWidth();
        }

        @Override
        public float pageHeight() {
            return size.getHeight();
        }

        @Override
        public float leftMargin() {
            return MARGIN;
        }

        @Override
        public float rightMargin() {
            return MARGIN;
        }

        @Override
        public float topMargin() {
            return MARGIN;
        }

        @Override
        public float bottomMargin() {
            return MARGIN - 20f;
        }
    }

    public Result renderToFile(DocumentSpec spec, File outFile) throws IOException {
        DocumentSpec effective = spec == null ? new DocumentSpec() : spec;
        if (effective.pageSize == null) effective.pageSize = "A4";
        if (effective.orientation == null) effective.orientation = "portrait";
        if (effective.sections == null) effective.sections = new java.util.ArrayList<>();

        PDRectangle size = PageSizes.resolve(effective.pageSize, effective.orientation);
        try (PDDocument doc = new PDDocument()) {
            PDFont regular = PDType1Font.HELVETICA;
            PDFont bold = PDType1Font.HELVETICA_BOLD;

            Canvas canvas = new Canvas(doc, size);
            TextLayoutEngine layout = new TextLayoutEngine(doc, canvas);

            if (effective.coverPage) {
                renderCover(layout, size, regular, bold, effective);
            }
            if (effective.tableOfContents) {
                renderToc(layout, regular, bold, effective);
            }
            renderBody(layout, regular, bold, effective);

            addFooters(doc, regular);

            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists()) //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                doc.save(fos);
            }
            return new Result(outFile, doc.getNumberOfPages());
        }
    }

    private void renderCover(TextLayoutEngine layout, PDRectangle size,
                             PDFont regular, PDFont bold, DocumentSpec spec) throws IOException {
        TextLayoutEngine.Cursor c = layout.begin();
        c.y = size.getHeight() * 0.60f;
        c = layout.writeText(c, spec.title == null ? "" : spec.title, bold, 30f, 42f, 12f);
        if (spec.subtitle != null && !spec.subtitle.isEmpty()) {
            c = layout.writeText(c, spec.subtitle, regular, 15f, 22f, 12f);
        }
        if (spec.author != null && !spec.author.isEmpty()) {
            c = layout.writeText(c, spec.author, regular, 12f, 18f, 0f);
        }
        layout.endStream(c);
    }

    private void renderToc(TextLayoutEngine layout, PDFont regular, PDFont bold, DocumentSpec spec)
            throws IOException {
        TextLayoutEngine.Cursor c = layout.pageBreak(layout.begin());
        c = layout.writeText(c, "Table of Contents", bold, 20f, 28f, 14f);
        int index = 1;
        for (DocumentSpec.Section s : spec.sections) {
            String line = index + ".  " + (s.heading == null ? "" : s.heading);
            c = layout.writeText(c, line, regular, 12f, 18f, 2f);
            index++;
        }
        layout.endStream(c);
    }

    private void renderBody(TextLayoutEngine layout, PDFont regular, PDFont bold, DocumentSpec spec)
            throws IOException {
        TextLayoutEngine.Cursor c = layout.pageBreak(layout.begin());
        for (DocumentSpec.Section section : spec.sections) {
            c = layout.writeText(c, section.heading == null ? "" : section.heading, bold, 17f, 26f, 10f);
            if (section.elements == null) continue;
            for (DocumentSpec.Element el : section.elements) {
                if (el == null) continue;
                switch (el.type) {
                    case "heading":
                        c = layout.writeText(c, el.text, bold, el.level <= 1 ? 15f : 13f, 22f, 6f);
                        break;
                    case "list":
                        c = renderList(c, el, layout, regular);
                        break;
                    case "table":
                        c = renderTable(c, el, layout, regular, bold);
                        break;
                    case "pageBreak":
                        c = layout.pageBreak(c);
                        break;
                    case "paragraph":
                    default:
                        c = layout.writeText(c, el.text, regular, 11.5f, 17f, 10f);
                        break;
                }
            }
        }
        layout.endStream(c);
    }

    private TextLayoutEngine.Cursor renderList(TextLayoutEngine.Cursor c, DocumentSpec.Element el,
                                               TextLayoutEngine layout, PDFont font) throws IOException {
        int i = 1;
        for (String item : el.items) {
            String prefix = el.ordered ? (i + ". ") : "- ";
            c = layout.writeText(c, prefix + (item == null ? "" : item), font, 11.5f, 17f, 1f);
            i++;
        }
        c.y -= 8;
        return c;
    }

    private TextLayoutEngine.Cursor renderTable(TextLayoutEngine.Cursor c, DocumentSpec.Element el,
                                                TextLayoutEngine layout, PDFont regular, PDFont bold)
            throws IOException {
        int cols = el.headers != null && !el.headers.isEmpty() ? el.headers.size() : 0;
        if (cols == 0 && el.rows != null && !el.rows.isEmpty()) {
            cols = el.rows.get(0).size();
        }
        if (cols == 0) return c;
        float colWidth = layout.usableWidth() / cols;
        float rowHeight = 20f;
        float fontSize = 10f;
        float bottomLimit = MARGIN;

        if (el.headers != null && !el.headers.isEmpty()) {
            if (c.y - rowHeight < bottomLimit) c = layout.pageBreak(c);
            drawRow(c, el.headers, colWidth, rowHeight, bold, fontSize, true);
            c.y -= rowHeight;
        }
        if (el.rows != null) {
            for (List<String> row : el.rows) {
                if (c.y - rowHeight < bottomLimit) c = layout.pageBreak(c);
                drawRow(c, row, colWidth, rowHeight, regular, fontSize, false);
                c.y -= rowHeight;
            }
        }
        c.y -= 10;
        return c;
    }

    private void drawRow(TextLayoutEngine.Cursor c, List<String> cells, float colWidth,
                         float rowHeight, PDFont font, float fontSize, boolean header) throws IOException {
        c.stream.setLineWidth(0.5f);
        if (header) {
            c.stream.setStrokingColor(90, 90, 90);
            c.stream.setNonStrokingColor(235, 235, 245);
        } else {
            c.stream.setStrokingColor(200, 200, 200);
        }
        float x = MARGIN;
        c.stream.addRect(x, c.y - rowHeight, colWidth * cells.size(), rowHeight);
        c.stream.fillAndStroke();
        c.stream.setNonStrokingColor(0, 0, 0);
        for (int i = 0; i < cells.size(); i++) {
            String cell = cells.get(i) == null ? "" : cells.get(i);
            String display = trimToWidth(cell, font, fontSize, colWidth - 8);
            c.stream.beginText();
            c.stream.setFont(font, fontSize);
            c.stream.newLineAtOffset(x + 4, c.y - rowHeight + 6);
            c.stream.showText(display);
            c.stream.endText();
            x += colWidth;
        }
    }

    private String trimToWidth(String text, PDFont font, float fontSize, float maxWidth) {
        String safe = TextLayoutEngine.sanitizeForFont(text, font).replace("\n", " ");
        if (TextLayoutEngine.stringWidth(safe, font, fontSize) <= maxWidth) return safe;
        StringBuilder sb = new StringBuilder();
        for (char ch : safe.toCharArray()) {
            if (TextLayoutEngine.stringWidth(sb.toString() + ch + "\u2026", font, fontSize) > maxWidth) break;
            sb.append(ch);
        }
        return sb + "\u2026";
    }

    private void addFooters(PDDocument doc, PDFont font) throws IOException {
        int pageCount = doc.getNumberOfPages();
        for (int i = 0; i < pageCount; i++) {
            PDPage page = doc.getPage(i);
            PDRectangle box = page.getMediaBox();
            String label = "Page " + (i + 1) + " of " + pageCount;
            float fontSize = 9f;
            float textWidth = TextLayoutEngine.stringWidth(label, font, fontSize);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page, AppendMode.APPEND, true, true)) {
                cs.beginText();
                cs.setFont(font, fontSize);
                cs.newLineAtOffset((box.getWidth() - textWidth) / 2f, 28f);
                cs.showText(label);
                cs.endText();
            }
        }
    }

    /** Renders images to a single multi-page PDF, one image per page. */
    public Result imagesToPdf(List<Uri> images, String pageSize, String orientation, File outFile) throws IOException {
        PDRectangle size = PageSizes.resolve(pageSize, orientation);
        try (PDDocument doc = new PDDocument()) {
            for (Uri uri : images) {
                Bitmap bitmap = decodeSampled(uri, 1600);
                if (bitmap == null) continue;
                PDPage page = new PDPage(size);
                doc.addPage(page);
                PDImageXObject image = LosslessFactory.createFromImage(doc, bitmap);
                float pw = size.getWidth() - 2 * 36;
                float ph = size.getHeight() - 2 * 36;
                float scale = Math.min(pw / image.getWidth(), ph / image.getHeight());
                float w = image.getWidth() * scale;
                float h = image.getHeight() * scale;
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(image, (size.getWidth() - w) / 2f, (size.getHeight() - h) / 2f, w, h);
                }
                bitmap.recycle();
            }
            if (doc.getNumberOfPages() == 0) {
                throw new IOException("No readable images were provided");
            }
            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists()) //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                doc.save(fos);
            }
            return new Result(outFile, doc.getNumberOfPages());
        }
    }

    /** Renders plain (markdown-ish) text into a PDF. */
    public Result textToPdf(String title, String text, String pageSize, String orientation, File outFile)
            throws IOException {
        DocumentSpec spec = new DocumentSpec();
        spec.title = title == null || title.trim().isEmpty() ? "Document" : title.trim();
        spec.coverPage = false;
        spec.tableOfContents = false;
        spec.pageSize = pageSize;
        spec.orientation = orientation;
        DocumentSpec.Section section = new DocumentSpec.Section();
        section.heading = spec.title;
        section.elements = new java.util.ArrayList<>();
        for (String para : (text == null ? "" : text).split("\n\n+")) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("#")) {
                int level = 1;
                while (level < trimmed.length() && trimmed.charAt(level) == '#') level++;
                section.elements.add(DocumentSpec.Element.heading(trimmed.replaceFirst("^#+\\s*", ""), level));
            } else {
                section.elements.add(DocumentSpec.Element.paragraph(trimmed));
            }
        }
        if (section.elements.isEmpty()) {
            section.elements.add(DocumentSpec.Element.paragraph(text == null ? "" : text));
        }
        spec.sections.add(section);
        return renderToFile(spec, outFile);
    }

    private Bitmap decodeSampled(Uri uri, int maxDim) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            int sample = 1;
            int larger = Math.max(bounds.outWidth, bounds.outHeight);
            while (larger / sample > maxDim) sample *= 2;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(in, null, opts);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
