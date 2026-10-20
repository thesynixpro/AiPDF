package com.aipdf.assistant.pdf;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lays text out onto PDF pages, handling pagination, simple word wrapping and
 * page breaks. Kept independent of the document-spec model so it is unit
 * testable with plain strings.
 */
public class TextLayoutEngine {

    public interface Canvas {
        PDPage newPage();

        float pageWidth();

        float pageHeight();

        float leftMargin();

        float rightMargin();

        float topMargin();

        float bottomMargin();
    }

    private final Canvas canvas;
    private final PDDocument document;

    public TextLayoutEngine(PDDocument document, Canvas canvas) {
        this.document = document;
        this.canvas = canvas;
    }

    public class Cursor {
        public PDPage page;
        public PDPageContentStream stream;
        public float y;

        public float canvasLeft() {
            return canvas.leftMargin();
        }
    }

    public Cursor begin() throws IOException {
        Cursor c = new Cursor();
        c.page = canvas.newPage();
        c.stream = new PDPageContentStream(document, c.page);
        c.y = canvas.pageHeight() - canvas.topMargin();
        return c;
    }

    public Canvas canvas() {
        return canvas;
    }

    public float usableWidth() {
        return canvas.pageWidth() - canvas.leftMargin() - canvas.rightMargin();
    }

    /** Writes wrapped text, creating new pages as required. Returns the cursor. */
    public Cursor writeText(Cursor cursor, String text, PDFont font, float fontSize,
                            float leading, float spaceAfter) throws IOException {
        if (text == null) text = "";
        float width = usableWidth();
        List<String> lines = wrap(text, font, fontSize, width);
        for (String line : lines) {
            if (cursor.y - leading < canvas.bottomMargin()) {
                endStream(cursor);
                cursor.page = canvas.newPage();
                cursor.stream = new PDPageContentStream(document, cursor.page);
                cursor.y = canvas.pageHeight() - canvas.topMargin();
            }
            cursor.stream.beginText();
            cursor.stream.setFont(font, fontSize);
            cursor.stream.newLineAtOffset(canvas.leftMargin(), cursor.y - fontSize);
            cursor.stream.showText(sanitize(sanitizeForFont(line, font)));
            cursor.stream.endText();
            cursor.y -= leading;
        }
        cursor.y -= spaceAfter;
        return cursor;
    }

    public void endStream(Cursor cursor) throws IOException {
        if (cursor.stream != null) {
            cursor.stream.close();
            cursor.stream = null;
        }
    }

    /** Creates a new page for explicit page breaks. */
    public Cursor pageBreak(Cursor cursor) throws IOException {
        endStream(cursor);
        Cursor c = new Cursor();
        c.page = canvas.newPage();
        c.stream = new PDPageContentStream(document, c.page);
        c.y = canvas.pageHeight() - canvas.topMargin();
        return c;
    }

    public List<String> wrap(String text, PDFont font, float fontSize, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null) return lines;
        for (String rawLine : text.split("\n", -1)) {
            if (rawLine.isEmpty()) {
                lines.add("");
                continue;
            }
            String[] words = rawLine.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                float w = stringWidth(sanitizeForFont(candidate, font), font, fontSize);
                if (w > maxWidth && current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            lines.add(current.toString());
        }
        return lines;
    }

    public static float stringWidth(String text, PDFont font, float fontSize) {
        if (text == null || text.isEmpty()) return 0;
        try {
            return font.getStringWidth(text) / 1000f * fontSize;
        } catch (Exception e) {
            // Estimate when font metrics are unavailable or the text is not
            // encodable by the font.
            return text.length() * fontSize * 0.5f;
        }
    }

    /** Standard 14 fonts are WinAnsi; replace unsupported glyphs to avoid exceptions. */
    public static String sanitizeForFont(String text, PDFont font) {
        if (text == null) return "";
        boolean standard = isStandard14Font(font);
        if (!standard) return text;
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\t') {
                sb.append("    ");
            } else if (ch >= 0x20 && ch < 0x7f) {
                // Printable ASCII is always safe in WinAnsi.
                sb.append(ch);
            } else if (ch >= 0xa0 && ch <= 0xff) {
                // Latin-1 printable range maps directly to WinAnsi.
                sb.append(ch);
            } else if (ch == '\n' || ch == '\r') {
                sb.append(ch);
            } else {
                // Control characters and non-Latin-1 glyphs are not encodable by
                // the standard-14 fonts; transliterate instead of crashing.
                sb.append(transliterate(ch));
            }
        }
        return sb.toString();
    }

    /**
     * Standard-14 (non-embedded Type1) fonts only cover WinAnsi, so glyphs
     * outside Latin-1 must be transliterated. Detected via the font subtype to
     * avoid pulling in Android-dependent font resources.
     */
    static boolean isStandard14Font(PDFont font) {
        if (font == null) return false;
        try {
            String subtype = font.getSubType();
            return "Type1".equals(subtype) || "Type1C".equals(subtype);
        } catch (Exception e) {
            return false;
        }
    }

    static String transliterate(char ch) {
        switch (ch) {
            case '—':
            case '–':
                return "-";
            case '“':
            case '”':
                return "\"";
            case '‘':
            case '’':
                return "'";
            case '…':
                return "...";
            case '•':
                return "-";
            case '™':
                return "(TM)";
            case '®':
                return "(R)";
            case '©':
                return "(C)";
            case '€':
                return "EUR";
            case '£':
                return "GBP";
            case '\u00a0':
                return " ";
            default:
                return "?";
        }
    }

    private static String sanitize(String s) {
        return s == null ? "" : s.replace("\r", "");
    }
}
