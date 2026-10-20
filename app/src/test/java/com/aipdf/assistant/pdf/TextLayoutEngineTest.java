package com.aipdf.assistant.pdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.tom_roush.pdfbox.pdmodel.font.PDFont;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

public class TextLayoutEngineTest {

    /**
     * A mock font whose width is a fixed fraction of the character count, so
     * layout can be verified without loading real font metrics.
     */
    private static PDFont fixedWidthFont(final float perChar) {
        PDFont font = Mockito.mock(PDFont.class);
        try {
            when(font.getStringWidth(anyString())).thenAnswer(invocation -> {
                String text = invocation.getArgument(0);
                return text.length() * perChar * 1000f;
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return font;
    }

    private static PDFont standard14Font() {
        PDFont font = fixedWidthFont(10f);
        when(font.getSubType()).thenReturn("Type1");
        return font;
    }

    @Test
    public void wrapBreaksLongLineAtSpaces() {
        TextLayoutEngine engine = new TextLayoutEngine(null, null);
        PDFont font = fixedWidthFont(10f);
        String text = "the quick brown fox jumps over the lazy dog";
        List<String> lines = engine.wrap(text, font, 10f, 150f);
        assertTrue("expected multiple lines", lines.size() > 1);
        for (String line : lines) {
            assertFalse(line.isEmpty());
        }
        assertEquals(text, String.join(" ", lines));
    }

    @Test
    public void wrapKeepsShortLineIntact() {
        TextLayoutEngine engine = new TextLayoutEngine(null, null);
        List<String> lines = engine.wrap("short", fixedWidthFont(1f), 10f, 10000f);
        assertEquals(1, lines.size());
        assertEquals("short", lines.get(0));
    }

    @Test
    public void wrapPreservesExplicitNewlines() {
        TextLayoutEngine engine = new TextLayoutEngine(null, null);
        List<String> lines = engine.wrap("a\n\nb", fixedWidthFont(1f), 10f, 10000f);
        assertEquals(3, lines.size());
        assertEquals("a", lines.get(0));
        assertEquals("", lines.get(1));
        assertEquals("b", lines.get(2));
    }

    @Test
    public void wrapHandlesNull() {
        TextLayoutEngine engine = new TextLayoutEngine(null, null);
        assertTrue(engine.wrap(null, fixedWidthFont(1f), 12f, 100f).isEmpty());
    }

    @Test
    public void transliteratesUnsupportedGlyphs() {
        assertEquals("-", TextLayoutEngine.transliterate('\u2014'));
        assertEquals("-", TextLayoutEngine.transliterate('\u2013'));
        assertEquals("\"", TextLayoutEngine.transliterate('\u201c'));
        assertEquals("\"", TextLayoutEngine.transliterate('\u201d'));
        assertEquals("'", TextLayoutEngine.transliterate('\u2019'));
        assertEquals("...", TextLayoutEngine.transliterate('\u2026'));
        assertEquals("-", TextLayoutEngine.transliterate('\u2022'));
        assertEquals("(TM)", TextLayoutEngine.transliterate('\u2122'));
        assertEquals("(R)", TextLayoutEngine.transliterate('\u00ae'));
        assertEquals("(C)", TextLayoutEngine.transliterate('\u00a9'));
        assertEquals("EUR", TextLayoutEngine.transliterate('\u20ac'));
        assertEquals("GBP", TextLayoutEngine.transliterate('\u00a3'));
        assertEquals(" ", TextLayoutEngine.transliterate('\u00a0'));
        assertEquals("?", TextLayoutEngine.transliterate('\u4e2d'));
    }

    @Test
    public void sanitizeLeavesNonStandardFontTextUnchanged() {
        String input = "caf\u00e9 \u4e2d\u6587";
        String out = TextLayoutEngine.sanitizeForFont(input, fixedWidthFont(1f));
        assertEquals(input, out);
    }

    @Test
    public void sanitizeTransliteratesForStandard14Font() {
        String out = TextLayoutEngine.sanitizeForFont("Em\u2014dash \u201cquote\u201d \u2026 \u20ac5",
                standard14Font());
        assertFalse(out.contains("\u2014"));
        assertFalse(out.contains("\u201c"));
        assertTrue(out.contains("-"));
        assertTrue(out.contains("..."));
        assertTrue(out.contains("EUR"));
    }

    @Test
    public void sanitizeExpandsTabsForStandard14Font() {
        String out = TextLayoutEngine.sanitizeForFont("a\tb", standard14Font());
        assertEquals("a    b", out);
    }

    @Test
    public void sanitizeHandlesNull() {
        assertEquals("", TextLayoutEngine.sanitizeForFont(null, fixedWidthFont(1f)));
    }

    @Test
    public void stringWidthScalesWithFontSize() {
        PDFont font = fixedWidthFont(10f);
        float w12 = TextLayoutEngine.stringWidth("hello", font, 12f);
        float w24 = TextLayoutEngine.stringWidth("hello", font, 24f);
        assertEquals(w12 * 2, w24, 0.001f);
        assertTrue(w12 > 0f);
        assertEquals(0f, TextLayoutEngine.stringWidth("", font, 12f), 0.0001f);
    }

    @Test
    public void stringWidthFallsBackWhenFontThrows() throws IOException {
        PDFont throwing = Mockito.mock(PDFont.class);
        when(throwing.getStringWidth(anyString())).thenThrow(new IOException("no metrics"));
        float w = TextLayoutEngine.stringWidth("abcd", throwing, 10f);
        assertTrue(w > 0f);
    }
}
